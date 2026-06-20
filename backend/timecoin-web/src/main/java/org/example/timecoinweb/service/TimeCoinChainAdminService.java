package org.example.timecoinweb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pojo.ChainAccountMirror;
import org.example.pojo.User;
import org.example.timecoinweb.config.BlockchainProperties;
import org.example.timecoinweb.mapper.ChainAccountMirrorMapper;
import org.example.timecoinweb.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员时间币：聚合数据库用户与链上余额、事件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeCoinChainAdminService {

    private final TimeCoinChainService timeCoinChainService;
    private final UserMapper userMapper;
    private final ChainAccountMirrorMapper chainAccountMirrorMapper;
    private final BlockchainProperties blockchainProperties;

    public Map<String, Object> overview(int eventLimit) {
        Map<String, Object> out = new HashMap<>();
        List<User> allUsers = userMapper.selectUsers(null, null, null, null, null);
        if (allUsers == null) {
            allUsers = new ArrayList<>();
        }
        out.put("registeredUsers", allUsers.size());

        if (!timeCoinChainService.isChainReady()) {
            out.put("ready", false);
            out.put("reason", timeCoinChainService.getNotReadyReason());
            out.put("totalCirculating", "0");
            out.put("holdersCount", 0);
            out.put("mintEventCount", 0L);
            out.put("transferEventCount", 0L);
            out.put("events", new ArrayList<>());
            out.put("users", buildUserRowsOffline(allUsers));
            return out;
        }

        out.put("ready", true);
        List<Map<String, Object>> userRows = new ArrayList<>();
        BigInteger sum = BigInteger.ZERO;
        int holders = 0;
        int mismatchCount = 0;
        for (User u : allUsers) {
            Map<String, Object> row = baseUserRow(u);
            String chainUid = String.valueOf(u.getId());
            row.put("chainUserId", chainUid);
            long dbBalance = u.getCoinBalance() != null ? u.getCoinBalance() : 0L;
            row.put("dbBalance", String.valueOf(dbBalance));
            try {
                BigInteger bal = timeCoinChainService.balanceOf(chainUid);
                row.put("coinBalance", bal.toString());
                boolean matched = dbBalance == bal.longValue();
                row.put("balanceMatched", matched);
                if (!matched) {
                    mismatchCount++;
                    row.put("balanceDiff", String.valueOf(bal.longValue() - dbBalance));
                }
                sum = sum.add(bal);
                if (bal.signum() > 0) {
                    holders++;
                }
            } catch (Exception e) {
                log.warn("balanceOf 失败 userId={}", chainUid, e);
                row.put("coinBalance", null);
                row.put("balanceError", e.getMessage());
            }
            userRows.add(row);
        }
        out.put("users", userRows);
        out.put("totalCirculating", sum.toString());
        out.put("holdersCount", holders);
        out.put("balanceMismatchCount", mismatchCount);

        String platformId = blockchainProperties.getFeeRecipientUserId();
        if (platformId != null && !platformId.isEmpty()) {
            Map<String, Object> platformRow = new HashMap<>();
            platformRow.put("accountId", platformId);
            platformRow.put("username", "平台账户");
            try {
                BigInteger chainBal = timeCoinChainService.balanceOf(platformId);
                platformRow.put("coinBalance", chainBal.toString());
                ChainAccountMirror mirror = chainAccountMirrorMapper.selectById(platformId);
                long dbBal = mirror != null && mirror.getCoinBalance() != null ? mirror.getCoinBalance() : 0L;
                platformRow.put("dbBalance", String.valueOf(dbBal));
                boolean matched = dbBal == chainBal.longValue();
                platformRow.put("balanceMatched", matched);
                if (!matched) {
                    mismatchCount++;
                    platformRow.put("balanceDiff", String.valueOf(chainBal.longValue() - dbBal));
                }
            } catch (Exception e) {
                platformRow.put("balanceError", e.getMessage());
            }
            out.put("platformAccount", platformRow);
            out.put("balanceMismatchCount", mismatchCount);
        }

        List<Map<String, Object>> events;
        try {
            events = timeCoinChainService.listMintTransferEvents(eventLimit);
        } catch (Exception e) {
            log.warn("listMintTransferEvents 失败", e);
            events = new ArrayList<>();
            out.put("eventsError", e.getMessage());
        }
        out.put("events", events);
        long mints = events.stream().filter(m -> "MINT".equals(m.get("type"))).count();
        long transfers = events.stream().filter(m -> "TRANSFER".equals(m.get("type"))).count();
        out.put("mintEventCount", mints);
        out.put("transferEventCount", transfers);
        return out;
    }

    private static Map<String, Object> baseUserRow(User u) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", u.getId());
        row.put("username", u.getUsername());
        row.put("email", u.getEmail() != null ? u.getEmail() : "");
        row.put("role", u.getRole());
        return row;
    }

    private static List<Map<String, Object>> buildUserRowsOffline(List<User> allUsers) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : allUsers) {
            Map<String, Object> row = baseUserRow(u);
            row.put("chainUserId", String.valueOf(u.getId()));
            row.put("dbBalance", u.getCoinBalance() != null ? String.valueOf(u.getCoinBalance()) : "0");
            row.put("coinBalance", null);
            list.add(row);
        }
        return list;
    }
}

package org.example.timecoinweb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pojo.User;
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
        for (User u : allUsers) {
            Map<String, Object> row = baseUserRow(u);
            String chainUid = String.valueOf(u.getId());
            row.put("chainUserId", chainUid);
            try {
                BigInteger bal = timeCoinChainService.balanceOf(chainUid);
                row.put("coinBalance", bal.toString());
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
            row.put("coinBalance", null);
            list.add(row);
        }
        return list;
    }
}

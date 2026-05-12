package org.example.timecoinweb.controller;

import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.pojo.Result;
import org.example.pojo.User;
import org.example.timecoinweb.mapper.RegisterMapper;
import org.example.timecoinweb.service.TimeCoinChainService;
import org.example.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/info/coin")
public class UserCoinController {

    @Autowired
    private TimeCoinChainService timeCoinChainService;

    @Autowired
    private RegisterMapper registerMapper;

    @GetMapping("/balance")
    public Result getMyBalance(@RequestHeader("token") String token) {
        try {
            Claims claims = JwtUtils.parseJWT(token);
            Integer id = (Integer) claims.get("id");
            String userId = String.valueOf(id);

            Map<String, Object> body = new HashMap<>();
            if (!timeCoinChainService.isChainReady()) {
                body.put("balance", "0");
                body.put("chainReady", false);
                body.put("reason", timeCoinChainService.getNotReadyReason());
                return Result.success(body);
            }

            BigInteger bal = timeCoinChainService.balanceOf(userId);
            body.put("balance", bal.toString());
            body.put("chainReady", true);
            return Result.success(body);
        } catch (Exception e) {
            log.warn("获取余额失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/transfer")
    public Result transfer(@RequestHeader("token") String token, @RequestBody TransferRequest request) {
        try {
            Claims claims = JwtUtils.parseJWT(token);
            Integer fromId = (Integer) claims.get("id");
            String fromUserId = String.valueOf(fromId);

            if (request == null || request.getToUserId() == null || request.getAmount() == null) {
                return Result.error("请提供收款用户ID和转账金额");
            }

            if (!StringUtils.hasText(request.getPassword())) {
                return Result.error("请输入登录密码进行验证");
            }

            User currentUser = registerMapper.getById(fromId);
            if (currentUser == null) {
                return Result.error("用户不存在");
            }

            if (!request.getPassword().equals(currentUser.getPassword())) {
                return Result.error("密码错误");
            }

            BigInteger amount = new BigInteger(request.getAmount().trim());
            if (amount.signum() <= 0) {
                return Result.error("转账金额必须大于0");
            }

            if (fromUserId.equals(request.getToUserId().trim())) {
                return Result.error("不能转账给自己");
            }

            if (!timeCoinChainService.isChainReady()) {
                return Result.error("区块链未就绪，无法转账");
            }

            BigInteger balance = timeCoinChainService.balanceOf(fromUserId);
            if (balance.compareTo(amount) < 0) {
                return Result.error("余额不足！当前余额：" + balance + "，需要转账：" + amount);
            }

            String txHash = timeCoinChainService.transfer(fromUserId, request.getToUserId().trim(), amount);

            Map<String, Object> body = new HashMap<>();
            body.put("txHash", txHash);
            body.put("fromUserId", fromUserId);
            body.put("toUserId", request.getToUserId());
            body.put("amount", amount.toString());

            log.info("用户转账成功 from={} to={} amount={}", fromUserId, request.getToUserId(), amount);
            return Result.success(body);
        } catch (Exception e) {
            log.warn("转账失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/history")
    public Result getTransferHistory(@RequestHeader("token") String token, @RequestParam(defaultValue = "50") int limit) {
        try {
            Claims claims = JwtUtils.parseJWT(token);
            Integer id = (Integer) claims.get("id");
            String userId = String.valueOf(id);

            int safeLimit = Math.max(1, Math.min(limit, 200));
            List<Map<String, Object>> history = timeCoinChainService.listUserMintTransferEvents(userId, safeLimit);

            return Result.success(history);
        } catch (Exception e) {
            log.warn("获取转账历史失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/history/paged")
    public Result getTransferHistoryPaged(@RequestHeader("token") String token, 
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Claims claims = JwtUtils.parseJWT(token);
            Integer id = (Integer) claims.get("id");
            String userId = String.valueOf(id);

            int safePageSize = Math.max(5, Math.min(pageSize, 100));
            Map<String, Object> history = timeCoinChainService.listUserMintTransferEventsPaged(userId, page, safePageSize);

            return Result.success(history);
        } catch (Exception e) {
            log.warn("获取分页转账历史失败", e);
            return Result.error(e.getMessage());
        }
    }

    @Data
    public static class TransferRequest {
        private String toUserId;
        private String amount;
        private String password;
    }
}

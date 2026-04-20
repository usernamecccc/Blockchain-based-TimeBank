package org.example.timecoinweb.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.pojo.Result;
import org.example.timecoinweb.service.TimeCoinChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员链上操作（路径须含 administrator 以通过 {@link org.example.timecoinweb.interceptor.LoginCheckInterceptor}）。
 */
@Slf4j
@RestController
@RequestMapping("/administrator/chain")
public class ChainController {

    @Autowired
    private TimeCoinChainService timeCoinChainService;

    @GetMapping("/status")
    public Result status() {
        Map<String, Object> m = new HashMap<>();
        m.put("ready", timeCoinChainService.isChainReady());
        if (!timeCoinChainService.isChainReady()) {
            m.put("reason", timeCoinChainService.getNotReadyReason());
        }
        return Result.success(m);
    }

    @GetMapping("/balance")
    public Result balance(@RequestParam String userId) {
        try {
            BigInteger balance = timeCoinChainService.balanceOf(userId);
            Map<String, Object> m = new HashMap<>();
            m.put("userId", userId);
            m.put("balance", balance.toString());
            return Result.success(m);
        } catch (Exception e) {
            log.warn("balanceOf failed", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/mint")
    public Result mint(@RequestBody MintBody body) {
        try {
            if (body == null || body.getUserId() == null || body.getAmount() == null) {
                return Result.error("请提供 userId 与 amount");
            }
            BigInteger amount = new BigInteger(body.getAmount().trim());
            String txHash = timeCoinChainService.mint(body.getUserId().trim(), amount);
            Map<String, Object> m = new HashMap<>();
            m.put("txHash", txHash);
            return Result.success(m);
        } catch (Exception e) {
            log.warn("mint failed", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/transfer")
    public Result transfer(@RequestBody TransferBody body) {
        try {
            if (body == null || body.getFromUserId() == null || body.getToUserId() == null || body.getAmount() == null) {
                return Result.error("请提供 fromUserId、toUserId、amount");
            }
            BigInteger amount = new BigInteger(body.getAmount().trim());
            String txHash = timeCoinChainService.transfer(
                    body.getFromUserId().trim(), body.getToUserId().trim(), amount);
            Map<String, Object> m = new HashMap<>();
            m.put("txHash", txHash);
            return Result.success(m);
        } catch (Exception e) {
            log.warn("transfer failed", e);
            return Result.error(e.getMessage());
        }
    }

    @Data
    public static class MintBody {
        private String userId;
        /** 十进制整数字符串，如 "100" */
        private String amount;
    }

    @Data
    public static class TransferBody {
        private String fromUserId;
        private String toUserId;
        private String amount;
    }
}

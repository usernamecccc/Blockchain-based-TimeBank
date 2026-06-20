package org.example.timecoinweb.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.pojo.Result;
import org.example.timecoinweb.service.CoinBalanceLedgerService;
import org.example.timecoinweb.service.TimeCoinChainAdminService;
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

    @Autowired
    private TimeCoinChainAdminService timeCoinChainAdminService;

    @Autowired
    private CoinBalanceLedgerService coinBalanceLedgerService;

    /**
     * 时间币管理页：链状态、各用户链上余额、最近 Mint/Transfer 事件。
     *
     * @param eventLimit 最多返回事件条数（1–500）
     */
    @GetMapping("/overview")
    public Result overview(@RequestParam(defaultValue = "100") int eventLimit) {
        int lim = Math.max(1, Math.min(eventLimit, 500));
        return Result.success(timeCoinChainAdminService.overview(lim));
    }

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
            String txHash = timeCoinChainService.mint(body.getUserId().trim(), amount, "ADMIN_MINT", body.getUserId());
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
                    body.getFromUserId().trim(), body.getToUserId().trim(), amount, "ADMIN_TRANSFER",
                    body.getFromUserId() + "->" + body.getToUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("txHash", txHash);
            return Result.success(m);
        } catch (Exception e) {
            log.warn("transfer failed", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 手动触发链上余额与数据库镜像对账。
     *
     * @param autoFix 是否以链上余额为准自动修正数据库，默认 true
     */
    @PostMapping("/reconcile")
    public Result reconcile(@RequestParam(defaultValue = "true") boolean autoFix) {
        try {
            return Result.success(coinBalanceLedgerService.reconcileAll(autoFix));
        } catch (Exception e) {
            log.warn("reconcile failed", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 最近对账记录与链上交易台账。
     */
    @GetMapping("/reconcile/logs")
    public Result reconcileLogs(@RequestParam(defaultValue = "20") int limit) {
        Map<String, Object> body = new HashMap<>();
        body.put("reconcileLogs", coinBalanceLedgerService.listRecentReconcileLogs(limit));
        body.put("txLogs", coinBalanceLedgerService.listRecentTxLogs(limit));
        return Result.success(body);
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

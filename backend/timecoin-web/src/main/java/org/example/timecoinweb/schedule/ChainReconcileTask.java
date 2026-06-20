package org.example.timecoinweb.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.timecoinweb.config.BlockchainProperties;
import org.example.timecoinweb.service.CoinBalanceLedgerService;
import org.example.timecoinweb.service.TimeCoinChainService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定期将数据库时间币余额镜像与链上 balanceOf 对账；链上为权威数据源。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainReconcileTask implements ApplicationRunner {

    private final BlockchainProperties blockchainProperties;
    private final TimeCoinChainService timeCoinChainService;
    private final CoinBalanceLedgerService coinBalanceLedgerService;

    @Override
    public void run(ApplicationArguments args) {
        if (!blockchainProperties.isEnabled() || !blockchainProperties.isReconcileOnStartup()) {
            return;
        }
        if (!timeCoinChainService.isChainReady()) {
            log.info("跳过启动对账：{}", timeCoinChainService.getNotReadyReason());
            return;
        }
        log.info("启动时执行时间币余额全量对账…");
        coinBalanceLedgerService.reconcileAll(blockchainProperties.isReconcileAutoFix());
    }

    @Scheduled(fixedDelayString = "${blockchain.reconcile-interval-ms:1800000}")
    public void scheduledReconcile() {
        if (!blockchainProperties.isEnabled() || !blockchainProperties.isReconcileEnabled()) {
            return;
        }
        if (!timeCoinChainService.isChainReady()) {
            return;
        }
        log.info("定时执行时间币余额对账…");
        coinBalanceLedgerService.reconcileAll(blockchainProperties.isReconcileAutoFix());
    }
}

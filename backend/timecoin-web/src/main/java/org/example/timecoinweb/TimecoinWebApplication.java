package org.example.timecoinweb;

import org.example.timecoinweb.config.BlockchainProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BlockchainProperties.class)
public class TimecoinWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimecoinWebApplication.class, args);
    }

    /**
     * 链上答谢成功后单独提交 reward_paid，避免「链已成功 + 主事务回滚」导致重试时重复划转。
     */
    @Bean(name = "volunteerRewardMarkTemplate")
    public TransactionTemplate volunteerRewardMarkTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate tpl = new TransactionTemplate(transactionManager);
        tpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tpl;
    }
}

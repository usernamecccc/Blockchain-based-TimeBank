package org.example.timecoinweb;

import org.example.timecoinweb.config.BlockchainProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BlockchainProperties.class)
public class TimecoinWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimecoinWebApplication.class, args);
    }

}

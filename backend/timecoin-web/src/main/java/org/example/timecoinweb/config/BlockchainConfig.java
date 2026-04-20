package org.example.timecoinweb.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Slf4j
@Configuration
public class BlockchainConfig {

    @Bean
    @ConditionalOnProperty(name = "blockchain.enabled", havingValue = "true")
    public Web3j web3j(BlockchainProperties props) {
        log.info("Blockchain RPC: {}", props.getRpcUrl());
        return Web3j.build(new HttpService(props.getRpcUrl()));
    }

    @Bean
    @ConditionalOnProperty(name = "blockchain.enabled", havingValue = "true")
    public Credentials blockchainCredentials(BlockchainProperties props) {
        String pk = props.getPrivateKey();
        if (pk == null || pk.isBlank()) {
            throw new IllegalStateException("blockchain.enabled=true 但 blockchain.private-key 为空");
        }
        String normalized = pk.startsWith("0x") ? pk : "0x" + pk;
        return Credentials.create(normalized);
    }
}

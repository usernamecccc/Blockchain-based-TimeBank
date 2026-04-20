package org.example.timecoinweb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.timecoinweb.config.BlockchainProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeCoinChainService {

    private final BlockchainProperties blockchainProperties;
    private final ObjectProvider<Web3j> web3jProvider;
    private final ObjectProvider<Credentials> credentialsProvider;

    public boolean isChainReady() {
        if (!blockchainProperties.isEnabled()) {
            return false;
        }
        if (!StringUtils.hasText(blockchainProperties.getContractAddress())) {
            return false;
        }
        return web3jProvider.getIfAvailable() != null
                && credentialsProvider.getIfAvailable() != null;
    }

    public String getNotReadyReason() {
        if (!blockchainProperties.isEnabled()) {
            return "区块链未启用（blockchain.enabled=false）";
        }
        if (!StringUtils.hasText(blockchainProperties.getContractAddress())) {
            return "未配置合约地址（blockchain.contract-address），请先部署 TimeCoin 并填写";
        }
        if (web3jProvider.getIfAvailable() == null) {
            return "Web3j 未初始化（请设置 blockchain.enabled=true 且配置私钥）";
        }
        return "未知原因";
    }

    /**
     * @param userId 业务用户标识（与合约中 string 一致，例如数据库主键字符串）
     */
    public String mint(String userId, BigInteger amount) throws Exception {
        requireReady();
        if (!StringUtils.hasText(userId) || amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("userId 与 amount 无效");
        }
        Function function = new Function(
                "mint",
                Arrays.asList(new Utf8String(userId), new Uint256(amount)),
                Collections.emptyList());
        return sendTransaction(FunctionEncoder.encode(function));
    }

    public String transfer(String fromUserId, String toUserId, BigInteger amount) throws Exception {
        requireReady();
        Function function = new Function(
                "transfer",
                Arrays.asList(new Utf8String(fromUserId), new Utf8String(toUserId), new Uint256(amount)),
                Collections.emptyList());
        return sendTransaction(FunctionEncoder.encode(function));
    }

    public BigInteger balanceOf(String userId) throws Exception {
        requireReady();
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        Web3j web3j = web3jProvider.getIfAvailable();
        Credentials credentials = credentialsProvider.getIfAvailable();
        String contract = blockchainProperties.getContractAddress();

        Function function = new Function(
                "balanceOf",
                Collections.singletonList(new Utf8String(userId)),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));

        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        credentials.getAddress(), contract, encoded),
                DefaultBlockParameterName.LATEST).send();

        String value = response.getValue();
        if (value == null || "0x".equals(value) || value.isEmpty()) {
            return BigInteger.ZERO;
        }
        List<Type> decoded = FunctionReturnDecoder.decode(value, function.getOutputParameters());
        if (decoded.isEmpty()) {
            return BigInteger.ZERO;
        }
        return (BigInteger) decoded.get(0).getValue();
    }

    private void requireReady() {
        if (!isChainReady()) {
            throw new IllegalStateException(getNotReadyReason());
        }
    }

    private String sendTransaction(String encodedData) throws Exception {
        Web3j web3j = web3jProvider.getIfAvailable();
        Credentials credentials = credentialsProvider.getIfAvailable();
        long chainId = web3j.ethChainId().send().getChainId().longValue();
        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);
        String contract = blockchainProperties.getContractAddress();

        EthSendTransaction send = txManager.sendTransaction(
                DefaultGasProvider.GAS_PRICE,
                DefaultGasProvider.GAS_LIMIT,
                contract,
                encodedData,
                BigInteger.ZERO);

        if (send.hasError()) {
            throw new IllegalStateException("链上交易失败: " + send.getError().getMessage());
        }
        return send.getTransactionHash();
    }
}

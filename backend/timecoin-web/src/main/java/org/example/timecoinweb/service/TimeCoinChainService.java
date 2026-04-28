package org.example.timecoinweb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.timecoinweb.config.BlockchainProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * eth_getLogs 返回的 data 在部分版本为 String，部分反序列化为 byte[]，统一成带 0x 的 hex 再解码。
     */
    private static String logDataToHex(Object raw) {
        if (raw == null) {
            return "";
        }
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            return s.isEmpty() ? "" : (s.startsWith("0x") ? s : ("0x" + s));
        }
        if (raw instanceof byte[]) {
            byte[] bytes = (byte[]) raw;
            return bytes.length == 0 ? "" : Numeric.toHexString(bytes);
        }
        return raw.toString();
    }

    /** Web3j decode 第二参数需要 List(TypeReference(Type))，不能与通配符列表混用。 */
    @SuppressWarnings("unchecked")
    private static TypeReference<Type> upcastRef(TypeReference<?> ref) {
        return (TypeReference<Type>) ref;
    }

    private static List<TypeReference<Type>> mintEventDecodeParams() {
        List<TypeReference<Type>> list = new ArrayList<>(2);
        list.add(upcastRef(new TypeReference<Utf8String>() {}));
        list.add(upcastRef(new TypeReference<Uint256>() {}));
        return list;
    }

    private static List<TypeReference<Type>> transferEventDecodeParams() {
        List<TypeReference<Type>> list = new ArrayList<>(3);
        list.add(upcastRef(new TypeReference<Utf8String>() {}));
        list.add(upcastRef(new TypeReference<Utf8String>() {}));
        list.add(upcastRef(new TypeReference<Uint256>() {}));
        return list;
    }

    private static final Event EVENT_MINT = new Event(
            "Mint",
            Arrays.asList(
                    new TypeReference<Utf8String>() {},
                    new TypeReference<Uint256>() {}));
    private static final Event EVENT_TRANSFER = new Event(
            "Transfer",
            Arrays.asList(
                    new TypeReference<Utf8String>() {},
                    new TypeReference<Utf8String>() {},
                    new TypeReference<Uint256>() {}));
    private static final String TOPIC_MINT = EventEncoder.encode(EVENT_MINT);
    private static final String TOPIC_TRANSFER = EventEncoder.encode(EVENT_TRANSFER);

    /**
     * 合约 Mint / Transfer 事件（本地节点从创世扫描；区块多时可能较慢）。
     */
    public List<Map<String, Object>> listMintTransferEvents(int limit) throws Exception {
        requireReady();
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Web3j web3j = web3jProvider.getIfAvailable();
        String contract = blockchainProperties.getContractAddress();

        EthFilter filter = new EthFilter(
                DefaultBlockParameterName.EARLIEST,
                DefaultBlockParameterName.LATEST,
                contract);
        filter.addOptionalTopics(TOPIC_MINT, TOPIC_TRANSFER);

        EthLog ethLog = web3j.ethGetLogs(filter).send();
        if (ethLog.hasError()) {
            throw new IllegalStateException("eth_getLogs 失败: " + ethLog.getError().getMessage());
        }
        List<EthLog.LogResult> raw = ethLog.getLogs();
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }

        List<Log> logs = new ArrayList<>();
        for (EthLog.LogResult<?> lr : raw) {
            logs.add((Log) lr.get());
        }
        logs.sort(Comparator
                .comparing(Log::getBlockNumber, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Log::getLogIndex, Comparator.nullsFirst(Comparator.naturalOrder())));
        Collections.reverse(logs);
        if (logs.size() > limit) {
            logs = new ArrayList<>(logs.subList(0, limit));
        }

        Map<BigInteger, Long> blockTsCache = new HashMap<>();
        List<Map<String, Object>> out = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Shanghai"));

        for (Log lg : logs) {
            Map<String, Object> row = new HashMap<>();
            row.put("txHash", lg.getTransactionHash());
            row.put("blockNumber", lg.getBlockNumber() != null ? lg.getBlockNumber().toString() : "");
            String topic0 = lg.getTopics() != null && !lg.getTopics().isEmpty()
                    ? lg.getTopics().get(0) : "";

            long tsSec = 0L;
            if (lg.getBlockNumber() != null) {
                tsSec = blockTsCache.computeIfAbsent(lg.getBlockNumber(), bn -> {
                    try {
                        EthBlock blk = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(bn), false).send();
                        if (blk.getBlock() == null || blk.getBlock().getTimestamp() == null) {
                            return 0L;
                        }
                        return blk.getBlock().getTimestamp().longValue();
                    } catch (Exception e) {
                        log.warn("读取区块时间失败 {}", bn, e);
                        return 0L;
                    }
                });
            }
            if (tsSec > 0) {
                row.put("time", fmt.format(Instant.ofEpochSecond(tsSec)));
            } else {
                row.put("time", "—");
            }

            String dataHex = logDataToHex(lg.getData());
            if (!StringUtils.hasText(dataHex) || "0x".equalsIgnoreCase(dataHex.trim())) {
                row.put("type", "UNKNOWN");
                row.put("amount", "0");
                row.put("parties", "—");
                out.add(row);
                continue;
            }

            try {
                if (TOPIC_MINT.equalsIgnoreCase(topic0)) {
                    List<Type> decoded = FunctionReturnDecoder.decode(dataHex, mintEventDecodeParams());
                    if (decoded == null || decoded.size() < 2) {
                        throw new IllegalStateException("Mint 解码结果不足");
                    }
                    String to = decoded.get(0).getValue().toString();
                    BigInteger amt = (BigInteger) decoded.get(1).getValue();
                    row.put("type", "MINT");
                    row.put("amount", amt.toString());
                    row.put("parties", "平台 → 用户 " + to);
                } else if (TOPIC_TRANSFER.equalsIgnoreCase(topic0)) {
                    List<Type> decoded = FunctionReturnDecoder.decode(dataHex, transferEventDecodeParams());
                    if (decoded == null || decoded.size() < 3) {
                        throw new IllegalStateException("Transfer 解码结果不足");
                    }
                    String from = decoded.get(0).getValue().toString();
                    String to = decoded.get(1).getValue().toString();
                    BigInteger amt = (BigInteger) decoded.get(2).getValue();
                    row.put("type", "TRANSFER");
                    row.put("amount", amt.toString());
                    row.put("parties", "用户 " + from + " → 用户 " + to);
                } else {
                    row.put("type", "UNKNOWN");
                    row.put("amount", "0");
                    row.put("parties", "—");
                }
            } catch (Exception e) {
                log.warn("解析链上日志失败 tx={}", lg.getTransactionHash(), e);
                row.put("type", "PARSE_ERROR");
                row.put("amount", "0");
                row.put("parties", (dataHex.length() > 22 ? dataHex.substring(0, 22) + "…" : dataHex));
            }
            out.add(row);
        }
        return out;
    }
}

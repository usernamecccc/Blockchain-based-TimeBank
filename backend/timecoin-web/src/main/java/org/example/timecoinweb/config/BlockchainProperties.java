package org.example.timecoinweb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigInteger;

/**
 * TimeCoin 合约 JSON-RPC 配置。私钥仅用于开发环境（合约 owner 发交易）。
 */
@Data
@ConfigurationProperties(prefix = "blockchain")
public class BlockchainProperties {

    /** 为 false 时不连接节点，链相关接口返回明确提示 */
    private boolean enabled = false;

    private String rpcUrl = "http://127.0.0.1:8545/";

    /** 部署后的 TimeCoin 合约地址（0x...），与 TimeCoinSystem/constants.js 或 hardhat-deploy 输出一致 */
    private String contractAddress = "";

    /**
     * 部署账户私钥（须为合约 owner），例如 Hardhat 本地默认账户 #0。
     * 切勿在生产环境把真实私钥写入配置文件。
     */
    private String privateKey = "";

    /**
     * 老人发布活动时从链上余额扣除的时间币数量；≤0 或未启用链时不扣费。
     */
    private BigInteger oldPublishActivityCost = BigInteger.ZERO;

    /**
     * 扣费转入的业务 userId（合约 string），余额记在合约 mapping 中，便于区分「池」。
     */
    private String feeRecipientUserId = "platform";

    /**
     * 老人向志愿者单次答谢的金额上限（含）；服务端校验与前端提示保持一致。
     * 若为 null、零或负数则视作不限制上限（不推荐生产环境）。
     */
    private BigInteger volunteerRewardMax = BigInteger.valueOf(999);
}

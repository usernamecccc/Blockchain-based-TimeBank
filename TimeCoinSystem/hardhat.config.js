require("@nomicfoundation/hardhat-toolbox");
require('hardhat-deploy');
require("dotenv").config();

const { PRIVATE_KEY, RPC_URL } = process.env;

const networks = {
  localhost: {
    url: "http://127.0.0.1:8545/",
    chainId: 31337,
  },
};

// 仅当配置了环境变量时注册测试网（Rinkeby 已弃用，仍可按需使用）
if (RPC_URL && PRIVATE_KEY) {
  networks.rinkeby = {
    url: RPC_URL,
    accounts: [PRIVATE_KEY],
    chainId: 4,
  };
}

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  // hardhat-deploy 要求默认网络为 hardhat，否则 `npx hardhat node` 会报错
  defaultNetwork: "hardhat",
  networks,
  namedAccounts: {
    deployer: {
      default: 0,
    },
  },
  solidity: "0.8.0",
};

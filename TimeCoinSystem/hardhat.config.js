require("@nomicfoundation/hardhat-toolbox");
require("hardhat-deploy");
require("dotenv").config();

const {
  PRIVATE_KEY,
  RPC_URL,
  GETH_RPC_URL,
  GETH_CHAIN_ID,
} = process.env;

const defaultDevPrivateKey =
  "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";

const networks = {
  localhost: {
    url: "http://127.0.0.1:8545/",
    chainId: 31337,
  },
  gethLocal: {
    url: GETH_RPC_URL || "http://127.0.0.1:8545/",
    accounts: [PRIVATE_KEY || defaultDevPrivateKey],
    chainId: Number(GETH_CHAIN_ID || 20260618),
  },
};

if (RPC_URL && PRIVATE_KEY) {
  networks.remote = {
    url: RPC_URL,
    accounts: [PRIVATE_KEY],
  };
}

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  defaultNetwork: "hardhat",
  networks,
  namedAccounts: {
    deployer: {
      default: 0,
    },
  },
  solidity: "0.8.0",
};

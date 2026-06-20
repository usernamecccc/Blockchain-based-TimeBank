# Geth local private chain

This folder contains local-only scripts for replacing the Hardhat development chain with a Geth private chain.

The scripts use the standard Hardhat development account for convenience:

- address: `0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266`
- private key: `0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80`
- chain id: `20260618`
- JSON-RPC: `http://127.0.0.1:8545`

This is only for local development and course demonstration. Do not use this key or password in production.

## Prerequisite

Install a Geth version that still supports the classic local Clique workflow, for example Geth 1.13.x.

Check:

```powershell
geth version
```

## 1. Initialize the private chain

From `TimeCoinSystem/geth-local`:

```powershell
.\init-dev-chain.ps1
```

This imports the local development private key, generates `genesis.json`, and runs `geth init`.

## 2. Start Geth

```powershell
.\start-geth.ps1
```

Keep this terminal open. The JSON-RPC endpoint will listen on `127.0.0.1:8545`.

## 3. Deploy TimeCoin to Geth

Open another terminal in `TimeCoinSystem`:

```powershell
$env:PRIVATE_KEY='0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80'
$env:GETH_RPC_URL='http://127.0.0.1:8545/'
$env:GETH_CHAIN_ID='20260618'
npm run deploy:geth
```

Copy the deployed contract address from `deployments/gethLocal/TimeCoin.json`.

## 4. Start backend with Geth config

Set the backend environment variables before starting Spring Boot:

```powershell
$env:BLOCKCHAIN_RPC_URL='http://127.0.0.1:8545/'
$env:BLOCKCHAIN_CONTRACT_ADDRESS='0xYOUR_DEPLOYED_TIMECOIN_ADDRESS'
$env:BLOCKCHAIN_PRIVATE_KEY='0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80'
```

Then start `timecoin-web` normally.


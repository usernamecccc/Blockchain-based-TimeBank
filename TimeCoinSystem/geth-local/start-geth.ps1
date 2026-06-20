$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$DataDir = Join-Path $ScriptDir "data"
$PasswordPath = Join-Path $ScriptDir "password.txt"
$SignerAddress = "0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266"
$NetworkId = 20260618

if (-not (Get-Command geth -ErrorAction SilentlyContinue)) {
  throw "geth was not found in PATH. Install Geth 1.13.x first."
}

if (-not (Test-Path $DataDir)) {
  throw "Missing data directory. Run .\init-dev-chain.ps1 first."
}

geth `
  --datadir $DataDir `
  --networkid $NetworkId `
  --http `
  --http.addr "127.0.0.1" `
  --http.port 8545 `
  --http.api "eth,net,web3,personal,clique,miner" `
  --http.corsdomain "*" `
  --http.vhosts "*" `
  --allow-insecure-unlock `
  --unlock $SignerAddress `
  --password $PasswordPath `
  --mine `
  --miner.etherbase $SignerAddress


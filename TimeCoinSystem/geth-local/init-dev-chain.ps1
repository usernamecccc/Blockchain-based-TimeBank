$ErrorActionPreference = "Stop"

$ChainId = 20260618
$SignerAddress = "f39fd6e51aad88f6f4ce6ab8827279cfffb92266"
$PrivateKey = "ac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"
$DevPassword = "timecoin-dev-password"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$DataDir = Join-Path $ScriptDir "data"
$GenesisPath = Join-Path $ScriptDir "genesis.json"
$PasswordPath = Join-Path $ScriptDir "password.txt"
$PrivateKeyPath = Join-Path $ScriptDir "private-key.txt"

if (-not (Get-Command geth -ErrorAction SilentlyContinue)) {
  throw "geth was not found in PATH. Install Geth 1.13.x first, then rerun this script."
}

New-Item -ItemType Directory -Force -Path $DataDir | Out-Null
Set-Content -LiteralPath $PasswordPath -Value $DevPassword -NoNewline -Encoding ascii
Set-Content -LiteralPath $PrivateKeyPath -Value $PrivateKey -NoNewline -Encoding ascii

$KeystoreDir = Join-Path $DataDir "keystore"
$HasAccount = (Test-Path $KeystoreDir) -and ((Get-ChildItem -LiteralPath $KeystoreDir -File -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0)
if (-not $HasAccount) {
  geth account import --datadir $DataDir --password $PasswordPath $PrivateKeyPath
}

$ExtraData = "0x" + ("0" * 64) + $SignerAddress + ("0" * 130)
$Genesis = [ordered]@{
  config = [ordered]@{
    chainId = $ChainId
    homesteadBlock = 0
    eip150Block = 0
    eip155Block = 0
    eip158Block = 0
    byzantiumBlock = 0
    constantinopleBlock = 0
    petersburgBlock = 0
    istanbulBlock = 0
    clique = [ordered]@{
      period = 3
      epoch = 30000
    }
  }
  difficulty = "1"
  gasLimit = "8000000"
  extraData = $ExtraData
  alloc = [ordered]@{
    $SignerAddress = [ordered]@{
      balance = "1000000000000000000000000000"
    }
  }
}

$GenesisJson = $Genesis | ConvertTo-Json -Depth 10
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($GenesisPath, $GenesisJson, $Utf8NoBom)
geth --datadir $DataDir init $GenesisPath

Write-Host "Geth private chain initialized."
Write-Host "Signer address: 0x$SignerAddress"
Write-Host "Chain ID: $ChainId"
Write-Host "Next: .\start-geth.ps1"

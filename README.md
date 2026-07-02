# FlashArb: Real-Time DeFi Arbitrage Execution Bot

FlashArb is a production-grade Android application designed for scanning and executing DeFi Flash Loan arbitrage opportunities. It leverages Aave V3 for liquidity and executes multi-hop swaps across Uniswap V3 and QuickSwap on the Polygon network.

## 🚀 Key Features

- **Real-Time On-Chain Scanning:** Direct integration with Uniswap V3 Quoter and QuickSwap Router for live price feeds.
- **Secure Flash Loan Execution:** Utilizes Aave V3's robust flash loan infrastructure.
- **Hardware-Backed Security:** Private keys are encrypted using the **Android Keystore System**, ensuring they never leave the device in plain text.
- **Intelligent Gas Management:** Dynamic gas price estimation using EIP-1559 standards for faster confirmations.
- **Multi-RPC Support:** Load-balanced RPC connections with automatic failover for high reliability.
- **Transaction Monitoring:** Real-time tracking of transaction status (Pending, Success, Failed) directly from the blockchain.
- **AI Analyst:** Gemini-powered market analysis to evaluate the viability of trade opportunities.

## 🛡️ Security First

- **Encrypted Storage:** Sensitive data is stored using `EncryptedSharedPreferences` and the Android Keystore.
- **Non-Custodial:** Your keys, your funds. The bot executes trades directly from your wallet to your deployed smart contract.
- **Smart Contract Safety:** Built with OpenZeppelin's `ReentrancyGuard` and strict `Access Control`.

## 🛠️ Getting Started

### 1. Smart Contract Deployment
Deploy the `FlashLoanArbitrage.sol` contract (found in `/contracts`) to Polygon Mainnet.
- **Pool Addresses Provider (Polygon):** `0xa97684ead0e451d98659253718054304545e14dd`
- **Router (Uniswap V3):** `0xE592427A0AEce92De3Edee1F18E0157C05861564`

### 2. App Configuration
- Open the **Execution** tab in the app.
- Go to **Configuration** (⚙️ icon).
- Provide your **Wallet Address**, **Deployed Contract Address**, and **Private Key**.
- Save settings (Key will be securely encrypted).

### 3. Execution
- Click **"Start Arbitrage Scan"**.
- When an opportunity is found, review the potential profit and click **"Execute"**.

## 📊 Roadmap
- [ ] WalletConnect Integration (Priority)
- [ ] Dynamic Slippage Calculation in Contract
- [ ] Support for BSC and Avalanche Networks
- [ ] Comprehensive Unit & Integration Tests

## ⚖️ Disclaimer
This software is for advanced users. DeFi arbitrage involves significant risks including front-running, slippage, and smart contract vulnerabilities. Use only with funds you can afford to lose.

## License
MIT

# FlashArb DeFi Arbitrage Bot

FlashArb is an Android-based DeFi Flash Loan arbitrage simulator and execution tool. It allows users to scan for price differences between Uniswap V3 and QuickSwap on the Polygon network and execute real flash loans using Aave V3.

## 🚨 Security & Risk Disclaimer

**IMPORTANT:** This project involves real cryptocurrency transactions. Using it carries significant financial risk.
- **Private Key Safety:** Never share your private key. The app stores it only in memory and never sends it to any server. However, for maximum safety, use a dedicated "hot wallet" with only the necessary funds for gas.
- **Slippage & Front-running:** Flash loans are susceptible to MEV (Miner Extractable Value) attacks. Without proper slippage protection and high gas fees, your transactions may be front-run, leading to loss of gas fees.
- **Audit:** The provided smart contract is for educational purposes. It has NOT been professionally audited. Use at your own risk.

## 🚀 Features

- **Real-time Price Scanning:** Fetches live prices from Uniswap V3 Quoter.
- **Flash Loan Execution:** Integrates with Aave V3 for high-liquidity flash loans.
- **Profit Calculator:** Built-in tool to estimate net profit considering gas fees and flash loan premiums.
- **AI Advisor:** Gemini-powered assistant to help with DeFi concepts and contract code.

## 🛠️ Smart Contract Deployment

To execute real trades, you must deploy the `FlashLoanArbitrage.sol` contract located in the `contracts/` directory.

1. Open [Remix IDE](https://remix.ethereum.org/).
2. Upload `FlashLoanArbitrage.sol` and its dependencies (OpenZeppelin, Aave).
3. Compile using Solidity 0.8.10 or higher.
4. Deploy to **Polygon Mainnet** using `Injected Provider - MetaMask`.
5. Use the Aave V3 Pool Addresses Provider for Polygon: `0xa97684ead0e451d98659253718054304545e14dd`.
6. Copy the deployed contract address into the app's settings.

## ⚙️ App Configuration

- **RPC URL:** Defaulted to `https://polygon-rpc.com`. You can provide your own Infura/Alchemy URL in settings for better performance.
- **Wallet:** Provide your public address to sync balances and your private key to sign execution transactions.

## 📝 TODO / Improvements
- [ ] Implement WalletConnect integration to remove the need for manual private key entry.
- [ ] Add dynamic slippage calculation in the smart contract.
- [ ] Implement more DEX integrations (SushiSwap, Balancer).
- [ ] Add comprehensive unit and integration tests.

## License
MIT

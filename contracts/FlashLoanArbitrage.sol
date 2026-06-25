// SPDX-License-Identifier: MIT
pragma solidity ^0.8.10;

import "https://github.com/aave/aave-v3-core/blob/master/contracts/flashloan/base/FlashLoanSimpleReceiverBase.sol";
import "https://github.com/aave/aave-v3-core/blob/master/contracts/interfaces/IPoolAddressesProvider.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/IERC20.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/security/ReentrancyGuard.sol";

// Uniswap V3 Router Interface
interface ISwapRouter {
    struct ExactInputSingleParams {
        address tokenIn;
        address tokenOut;
        uint24 fee;
        address recipient;
        uint256 deadline;
        uint256 amountIn;
        uint256 amountOutMinimum;
        uint160 sqrtPriceLimitX96;
    }
    function exactInputSingle(ExactInputSingleParams calldata params) external payable returns (uint256 amountOut);
}

// QuickSwap/Uniswap V2 Router Interface
interface IUniswapV2Router {
    function swapExactTokensForTokens(
        uint amountIn,
        uint amountOutMin,
        address[] calldata path,
        address to,
        uint deadline
    ) external returns (uint[] memory amounts);
}

contract FlashLoanArbitrage is FlashLoanSimpleReceiverBase, ReentrancyGuard {
    address public immutable owner;

    // Polygon Mainnet Addresses
    ISwapRouter public immutable uniswapRouter = ISwapRouter(0xE592427A0AEce92De3Edee1F18E0157C05861564);
    IUniswapV2Router public immutable quickswapRouter = IUniswapV2Router(0xa5E0829CaCEd8fFDD4De3c43696c57F7D7A678ff);

    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call");
        _;
    }

    constructor(address _addressProvider) FlashLoanSimpleReceiverBase(IPoolAddressesProvider(_addressProvider)) {
        owner = msg.sender;
    }

    function requestFlashLoan(address _token, uint256 _amount) public onlyOwner nonReentrant {
        POOL.flashLoanSimple(address(this), _token, _amount, "", 0);
    }

    /**
     * @dev This function is called after your contract has received the flash loaned amount
     */
    function executeOperation(
        address asset,
        uint256 amount,
        uint256 premium,
        address initiator,
        bytes calldata
    ) external override nonReentrant returns (bool) {
        // 1. Access Control: Only Aave Pool can call this
        require(msg.sender == address(POOL), "Only Pool can execute");
        require(initiator == address(this), "External flashloan not allowed");

        address wmatic = 0x0d500B1d8E8eF31E21C99d1Db9A6444d3ADf1270;

        // 2. Approve Router
        IERC20(asset).approve(address(uniswapRouter), amount);

        // 3. Swap on Uniswap V3 (Buy WMATIC)
        // Note: For production, amountOutMinimum should be calculated based on current price + slippage
        ISwapRouter.ExactInputSingleParams memory params = ISwapRouter.ExactInputSingleParams({
            tokenIn: asset,
            tokenOut: wmatic,
            fee: 3000,
            recipient: address(this),
            deadline: block.timestamp,
            amountIn: amount,
            amountOutMinimum: 0, // WARNING: Should be set for slippage protection
            sqrtPriceLimitX96: 0
        });

        uint256 amountReceived;
        try uniswapRouter.exactInputSingle(params) returns (uint256 _received) {
            amountReceived = _received;
        } catch {
            revert("Swap on Uniswap failed");
        }

        // 4. Swap back on QuickSwap (Sell WMATIC for original asset)
        IERC20(wmatic).approve(address(quickswapRouter), amountReceived);

        address[] memory path = new address[](2);
        path[0] = wmatic;
        path[1] = asset;

        try quickswapRouter.swapExactTokensForTokens(
            amountReceived,
            0, // WARNING: Should be set for slippage protection
            path,
            address(this),
            block.timestamp
        ) {
            // Success
        } catch {
            revert("Swap on QuickSwap failed");
        }

        // 5. Repay Loan + Premium
        uint256 amountToRepay = amount + premium;
        uint256 currentBalance = IERC20(asset).balanceOf(address(this));
        require(currentBalance >= amountToRepay, "Insufficient balance to repay loan");

        IERC20(asset).approve(address(POOL), amountToRepay);

        return true;
    }

    function withdraw(address _token) external onlyOwner nonReentrant {
        IERC20 token = IERC20(_token);
        uint256 balance = token.balanceOf(address(this));
        require(balance > 0, "Nothing to withdraw");
        token.transfer(owner, balance);
    }

    function withdrawPOL() external onlyOwner nonReentrant {
        uint256 balance = address(this).balance;
        require(balance > 0, "Nothing to withdraw");
        (bool success, ) = payable(owner).call{value: balance}("");
        require(success, "Transfer failed");
    }

    receive() external payable {}
}

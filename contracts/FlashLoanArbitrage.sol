// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "@openzeppelin/contracts/security/ReentrancyGuard.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@uniswap/v3-periphery/contracts/interfaces/ISwapRouter.sol";
import "@aave/core-v3/contracts/flashloan/interfaces/IFlashLoanReceiver.sol";
import "@aave/core-v3/contracts/interfaces/IPool.sol";

/**
 * @title FlashLoanArbitrage
 * @dev Implements a secure flash loan arbitrage bot using Aave V3 and Uniswap V3.
 */
contract FlashLoanArbitrage is ReentrancyGuard, Ownable, IFlashLoanReceiver {
    IPool public immutable POOL;
    ISwapRouter public immutable SWAP_ROUTER;

    uint256 public constant SLIPPAGE_BPS = 500; // 5% slippage tolerance
    uint256 public constant FEE_BPS = 900; // 0.09% Aave flash loan fee

    event ArbitrageExecuted(address token, uint256 profit, uint256 timestamp);
    event ArbitrageFailed(string reason);

    constructor(address _pool, address _swapRouter) {
        POOL = IPool(_pool);
        SWAP_ROUTER = ISwapRouter(_swapRouter);
    }

    /**
     * @dev Main entry point for the flash loan. Only callable by owner.
     */
    function requestFlashLoan(
        address _token,
        uint256 _amount,
        address _tokenToBuy,
        uint256 _minProfit
    ) external onlyOwner nonReentrant {
        bytes memory params = abi.encode(_tokenToBuy, _minProfit);

        address[] memory assets = new address[](1);
        assets[0] = _token;

        uint256[] memory amounts = new uint256[](1);
        amounts[0] = _amount;

        uint256[] memory interestRateModes = new uint256[](1);
        interestRateModes[0] = 0; // 0 = no debt, 1 = stable, 2 = variable

        POOL.flashLoan(
            address(this),
            assets,
            amounts,
            interestRateModes,
            address(this),
            params,
            0
        );
    }

    /**
     * @dev Aave V3 callback function.
     */
    function executeOperation(
        address[] calldata assets,
        uint256[] calldata amounts,
        uint256[] calldata premiums,
        address initiator,
        bytes calldata params
    ) external override nonReentrant returns (bool) {
        // Only Aave Pool can call this
        require(msg.sender == address(POOL), "Invalid caller");
        require(initiator == address(this), "External flashloan not allowed");

        // Decode parameters
        (address tokenToBuy, uint256 minProfit) = abi.decode(params, (address, uint256));

        address asset = assets[0];
        uint256 amount = amounts[0];
        uint256 premium = premiums[0];

        try this._performTrade(asset, amount, tokenToBuy) {
            uint256 currentBalance = IERC20(asset).balanceOf(address(this));
            uint256 amountOwed = amount + premium;

            require(currentBalance >= amountOwed, "Trade did not cover loan");
            uint256 profit = currentBalance - amountOwed;
            require(profit >= minProfit, "Profit too low");

            // Repay loan
            IERC20(asset).approve(address(POOL), amountOwed);

            emit ArbitrageExecuted(asset, profit, block.timestamp);
            return true;
        } catch {
            emit ArbitrageFailed("Arbitrage execution failed");
            revert("Arbitrage failed");
        }
    }

    /**
     * @dev Internal function to handle the actual arbitrage swaps.
     */
    function _performTrade(
        address _tokenIn,
        uint256 _amountIn,
        address _tokenOut
    ) external {
        require(msg.sender == address(this), "Internal call only");

        // Step 1: Swap TokenIn for TokenOut (e.g., USDT -> WMATIC)
        uint256 received = _swap(_tokenIn, _tokenOut, _amountIn, 0); // minOut set to 0 for initial swap

        // Step 2: Swap TokenOut back for TokenIn (e.g., WMATIC -> USDT)
        _swap(_tokenOut, _tokenIn, received, _amountIn); // Require at least original amount back
    }

    /**
     * @dev Internal swap helper using Uniswap V3.
     */
    function _swap(
        address tokenIn,
        address tokenOut,
        uint256 amountIn,
        uint256 amountOutMinimum
    ) internal returns (uint256 amountOut) {
        IERC20(tokenIn).approve(address(SWAP_ROUTER), amountIn);

        ISwapRouter.ExactInputSingleParams memory params = ISwapRouter.ExactInputSingleParams({
            tokenIn: tokenIn,
            tokenOut: tokenOut,
            fee: 3000, // 0.3% pool fee
            recipient: address(this),
            deadline: block.timestamp + 300,
            amountIn: amountIn,
            amountOutMinimum: amountOutMinimum,
            sqrtPriceLimitX96: 0
        });

        amountOut = SWAP_ROUTER.exactInputSingle(params);
    }

    /**
     * @dev Withdraw ERC20 tokens. Only owner.
     */
    function withdraw(address _token) external onlyOwner nonReentrant {
        uint256 balance = IERC20(_token).balanceOf(address(this));
        require(balance > 0, "Nothing to withdraw");
        IERC20(_token).transfer(owner(), balance);
    }

    /**
     * @dev Withdraw native currency. Only owner.
     */
    function withdrawNative() external onlyOwner nonReentrant {
        uint256 balance = address(this).balance;
        require(balance > 0, "Nothing to withdraw");
        (bool success, ) = payable(owner()).call{value: balance}("");
        require(success, "Transfer failed");
    }

    receive() external payable {}
}

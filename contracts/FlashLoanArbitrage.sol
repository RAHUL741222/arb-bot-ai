// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "@openzeppelin/contracts/security/ReentrancyGuard.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC20/utils/SafeERC20.sol";
import "@uniswap/v3-periphery/contracts/interfaces/ISwapRouter.sol";
import "@aave/core-v3/contracts/flashloan/interfaces/IFlashLoanReceiver.sol";
import "@aave/core-v3/contracts/interfaces/IPool.sol";

contract FlashLoanArbitrage is ReentrancyGuard, Ownable, IFlashLoanReceiver {
    using SafeERC20 for IERC20;

    IPool public immutable POOL;
    ISwapRouter public immutable SWAP_ROUTER;

    uint256 public maxSlippageBps = 500; // 5% dynamic
    address public treasuryWallet;
    uint256 public treasuryShareBps = 1000; // 10%

    mapping(address => bool) public whitelistedExecutors;

    event ArbitrageExecuted(address token, uint256 profit, uint256 timestamp);
    event ArbitrageFailed(string reason);

    modifier onlyWhitelisted() {
        require(whitelistedExecutors[msg.sender] || msg.sender == owner(), "Not whitelisted");
        _;
    }

    constructor(address _pool, address _swapRouter, address _treasury) {
        POOL = IPool(_pool);
        SWAP_ROUTER = ISwapRouter(_swapRouter);
        treasuryWallet = _treasury;
        whitelistedExecutors[msg.sender] = true;
    }

    function setWhitelistedExecutor(address _executor, bool _status) external onlyOwner {
        whitelistedExecutors[_executor] = _status;
    }

    function setMaxSlippage(uint256 _bps) external onlyOwner {
        require(_bps <= 2000, "Slippage too high");
        maxSlippageBps = _bps;
    }

    function requestFlashLoan(
        address _token,
        uint256 _amount,
        address _tokenToBuy,
        uint256 _minProfit
    ) external onlyWhitelisted nonReentrant {
        bytes memory params = abi.encode(_tokenToBuy, _minProfit);

        address[] memory assets = new address[](1);
        assets[0] = _token;

        uint256[] memory amounts = new uint256[](1);
        amounts[0] = _amount;

        uint256[] memory interestRateModes = new uint256[](1);
        interestRateModes[0] = 0;

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

    function executeOperation(
        address[] calldata assets,
        uint256[] calldata amounts,
        uint256[] calldata premiums,
        address initiator,
        bytes calldata params
    ) external override nonReentrant returns (bool) {
        require(msg.sender == address(POOL), "Invalid caller");

        (address tokenToBuy, uint256 minProfit) = abi.decode(params, (address, uint256));

        uint256 amountOwed = amounts[0] + premiums[0];

        try this._executeArbitrage(assets[0], tokenToBuy, amounts[0]) returns (uint256 amountOut) {
            require(amountOut >= amountOwed + minProfit, "Profit too low");

            uint256 profit = amountOut - amountOwed;

            // Profit sharing
            uint256 treasuryAmount = (profit * treasuryShareBps) / 10000;
            if (treasuryAmount > 0) {
                IERC20(assets[0]).safeTransfer(treasuryWallet, treasuryAmount);
            }

            IERC20(assets[0]).approve(address(POOL), amountOwed);
            emit ArbitrageExecuted(assets[0], profit - treasuryAmount, block.timestamp);
            return true;
        } catch {
            emit ArbitrageFailed("Arbitrage execution failed");
            revert("Execution reverted");
        }
    }

    function _executeArbitrage(
        address tokenIn,
        address tokenOut,
        uint256 amountIn
    ) external returns (uint256) {
        require(msg.sender == address(this), "Internal only");

        // 1. Swap In -> Out
        uint256 midAmount = _swap(tokenIn, tokenOut, amountIn, 0);

        // 2. Swap Out -> In
        uint256 finalAmount = _swap(tokenOut, tokenIn, midAmount, (amountIn * (10000 - maxSlippageBps)) / 10000);

        return finalAmount;
    }

    function _swap(
        address tokenIn,
        address tokenOut,
        uint256 amountIn,
        uint256 amountOutMinimum
    ) internal returns (uint256 amountOut) {
        IERC20(tokenIn).safeApprove(address(SWAP_ROUTER), amountIn);

        ISwapRouter.ExactInputSingleParams memory params = ISwapRouter.ExactInputSingleParams({
            tokenIn: tokenIn,
            tokenOut: tokenOut,
            fee: 3000,
            recipient: address(this),
            deadline: block.timestamp + 300,
            amountIn: amountIn,
            amountOutMinimum: amountOutMinimum,
            sqrtPriceLimitX96: 0
        });

        amountOut = SWAP_ROUTER.exactInputSingle(params);
    }

    function withdraw(address _token) external onlyOwner nonReentrant {
        uint256 balance = IERC20(_token).balanceOf(address(this));
        IERC20(_token).safeTransfer(owner(), balance);
    }

    receive() external payable {}
}

// SPDX-License-Identifier: MIT
pragma solidity ^0.8.10;

import "https://github.com/aave/aave-v3-core/blob/master/contracts/flashloan/base/FlashLoanSimpleReceiverBase.sol";
import "https://github.com/aave/aave-v3-core/blob/master/contracts/interfaces/IPoolAddressesProvider.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/IERC20.sol";

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

contract FlashLoanArbitrage is FlashLoanSimpleReceiverBase {
    address public owner;

    // Polygon Mainnet Addresses
    ISwapRouter public immutable uniswapRouter = ISwapRouter(0xE592427A0AEce92De3Edee1F18E0157C05861564);
    IUniswapV2Router public immutable quickswapRouter = IUniswapV2Router(0xa5E0829CaCEd8fFDD4De3c43696c57F7D7A678ff);

    constructor(address _addressProvider) FlashLoanSimpleReceiverBase(IPoolAddressesProvider(_addressProvider)) {
        owner = msg.sender;
    }

    function requestFlashLoan(address _token, uint256 _amount) public {
        require(msg.sender == owner, "Only owner can call");
        POOL.flashLoanSimple(address(this), _token, _amount, "", 0);
    }

    function executeOperation(
        address asset,
        uint256 amount,
        uint256 premium,
        address,
        bytes calldata
    ) external override returns (bool) {

        // ১. Uniswap V3 এ সোয়াপ (Example: Buy WMATIC)
        address wmatic = 0x0d500B1d8E8eF31E21C99d1Db9A6444d3ADf1270;
        IERC20(asset).approve(address(uniswapRouter), amount);

        ISwapRouter.ExactInputSingleParams memory params = ISwapRouter.ExactInputSingleParams({
            tokenIn: asset,
            tokenOut: wmatic,
            fee: 3000,
            recipient: address(this),
            deadline: block.timestamp,
            amountIn: amount,
            amountOutMinimum: 0,
            sqrtPriceLimitX96: 0
        });

        try uniswapRouter.exactInputSingle(params) returns (uint256 amountReceived) {
            // ২. QuickSwap এ আবার বিক্রি করে লোন পরিশোধের টোকেন ফেরত আনা
            IERC20(wmatic).approve(address(quickswapRouter), amountReceived);

            address[] memory path = new address[](2);
            path[0] = wmatic;
            path[1] = asset;

            quickswapRouter.swapExactTokensForTokens(
                amountReceived,
                0,
                path,
                address(this),
                block.timestamp
            );
        } catch {
            // Swap failed, but we still need to repay Aave
        }

        // ৩. লোন এবং ফি ফেরত দেওয়া
        uint256 amountToRepay = amount + premium;
        IERC20(asset).approve(address(POOL), amountToRepay);

        return true;
    }

    function withdraw(address _token) external {
        require(msg.sender == owner, "Only owner");
        IERC20 token = IERC20(_token);
        token.transfer(owner, token.balanceOf(address(this)));
    }

    function withdrawPOL() external {
        require(msg.sender == owner, "Only owner");
        (bool success, ) = payable(owner).call{value: address(this).balance}("");
        require(success, "Transfer failed");
    }

    receive() external payable {}
}

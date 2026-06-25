package com.example.data.model

data class NetworkConfig(
    val name: String,
    val rpcUrl: String,
    val chainId: Long,
    val flashLoanPool: String,
    val swapRouter: String,
    val nativeCurrency: String
)

object NetworkConfigs {
    val POLYGON = NetworkConfig(
        name = "Polygon",
        rpcUrl = "https://polygon-rpc.com",
        chainId = 137,
        flashLoanPool = "0x794a61358D6845594F94dc1DB02A252b5b4814aD", // Aave V3 Polygon
        swapRouter = "0xE592427A0AEce92De3Edee1F18E0157C05861564",
        nativeCurrency = "MATIC"
    )
    
    val BSC = NetworkConfig(
        name = "BSC",
        rpcUrl = "https://bsc-dataseed.binance.org/",
        chainId = 56,
        flashLoanPool = "0x6807dc923806fE8Fd134338EABCA509979a7e0cB", // Aave V3 BSC
        swapRouter = "0x1b81D678ffb9C0263b24A97847620C99d213eB14",
        nativeCurrency = "BNB"
    )
}

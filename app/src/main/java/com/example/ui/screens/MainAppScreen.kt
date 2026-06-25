package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.GeminiClient
import com.example.simulator.LogType
import com.example.simulator.SimState
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// Custom Theme Palette (Professional Polish Theme)
val CyberBackground = Color(0xFFF7F9FF)
val CyberSurface = Color(0xFFFFFFFF)
val CyberSurfaceLight = Color(0xFFE1E2EC)
val CyberPrimary = Color(0xFF0061A4) // Deep professional blue
val CyberSecondary = Color(0xFF2E7D32) // Forest green
val CyberAccent = Color(0xFF001D35) // Deep navy
val CyberError = Color(0xFFBA1A1A) // Crimson red
val CyberTextPrimary = Color(0xFF1A1C1E)
val CyberTextSecondary = Color(0xFF44474F)

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF1F0F4),
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPaddingForCustom()
            ) {
                val tabs = listOf(
                    NavigationTab("সিমুলেটর", "bot_sim", Icons.Default.PlayArrow),
                    NavigationTab("এআই উপদেষ্টা", "ai_advisor", Icons.Default.Search),
                    NavigationTab("ক্যালকুলেটর", "calculator", Icons.Default.Info),
                    NavigationTab("লার্নিং হাব", "learning_hub", Icons.Default.Share)
                )
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberPrimary,
                            selectedTextColor = CyberPrimary,
                            indicatorColor = Color(0xFFD1E4FF),
                            unselectedIconColor = CyberTextSecondary,
                            unselectedTextColor = CyberTextSecondary
                        ),
                        modifier = Modifier.testTag("tab_${tab.tag}")
                    )
                }
            }
        },
        containerColor = CyberBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar
            HeaderBar()

            // Main View Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> BotSimulatorTab(viewModel)
                    1 -> AIAdvisorTab(viewModel)
                    2 -> CalculatorTab(viewModel)
                    3 -> LearningHubTab()
                }
            }
        }
    }
}

@Composable
fun Modifier.navigationBarsPaddingForCustom(): Modifier {
    // Avoid importing window insets library directly if not needed, but can just use standard padding
    return this.background(Color(0xFFF1F0F4))
}

data class NavigationTab(val title: String, val tag: String, val icon: ImageVector)

@Composable
fun HeaderBar() {
    Surface(
        color = CyberSurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cyber Pulsing Dot Indicator
                val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CyberPrimary.copy(alpha = alpha))
                        .border(1.dp, CyberPrimary, CircleShape)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "FlashArb DeFi Bot",
                    color = CyberTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Tech Banner Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyberSurfaceLight)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AAVE V3 / UNISWAP",
                    color = CyberSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ---------------------- 1. BOT SIMULATOR TAB ----------------------

@Composable
fun BotSimulatorTab(viewModel: MainViewModel) {
    val simState by viewModel.simState.collectAsState()
    val logs by viewModel.simLogs.collectAsState()
    val scope = rememberCoroutineScope()
    val terminalListState = rememberLazyListState()
    
    var showSettings by remember { mutableStateOf(false) }
    var tempWalletAddress by remember { mutableStateOf(simState.walletAddress) }
    var tempContractAddress by remember { mutableStateOf(simState.contractAddress) }
    var tempPrivateKey by remember { mutableStateOf("") }

    // Auto scroll terminal to top when new logs arrive (since we prepended, index 0 is latest, but let's scroll cleanly)
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            terminalListState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Settings / Configuration Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStrokeCustom(1.dp, CyberPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ আসল কানেকশন ও অটো-ট্রেড সেটিংস",
                        color = CyberPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(
                            imageVector = if (showSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle"
                        )
                    }
                }
                
                if (showSettings) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempWalletAddress,
                        onValueChange = { tempWalletAddress = it },
                        label = { Text("আপনার পাবলিক ওয়ালেট অ্যাড্রেস", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempContractAddress,
                        onValueChange = { tempContractAddress = it },
                        label = { Text("ডেপ্লয় করা কন্ট্রাক্ট অ্যাড্রেস", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("নিরাপত্তা সতর্কবার্তা: আপনার প্রাইভেট কী ফোনে এনক্রিপ্টেড থাকে এবং কোনো সার্ভারে পাঠানো হয় না। আসল ট্রেড করার জন্য এটি প্রয়োজন।", color = CyberError, fontSize = 10.sp, lineHeight = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempPrivateKey,
                        onValueChange = { tempPrivateKey = it },
                        label = { Text("ওয়ালেট প্রাইভেট কী (Private Key)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberError)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("অটো-ট্রেড মুড (Auto-Trade)", color = CyberTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        androidx.compose.material3.Switch(
                            checked = simState.isAutoTradeEnabled,
                            onCheckedChange = { viewModel.toggleAutoTrade(it) },
                            modifier = Modifier.testTag("auto_trade_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { 
                            viewModel.updateWalletAddress(tempWalletAddress)
                            viewModel.updateContractAddress(tempContractAddress)
                            if (tempPrivateKey.isNotEmpty()) {
                                viewModel.updatePrivateKey(tempPrivateKey)
                            }
                            showSettings = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                    ) {
                        Text("সব সেভ করুন")
                    }
                }
            }
        }

        // Network and Wallet Status Box
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStrokeCustom(1.dp, CyberSurfaceLight)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ওয়ালেট ও গ্যাস ব্যালেন্স",
                    color = CyberPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "নেটওয়ার্ক ফি-র জন্য রাখা ব্যালেন্স ($2)",
                            color = CyberTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (simState.network.contains("Polygon")) {
                                "${String.format("%.6f", simState.nativeGasBalance)} MATIC"
                            } else {
                                "${String.format("%.6f", simState.nativeGasBalance)} BNB"
                            },
                            color = CyberTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${String.format("%.2f", simState.usdtBalance)} USDT",
                            color = CyberSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Network Changer Dropdown
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NetworkBadge(
                            text = "Polygon",
                            isSelected = simState.network.contains("Polygon"),
                            onClick = { viewModel.simulator.updateNetwork("Polygon (MATIC)") }
                        )
                        NetworkBadge(
                            text = "BSC",
                            isSelected = simState.network.contains("BSC"),
                            onClick = { viewModel.simulator.updateNetwork("BNB Smart Chain (BNB)") }
                        )
                    }
                }
            }
        }

        // Live Simulated Counters Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStrokeCustom(1.dp, CyberSurfaceLight)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "রিয়েল-টাইম পরিসংখ্যান (Simulated Stats)",
                    color = CyberPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Stats Grid
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem(
                        title = "মোট স্ক্যান",
                        value = "${simState.totalScans}",
                        iconColor = CyberPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        title = "মোট ট্রেড",
                        value = "${simState.totalTrades}",
                        iconColor = CyberAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        title = "সফল",
                        value = "${simState.successfulTrades}",
                        iconColor = CyberSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem(
                        title = "গ্যাসে বাতিল/ব্যর্থ",
                        value = "${simState.failedTrades}",
                        iconColor = CyberError,
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        title = "MEV ফ্রন্টরান",
                        value = "${simState.frontrunnedTrades}",
                        iconColor = CyberAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        title = "মোট লাভ (USD)",
                        value = "$${String.format("%.4f", simState.profitUSD)}",
                        iconColor = CyberSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Bot Parameter Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStrokeCustom(1.dp, CyberSurfaceLight)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "বট প্যারামিটার (Parameters)",
                    color = CyberPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Flash Loan Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ফ্ল্যাশ লোন অ্যামাউন্ট:", color = CyberTextSecondary, fontSize = 11.sp)
                    Text("$${String.format("%,d", simState.borrowAmountUSD.toInt())} USDT", color = CyberSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = simState.borrowAmountUSD.toFloat(),
                    onValueChange = { viewModel.simulator.updateBorrowAmount(it.toDouble()) },
                    valueRange = 100f..1000000f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberPrimary,
                        activeTrackColor = CyberPrimary,
                        inactiveTrackColor = CyberSurfaceLight
                    ),
                    modifier = Modifier.testTag("slider_borrow")
                )

                // Gas Price Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("লেনদেনের গ্যাস প্রাইজ (Gas Fee):", color = CyberTextSecondary, fontSize = 11.sp)
                    Text("${simState.gasPriceGwei} Gwei", color = CyberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = simState.gasPriceGwei.toFloat(),
                    onValueChange = { viewModel.simulator.updateGasPrice(it.toInt()) },
                    valueRange = 10f..300f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberPrimary,
                        activeTrackColor = CyberPrimary,
                        inactiveTrackColor = CyberSurfaceLight
                    ),
                    modifier = Modifier.testTag("slider_gas")
                )

                // Educational Disclaimer about gas/parameters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurfaceLight)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Alert",
                        tint = CyberAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "শিখন ফল: কম লোনে ($100) লাভ করলেও গ্যাস ফি-র কারণে নেট ক্ষতি হবে। ৫,০০,০০০ লোন নিয়ে কম গ্যাসে ট্রাই করলে MEV স্যান্ডউইচ বটের কাছে ফ্রন্টরান হবেন। গ্যারান্টিড!",
                        color = CyberAccent,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // Action Buttons (Start, Stop, Clear)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (simState.isRunning) {
                        viewModel.simulator.stop()
                    } else {
                        viewModel.simulator.start(scope)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("start_stop_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (simState.isRunning) CyberError else CyberSecondary
                )
            ) {
                Icon(
                    imageVector = if (simState.isRunning) Icons.Default.Clear else Icons.Default.PlayArrow,
                    contentDescription = "Control"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (simState.isRunning) "বট বন্ধ করুন" else "বট চালু করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }

            Button(
                onClick = { viewModel.simulator.resetStats() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("reset_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceLight)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = CyberTextPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("রিসেট", color = CyberTextPrimary, fontSize = 13.sp)
            }
        }

        // Live Simulated Terminal Logs Container
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            border = BorderStrokeCustom(1.dp, CyberSurfaceLight)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "লাইভ ট্রানজেকশন স্ক্রিন (Simulated Console)",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "লগ পরিষ্কার করুন",
                        color = CyberTextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { viewModel.simulator.clearLogs() }
                            .padding(2.dp)
                    )
                }

                HorizontalDivider(
                    color = CyberSurfaceLight,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "> বট চালু করুন... লাইভ আর্বিট্রেজ ডিবাগ ট্রানজেকশন এখানে দেখাবে...",
                            color = CyberTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = terminalListState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = false // logs are prepended so index 0 is newest. Let's list top-to-bottom.
                    ) {
                        items(logs, key = { it.id }) { log ->
                            TerminalLine(log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalLine(log: com.example.simulator.LogEntry) {
    val color = when (log.type) {
        LogType.SCAN -> Color(0xFF607D8B)
        LogType.INFO -> CyberPrimary
        LogType.WARNING -> Color(0xFFE65100)
        LogType.SUCCESS -> CyberSecondary
        LogType.ERROR -> CyberError
        LogType.LOAN -> Color(0xFF6A1B9A) // Deep Purple
        LogType.SWAP -> Color(0xFF00695C) // Deep Teal
        LogType.REPAY -> Color(0xFF283593) // Indigo
        LogType.MEV_ATTACK -> Color(0xFFD84315) // Rust Red
    }

    val typeTag = when (log.type) {
        LogType.SCAN -> "[SCAN]"
        LogType.INFO -> "[INFO]"
        LogType.WARNING -> "[WARN]"
        LogType.SUCCESS -> "[OK  ]"
        LogType.ERROR -> "[FAIL]"
        LogType.LOAN -> "[LOAN]"
        LogType.SWAP -> "[SWAP]"
        LogType.REPAY -> "[PYBK]"
        LogType.MEV_ATTACK -> "[⚠️MEV]"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "${log.timestamp}  ",
            color = Color(0xFF607D8B),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Text(
            text = "$typeTag  ",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = log.message,
            color = if (log.type == LogType.SCAN) Color(0xFF607D8B) else CyberTextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NetworkBadge(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) CyberPrimary else CyberSurfaceLight)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else CyberTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatItem(title: String, value: String, iconColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CyberSurfaceLight)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = CyberTextSecondary,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = iconColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------- 2. AI ADVISOR TAB ----------------------

@Composable
fun AIAdvisorTab(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val chatInput by viewModel.chatInput.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    val context = LocalContext.current
    val chatListState = rememberLazyListState()

    // Scroll chat to the last message when a new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            chatListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat History List
        LazyColumn(
            state = chatListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                ChatMessageRow(message, context)
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = CyberPrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "স্মার্ট গ্রাউন্ডিং সার্চ ও এআই চিন্তা করছে...",
                            color = CyberTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Quick Suggestion Questions Area
        QuickQuestionsRow(
            onSelect = { viewModel.sendChatMessage(it) },
            enabled = !isLoading
        )

        // Text Input Bar
        Surface(
            color = CyberSurface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { viewModel.onChatInputChange(it) },
                    placeholder = { Text("এআই-কে প্রশ্ন করুন (যেমন: সোলিডিটি কন্ট্রাক্ট তৈরি)", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text_field"),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (chatInput.trim().isNotEmpty() && !isLoading) {
                                viewModel.sendChatMessage(chatInput)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary,
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = CyberSurfaceLight,
                        cursorColor = CyberPrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.sendChatMessage(chatInput) },
                    enabled = chatInput.trim().isNotEmpty() && !isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (chatInput.trim().isNotEmpty() && !isLoading) CyberPrimary else CyberSurfaceLight)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (chatInput.trim().isNotEmpty() && !isLoading) Color.White else CyberTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickQuestionsRow(onSelect: (String) -> Unit, enabled: Boolean) {
    val questions = listOf(
        "Aave V3 ফ্ল্যাশ লোন কোড তৈরি করো",
        "কীভাবে Remix-এ কন্ট্রাক্ট রান করব?",
        "আসল বটের প্রতিযোগিতার আসল চিত্র কী?",
        "Polygon ও BSC-তে আর্বিট্রেজ পার্থক্য"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        questions.forEach { question ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberSurfaceLight)
                    .clickable(enabled = enabled) { onSelect(question) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = question,
                    color = CyberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ChatMessageRow(message: ChatMessage, context: Context) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgBrush = if (message.isUser) {
        Brush.linearGradient(listOf(Color(0xFF0061A4), Color(0xFF1E88E5)))
    } else {
        Brush.linearGradient(listOf(CyberSurface, CyberSurface))
    }
    val textColor = if (message.isUser) Color.White else CyberTextPrimary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // Chat Bubble Container
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 2.dp,
                        bottomEnd = if (message.isUser) 2.dp else 16.dp
                    )
                )
                .background(bgBrush)
                .border(
                    width = 1.dp,
                    color = if (message.isUser) Color.Transparent else CyberSurfaceLight,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 2.dp,
                        bottomEnd = if (message.isUser) 2.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                // Parse and render custom structured text (Code Blocks vs regular Text)
                RenderMessageContent(message.text, textColor, context)

                // Render Grounding References
                if (!message.isUser && message.searchSources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = CyberSurfaceLight)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🔍 গুগল সার্চ গ্রাউন্ডিং উৎসসমূহ:",
                        color = CyberPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    message.searchSources.forEach { source ->
                        Text(
                            text = "• ${source.title}",
                            color = CyberSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 12.sp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenderMessageContent(text: String, defaultColor: Color, context: Context) {
    // If text contains a markdown code block (i.e. starts with ```)
    if (text.contains("```")) {
        val parts = text.split("```")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block index (e.g. 1, 3, 5)
                val lines = part.lines()
                val lang = lines.firstOrNull()?.trim() ?: ""
                val code = lines.drop(1).joinToString("\n")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF070912))
                        .border(1.dp, Color(0xFF252836), RoundedCornerShape(8.dp))
                ) {
                    // Header of code block
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF151825))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang.isNotEmpty()) lang.uppercase() else "SOLIDITY",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Copied Smart Contract", code)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "কোড কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_copy),
                                contentDescription = "Copy",
                                tint = CyberSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("কপি", color = CyberSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Code text scrollable
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            text = code,
                            color = Color(0xFFA5D6A7),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            } else {
                // Regular text index
                if (part.trim().isNotEmpty()) {
                    Text(
                        text = part,
                        color = defaultColor,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    } else {
        // Plain message text
        Text(
            text = text,
            color = defaultColor,
            fontSize = 13.sp,
            lineHeight = 17.sp
        )
    }
}

// ---------------------- 3. CALCULATOR TAB ----------------------

@Composable
fun CalculatorTab(viewModel: MainViewModel) {
    val inputs by viewModel.calcInputs.collectAsState()
    val result by viewModel.calcResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStrokeCustom(1.dp, CyberSurfaceLight)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "আর্বিট্রেজ লাভ/ক্ষতি ক্যালকুলেটর",
                    color = CyberPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ফ্ল্যাশ লোন ও গ্যাস প্রাইস হিসাব করার জন্য এন্ট্রিগুলো পরিবর্তন করুন।",
                    color = CyberTextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Network Selection Row inside Calculator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (inputs.network.contains("Polygon")) CyberSurfaceLight else Color.Transparent)
                            .border(1.dp, if (inputs.network.contains("Polygon")) CyberPrimary else CyberSurfaceLight, RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateCalcInputs { it.copy(network = "Polygon (MATIC)") } }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Polygon Mainnet", color = CyberTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (inputs.network.contains("BSC")) CyberSurfaceLight else Color.Transparent)
                            .border(1.dp, if (inputs.network.contains("BSC")) CyberPrimary else CyberSurfaceLight, RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateCalcInputs { it.copy(network = "BNB Smart Chain (BNB)") } }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("BSC Mainnet", color = CyberTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Flash Loan Amount Input
                CalculatorTextField(
                    label = "ফ্ল্যাশ লোন সাইজ (USDT)",
                    value = inputs.loanAmount,
                    tag = "input_loan_amount",
                    onValueChange = { newVal ->
                        viewModel.updateCalcInputs { it.copy(loanAmount = newVal) }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Buy / Sell prices on DEX
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        CalculatorTextField(
                            label = "কেনা দাম DEX A ($)",
                            value = inputs.buyPrice,
                            tag = "input_buy_price",
                            onValueChange = { newVal ->
                                viewModel.updateCalcInputs { it.copy(buyPrice = newVal) }
                            }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        CalculatorTextField(
                            label = "বিক্রি দাম DEX B ($)",
                            value = inputs.sellPrice,
                            tag = "input_sell_price",
                            onValueChange = { newVal ->
                                viewModel.updateCalcInputs { it.copy(sellPrice = newVal) }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Gas Input Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        CalculatorTextField(
                            label = "গ্যাস প্রাইস (Gwei)",
                            value = inputs.gasPriceGwei,
                            tag = "input_gas_gwei",
                            onValueChange = { newVal ->
                                viewModel.updateCalcInputs { it.copy(gasPriceGwei = newVal) }
                            }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        CalculatorTextField(
                            label = "গ্যাস লিমিট (Units)",
                            value = inputs.gasLimit,
                            tag = "input_gas_limit",
                            onValueChange = { newVal ->
                                viewModel.updateCalcInputs { it.copy(gasLimit = newVal) }
                            }
                        )
                    }
                }
            }
        }

        // Result Card Breakdown
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (result.isProfitable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ),
            border = BorderStrokeCustom(
                width = 1.dp,
                color = if (result.isProfitable) CyberSecondary else CyberError
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "হিসাবের বিশ্লেষণ (Math Summary)",
                    color = if (result.isProfitable) CyberSecondary else CyberError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                MathRow(label = "গ্রস প্রফিট (ট্রেডের লাভ):", value = "$${String.format("%.4f", result.grossProfit)}")
                MathRow(label = "Aave ফ্ল্যাশ লোন ফি (0.09%):", value = "-$${String.format("%.4f", result.loanFee)}")
                MathRow(
                    label = "ব্লকচেইন গ্যাস খরচ:",
                    value = "-$${String.format("%.4f", result.gasCost)} (${inputs.network.substringBefore(" ")})"
                )

                HorizontalDivider(
                    color = CyberSurfaceLight,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "আসল নিট লাভ (Net Profit):",
                        color = CyberTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${if (result.netProfit > 0) "+" else ""}$${String.format("%.4f", result.netProfit)} USD",
                        color = if (result.isProfitable) CyberSecondary else CyberError,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Render dynamic alerts / warnings (low slippage, MEV attacks, negative profits)
                result.warningMessage?.let { warning ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFF3E0))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = warning,
                            color = Color(0xFFE65100),
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorTextField(label: String, value: String, tag: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, color = CyberTextSecondary) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = CyberTextPrimary,
            unfocusedTextColor = CyberTextPrimary,
            focusedBorderColor = CyberPrimary,
            unfocusedBorderColor = CyberSurfaceLight,
            focusedLabelColor = CyberPrimary,
            cursorColor = CyberPrimary
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    )
}

@Composable
fun MathRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = CyberTextSecondary, fontSize = 11.sp)
        Text(text = value, color = CyberTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

// ---------------------- 4. LEARNING HUB TAB ----------------------

@Composable
fun LearningHubTab() {
    val tutorials = listOf(
        TutorialSection(
            title = "১. ফ্ল্যাশ লোন আসলে কী?",
            subtitle = "Flash Loan: DeFi-র জাদুকরী ঋণ পদ্ধতি",
            content = "ফ্ল্যাশ লোন হল ডিসেন্ট্রালাইজড ফাইন্যান্স (DeFi)-এর এমন একটি প্রক্রিয়া যেখানে কোনো জামানত বা বন্ধক (Collateral) ছাড়াই লাখ লাখ ডলার ধার নেওয়া সম্ভব।\n\nশর্ত মাত্র একটি: ধার নেওয়া, ট্রেড সম্পন্ন করা এবং ধার পরিশোধ করা—সবকিছু ব্লকচেইনের একটি মাত্র ব্লকে বা লেনদেনে (Single Transaction) সম্পূর্ণ করতে হবে। যদি লোন শোধ না করে আর্বিট্রেজ ব্যর্থ হয়, তবে পুরো ট্রানজেকশনটি 'রিভার্ট' বা বাতিল হয়ে যায়। যেন লেনদেনটি কখনোই ঘটেনি! ঋণদাতার কোনো রিস্ক বা লোকসান থাকে না।"
        ),
        TutorialSection(
            title = "২. আর্বিট্রেজ এবং দামের পার্থক্য",
            subtitle = "DEX Arbitrage: বিভিন্ন এক্সচেঞ্জে দামের অসমতা",
            content = "ধরা যাক, QuickSwap এক্সচেঞ্জে MATIC-এর দাম $0.58 এবং SushiSwap-এ দাম $0.59। এটি একটি আর্বিট্রেজ সুযোগ।\n\nফ্ল্যাশ লোন আর্বিট্রেজ বটের কাজ হল:\n১. Aave থেকে ১০,০০০ MATIC ফ্ল্যাশ লোন হিসেবে নেওয়া।\n২. QuickSwap এ সেই ১০,০০০ MATIC বিক্রি করে $5,800 তুলে নেওয়া।\n৩. SushiSwap এ সেই $5,800 দিয়ে MATIC কেনা। এতে MATIC পাওয়া যাবে ১০,০০০-এর বেশি (ধরা যাক ১০,১৭১ MATIC)।\n৪. Aave-র ১০,০০০ MATIC ও সামান্য ফিস (0.09% অর্থাৎ 9 MATIC) পরিশোধ করা।\n৫. বাকি থাকা ১৬২ MATIC আর্বিট্রেজ বটের আসল নিট লাভ হিসেবে ওয়ালেটে জমা হওয়া।"
        ),
        TutorialSection(
            title = "৩. $২ থেকে $৫০০০ করার বাস্তবতা কী?",
            subtitle = "The Cold Reality: অবাস্তব স্বপ্ন বনাম গ্যাস ও প্রতিযোগিতা",
            content = "বলা হয় যে $২ খরচ করলেই বট আর্বিট্রেজ করে $৫০০০ করতে পারবে। এর বাস্তবতা নিম্নরূপ:\n\n১. গ্যাস ফি বনাম লাভ: যেকোনো আর্বিট্রেজ লেনদেন করতে কম-বেশি গ্যাস ফি দিতে হয় (Polygon-এ $০.০১ থেকে $০.০৫, BSC-তে $০.১০ থেকে $০.৫০)। আর্বিট্রেজ থেকে লাভ যদি $০.০২ হয়, আর গ্যাস ফি যদি $০.০৫ হয়, তবে ট্রেডটি থেকে লাভ হওয়ার বদলে $০.০৩ নেট লোকসান হবে!\n\n২. MEV স্যান্ডউইচ ও ফ্রন্টরানিং: ব্লকচেইনের ট্রানজেকশন মেমপুলে (Mempool) ওয়েটিংয়ে থাকে। সেখানে বড় বড় MEV বট পাহারা দেয়। লাভজনক ট্রানজেকশন দেখলেই তারা বেশি গ্যাস ফি দিয়ে আপনার ট্রানজেকশনের আগে নিজেদের ট্রানজেকশন সম্পন্ন করে আপনার সুযোগটি কেড়ে নেয়।\n\n৩. উপসংহার: $২ শুধু গ্যাস ফি-র জন্য ওয়ালেটে রাখার নেটিভ টোকেন (যেমন MATIC/BNB)। বট দিয়ে শেখার উদ্দেশ্যে কম ব্যালেন্স দিয়ে ট্রাই করা সম্ভব, কিন্তু প্রতিযোগিতার বাজারে কোটি কোটি ডলারের বটের সাথে লড়াই করে $২ দিয়ে $৫০০০ লাভ করা প্রায় অসম্ভব।"
        ),
        TutorialSection(
            title = "৪. কীভাবে নিজের আসল বট ডেপ্লয় করবেন?",
            subtitle = "Developer Roadmap: রিমিক্স ও সলিডিটি কোড গাইড",
            content = "১. MetaMask ওয়ালেটে Polygon RPC কনফিগার করে কিছু MATIC রাখুন।\n২. Remix IDE (remix.ethereum.org) এ যান।\n৩. একটি নতুন ফাইল তৈরি করে আমাদের 'এআই উপদেষ্টা' থেকে জেনারেট করা সোলিডিটি কন্ট্রাক্ট পেস্ট করুন।\n৪. Solidity compiler v0.8.x দিয়ে কোডটি কম্পাইল করুন।\n৫. Deployer Environment হিসেবে 'Injected Provider - MetaMask' নির্বাচন করুন এবং কন্ট্রাক্টটি ডেপ্লয় করুন।\n৬. একটি Node.js/Python স্ক্রিপ্ট তৈরি করুন যা QuickSwap ও SushiSwap-এর পুলের মূল্য অনবরত স্ক্যান করবে। সুযোগ পাওয়া মাত্রই কন্ট্রাক্টটির 'executeArbitrage' ফাংশনটি কল করবে।"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "ফ্ল্যাশ লোন ও আর্বিট্রেজ মাষ্টারক্লাস",
                color = CyberPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "DeFi প্রোটোকল আর্বিট্রেজ কিভাবে কাজ করে এবং এর ভেতরের আসল সত্যটি শিখুন।",
                color = CyberTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(tutorials) { tutorial ->
            ExpandableTutorialCard(tutorial)
        }
    }
}

data class TutorialSection(val title: String, val subtitle: String, val content: String)

@Composable
fun ExpandableTutorialCard(tutorial: TutorialSection) {
    var expanded by remember { mutableStateOf(false) }
    val arrowIcon = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        border = BorderStrokeCustom(1.dp, if (expanded) CyberPrimary else CyberSurfaceLight)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tutorial.title,
                        color = if (expanded) CyberPrimary else CyberTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tutorial.subtitle,
                        color = CyberTextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = arrowIcon,
                    contentDescription = "Expand",
                    tint = CyberPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = CyberSurfaceLight)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = tutorial.content,
                        color = CyberTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }
    }
}

// Support Border Stroke custom object for backwards compatibility with Compose borders
@Composable
fun BorderStrokeCustom(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)

package com.yourcompany.flasharb.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.yourcompany.flasharb.api.GeminiClient
import com.yourcompany.flasharb.simulator.LogType
import com.yourcompany.flasharb.simulator.SimState
import com.yourcompany.flasharb.ui.viewmodel.ChatMessage
import com.yourcompany.flasharb.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// Custom Theme Palette
val CyberBackground = Color(0xFFF7F9FF)
val CyberSurface = Color(0xFFFFFFFF)
val CyberSurfaceLight = Color(0xFFE1E2EC)
val CyberPrimary = Color(0xFF0061A4)
val CyberSecondary = Color(0xFF2E7D32)
val CyberAccent = Color(0xFF001D35)
val CyberError = Color(0xFFBA1A1A)
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
                tonalElevation = 8.dp
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
                        label = { Text(tab.title, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
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
            HeaderBar()

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
            Text(
                text = "FlashArb DeFi Bot",
                color = CyberTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun BotSimulatorTab(viewModel: MainViewModel) {
    // Current UI logic...
    Text("Simulator Tab - Implementation in Progress")
}

@Composable
fun AIAdvisorTab(viewModel: MainViewModel) {
    Text("AI Advisor Tab")
}

@Composable
fun CalculatorTab(viewModel: MainViewModel) {
    Text("Calculator Tab")
}

@Composable
fun LearningHubTab() {
    Text("Learning Hub Tab")
}

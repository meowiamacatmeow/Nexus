package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.ui.NexusNavDestination
import com.example.nexus.ui.NexusViewModel
import com.example.nexus.ui.components.NexusHeader
import com.example.nexus.ui.screens.AgentsScreen
import com.example.nexus.ui.screens.ChatScreen
import com.example.nexus.ui.screens.DashboardScreen
import com.example.nexus.ui.screens.FileManagerScreen
import com.example.nexus.ui.screens.MemoryScreen
import com.example.nexus.ui.screens.SettingsScreen
import com.example.nexus.ui.screens.TerminalScreen
import com.example.nexus.ui.screens.ToolsScreen
import com.example.nexus.ui.theme.NexusBg
import com.example.nexus.ui.theme.NexusBlue
import com.example.nexus.ui.theme.NexusBorder
import com.example.nexus.ui.theme.NexusCyan
import com.example.nexus.ui.theme.NexusMuted
import com.example.nexus.ui.theme.NexusPanel
import com.example.nexus.ui.theme.NexusPanel2
import com.example.nexus.ui.theme.NexusText
import com.example.nexus.ui.theme.NexusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexusTheme {
                NexusMainApp()
            }
        }
    }
}

@Composable
fun NexusMainApp(viewModel: NexusViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 640

    // Handle back button: if on a sub-screen, return to Dashboard
    BackHandler(enabled = uiState.currentDestination != NexusNavDestination.DASHBOARD) {
        viewModel.navigateTo(NexusNavDestination.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusBg),
        containerColor = NexusBg,
        topBar = {
            NexusHeader(
                providerType = uiState.activeProviderType,
                onToggleProvider = { viewModel.toggleProvider() },
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            if (!isWideScreen) {
                NexusCompactBottomNav(
                    currentDestination = uiState.currentDestination,
                    onSelect = { viewModel.navigateTo(it) }
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWideScreen) {
                NexusWideNavigationRail(
                    currentDestination = uiState.currentDestination,
                    onSelect = { viewModel.navigateTo(it) }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(NexusBg)
            ) {
                when (uiState.currentDestination) {
                    NexusNavDestination.DASHBOARD -> DashboardScreen(uiState, viewModel)
                    NexusNavDestination.CHAT -> ChatScreen(uiState, viewModel)
                    NexusNavDestination.TERMINAL -> TerminalScreen(uiState, viewModel)
                    NexusNavDestination.TOOLS -> ToolsScreen(uiState, viewModel)
                    NexusNavDestination.FILES -> FileManagerScreen(uiState, viewModel)
                    NexusNavDestination.AGENTS -> AgentsScreen(uiState, viewModel)
                    NexusNavDestination.MEMORY -> MemoryScreen(uiState, viewModel)
                    NexusNavDestination.SETTINGS -> SettingsScreen(uiState, viewModel)
                }
            }
        }
    }
}

fun getNavIcon(destination: NexusNavDestination): ImageVector {
    return when (destination) {
        NexusNavDestination.DASHBOARD -> Icons.Default.Dashboard
        NexusNavDestination.CHAT -> Icons.Default.Chat
        NexusNavDestination.TERMINAL -> Icons.Default.Terminal
        NexusNavDestination.TOOLS -> Icons.Default.Build
        NexusNavDestination.FILES -> Icons.Default.Folder
        NexusNavDestination.AGENTS -> Icons.Default.SmartToy
        NexusNavDestination.MEMORY -> Icons.Default.Memory
        NexusNavDestination.SETTINGS -> Icons.Default.Settings
    }
}

@Composable
fun NexusCompactBottomNav(
    currentDestination: NexusNavDestination,
    onSelect: (NexusNavDestination) -> Unit
) {
    Surface(
        color = Color(0xFF101722),
        border = androidx.compose.foundation.BorderStroke(1.dp, NexusBorder),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexusNavDestination.values().forEach { dest ->
                val selected = currentDestination == dest
                val itemBg = if (selected) Color(0xFF16283C) else Color.Transparent
                val itemBorder = if (selected) Color(0xFF28527D) else Color.Transparent
                val contentColor = if (selected) NexusBlue else NexusMuted

                Surface(
                    color = itemBg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, itemBorder),
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .testTag("nav_item_${dest.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onSelect(dest) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getNavIcon(dest),
                            contentDescription = dest.label,
                            tint = contentColor,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = dest.label,
                            color = contentColor,
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NexusWideNavigationRail(
    currentDestination: NexusNavDestination,
    onSelect: (NexusNavDestination) -> Unit
) {
    NavigationRail(
        containerColor = Color(0xFF101722),
        contentColor = NexusText,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        NexusNavDestination.values().forEach { dest ->
            val selected = currentDestination == dest
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(dest) },
                icon = {
                    Icon(
                        imageVector = getNavIcon(dest),
                        contentDescription = dest.label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = { Text(dest.label, fontSize = 10.5.sp) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = NexusBlue,
                    selectedTextColor = NexusBlue,
                    indicatorColor = Color(0xFF16283C),
                    unselectedIconColor = NexusMuted,
                    unselectedTextColor = NexusMuted
                ),
                modifier = Modifier.testTag("rail_item_${dest.name.lowercase()}")
            )
        }
    }
}

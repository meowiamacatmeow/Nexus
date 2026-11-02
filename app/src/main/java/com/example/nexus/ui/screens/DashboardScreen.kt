package com.example.nexus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.model.AIProviderType
import com.example.nexus.model.TaskStatus
import com.example.nexus.ui.NexusNavDestination
import com.example.nexus.ui.NexusUiState
import com.example.nexus.ui.NexusViewModel
import com.example.nexus.ui.components.TaskCard
import com.example.nexus.ui.theme.NexusAmber
import com.example.nexus.ui.theme.NexusBlue
import com.example.nexus.ui.theme.NexusBorder
import com.example.nexus.ui.theme.NexusCyan
import com.example.nexus.ui.theme.NexusGreen
import com.example.nexus.ui.theme.NexusMuted
import com.example.nexus.ui.theme.NexusPanel
import com.example.nexus.ui.theme.NexusPanel2
import com.example.nexus.ui.theme.NexusRed
import com.example.nexus.ui.theme.NexusText

@Composable
fun DashboardScreen(
    uiState: NexusUiState,
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Environment Status Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusPanel),
                border = BorderStroke(1.dp, NexusBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SYSTEM OVERVIEW",
                                color = NexusBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "NEXUS Controlled Userspace",
                                color = NexusText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF13283E))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "OFFLINE-FIRST",
                                color = NexusCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Autonomous task engine and isolated sandbox active. The terminal functions independently, while all AI-proposed actions pass through centralized policy verification.",
                        color = NexusMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // 2. Hardware Resource Telemetry Grid
        item {
            val sys = uiState.systemInfo
            Text(
                text = "RESOURCE TELEMETRY",
                color = NexusMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // RAM Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = NexusPanel2),
                    border = BorderStroke(1.dp, NexusBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "MEMORY (RAM)", color = NexusMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${sys?.ramFreeMb ?: 0} MB",
                            color = NexusBlue,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "Free of ${sys?.ramTotalMb ?: 0} MB", color = NexusMuted, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        val ramProgress = (sys?.ramPercent ?: 0) / 100f
                        LinearProgressIndicator(
                            progress = { ramProgress },
                            color = if (ramProgress > 0.85f) NexusRed else NexusCyan,
                            trackColor = Color(0xFF0E1621),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Storage Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = NexusPanel2),
                    border = BorderStroke(1.dp, NexusBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "NEXUS STORAGE", color = NexusMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.1f GB".format(sys?.storageFreeGb ?: 0.0),
                            color = NexusGreen,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "Free on device", color = NexusMuted, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { 0.4f },
                            color = NexusGreen,
                            trackColor = Color(0xFF0E1621),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Battery / CPU Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = NexusPanel2),
                    border = BorderStroke(1.dp, NexusBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "PROCESSOR", color = NexusMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${sys?.cpuCores ?: 4} Cores",
                            color = NexusAmber,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (sys?.isCharging == true) "⚡ Charging (${sys.batteryLevel}%)" else "Battery: ${sys?.batteryLevel ?: 0}%",
                            color = NexusMuted,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = sys?.abi?.take(8) ?: "aarch64",
                            color = NexusMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 3. Active Autonomous Tasks
        val activeTasks = uiState.activeTasks.filter { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED }
        if (activeTasks.isNotEmpty()) {
            item {
                Text(
                    text = "ACTIVE AUTONOMOUS TASKS (${activeTasks.size})",
                    color = NexusAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeTasks.forEach { task ->
                        TaskCard(
                            task = task,
                            onRunNext = { viewModel.executeTaskNextStep(task.id) },
                            onCancel = { viewModel.cancelTask(task.id) }
                        )
                    }
                }
            }
        }

        // 4. Quick Action Objectives
        item {
            Text(
                text = "QUICK OBJECTIVES",
                color = NexusMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.navigateTo(NexusNavDestination.CHAT)
                        viewModel.sendChatMessage("Set up C++ and run a hello world command")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusPanel2),
                    border = BorderStroke(1.dp, NexusBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_setup_cpp")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = NexusGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Set up C++ & Run Hello World", color = NexusText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("LLVM clang++ compiler toolchain, main.cpp scaffolding, native execution", color = NexusMuted, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.navigateTo(NexusNavDestination.TERMINAL)
                        viewModel.executeTerminalCommand("nmap scanme.nmap.org")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusPanel2),
                    border = BorderStroke(1.dp, NexusBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_nmap_scan")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NexusAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Audit Network Host (nmap scanme.nmap.org)", color = NexusText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Centralized policy check, zero-trust approval, port telemetry report", color = NexusMuted, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.navigateTo(NexusNavDestination.CHAT)
                        viewModel.sendChatMessage("Set up a Python development environment with verified scaffolding and requirements.txt")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusPanel2),
                    border = BorderStroke(1.dp, NexusBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_setup_python")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Set up Python Development Environment", color = NexusText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Plan, sandbox inspect, create workspace, verify dependencies", color = NexusMuted, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.navigateTo(NexusNavDestination.TERMINAL)
                        viewModel.executeTerminalCommand("ls -la")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusPanel2),
                    border = BorderStroke(1.dp, NexusBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = NexusBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Open Sandboxed Terminal", color = NexusText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Execute shell commands directly in workspace", color = NexusMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 5. Security & Permission Health
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusPanel),
                border = BorderStroke(1.dp, NexusBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Security Posture", color = NexusText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "${uiState.auditLog.size} audit events",
                            color = NexusMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Active policy: Workspace (${if (uiState.permissions.workspace) "ON" else "OFF"}), Filesystem (${if (uiState.permissions.filesystem) "ON" else "OFF"}), Packages (${if (uiState.permissions.packages) "ON" else "OFF"}), Network (${if (uiState.permissions.network) "ON" else "OFF"}), Shell (${if (uiState.permissions.shellScripts) "ON" else "OFF"}).",
                        color = NexusMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(NexusNavDestination.SETTINGS) },
                        border = BorderStroke(1.dp, NexusBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manage Permissions & View Audit Logs", fontSize = 12.sp, color = NexusCyan)
                    }
                }
            }
        }
    }
}

package com.example.nexus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.model.Agent
import com.example.nexus.ui.NexusNavDestination
import com.example.nexus.ui.NexusUiState
import com.example.nexus.ui.NexusViewModel
import com.example.nexus.ui.theme.NexusBlue
import com.example.nexus.ui.theme.NexusBorder
import com.example.nexus.ui.theme.NexusCyan
import com.example.nexus.ui.theme.NexusGreen
import com.example.nexus.ui.theme.NexusMuted
import com.example.nexus.ui.theme.NexusPanel
import com.example.nexus.ui.theme.NexusPanel2
import com.example.nexus.ui.theme.NexusText

@Composable
fun AgentsScreen(
    uiState: NexusUiState,
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var agentName by remember { mutableStateOf("") }
    var agentTitle by remember { mutableStateOf("") }
    var agentDescription by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf("workspace", "filesystem")) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusPanel),
                border = BorderStroke(1.dp, NexusBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SPECIALIZED AI AGENTS",
                        color = NexusBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Persona Registry & Operational Boundaries",
                        color = NexusText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Each agent has strict instructions, allowed policy categories, and operational boundaries. Proposals outside an agent's scope are blocked with suggestions to delegate to the appropriate specialist.",
                        color = NexusMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF287AC9)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_agent_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Custom Agent", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "ACTIVE AGENTS (${uiState.agents.size})",
                color = NexusMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        items(uiState.agents, key = { it.id }) { agent ->
            AgentCard(
                agent = agent,
                onStartChat = {
                    viewModel.createNewConversation(agent.id)
                    viewModel.navigateTo(NexusNavDestination.CHAT)
                }
            )
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Configure Custom Agent", color = NexusText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = agentName,
                        onValueChange = { agentName = it },
                        placeholder = { Text("Agent Name (e.g. Audit Assistant)", color = NexusMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = NexusText, unfocusedTextColor = NexusText)
                    )
                    OutlinedTextField(
                        value = agentTitle,
                        onValueChange = { agentTitle = it },
                        placeholder = { Text("Role (e.g. Code Reviewer)", color = NexusMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = NexusText, unfocusedTextColor = NexusText)
                    )
                    OutlinedTextField(
                        value = agentDescription,
                        onValueChange = { agentDescription = it },
                        placeholder = { Text("Operational boundaries & system instructions...", color = NexusMuted) },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = NexusText, unfocusedTextColor = NexusText)
                    )

                    Text("Permitted Policy Categories:", color = NexusCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val availableCats = listOf("workspace", "filesystem", "packages", "network", "shellScripts")
                    availableCats.forEach { cat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedCategories.contains(cat),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) selectedCategories + cat else selectedCategories - cat
                                },
                                colors = CheckboxDefaults.colors(checkedColor = NexusBlue)
                            )
                            Text(cat, color = NexusText, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (agentName.isNotBlank()) {
                            viewModel.addCustomAgent(agentName, agentTitle, agentDescription, selectedCategories)
                            agentName = ""
                            agentTitle = ""
                            agentDescription = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Save Agent")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = NexusMuted) }
            },
            containerColor = NexusPanel
        )
    }
}

@Composable
fun AgentCard(
    agent: Agent,
    onStartChat: () -> Unit
) {
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
                    val iconVector = when (agent.iconName) {
                        "code" -> Icons.Default.Code
                        "settings" -> Icons.Default.Settings
                        "shield" -> Icons.Default.Security
                        "storage" -> Icons.Default.Storage
                        "search" -> Icons.Default.Search
                        else -> Icons.Default.SmartToy
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF13283E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconVector, contentDescription = null, tint = NexusBlue, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = agent.name,
                            color = NexusText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = agent.title,
                            color = NexusMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                if (agent.isCustom) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1F352A))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("CUSTOM", color = NexusGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = agent.description,
                color = NexusMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Permitted Category Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (agent.permittedCategories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF132030))
                            .border(1.dp, Color(0xFF223850), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("INFO / REASONING ONLY", color = NexusMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    agent.permittedCategories.forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF132030))
                                .border(1.dp, Color(0xFF223850), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(cat.uppercase(), color = NexusCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onStartChat,
                colors = ButtonDefaults.buttonColors(containerColor = NexusPanel2),
                border = BorderStroke(1.dp, NexusBorder),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Conversation with ${agent.name}", fontSize = 11.5.sp, color = NexusBlue)
            }
        }
    }
}

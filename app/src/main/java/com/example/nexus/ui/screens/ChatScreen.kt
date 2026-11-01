package com.example.nexus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.model.ChatMessage
import com.example.nexus.ui.NexusUiState
import com.example.nexus.ui.NexusViewModel
import com.example.nexus.ui.components.OperationCard
import com.example.nexus.ui.components.TaskCard
import com.example.nexus.ui.theme.NexusBlue
import com.example.nexus.ui.theme.NexusBorder
import com.example.nexus.ui.theme.NexusCyan
import com.example.nexus.ui.theme.NexusMuted
import com.example.nexus.ui.theme.NexusPanel
import com.example.nexus.ui.theme.NexusPanel2
import com.example.nexus.ui.theme.NexusRed
import com.example.nexus.ui.theme.NexusText

@Composable
fun ChatScreen(
    uiState: NexusUiState,
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    val activeConv = uiState.conversations.find { it.id == uiState.activeConversationId }
        ?: uiState.conversations.firstOrNull()
    val activeAgent = uiState.agents.find { it.id == activeConv?.agentId } ?: uiState.agents.first()

    var messageInput by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTitleInput by remember { mutableStateOf("") }
    var showAgentMenu by remember { mutableStateOf(false) }
    var showConvMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Scroll to bottom on new messages
    LaunchedEffect(activeConv?.messages?.size) {
        if ((activeConv?.messages?.size ?: 0) > 0) {
            listState.animateScrollToItem(activeConv!!.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Chat Top Navigation Header
        Surface(
            color = Color(0xFF101722),
            border = BorderStroke(1.dp, NexusBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Conversation title & dropdown
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showConvMenu = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Column {
                                Text(
                                    text = activeConv?.title ?: "No conversation",
                                    color = NexusText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${activeAgent.name} · ${uiState.conversations.size} convs",
                                    color = NexusMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NexusMuted)
                        }

                        DropdownMenu(
                            expanded = showConvMenu,
                            onDismissRequest = { showConvMenu = false },
                            modifier = Modifier.background(NexusPanel)
                        ) {
                            uiState.conversations.forEach { conv ->
                                DropdownMenuItem(
                                    text = { Text(conv.title, color = NexusText, fontSize = 12.5.sp) },
                                    onClick = {
                                        viewModel.setActiveConversation(conv.id)
                                        showConvMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Conversation Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Agent Selector Chip
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF13283E))
                                    .border(1.dp, Color(0xFF1E4670), RoundedCornerShape(16.dp))
                                    .clickable { showAgentMenu = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("agent_selector_chip")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeAgent.name.replace(" Agent", ""),
                                        color = NexusCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showAgentMenu,
                                onDismissRequest = { showAgentMenu = false },
                                modifier = Modifier.background(NexusPanel)
                            ) {
                                uiState.agents.forEach { agent ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(agent.name, color = NexusText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(agent.title, color = NexusMuted, fontSize = 10.sp)
                                            }
                                        },
                                        onClick = {
                                            if (activeConv != null) {
                                                viewModel.setConversationAgent(activeConv.id, agent.id)
                                            }
                                            showAgentMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                renameTitleInput = activeConv?.title ?: ""
                                showRenameDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = NexusMuted, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = { viewModel.createNewConversation(activeAgent.id) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("new_chat_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Conversation", tint = NexusBlue, modifier = Modifier.size(18.dp))
                        }

                        if (uiState.conversations.size > 1 && activeConv != null) {
                            IconButton(
                                onClick = { viewModel.deleteConversation(activeConv.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NexusRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val messages = activeConv?.messages ?: emptyList()
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    onApproveOp = { op -> viewModel.decideOperation(op, approved = true, source = "chat") },
                    onDenyOp = { op -> viewModel.decideOperation(op, approved = false, source = "chat") },
                    onRunNextTask = { task -> viewModel.executeTaskNextStep(task.id) },
                    onCancelTask = { task -> viewModel.cancelTask(task.id) }
                )
            }
        }

        // Composer
        Surface(
            color = Color(0xFF101722),
            border = BorderStroke(1.dp, NexusBorder),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Define objective or ask NEXUS…", color = NexusMuted, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0E1621),
                            unfocusedContainerColor = Color(0xFF0E1621),
                            focusedBorderColor = NexusBlue,
                            unfocusedBorderColor = NexusBorder,
                            focusedTextColor = NexusText,
                            unfocusedTextColor = NexusText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Button(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                val text = messageInput
                                messageInput = ""
                                viewModel.sendChatMessage(text)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF287AC9)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = messageInput.isNotBlank() && !uiState.isSendingChat,
                        modifier = Modifier.testTag("chat_send_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Offline-First · Policy-Enforced Engine · Proposed actions require your approval",
                    color = NexusMuted,
                    fontSize = 9.5.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    if (showRenameDialog && activeConv != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Conversation", color = NexusText) },
            text = {
                OutlinedTextField(
                    value = renameTitleInput,
                    onValueChange = { renameTitleInput = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NexusText,
                        unfocusedTextColor = NexusText
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameTitleInput.isNotBlank()) {
                            viewModel.renameConversation(activeConv.id, renameTitleInput)
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = NexusMuted)
                }
            },
            containerColor = NexusPanel
        )
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onApproveOp: (com.example.nexus.model.Operation) -> Unit,
    onDenyOp: (com.example.nexus.model.Operation) -> Unit,
    onRunNextTask: (com.example.nexus.model.NexusTask) -> Unit,
    onCancelTask: (com.example.nexus.model.NexusTask) -> Unit
) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isUser) "YOU" else "NEXUS AGENT",
            color = if (isUser) NexusCyan else NexusBlue,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Surface(
            color = if (isUser) Color(0xFF18385B) else NexusPanel,
            border = BorderStroke(1.dp, if (isUser) Color(0xFF28527D) else NexusBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        color = NexusText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }

                // If message has an operation proposal
                if (message.operation != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OperationCard(
                        op = message.operation,
                        onApprove = { onApproveOp(message.operation) },
                        onDeny = { onDenyOp(message.operation) }
                    )
                }

                // If message has a task snapshot
                if (message.taskSnapshot != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TaskCard(
                        task = message.taskSnapshot,
                        onRunNext = { onRunNextTask(message.taskSnapshot) },
                        onCancel = { onCancelTask(message.taskSnapshot) }
                    )
                }
            }
        }
    }
}

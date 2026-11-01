package com.example.nexus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.model.TerminalEntry
import com.example.nexus.model.TerminalKind
import com.example.nexus.ui.NexusUiState
import com.example.nexus.ui.NexusViewModel
import com.example.nexus.ui.components.OperationCard
import com.example.nexus.ui.theme.NexusAmber
import com.example.nexus.ui.theme.NexusBlue
import com.example.nexus.ui.theme.NexusBorder
import com.example.nexus.ui.theme.NexusCyan
import com.example.nexus.ui.theme.NexusMuted
import com.example.nexus.ui.theme.NexusRed
import com.example.nexus.ui.theme.NexusTerminalBg
import com.example.nexus.ui.theme.NexusText

@Composable
fun TerminalScreen(
    uiState: NexusUiState,
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    var commandInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.terminalEntries.size) {
        if (uiState.terminalEntries.isNotEmpty()) {
            listState.animateScrollToItem(uiState.terminalEntries.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Status Bar
        Surface(
            color = Color(0xFF101722),
            border = BorderStroke(1.dp, NexusBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "nexus@android:/workspaces/${uiState.terminalCwd.ifEmpty { "" }}",
                    color = NexusCyan,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "SANDBOXED SHELL",
                    color = NexusMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Terminal Output Screen
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(NexusTerminalBg)
                .padding(horizontal = 12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.terminalEntries, key = { it.id }) { entry ->
                    when (entry.kind) {
                        TerminalKind.CMD -> {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "$ ",
                                    color = NexusBlue,
                                    fontSize = 12.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = entry.text,
                                    color = NexusText,
                                    fontSize = 12.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        TerminalKind.OUT -> {
                            Text(
                                text = entry.text,
                                color = Color(0xFFC7D4E6),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        TerminalKind.BLOCKED -> {
                            Text(
                                text = entry.text,
                                color = NexusRed,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        TerminalKind.META -> {
                            Text(
                                text = "· ${entry.text}",
                                color = NexusMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        TerminalKind.SYSTEM -> {
                            Text(
                                text = entry.text,
                                color = NexusCyan,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        TerminalKind.WARN -> {
                            Column {
                                Text(
                                    text = entry.text,
                                    color = NexusAmber,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (entry.operation != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OperationCard(
                                        op = entry.operation,
                                        onApprove = { viewModel.decideOperation(entry.operation, approved = true, source = "terminal") },
                                        onDeny = { viewModel.decideOperation(entry.operation, approved = false, source = "terminal") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Soft Keys Bar (Terminal Quick Tools)
        val shortcuts = listOf("ls", "pwd", "mkdir", "cat", "python", "help", "clear", "history")
        Surface(
            color = Color(0xFF0F1622),
            border = BorderStroke(1.dp, NexusBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                shortcuts.forEach { cmd ->
                    Surface(
                        color = Color(0xFF162334),
                        border = BorderStroke(1.dp, Color(0xFF283F5B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                    ) {
                        Text(
                            text = cmd,
                            color = NexusCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .clickable {
                                    if (cmd == "clear" || cmd == "pwd" || cmd == "history" || cmd == "help") {
                                        viewModel.executeTerminalCommand(cmd)
                                    } else {
                                        commandInput = "$cmd "
                                    }
                                }
                        )
                    }
                }
            }
        }

        // Interactive Input Bar
        Surface(
            color = Color(0xFF101722),
            border = BorderStroke(1.dp, NexusBorder),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    prefix = { Text("$ ", color = NexusBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("type command… (e.g. ls, mkdir project, python main.py)", color = NexusMuted, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF070B12),
                        unfocusedContainerColor = Color(0xFF070B12),
                        focusedBorderColor = NexusBlue,
                        unfocusedBorderColor = NexusBorder,
                        focusedTextColor = NexusText,
                        unfocusedTextColor = NexusText
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_input_field")
                )

                Button(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            val cmd = commandInput
                            commandInput = ""
                            viewModel.executeTerminalCommand(cmd)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF287AC9)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("terminal_run_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

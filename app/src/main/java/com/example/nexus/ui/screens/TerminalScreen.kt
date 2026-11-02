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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.nexus.ui.theme.NexusGreen
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
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "nexus@android:/workspaces/${uiState.terminalCwd.ifEmpty { "" }}",
                        color = NexusCyan,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (uiState.isTerminalRunning) "⚡ EXECUTING..." else "SANDBOXED",
                        color = if (uiState.isTerminalRunning) NexusAmber else NexusMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = { viewModel.executeTerminalCommand("clear") },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = NexusMuted, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("CLEAR", color = NexusMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
                        TerminalKind.ERR -> {
                            Column {
                                Text(
                                    text = entry.text,
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Surface(
                                    color = Color(0xFF261D15),
                                    border = BorderStroke(1.dp, NexusAmber.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.clickable {
                                        viewModel.askAgentToDiagnoseError(commandInput.ifEmpty { "failed command" }, entry.text)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Build,
                                            contentDescription = null,
                                            tint = NexusAmber,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "🤖 Ask Developer to Fix",
                                            color = NexusAmber,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
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

        // Soft Keys Bar (Expanded Terminal Quick Tools)
        val shortcuts = listOf(
            "ls -la", "pwd", "nmap local", "nmap scanme.nmap.org", "ping 8.8.8.8",
            "date", "uname -a", "df -h", "clang++", "./main", "python",
            "ps", "env", "clear", "history", "help"
        )
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
                    val isActionable = cmd in listOf("clear", "pwd", "history", "help", "ps", "env", "ls -la", "./main", "nmap local", "nmap scanme.nmap.org", "date", "uname -a", "df -h")
                    Surface(
                        color = Color(0xFF162334),
                        border = BorderStroke(1.dp, Color(0xFF283F5B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                    ) {
                        Text(
                            text = cmd,
                            color = when {
                                cmd.contains("clang") || cmd.contains("main") -> NexusGreen
                                cmd.contains("nmap") || cmd.contains("ping") -> NexusAmber
                                else -> NexusCyan
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .clickable {
                                    if (isActionable) {
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

        // Auto-Completion Suggestions Bar (Workspace Files & Commands)
        val lastWord = commandInput.substringAfterLast(" ")
        val suggestions = remember(commandInput, uiState.workspaceFiles) {
            if (lastWord.isNotBlank() && commandInput.isNotBlank()) {
                val fileMatches = uiState.workspaceFiles.map { it.name }
                    .filter { it.startsWith(lastWord, ignoreCase = true) && it != lastWord }
                val cmdMatches = listOf("clang++", "python", "nmap", "ping", "cat", "echo", "mkdir", "touch", "clear")
                    .filter { it.startsWith(lastWord, ignoreCase = true) && it != lastWord }
                (fileMatches + cmdMatches).take(5)
            } else emptyList()
        }

        if (suggestions.isNotEmpty()) {
            Surface(
                color = Color(0xFF0C131D),
                border = BorderStroke(1.dp, NexusBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TAB:", color = NexusMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    suggestions.forEach { suggestion ->
                        Surface(
                            color = Color(0xFF192A40),
                            border = BorderStroke(1.dp, NexusBlue),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable {
                                val prefix = commandInput.substringBeforeLast(" ", "")
                                commandInput = if (prefix.isEmpty()) suggestion else "$prefix $suggestion"
                            }
                        ) {
                            Text(
                                text = suggestion,
                                color = NexusCyan,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Interactive Input Bar with Mobile History Arrows (↑ / ↓)
        var historyCursor by remember { mutableStateOf(-1) }

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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // History Up / Down controls
                Row {
                    IconButton(
                        onClick = {
                            val hist = viewModel.terminalEngine.history
                            if (hist.isNotEmpty()) {
                                val next = if (historyCursor == -1) hist.size - 1 else (historyCursor - 1).coerceAtLeast(0)
                                historyCursor = next
                                commandInput = hist[next]
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("terminal_history_up")
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Command", tint = NexusCyan)
                    }
                    IconButton(
                        onClick = {
                            val hist = viewModel.terminalEngine.history
                            if (hist.isNotEmpty() && historyCursor != -1) {
                                if (historyCursor < hist.size - 1) {
                                    historyCursor++
                                    commandInput = hist[historyCursor]
                                } else {
                                    historyCursor = -1
                                    commandInput = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("terminal_history_down")
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Command", tint = NexusCyan)
                    }
                }

                OutlinedTextField(
                    value = commandInput,
                    onValueChange = {
                        commandInput = it
                        historyCursor = -1
                    },
                    prefix = { Text("$ ", color = NexusBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("command… (e.g. nmap, ping, python)", color = NexusMuted, fontSize = 12.sp) },
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
                        fontSize = 12.5.sp
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_input_field")
                )

                if (uiState.isTerminalRunning) {
                    Button(
                        onClick = { viewModel.cancelTerminalCommand() },
                        colors = ButtonDefaults.buttonColors(containerColor = NexusRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("terminal_abort_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Abort (^C)", modifier = Modifier.size(18.dp))
                    }
                } else {
                    Button(
                        onClick = {
                            if (commandInput.isNotBlank()) {
                                val cmd = commandInput
                                commandInput = ""
                                historyCursor = -1
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
}

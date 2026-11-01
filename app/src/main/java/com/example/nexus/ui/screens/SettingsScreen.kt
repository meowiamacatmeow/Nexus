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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.model.AIProviderType
import com.example.nexus.model.AuditEntry
import com.example.nexus.ui.NexusUiState
import com.example.nexus.ui.NexusViewModel
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
import java.util.Date

@Composable
fun SettingsScreen(
    uiState: NexusUiState,
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Intelligence Provider
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusPanel),
                border = BorderStroke(1.dp, NexusBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "INTELLIGENCE ENGINE",
                        color = NexusBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AI Provider Configuration",
                        color = NexusText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.activeProviderType == AIProviderType.GEMINI) "Google Gemini (Optional Cloud)" else "Deterministic Local Engine",
                                color = NexusText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.activeProviderType == AIProviderType.GEMINI) "Requires network. Uses configured API credentials." else "100% offline. Zero network calls or external subscriptions.",
                                color = NexusMuted,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = uiState.activeProviderType == AIProviderType.GEMINI,
                            onCheckedChange = { viewModel.toggleProvider() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NexusBlue,
                                checkedTrackColor = Color(0xFF1B3D5E),
                                uncheckedThumbColor = NexusGreen,
                                uncheckedTrackColor = Color(0xFF10281E)
                            ),
                            modifier = Modifier.testTag("provider_toggle_switch")
                        )
                    }
                }
            }
        }

        // 2. Permission Registry Manager
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
                            Text("Security Permission Categories", color = NexusText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Operations outside enabled categories are immediately blocked before approval. Even when enabled, mutating actions still require explicit confirmation.",
                        color = NexusMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PermissionToggleRow("Workspace Management", "create_workspace, inspect_env", uiState.permissions.workspace) {
                        viewModel.togglePermission("workspace")
                    }
                    PermissionToggleRow("Filesystem Access", "write_file, delete_file, edit", uiState.permissions.filesystem) {
                        viewModel.togglePermission("filesystem")
                    }
                    PermissionToggleRow("Tool & Package Installs", "install_package, uninstall_package", uiState.permissions.packages) {
                        viewModel.togglePermission("packages")
                    }
                    PermissionToggleRow("Network Requests", "network_fetch, external api", uiState.permissions.network) {
                        viewModel.togglePermission("network")
                    }
                    PermissionToggleRow("Shell Script Execution", "run_script, python executable", uiState.permissions.shellScripts) {
                        viewModel.togglePermission("shellScripts")
                    }
                }
            }
        }

        // 3. Storage & Portable Data Manager (nexus.data)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusPanel),
                border = BorderStroke(1.dp, NexusBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = NexusGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Storage & Data Manager", color = NexusText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Unified directory: ${viewModel.storageManager.nexusDir.absolutePath}",
                        color = NexusMuted,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                exportedJsonText = viewModel.exportNexusData()
                                showExportDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF287AC9)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_nexus_data_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export nexus.data", fontSize = 11.5.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                importStatusMessage = null
                                importJsonInput = ""
                                showImportDialog = true
                            },
                            border = BorderStroke(1.dp, NexusBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_nexus_data_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import nexus.data", fontSize = 11.5.sp, color = NexusCyan)
                        }
                    }
                }
            }
        }

        // 4. Security Audit Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AUDIT TRAIL (${uiState.auditLog.size} EVENTS)",
                    color = NexusMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (uiState.auditLog.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAuditLog() }) {
                        Text("Clear Log", color = NexusRed, fontSize = 11.sp)
                    }
                }
            }
        }

        if (uiState.auditLog.isEmpty()) {
            item {
                Surface(
                    color = NexusPanel2,
                    border = BorderStroke(1.dp, NexusBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No operations recorded yet.",
                        color = NexusMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(uiState.auditLog.take(20), key = { it.id }) { log ->
                AuditLogRow(entry = log)
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exported nexus.data Snapshot", color = NexusText) },
            text = {
                Column {
                    Text(
                        text = "Saved to NEXUS/backups/. Portable JSON schema v5:",
                        color = NexusMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonText.take(1500) + if (exportedJsonText.length > 1500) "\n\n... (truncated for display)" else "",
                        onValueChange = {},
                        readOnly = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NexusCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF070B12),
                            unfocusedContainerColor = Color(0xFF070B12)
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showExportDialog = false }) { Text("Close") }
            },
            containerColor = NexusPanel
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import nexus.data", color = NexusText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste or type nexus.data JSON content to restore:",
                        color = NexusMuted,
                        fontSize = 11.5.sp
                    )
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("{\"format\":\"nexus.data\",\"schemaVersion\":5,...}", color = NexusMuted, fontSize = 11.sp) },
                        minLines = 4,
                        maxLines = 8,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NexusText),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF070B12),
                            unfocusedContainerColor = Color(0xFF070B12),
                            focusedTextColor = NexusText,
                            unfocusedTextColor = NexusText
                        )
                    )
                    if (importStatusMessage != null) {
                        Text(importStatusMessage!!, color = NexusRed, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val ok = viewModel.importNexusData(importJsonInput, "REPLACE")
                            if (ok) {
                                showImportDialog = false
                            } else {
                                importStatusMessage = "Invalid format. Must be valid JSON with 'format': 'nexus.data'."
                            }
                        }
                    ) {
                        Text("Replace")
                    }

                    OutlinedButton(
                        onClick = {
                            val ok = viewModel.importNexusData(importJsonInput, "MERGE")
                            if (ok) {
                                showImportDialog = false
                            } else {
                                importStatusMessage = "Invalid format. Must be valid JSON with 'format': 'nexus.data'."
                            }
                        }
                    ) {
                        Text("Merge")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel", color = NexusMuted) }
            },
            containerColor = NexusPanel
        )
    }
}

@Composable
fun PermissionToggleRow(
    title: String,
    scope: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NexusText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text(scope, color = NexusMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = NexusGreen,
                checkedTrackColor = Color(0xFF10281E)
            )
        )
    }
}

@Composable
fun AuditLogRow(entry: AuditEntry) {
    Surface(
        color = NexusPanel,
        border = BorderStroke(1.dp, NexusBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.opName,
                        color = NexusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "· ${entry.category}",
                        color = NexusMuted,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = entry.detail,
                    color = NexusMuted,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val statusColor = when (entry.status) {
                    "APPROVED" -> NexusGreen
                    "DENIED" -> NexusRed
                    else -> NexusAmber
                }
                Text(
                    text = entry.status,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Date(entry.timestamp).toString().takeLast(12),
                    color = NexusMuted,
                    fontSize = 9.sp
                )
            }
        }
    }
}

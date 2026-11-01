package com.example.nexus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.model.WorkspaceFile
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
fun FileManagerScreen(
    uiState: NexusUiState,
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileContent by remember { mutableStateOf("") }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var activeEditorFile by remember { mutableStateOf<WorkspaceFile?>(null) }
    var editorContent by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Navigation bar / Breadcrumb
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEXUS WORKSPACE EXPLORER",
                            color = NexusBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "/workspaces/${uiState.currentSubPath.ifEmpty { "" }}",
                            color = NexusCyan,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        IconButton(onClick = { viewModel.refreshWorkspaceFiles() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NexusMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: New Folder, New File, Up Directory
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.currentSubPath.isNotBlank()) {
                        Button(
                            onClick = {
                                val parts = uiState.currentSubPath.split("/").filter { it.isNotEmpty() }
                                val parent = parts.dropLast(1).joinToString("/")
                                viewModel.refreshWorkspaceFiles(parent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexusPanel2),
                            border = BorderStroke(1.dp, NexusBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(".. (Up)", fontSize = 11.sp, color = NexusCyan)
                        }
                    }

                    Button(
                        onClick = { showNewFolderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NexusPanel2),
                        border = BorderStroke(1.dp, NexusBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = NexusAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Folder", fontSize = 11.sp, color = NexusText)
                    }

                    Button(
                        onClick = { showNewFileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NexusPanel2),
                        border = BorderStroke(1.dp, NexusBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, tint = NexusGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New File", fontSize = 11.sp, color = NexusText)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Files List
        if (uiState.workspaceFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Directory is empty. Create a file or approve a task to populate.",
                    color = NexusMuted,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(uiState.workspaceFiles, key = { it.path }) { item ->
                    WorkspaceFileItemRow(
                        item = item,
                        onClick = {
                            if (item.isDirectory) {
                                viewModel.refreshWorkspaceFiles(item.relativePath)
                            } else {
                                val content = viewModel.storageManager.readFile(item.relativePath) ?: ""
                                editorContent = content
                                activeEditorFile = item
                            }
                        },
                        onDelete = {
                            viewModel.deleteWorkspaceItem(item.relativePath)
                        }
                    )
                }
            }
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create Workspace Folder", color = NexusText) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("folder_name", color = NexusMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NexusText,
                        unfocusedTextColor = NexusText
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(uiState.currentSubPath, newFolderName)
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel", color = NexusMuted) }
            },
            containerColor = NexusPanel
        )
    }

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create File", color = NexusText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        placeholder = { Text("script.py or README.md", color = NexusMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = NexusText, unfocusedTextColor = NexusText)
                    )
                    OutlinedTextField(
                        value = newFileContent,
                        onValueChange = { newFileContent = it },
                        placeholder = { Text("File content...", color = NexusMuted) },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = NexusText, unfocusedTextColor = NexusText)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createFile(uiState.currentSubPath, newFileName, newFileContent)
                            newFileName = ""
                            newFileContent = ""
                            showNewFileDialog = false
                        }
                    }
                ) {
                    Text("Create File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel", color = NexusMuted) }
            },
            containerColor = NexusPanel
        )
    }

    // Text File Editor Modal
    if (activeEditorFile != null) {
        AlertDialog(
            onDismissRequest = { activeEditorFile = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(activeEditorFile!!.name, color = NexusCyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Text("${editorContent.length} bytes", color = NexusMuted, fontSize = 11.sp)
                }
            },
            text = {
                OutlinedTextField(
                    value = editorContent,
                    onValueChange = { editorContent = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NexusText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF070B12),
                        unfocusedContainerColor = Color(0xFF070B12),
                        focusedBorderColor = NexusBlue,
                        unfocusedBorderColor = NexusBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val path = activeEditorFile!!.relativePath
                        viewModel.storageManager.writeFile(path, editorContent)
                        viewModel.refreshWorkspaceFiles()
                        activeEditorFile = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeEditorFile = null }) {
                    Text("Close", color = NexusMuted)
                }
            },
            containerColor = NexusPanel
        )
    }
}

@Composable
fun WorkspaceFileItemRow(
    item: WorkspaceFile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = NexusPanel,
        border = BorderStroke(1.dp, NexusBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (item.isDirectory) NexusAmber else NexusCyan,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = item.name,
                        color = NexusText,
                        fontSize = 13.sp,
                        fontWeight = if (item.isDirectory) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (item.isDirectory) "Directory" else "${item.sizeBytes} B · ${Date(item.lastModified)}",
                        color = NexusMuted,
                        fontSize = 10.sp
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NexusRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

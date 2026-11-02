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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
    var zipNotice by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (zipNotice != null) {
            Surface(
                color = Color(0xFF13221E),
                border = BorderStroke(1.dp, NexusGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable { zipNotice = null }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, tint = NexusGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(zipNotice!!, color = NexusGreen, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

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

                // Actions: New Folder, New File, Up Directory, Export ZIP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            Text("..", fontSize = 11.sp, color = NexusCyan)
                        }
                    }

                    Button(
                        onClick = { showNewFolderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NexusPanel2),
                        border = BorderStroke(1.dp, NexusBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = NexusAmber, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Folder", fontSize = 11.sp, color = NexusText)
                    }

                    Button(
                        onClick = { showNewFileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NexusPanel2),
                        border = BorderStroke(1.dp, NexusBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, tint = NexusGreen, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("File", fontSize = 11.sp, color = NexusText)
                    }

                    Button(
                        onClick = {
                            viewModel.exportWorkspaceZip(uiState.currentSubPath) { notice ->
                                zipNotice = notice
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexusPanel2),
                        border = BorderStroke(1.dp, NexusBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("ZIP", fontSize = 11.sp, color = NexusCyan)
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
                        onRun = {
                            viewModel.runWorkspaceFile(item)
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
                        placeholder = { Text("script.py, main.cpp, README.md", color = NexusMuted) },
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

    // Text File Editor Modal (IDE-grade with line numbers & direct execution)
    if (activeEditorFile != null) {
        val clipboardManager = LocalClipboardManager.current
        val file = activeEditorFile!!
        val isRunnable = !file.isDirectory && (
            file.name.endsWith(".py") || file.name.endsWith(".cpp") ||
            file.name.endsWith(".c") || file.name.endsWith(".sh") || !file.name.contains(".")
        )
        val lang = when {
            file.name.endsWith(".py") -> "PYTHON"
            file.name.endsWith(".cpp") || file.name.endsWith(".cc") -> "C++"
            file.name.endsWith(".c") -> "C"
            file.name.endsWith(".sh") -> "SHELL"
            file.name.endsWith(".md") -> "MARKDOWN"
            file.name.endsWith(".json") -> "JSON"
            else -> "CODE"
        }

        val lineCount = remember(editorContent) { editorContent.lines().size.coerceAtLeast(1) }
        val lineNumbersText = remember(lineCount) { (1..lineCount).joinToString("\n") }

        AlertDialog(
            onDismissRequest = { activeEditorFile = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF13283E),
                            border = BorderStroke(1.dp, Color(0xFF28527D)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = lang,
                                color = NexusCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = file.name,
                            color = NexusText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(editorContent))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NexusMuted, modifier = Modifier.size(15.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${editorContent.length} B", color = NexusMuted, fontSize = 10.sp)
                    }
                }
            },
            text = {
                Surface(
                    color = Color(0xFF070B12),
                    border = BorderStroke(1.dp, NexusBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        // Line numbering gutter
                        Text(
                            text = lineNumbersText,
                            color = Color(0xFF385070),
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 17.sp,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .width(28.dp)
                        )

                        // Editable code
                        OutlinedTextField(
                            value = editorContent,
                            onValueChange = { editorContent = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = NexusText
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("file_editor_textfield"),
                            visualTransformation = remember(file.name) {
                                VisualTransformation { text ->
                                    TransformedText(highlightSyntax(text.text, file.name), OffsetMapping.Identity)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRunnable) {
                        Button(
                            onClick = {
                                val path = file.relativePath
                                viewModel.storageManager.writeFile(path, editorContent)
                                viewModel.refreshWorkspaceFiles()
                                activeEditorFile = null
                                viewModel.runWorkspaceFile(file)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexusGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("editor_save_run_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save & Run", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            val path = file.relativePath
                            viewModel.storageManager.writeFile(path, editorContent)
                            viewModel.refreshWorkspaceFiles()
                            activeEditorFile = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexusBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Text("Save", fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { activeEditorFile = null }) {
                    Text("Close", color = NexusMuted, fontSize = 11.sp)
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
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    val isRunnable = !item.isDirectory && (
        item.name.endsWith(".py") || item.name.endsWith(".cpp") ||
        item.name.endsWith(".c") || item.name.endsWith(".sh") || !item.name.contains(".")
    )

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
                    imageVector = when {
                        item.isDirectory -> Icons.Default.Folder
                        item.name.endsWith(".py") || item.name.endsWith(".cpp") || item.name.endsWith(".c") -> Icons.Default.Code
                        else -> Icons.Default.Description
                    },
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRunnable) {
                    IconButton(
                        onClick = onRun,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("run_file_${item.name}")
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Run file",
                            tint = NexusGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = NexusRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Lightweight syntax highlighting for Python, C++, and Shell scripts.
 */
fun highlightSyntax(code: String, fileName: String): AnnotatedString {
    val builder = AnnotatedString.Builder(code)
    val isPython = fileName.endsWith(".py")
    val isCpp = fileName.endsWith(".cpp") || fileName.endsWith(".c") || fileName.endsWith(".h") || fileName.endsWith(".hpp")
    val isShell = fileName.endsWith(".sh")

    val keywords = when {
        isPython -> setOf("def", "class", "import", "from", "return", "if", "elif", "else", "for", "while", "try", "except", "with", "as", "pass", "True", "False", "None", "print")
        isCpp -> setOf("int", "float", "double", "char", "void", "bool", "auto", "class", "struct", "template", "typename", "namespace", "using", "return", "if", "else", "for", "while", "public", "private", "protected", "include", "const", "new", "delete")
        isShell -> setOf("if", "then", "else", "elif", "fi", "for", "in", "do", "done", "case", "esac", "while", "echo", "exit", "local", "export")
        else -> setOf("function", "var", "let", "const", "return", "if", "else", "true", "false")
    }

    // Highlight keywords
    for (kw in keywords) {
        val pattern = "\\b$kw\\b".toRegex()
        pattern.findAll(code).forEach { match ->
            builder.addStyle(
                SpanStyle(color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    // Highlight strings
    val stringPattern = "\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'".toRegex()
    stringPattern.findAll(code).forEach { match ->
        builder.addStyle(
            SpanStyle(color = Color(0xFFA5D6A7)),
            match.range.first,
            match.range.last + 1
        )
    }

    // Highlight comments
    val commentPattern = if (isPython || isShell) "#.*".toRegex() else "(//.*)".toRegex()
    commentPattern.findAll(code).forEach { match ->
        builder.addStyle(
            SpanStyle(color = Color(0xFF78909C)),
            match.range.first,
            match.range.last + 1
        )
    }

    return builder.toAnnotatedString()
}


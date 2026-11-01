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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.nexus.model.MemoryItem
import com.example.nexus.ui.NexusUiState
import com.example.nexus.ui.NexusViewModel
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
fun MemoryScreen(
    uiState: NexusUiState,
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Project Context") }

    val categories = listOf("ALL", "Environment", "Configuration", "Project Context", "Learned Rules")

    val filtered = uiState.memoryItems.filter { item ->
        val matchesCat = selectedCategory == "ALL" || item.category == selectedCategory
        val matchesQuery = searchQuery.isBlank() ||
                item.key.contains(searchQuery, ignoreCase = true) ||
                item.value.contains(searchQuery, ignoreCase = true)
        matchesCat && matchesQuery
    }

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
                        text = "PERSISTENT MEMORY & KNOWLEDGE",
                        color = NexusBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Remembered Context & Configurations",
                        color = NexusText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "NEXUS retains approved system parameters, user tool configurations, and project contexts offline. You have total inspection, export and deletion control.",
                        color = NexusMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF287AC9)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_memory_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Memory Record", fontSize = 12.sp)
                    }
                }
            }
        }

        // Search & Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search memory records…", color = NexusMuted, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NexusMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0E1621),
                        unfocusedContainerColor = Color(0xFF0E1621),
                        focusedBorderColor = NexusBlue,
                        unfocusedBorderColor = NexusBorder,
                        focusedTextColor = NexusText,
                        unfocusedTextColor = NexusText
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1B3D5E),
                                selectedLabelColor = NexusCyan,
                                containerColor = Color(0xFF101A27),
                                labelColor = NexusMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategory == cat,
                                borderColor = NexusBorder,
                                selectedBorderColor = NexusBlue
                            )
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "RECORDS (${filtered.size})",
                color = NexusMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        items(filtered, key = { it.id }) { item ->
            MemoryItemCard(
                item = item,
                onDelete = { viewModel.deleteMemoryItem(item.id) }
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Memory Record", color = NexusText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        placeholder = { Text("KEY (e.g. WORKSPACE_PREF)", color = NexusMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = NexusText, unfocusedTextColor = NexusText)
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        placeholder = { Text("Value / Description", color = NexusMuted) },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = NexusText, unfocusedTextColor = NexusText)
                    )
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        placeholder = { Text("Category (e.g. Project Context)", color = NexusMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = NexusText, unfocusedTextColor = NexusText)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank()) {
                            viewModel.addMemoryItem(newKey, newValue, newCategory)
                            newKey = ""
                            newValue = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = NexusMuted) }
            },
            containerColor = NexusPanel
        )
    }
}

@Composable
fun MemoryItemCard(
    item: MemoryItem,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NexusPanel),
        border = BorderStroke(1.dp, NexusBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.key,
                    color = NexusCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF132030))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.category, color = NexusBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NexusRed.copy(alpha = 0.6f), modifier = Modifier.size(15.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.value,
                color = NexusText,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Source: ${item.source} · Updated ${Date(item.updatedAt)}",
                color = NexusMuted,
                fontSize = 10.sp
            )
        }
    }
}

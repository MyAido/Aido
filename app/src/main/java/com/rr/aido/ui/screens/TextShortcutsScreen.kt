package com.rr.aido.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rr.aido.ui.viewmodels.TextShortcutsViewModel
import com.rr.aido.data.models.TextShortcut

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextShortcutsScreen(
    viewModel: TextShortcutsViewModel,
    onNavigateBack: () -> Unit
) {
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingShortcut by remember { mutableStateOf<TextShortcut?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Text Shortcuts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingShortcut = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Shortcut")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (shortcuts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No shortcuts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add a new shortcut",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                ) {
                    items(shortcuts, key = { it.id }) { shortcut ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = shortcut.trigger,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = shortcut.replacement,
                                    maxLines = 2
                                )
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        editingShortcut = shortcut
                                        showDialog = true
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { viewModel.removeShortcut(shortcut.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }

        if (showDialog) {
            ShortcutDialog(
                initialTrigger = editingShortcut?.trigger ?: "",
                initialReplacement = editingShortcut?.replacement ?: "",
                isEditing = editingShortcut != null,
                onDismiss = { showDialog = false },
                onConfirm = { trigger, replacement ->
                    if (editingShortcut != null) {
                        viewModel.updateShortcut(editingShortcut!!.copy(trigger = trigger, replacement = replacement))
                    } else {
                        viewModel.addShortcut(trigger, replacement)
                    }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun ShortcutDialog(
    initialTrigger: String,
    initialReplacement: String,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var trigger by remember { mutableStateOf(initialTrigger) }
    var replacement by remember { mutableStateOf(initialReplacement) }
    var triggerError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Shortcut" else "New Shortcut") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = {
                        trigger = it
                        triggerError = false
                    },
                    label = { Text("Shortcut (e.g., !email)") },
                    singleLine = true,
                    isError = triggerError,
                    supportingText = if (triggerError) { { Text("Required") } } else null
                )

                OutlinedTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    label = { Text("Expansion Text") },
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (trigger.isBlank()) {
                        triggerError = true
                    } else {
                        onConfirm(trigger, replacement)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

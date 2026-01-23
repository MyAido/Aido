package com.rr.aido.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rr.aido.data.models.Preprompt
import com.rr.aido.ui.viewmodels.PrepromptsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepromptsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrepromptsViewModel = viewModel()
) {
    val preprompts by viewModel.preprompts.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Show error message
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preprompts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleReorderMode() }) {
                        Icon(
                            imageVector = if (uiState.isReorderMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (uiState.isReorderMode) "Done" else "Reorder"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(Icons.Default.Add, "Add Preprompt")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PrepromptInfoCard()
            }

            // Import/Export moved to Settings
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Saved triggers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Edit or create triggers to tailor Aido to your workflow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(preprompts, key = { it.trigger }) { preprompt ->
                val index = preprompts.indexOf(preprompt)
                PrepromptCard(
                    preprompt = preprompt,
                    isReorderMode = uiState.isReorderMode,
                    onEdit = { viewModel.showEditDialog(preprompt) },
                    onDelete = { viewModel.deletePreprompt(preprompt) },
                    onDragStart = {  },
                    onDragEnd = {  },
                    onMove = { from, to -> viewModel.reorderPreprompts(from, to) },
                    index = index,
                    totalCount = preprompts.size
                )
            }
            item {
                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }

    // Add/Edit Dialog
    if (uiState.showAddDialog) {
        // Pause accessibility service while editing preprompts
        DisposableEffect(Unit) {
            com.rr.aido.service.AidoAccessibilityService.isPaused = true
            android.util.Log.d("PrepromptsScreen", "Accessibility service paused")
            onDispose {
                com.rr.aido.service.AidoAccessibilityService.isPaused = false
                android.util.Log.d("PrepromptsScreen", "Accessibility service resumed")
            }
        }

        AddPrepromptDialog(
            trigger = uiState.dialogTrigger,
            instruction = uiState.dialogInstruction,
            example = uiState.dialogExample,
            isEditing = uiState.editingPreprompt != null,
            onTriggerChange = { viewModel.updateDialogTrigger(it) },
            onInstructionChange = { viewModel.updateDialogInstruction(it) },
            onExampleChange = { viewModel.updateDialogExample(it) },
            onSave = { viewModel.savePreprompt() },
            onDismiss = { viewModel.hideDialog() }
        )
    }

}

@Composable
fun PrepromptCard(
    preprompt: Preprompt,
    isReorderMode: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onMove: (Int, Int) -> Unit = { _, _ -> },
    index: Int = 0,
    totalCount: Int = 0
) {
    // Simple drag implementation for now - just visual indication in reorder mode
    // For full drag and drop in LazyColumn, we might need a more complex setup or a library
    // But let's try a basic implementation using standard Compose modifiers if possible,
    // or just show the handle and rely on the list being small enough for simple reordering?
    // Actually, LazyColumn reordering is tricky without a library.
    // Let's implement a simple "move up/down" for now if drag is too complex, OR
    // use a simplified drag approach where we just swap items on drop?
    //
    // Wait, the user specifically asked for "Hold and drop".
    // Let's try to implement a basic drag gesture that updates the list.

    val isDragging = remember { mutableStateOf(false) }
    val offsetY = remember { mutableStateOf(0f) }

    val currentIndex by rememberUpdatedState(index)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentTotalCount by rememberUpdatedState(totalCount)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isReorderMode) {
                    Modifier
                        .zIndex(if (isDragging.value) 1f else 0f)
                        .graphicsLayer {
                            translationY = offsetY.value
                            shadowElevation = if (isDragging.value) 8.dp.toPx() else 0f
                        }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isReorderMode) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Drag Handle",
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging.value = true
                                    onDragStart()
                                },
                                onDragEnd = {
                                    isDragging.value = false
                                    offsetY.value = 0f
                                    onDragEnd()
                                },
                                onDragCancel = {
                                    isDragging.value = false
                                    offsetY.value = 0f
                                    onDragEnd()
                                },
                                onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                    change.consume()

                                    val dragThreshold = 150f // Increased threshold for stability
                                    offsetY.value += dragAmount.y

                                    if (offsetY.value > dragThreshold) {
                                        if (currentIndex < currentTotalCount - 1) {
                                            currentOnMove(currentIndex, currentIndex + 1)
                                            offsetY.value = 0f
                                        }
                                    } else if (offsetY.value < -dragThreshold) {
                                        if (currentIndex > 0) {
                                            currentOnMove(currentIndex, currentIndex - 1)
                                            offsetY.value = 0f
                                        }
                                    }
                                }
                            )
                        }
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preprompt.trigger,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Hide edit/delete buttons in reorder mode to avoid clutter/conflicts
            if (!isReorderMode) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = preprompt.instruction,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (preprompt.example.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Example: ${preprompt.example}",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (preprompt.isDefault) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("Default", fontWeight = FontWeight.Medium) },
                        enabled = false,
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun PrepromptInfoCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "How preprompts work",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Triggers automatically prepend instructions to anything you type. They work anywhere Aido can see your text. You can use any symbol to start a trigger!",
                style = MaterialTheme.typography.bodyMedium
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "• @fixg → Fix grammar, spelling, and punctuation",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• @summ → Summarise the selected text",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• @aido → Answer the query directly",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• ~test, !ai, #sum → Create custom triggers with any symbol @ ~ ! # $ % ^ & * ( ) - _ = + [ ] { } \\ | ; : ' \" , < . > / ?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Customize or create your own triggers below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AddPrepromptDialog(
    trigger: String,
    instruction: String,
    example: String,
    isEditing: Boolean,
    onTriggerChange: (String) -> Unit,
    onInstructionChange: (String) -> Unit,
    onExampleChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    // Trigger validation - check if it starts with a valid symbol
    val validSymbols = "`~!@#$%^&*()-_=+[]{}\\|;:'\",<.>/?"

    // More flexible validation
    val isTriggerValid = when {
        trigger.isEmpty() -> false
        trigger.length < 2 -> false
        !validSymbols.contains(trigger.first()) -> false
        trigger.length == 1 -> false // Only symbol, no text
        else -> {
            // Check if after symbol there are only word characters
            val afterSymbol = trigger.substring(1)
            afterSymbol.matches(Regex("\\w+"))
        }
    }

    val triggerError = when {
        trigger.isEmpty() -> null // Don't show error for empty
        trigger.length == 1 && validSymbols.contains(trigger.first()) -> null // Just typed symbol
        trigger.length < 2 -> "Trigger too short"
        !validSymbols.contains(trigger.first()) -> "Must start with: @ ~ ! # $ % ^ & * ( ) - _ = + [ ] { } \\ | ; : ' \" , < . > / ?"
        !trigger.substring(1).matches(Regex("\\w+")) -> "After symbol, only letters/numbers allowed"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Edit Preprompt" else "Add New Preprompt")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = onTriggerChange,
                    label = { Text("Trigger") },
                    placeholder = { Text("@mytrigger") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "aido_trigger_input_field" },
                    singleLine = true,
                    isError = triggerError != null,
                    supportingText = {
                        if (triggerError != null) {
                            Text(
                                text = triggerError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                text = "Use any symbol you want!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = instruction,
                    onValueChange = onInstructionChange,
                    label = { Text("Preprompt instruction") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = example,
                    onValueChange = onExampleChange,
                    label = { Text("Example (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Example: When you type 'Hello world${trigger}', it becomes '$instruction Hello world'",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = isTriggerValid && instruction.isNotEmpty()
            ) {
                Text(if (isEditing) "Update" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

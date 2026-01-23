package com.rr.aido.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rr.aido.data.models.SearchEngine
import com.rr.aido.ui.viewmodels.SettingsViewModel
import com.rr.aido.utils.PromptParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialCommandsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showSmartReplyEditDialog by remember { mutableStateOf(false) }
    var showToneRewriteEditDialog by remember { mutableStateOf(false) }

    if (showSmartReplyEditDialog) {
        DisposableEffect(Unit) {
            com.rr.aido.service.AidoAccessibilityService.isPaused = true
            onDispose {
                com.rr.aido.service.AidoAccessibilityService.isPaused = false
            }
        }
        EditCommandDialog(
            title = "Edit Smart Reply",
            currentTrigger = uiState.smartReplyTrigger,
            currentPrompt = uiState.smartReplyPrompt,
            defaultPrompt = PromptParser.DEFAULT_SMART_REPLY_INSTRUCTIONS,
            onDismiss = { showSmartReplyEditDialog = false },
            onSave = { trigger, prompt ->
                viewModel.updateSmartReplyTrigger(trigger)
                viewModel.updateSmartReplyPrompt(prompt)
            }
        )
    }

    if (showToneRewriteEditDialog) {
        DisposableEffect(Unit) {
            com.rr.aido.service.AidoAccessibilityService.isPaused = true
            onDispose {
                com.rr.aido.service.AidoAccessibilityService.isPaused = false
            }
        }
        EditCommandDialog(
            title = "Edit Tone Rewrite",
            currentTrigger = uiState.toneRewriteTrigger,
            currentPrompt = uiState.toneRewritePrompt,
            defaultPrompt = PromptParser.DEFAULT_TONE_REWRITE_INSTRUCTIONS,
            onDismiss = { showToneRewriteEditDialog = false },
            onSave = { trigger, prompt ->
                viewModel.updateToneRewriteTrigger(trigger)
                viewModel.updateToneRewritePrompt(prompt)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Special Commands") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Smart Reply Section
            item {
                SettingsSection(title = "Smart Reply (${uiState.smartReplyTrigger})") {
                    SettingsToggle(
                        title = "Enable Smart Reply",
                        description = "Suggest replies in messaging apps",
                        icon = Icons.AutoMirrored.Filled.Comment,
                        checked = uiState.isSmartReplyEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.toggleSmartReply(enabled)
                            }
                        }
                    )

                    if (uiState.isSmartReplyEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showSmartReplyEditDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit")
                        }
                    }
                }
            }

            // Tone Rewrite Section
            item {
                SettingsSection(title = "Tone Rewrite (${uiState.toneRewriteTrigger})") {
                    SettingsToggle(
                        title = "Enable Tone Rewrite",
                        description = "Rewrite sentences in different tones",
                        icon = Icons.Default.Refresh,
                        checked = uiState.isToneRewriteEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !android.provider.Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.toggleToneRewrite(enabled)
                            }
                        }
                    )

                    if (uiState.isToneRewriteEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showToneRewriteEditDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit")
                        }
                    }
                }
            }

            // @all Trigger Section
            item {
                var showEditAllMenuDialog by remember { mutableStateOf(false) }

                SettingsSection(title = "All Commands (@all)") {
                    SettingsToggle(
                        title = "Enable @all Trigger",
                        description = "Show all commands with @all",
                        icon = Icons.Default.List,
                        checked = uiState.isAllTriggerEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !android.provider.Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.toggleAllTrigger(enabled)
                            }
                        }
                    )

                    if (uiState.isAllTriggerEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showEditAllMenuDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit")
                        }

                        if (showEditAllMenuDialog) {
                            EditAllMenuDialog(
                                currentOrder = uiState.allMenuOrder,
                                isSmartReplyEnabled = uiState.isSmartReplyEnabled,
                                isToneRewriteEnabled = uiState.isToneRewriteEnabled,
                                isSearchTriggerEnabled = uiState.isSearchTriggerEnabled,
                                smartReplyTrigger = uiState.smartReplyTrigger,
                                toneRewriteTrigger = uiState.toneRewriteTrigger,
                                searchTrigger = uiState.searchTrigger,
                                viewModel = viewModel,
                                onDismiss = { showEditAllMenuDialog = false },
                                onSave = { newOrder ->
                                    viewModel.updateAllMenuOrder(newOrder)
                                    showEditAllMenuDialog = false
                                }
                            )
                        }
                    }
                }
            }

            // @search Trigger Section
            item {
                SettingsSection(title = "Web Search (${uiState.searchTrigger})") {
                    SettingsToggle(
                        title = "Enable @search Trigger",
                        description = "Search the web and insert links",
                        icon = Icons.Default.Search,
                        checked = uiState.isSearchTriggerEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !android.provider.Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.toggleSearchTrigger(enabled)
                            }
                        }
                    )

                    if (uiState.isSearchTriggerEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        var showEditSearchTriggerDialog by remember { mutableStateOf(false) }

                        Button(
                            onClick = { showEditSearchTriggerDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit")
                        }

                        if (showEditSearchTriggerDialog) {
                            SearchSettingsDialog(
                                currentTrigger = uiState.searchTrigger,
                                currentEngine = uiState.searchEngine,
                                currentCustomUrl = uiState.customSearchUrl,
                                onDismiss = { showEditSearchTriggerDialog = false },
                                onSave = { trigger, engine, customUrl ->
                                    viewModel.updateSearchTrigger(trigger)
                                    viewModel.updateSearchEngine(engine)
                                    viewModel.updateCustomSearchUrl(customUrl)
                                    showEditSearchTriggerDialog = false
                                }
                            )
                        }
                    }
                }
            }

            // Text Selection Menu Section
            item {
                SettingsSection(title = "Text Selection Menu") {
                    SettingsToggle(
                        title = "Enable Text Selection Menu",
                        description = "Show trigger menu when you select text. Apply AI transformations to selected text only.",
                        icon = Icons.Default.TouchApp,
                        checked = uiState.isTextSelectionMenuEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !android.provider.Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.toggleTextSelectionMenu(enabled)
                            }
                        }
                    )

                    if (uiState.isTextSelectionMenuEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Menu Style Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Menu Style",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            com.rr.aido.data.models.SelectionMenuStyle.values().forEach { style ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updateTextSelectionMenuStyle(style) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (uiState.textSelectionMenuStyle == style),
                                        onClick = { viewModel.updateTextSelectionMenuStyle(style) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = style.displayName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // App Toggle (@on/@off) Section
            item {
                SettingsSection(title = "App On/Off Toggle") {
                    SettingsToggle(
                        title = "Enable @on/@off Controls",
                        description = "Use @on and @off commands to temporarily enable/disable Aido",
                        checked = uiState.isAppToggleEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleAppToggleEnabled(enabled)
                        }
                    )
                }
            }

            // Streaming Animation Section
            item {
                SettingsSection(title = "AI Response Animation") {
                    SettingsToggle(
                        title = "Enable Streaming Animation",
                        description = "Show AI responses appearing word-by-word like typing. When disabled, text appears instantly.",
                        checked = uiState.isStreamingModeEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleStreamingMode(enabled)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EditCommandDialog(
    title: String,
    currentTrigger: String,
    currentPrompt: String,
    defaultPrompt: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var trigger by remember { mutableStateOf(currentTrigger) }
    var prompt by remember { mutableStateOf(currentPrompt.ifEmpty { defaultPrompt }) }

    // Trigger validation
    val validSymbols = "`~!@#$%^&*()-_=+[]{}\\|;:'\",<.>/?"
    val isTriggerValid = when {
        trigger.isEmpty() -> false
        trigger.length < 2 -> false
        !validSymbols.contains(trigger.first()) -> false
        trigger.length == 1 -> false
        else -> {
            val afterSymbol = trigger.substring(1)
            afterSymbol.matches(Regex("\\w+"))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("Trigger") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !isTriggerValid
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Prompt Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trigger, prompt); onDismiss() },
                enabled = isTriggerValid && prompt.isNotEmpty()
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

@Composable
fun SearchSettingsDialog(
    currentTrigger: String,
    currentEngine: com.rr.aido.data.models.SearchEngine,
    currentCustomUrl: String,
    onDismiss: () -> Unit,
    onSave: (String, com.rr.aido.data.models.SearchEngine, String) -> Unit
) {
    var trigger by remember { mutableStateOf(currentTrigger) }
    var selectedEngine by remember { mutableStateOf(currentEngine) }
    var customUrl by remember { mutableStateOf(currentCustomUrl) }

    // Trigger validation
    val validSymbols = "`~!@#$%^&*()-_=+[]{}\\|;:'\",<.>/?"
    val isTriggerValid = when {
        trigger.isEmpty() -> false
        trigger.length < 2 -> false
        !validSymbols.contains(trigger.first()) -> false
        trigger.length == 1 -> false
        else -> {
            val afterSymbol = trigger.substring(1)
            afterSymbol.matches(Regex("\\w+"))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Trigger Input
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("Trigger") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !isTriggerValid,
                    supportingText = { Text("Must start with symbol (e.g. @search)") }
                )

                HorizontalDivider()

                // Search Engine Selection
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = "Search Engine",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    com.rr.aido.data.models.SearchEngine.values().forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedEngine = engine }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedEngine == engine),
                                onClick = { selectedEngine = engine }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = engine.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Custom URL Input (only if Custom is selected)
                if (selectedEngine == com.rr.aido.data.models.SearchEngine.CUSTOM) {
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("Custom URL Template") },
                        placeholder = { Text("https://example.com/search?q=%s") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("Use %s for query placeholder") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trigger, selectedEngine, customUrl); onDismiss() },
                enabled = isTriggerValid && (selectedEngine != com.rr.aido.data.models.SearchEngine.CUSTOM || customUrl.isNotEmpty())
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

@Composable
fun EditAllMenuDialog(
    currentOrder: List<String>,
    isSmartReplyEnabled: Boolean,
    isToneRewriteEnabled: Boolean,
    isSearchTriggerEnabled: Boolean,
    smartReplyTrigger: String,
    toneRewriteTrigger: String,
    searchTrigger: String,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    // Collect preprompts
    val preprompts by viewModel.dataStoreManager.prepromptsFlow.collectAsState(initial = emptyList())

    // Build complete list of available commands
    val availableCommands = remember(
        isSmartReplyEnabled, isToneRewriteEnabled, isSearchTriggerEnabled,
        smartReplyTrigger, toneRewriteTrigger, searchTrigger, preprompts
    ) {
        mutableListOf<Pair<String, String>>().apply {
            // Special commands with their display names
            if (isSmartReplyEnabled) add(smartReplyTrigger to "Smart Reply")
            if (isToneRewriteEnabled) add(toneRewriteTrigger to "Tone Rewrite")
            if (isSearchTriggerEnabled) add(searchTrigger to "Web Search")
            // Preprompts
            preprompts.forEach { add(it.trigger to it.trigger) }
        }
    }

    // Initialize local order - use current order if available, otherwise build default
    val localOrder = remember(currentOrder, availableCommands) {
        if (currentOrder.isNotEmpty()) {
            // Filter current order to only include available commands, then add any new ones
            val orderedTriggers = currentOrder.filter { trigger ->
                availableCommands.any { it.first == trigger }
            }.toMutableList()

            // Add any new commands that aren't in the current order
            availableCommands.forEach { (trigger, _) ->
                if (trigger !in orderedTriggers) {
                    orderedTriggers.add(trigger)
                }
            }
            mutableStateListOf(*orderedTriggers.toTypedArray())
        } else {
            // Default order: special commands first, then preprompts
            mutableStateListOf(*availableCommands.map { it.first }.toTypedArray())
        }
    }

    // Track which commands are visible (checkbox state)
    val visibilityMap = remember(currentOrder, availableCommands) {
        mutableStateMapOf<String, Boolean>().apply {
            availableCommands.forEach { (trigger, _) ->
                // If we have a custom order and the trigger is in it, it's visible
                // Otherwise, default to visible for all
                this[trigger] = if (currentOrder.isNotEmpty()) {
                    trigger in currentOrder
                } else {
                    true
                }
            }
        }
    }

    val displayNameMap = remember(availableCommands) {
        availableCommands.toMap()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit @all Menu") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Drag to reorder, uncheck to hide:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Scrollable list of commands
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(localOrder.size) { index ->
                        val trigger = localOrder[index]
                        val displayName = displayNameMap[trigger] ?: trigger
                        val isVisible = visibilityMap[trigger] ?: true

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Command name and checkbox
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isVisible,
                                        onCheckedChange = { visibilityMap[trigger] = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = trigger,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (trigger != displayName) {
                                            Text(
                                                text = displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Arrow buttons
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val temp = localOrder[index]
                                                localOrder[index] = localOrder[index - 1]
                                                localOrder[index - 1] = temp
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Text("↑", fontSize = 20.sp)
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < localOrder.size - 1) {
                                                val temp = localOrder[index]
                                                localOrder[index] = localOrder[index + 1]
                                                localOrder[index + 1] = temp
                                            }
                                        },
                                        enabled = index < localOrder.size - 1
                                    ) {
                                        Text("↓", fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Preview
                Text(
                    text = "Preview:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                val visibleCommands = localOrder.filter { visibilityMap[it] == true }
                Text(
                    text = if (visibleCommands.isNotEmpty()) {
                        visibleCommands.joinToString(" → ")
                    } else {
                        "(No commands visible)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Save only visible commands in their current order
                    val finalOrder = localOrder.filter { visibilityMap[it] == true }
                    onSave(finalOrder)
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

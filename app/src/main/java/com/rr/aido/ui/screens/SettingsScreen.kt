package com.rr.aido.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rr.aido.data.models.AiProvider
import com.rr.aido.data.models.GeminiModels
import com.rr.aido.data.models.ProcessingAnimationType
import com.rr.aido.data.models.TriggerMethod
import com.rr.aido.ui.viewmodels.SettingsViewModel
import com.rr.aido.utils.AccessibilityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToKeyboardSettings: () -> Unit,
    onNavigateToSpecialCommands: () -> Unit,
    onNavigateToManageApps: () -> Unit,
    onNavigateToTextShortcuts: () -> Unit,
    targetSection: String? = null,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState(initial = com.rr.aido.data.models.Settings())

    // Handle deep link to specific section
    LaunchedEffect(targetSection) {
        if (targetSection != null) {
            viewModel.toggleSection("Trigger & Setup", false)
            viewModel.toggleSection(targetSection, true)
        }
    }

    // Setup logic
    var expanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isKeyboardEnabled by remember { mutableStateOf(false) }
    var isKeyboardActive by remember { mutableStateOf(false) }

    // Check status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
            isKeyboardEnabled = AccessibilityUtils.isAidoKeyboardEnabled(context)
            isKeyboardActive = AccessibilityUtils.isAidoKeyboardActive(context)
            delay(1000)
        }
    }

    // Show snackbar for test result
    LaunchedEffect(uiState.testResult, uiState.errorMessage) {
        uiState.testResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTestResult()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTestResult()
        }
    }

    // Modern Gradient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background blobs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PremiumIconButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Trigger & Setup Section
                item {
                    PremiumSection(
                        title = "Trigger & Setup",
                        icon = Icons.Outlined.Build,
                        isExpanded = uiState.expandedSections["Trigger & Setup"] ?: true,
                        onExpandedChange = { viewModel.toggleSection("Trigger & Setup", it) }
                    ) {
                        TriggerMethodSelector(
                            currentMethod = settings.triggerMethod,
                            onMethodSelected = { viewModel.updateTriggerMethod(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        when (settings.triggerMethod) {
                            TriggerMethod.ACCESSIBILITY -> {
                                PremiumStatusCard(
                                    title = "Accessibility",
                                    description = "Required to detect triggers in other apps",
                                    isEnabled = isAccessibilityEnabled,
                                    buttonText = "Enable",
                                    onAction = {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                )
                            }
                            TriggerMethod.KEYBOARD -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PremiumStatusCard(
                                        title = "Enable Keyboard",
                                        description = "Allow Aido Keyboard in system settings",
                                        isEnabled = isKeyboardEnabled,
                                        buttonText = "Enable",
                                        onAction = { AccessibilityUtils.redirectToKeyboardSettings(context) }
                                    )

                                    if (isKeyboardEnabled) {
                                        PremiumStatusCard(
                                            title = "Select Keyboard",
                                            description = "Set Aido as your default keyboard",
                                            isEnabled = isKeyboardActive,
                                            buttonText = "Select",
                                            onAction = { AccessibilityUtils.showKeyboardPicker(context) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Configuration Section
                item {
                    PremiumSection(
                        title = "AI Configuration",
                        icon = Icons.Outlined.SmartToy,
                        isExpanded = uiState.expandedSections["AI Configuration"] ?: false,
                        onExpandedChange = { viewModel.toggleSection("AI Configuration", it) }
                    ) {
                        AiProviderSelector(
                            currentProvider = uiState.provider,
                            onProviderSelected = { viewModel.updateProvider(it) }
                        )

                        AnimatedVisibility(visible = uiState.provider == AiProvider.GEMINI) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                GeminiConfiguration(
                                    apiKey = uiState.apiKey,
                                    onApiKeyChange = { viewModel.updateApiKey(it) },
                                    selectedModel = uiState.selectedModel,
                                    onModelSelected = { viewModel.updateSelectedModel(it) },
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it }
                                )
                            }
                        }

                        AnimatedVisibility(visible = uiState.provider == AiProvider.CUSTOM) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                CustomApiConfiguration(
                                    customApiUrl = uiState.customApiUrl,
                                    onCustomApiUrlChange = { viewModel.updateCustomApiUrl(it) },
                                    customApiKey = uiState.customApiKey,
                                    onCustomApiKeyChange = { viewModel.updateCustomApiKey(it) },
                                    customModelName = uiState.customModelName,
                                    onCustomModelNameChange = { viewModel.updateCustomModelName(it) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Test Connection Button
                        val actionButtonEnabled = when (uiState.provider) {

                            AiProvider.GEMINI -> !uiState.isTesting && uiState.apiKey.isNotEmpty()
                            AiProvider.CUSTOM -> !uiState.isTesting && uiState.customApiUrl.isNotEmpty() && uiState.customApiKey.isNotEmpty()
                        }

                        Button(
                            onClick = { viewModel.testAndSaveApiKey() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = actionButtonEnabled,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (uiState.isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Verifying...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.CheckCircle, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Verify & Save",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Features Section
                item {
                    PremiumSection(
                        title = "Features",
                        icon = Icons.Outlined.Star,
                        isExpanded = uiState.expandedSections["Features"] ?: false,
                        onExpandedChange = { viewModel.toggleSection("Features", it) }
                    ) {
                        PremiumSettingItem(
                            title = "Special Commands",
                            subtitle = "Smart Reply, Tone Rewrite, @all",
                            icon = Icons.Default.AutoAwesome,
                            onClick = onNavigateToSpecialCommands
                        )

                        PremiumSettingItem(
                            title = "Text Shortcuts",
                            subtitle = "Create snippets (e.g., !email -> my@email.com)",
                            icon = Icons.Default.Bolt,
                            onClick = onNavigateToTextShortcuts
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // Processing Animation
                        var showAnimationDialog by remember { mutableStateOf(false) }
                        val overlayPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) {
                             if (Settings.canDrawOverlays(context)) {
                                 coroutineScope.launch { snackbarHostState.showSnackbar("Overlay permission granted!") }
                             }
                        }

                        PremiumSwitchItem(
                            title = "Processing Animation",
                            subtitle = if (settings.isProcessingAnimationEnabled) settings.processingAnimationType.displayName else "Show visual feedback",
                            icon = Icons.Outlined.Animation, // Requires newer Material Icon, using Outlined.Build if Animation missing
                            checked = settings.isProcessingAnimationEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    viewModel.toggleProcessingAnimation(true)
                                    if (!Settings.canDrawOverlays(context)) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                        try { overlayPermissionLauncher.launch(intent) } catch (e: Exception) {}
                                    }
                                } else {
                                    viewModel.toggleProcessingAnimation(false)
                                }
                            },
                            onClick = {
                                if (settings.isProcessingAnimationEnabled) showAnimationDialog = true
                                else {
                                     viewModel.toggleProcessingAnimation(true)
                                     if (!Settings.canDrawOverlays(context)) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                        try { overlayPermissionLauncher.launch(intent) } catch (e: Exception) {}
                                     }
                                }
                            }
                        )

                        if (showAnimationDialog) {
                            AnimationSelectionDialog(
                                currentType = settings.processingAnimationType,
                                onDismiss = { showAnimationDialog = false },
                                onTypeSelected = { viewModel.updateProcessingAnimationType(it); showAnimationDialog = false }
                            )
                        }

                        PremiumSwitchItem(
                            title = "Undo/Redo Popup",
                            subtitle = "Show options after generation",
                            icon = Icons.Default.Refresh,
                            checked = uiState.isUndoRedoEnabled,
                            onCheckedChange = { viewModel.toggleUndoRedo(it) }
                        )
                    }
                }

                // Keyboard Section
                item {
                    PremiumSection(
                        title = "Keyboard",
                        icon = Icons.Outlined.Keyboard,
                        isExpanded = uiState.expandedSections["Keyboard"] ?: false,
                        onExpandedChange = { viewModel.toggleSection("Keyboard", it) }
                    ) {
                        PremiumSettingItem(
                            title = "Keyboard Settings",
                            subtitle = "Theme, haptics, and layout",
                            icon = Icons.Outlined.Keyboard,
                            onClick = onNavigateToKeyboardSettings
                        )
                    }
                }

                // Privacy & Data Section
                item {
                    PremiumSection(
                        title = "Privacy & Data",
                        icon = Icons.Outlined.Security,
                        isExpanded = uiState.expandedSections["Privacy & Data"] ?: false,
                        onExpandedChange = { viewModel.toggleSection("Privacy & Data", it) }
                    ) {
                        PremiumSwitchItem(
                            title = "Offline Mode",
                            subtitle = "Disable network calls for privacy",
                            icon = Icons.Outlined.CloudOff,
                            checked = uiState.isOfflineMode,
                            onCheckedChange = { viewModel.toggleOfflineMode(it) }
                        )

                        PremiumSettingItem(
                            title = "Manage Apps",
                            subtitle = "Blacklist apps where Aido should not appear",
                            icon = Icons.Outlined.Apps, // Fallback if Apps icon missing
                            onClick = onNavigateToManageApps
                        )
                    }
                }

                // Backup & Restore
                item {
                    BackupRestoreSection(viewModel, snackbarHostState)
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

// --- Premium Components ---

@Composable
fun PremiumSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!isExpanded) }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrow")
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun PremiumSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun PremiumSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PremiumIconButton(
    onClick: () -> Unit,
    icon: ImageVector
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun PremiumStatusCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    buttonText: String,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isEnabled) {

                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(buttonText, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun BackupRestoreSection(viewModel: SettingsViewModel, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup { json ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray())
                        output.flush()
                    }
                    coroutineScope.launch { snackbarHostState.showSnackbar("Backup saved successfully") }
                } catch (e: Exception) {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Failed to save: ${e.message}") }
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                     viewModel.importBackup(json) { error ->
                        val msg = error ?: "Backup restored successfully!"
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                }
            } catch (e: Exception) {
                coroutineScope.launch { snackbarHostState.showSnackbar("Failed to read backup") }
            }
        }
    }

    PremiumSection(
        title = "Backup & Restore",
        icon = Icons.Outlined.CloudSync, // Requires dependency or workaround if missing
        isExpanded = viewModel.uiState.collectAsState().value.expandedSections["Backup & Restore"] ?: false,
        onExpandedChange = { viewModel.toggleSection("Backup & Restore", it) }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    try { createDocumentLauncher.launch("aido-backup-${System.currentTimeMillis()}.json") }
                    catch (e: Exception) { coroutineScope.launch { snackbarHostState.showSnackbar("Error: No file manager found.") } }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp)) // Fallback icon
                Spacer(Modifier.width(8.dp))
                Text("Backup")
            }
            Button(
                onClick = {
                    try { openDocumentLauncher.launch(arrayOf("application/json", "text/plain")) }
                    catch (e: Exception) { coroutineScope.launch { snackbarHostState.showSnackbar("Error: No file picker found.") } }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp)) // Fallback icon
                Spacer(Modifier.width(8.dp))
                Text("Restore")
            }
        }
    }
}

@Composable
fun AnimationSelectionDialog(
    currentType: ProcessingAnimationType,
    onDismiss: () -> Unit,
    onTypeSelected: (ProcessingAnimationType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Animation Style") },
        text = {
            LazyColumn {
                items(ProcessingAnimationType.values().size) { index ->
                    val type = ProcessingAnimationType.values()[index]
                    val isSelected = type == currentType
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTypeSelected(type) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = { onTypeSelected(type) })
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(type.displayName, fontWeight = FontWeight.Bold)
                            Text(type.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// Re-implementing helper components from previous file to ensure compatibility
// but styling them to fit the new theme if needed.

@Composable
fun TriggerMethodSelector(
    currentMethod: TriggerMethod,
    onMethodSelected: (TriggerMethod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TriggerMethod.values().forEach { method ->
            val isSelected = currentMethod == method
            val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clickable { onMethodSelected(method) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (method == TriggerMethod.ACCESSIBILITY) Icons.Outlined.Build else Icons.Outlined.Keyboard,
                        contentDescription = null,
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = method.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun AiProviderSelector(
    currentProvider: AiProvider,
    onProviderSelected: (AiProvider) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AiProvider.values().forEach { provider ->
            val isSelected = currentProvider == provider

            // Explicit colors for visibility
            val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            val titleColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            val descriptionColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            val radioButtonColors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProviderSelected(provider) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onProviderSelected(provider) },
                        colors = radioButtonColors
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = titleColor
                        )
                        Text(
                            text = provider.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = descriptionColor
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiConfiguration(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API Key") },
            placeholder = { Text("Paste Gemini API Key") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = selectedModel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                GeminiModels.models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            onModelSelected(model)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomApiConfiguration(
    customApiUrl: String,
    onCustomApiUrlChange: (String) -> Unit,
    customApiKey: String,
    onCustomApiKeyChange: (String) -> Unit,
    customModelName: String,
    onCustomModelNameChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = customApiUrl,
            onValueChange = onCustomApiUrlChange,
            label = { Text("Base URL") },
            placeholder = { Text("https://api.openai.com/v1/") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
            value = customApiKey,
            onValueChange = onCustomApiKeyChange,
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
            value = customModelName,
            onValueChange = onCustomModelNameChange,
            label = { Text("Model Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// --- Legacy Compatibility Components (Required for other screens) ---

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
             elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector = Icons.Default.Settings
) {
    PremiumSwitchItem(
        title = title,
        subtitle = description,
        icon = icon,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

@Composable
fun SettingsTile(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    PremiumSettingItem(
        title = title,
        subtitle = description,
        icon = icon,
        onClick = onClick
    )
}

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: androidx.compose.ui.unit.Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    androidx.compose.material3.HorizontalDivider(modifier = modifier, thickness = thickness, color = color)
}

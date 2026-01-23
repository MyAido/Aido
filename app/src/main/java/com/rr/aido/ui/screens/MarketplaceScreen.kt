package com.rr.aido.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.rr.aido.R
import com.rr.aido.data.models.*
import com.rr.aido.ui.viewmodels.MarketplaceUiState
import com.rr.aido.ui.viewmodels.MarketplaceTab
import com.rr.aido.ui.viewmodels.MarketplaceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val showUploadDialog by viewModel.showUploadDialog.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }

    // Show snackbar for errors and success messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marketplace") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // User Account Button
                    IconButton(onClick = { showAccountMenu = true }) {
                        if (currentUser?.isAnonymous == false) {
                            Icon(Icons.Default.Person, "Account")
                        } else {
                            Icon(Icons.Default.AccountCircle, "Anonymous")
                        }
                    }

                    IconButton(onClick = { viewModel.syncPreprompts() }) {
                        Icon(Icons.Default.Refresh, "Sync")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == MarketplaceTab.BROWSE,
                    onClick = { viewModel.selectTab(MarketplaceTab.BROWSE) },
                    text = { Text("Browse") }
                )
                Tab(
                    selected = selectedTab == MarketplaceTab.MY_SHARED,
                    onClick = { viewModel.selectTab(MarketplaceTab.MY_SHARED) },
                    text = { Text("My Shared") }
                )
                Tab(
                    selected = selectedTab == MarketplaceTab.FAVORITES,
                    onClick = { viewModel.selectTab(MarketplaceTab.FAVORITES) },
                    text = { Text("Favorites") }
                )
            }

            // Content
            when (selectedTab) {
                MarketplaceTab.BROWSE -> BrowseTab(uiState, viewModel)
                MarketplaceTab.MY_SHARED -> MySharedTab(uiState, viewModel)
                MarketplaceTab.FAVORITES -> FavoritesTab(uiState, viewModel)
            }
        }
    }

    // Account Menu Dialog
    if (showAccountMenu) {
        AccountDialog(
            currentUser = currentUser,
            onDismiss = { showAccountMenu = false },
            onSignIn = {
                showAccountMenu = false
                showSignInDialog = true
            },
            onSignOut = {
                viewModel.signOut()
                showAccountMenu = false
            }
        )
    }

    // Sign-In Dialog
    if (showSignInDialog) {
        SignInDialog(
            onDismiss = { showSignInDialog = false },
            onSignIn = { email, password, displayName ->
                viewModel.signInWithEmail(email, password, displayName)
                showSignInDialog = false
            }
        )
    }

    // Upload Dialog
    if (showUploadDialog) {
        UploadPrepromptDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideUploadDialog() }
        )
    }
}

@Composable
fun BrowseTab(uiState: MarketplaceUiState, viewModel: MarketplaceViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showSearchBar by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // Search Bar
        if (showSearchBar) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchPreprompts(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search preprompts...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchPreprompts("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true
            )
        } else {
            // Search and Filter buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                var showFilterDialog by remember { mutableStateOf(false) }

                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.MoreVert, "Filter")
                }
                IconButton(onClick = { showSearchBar = true }) {
                    Icon(Icons.Default.Search, "Search")
                }

                if (showFilterDialog) {
                    FilterDialog(
                        currentFilter = uiState.filter ?: MarketplaceFilter(),
                        onDismiss = { showFilterDialog = false },
                        onApply = { filter ->
                            viewModel.applyFilter(filter)
                            showFilterDialog = false
                        }
                    )
                }
            }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val prepromptsToShow = if (searchQuery.isNotEmpty() || uiState.filter != null) {
                uiState.searchResults
            } else {
                uiState.featuredPreprompts
            }

            if (prepromptsToShow.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotEmpty()) "No results found" else "No preprompts available",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (searchQuery.isNotEmpty()) "Try different search terms" else "Be the first to share!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(prepromptsToShow) { preprompt ->
                        PrepromptCard(preprompt, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MySharedTab(uiState: MarketplaceUiState, viewModel: MarketplaceViewModel) {
    var showUploadOptions by remember { mutableStateOf(false) }
    var uploadMode by remember { mutableStateOf<UploadMode?>(null) }
    var selectedPrepromptForUpload by remember { mutableStateOf<Preprompt?>(null) }
    var createdPreprompt by remember { mutableStateOf<Preprompt?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (uiState.userSharedPreprompts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No shared preprompts yet",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Share your custom commands with the community",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { showUploadOptions = true }) {
                    Icon(Icons.Default.Add, "Upload")
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Preprompt")
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                items(uiState.userSharedPreprompts) { preprompt ->
                    PrepromptCard(preprompt, viewModel)
                }
            }
        }

        // Floating Action Button for upload
        FloatingActionButton(
            onClick = { showUploadOptions = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Upload")
        }
    }

    // Step 1: Upload Options Dialog
    if (showUploadOptions) {
        UploadOptionDialog(
            onDismiss = { showUploadOptions = false },
            onUploadExisting = {
                uploadMode = UploadMode.EXISTING
                showUploadOptions = false
            },
            onCreateNew = {
                uploadMode = UploadMode.CREATE_NEW
                showUploadOptions = false
            }
        )
    }

    // Step 2a: Select Existing Preprompt
    if (uploadMode == UploadMode.EXISTING && selectedPrepromptForUpload == null) {
        SelectPrepromptDialog(
            viewModel = viewModel,
            onDismiss = { uploadMode = null },
            onSelect = { preprompt ->
                selectedPrepromptForUpload = preprompt
            }
        )
    }

    // Step 2b: Create New Preprompt
    if (uploadMode == UploadMode.CREATE_NEW && createdPreprompt == null) {
        CreatePrepromptDialog(
            onDismiss = { uploadMode = null },
            onCreate = { preprompt ->
                createdPreprompt = preprompt
            }
        )
    }

    // Step 3: Upload Details Form (after selection/creation)
    val prepromptToUpload = selectedPrepromptForUpload ?: createdPreprompt
    if (prepromptToUpload != null) {
        UploadDetailsDialog(
            preprompt = prepromptToUpload,
            viewModel = viewModel,
            onDismiss = {
                uploadMode = null
                selectedPrepromptForUpload = null
                createdPreprompt = null
            },
            onUpload = { title, description, category ->
                viewModel.uploadPreprompt(prepromptToUpload, title, description, category)
                uploadMode = null
                selectedPrepromptForUpload = null
                createdPreprompt = null
            }
        )
    }
}

enum class UploadMode {
    EXISTING,
    CREATE_NEW
}

@Composable
fun FavoritesTab(uiState: MarketplaceUiState, viewModel: MarketplaceViewModel) {
    // Combine Firebase favorites and anonymous favorites
    val allPreprompts = (uiState.featuredPreprompts + uiState.searchResults + uiState.favoritePreprompts).distinctBy { it.id }
    val favoritedItems = allPreprompts.filter { uiState.favoritePrepromptIds.contains(it.id) }

    if (favoritedItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No favorites yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Like preprompts to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(favoritedItems) { preprompt ->
                PrepromptCard(preprompt, viewModel)
            }
        }
    }
}

@Composable
fun PrepromptCard(preprompt: SharedPreprompt, viewModel: MarketplaceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isInstalled = uiState.installedTriggers.contains(preprompt.trigger)
    val isOwner = currentUser?.uid == preprompt.authorId
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        preprompt.category.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (isOwner) {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Title
            Text(
                preprompt.title.ifBlank { preprompt.trigger },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Trigger
            Text(
                preprompt.trigger,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                preprompt.description.ifBlank { preprompt.instruction.take(100) + if (preprompt.instruction.length > 100) "..." else "" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(12.dp))

            androidx.compose.material3.HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(12.dp))

            // Author and Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "By ${preprompt.authorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "${preprompt.downloads} installs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    if (isInstalled) {
                        Button(
                            onClick = { },
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Check, "Installed")
                            Spacer(Modifier.width(4.dp))
                            Text("Installed")
                        }
                    } else {
                        Button(onClick = { viewModel.installPreprompt(preprompt) }) {
                            Icon(Icons.Default.Add, "Install")
                            Spacer(Modifier.width(4.dp))
                            Text("Install")
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    var showDetailsDialog by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { showDetailsDialog = true }) {
                        Text("Details")
                    }

                    if (showDetailsDialog) {
                        PrepromptDetailsDialog(
                            preprompt = preprompt,
                            onDismiss = { showDetailsDialog = false },
                            onInstall = {
                                viewModel.installPreprompt(preprompt)
                                showDetailsDialog = false
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(preprompt.id) }
                    ) {
                        val isFavorite = uiState.favoritePrepromptIds.contains(preprompt.id)
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorite",
                            tint = if (isFavorite)
                                androidx.compose.ui.graphics.Color.Red
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (preprompt.likes > 0) {
                        Text(
                            "${preprompt.likes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog && isOwner) {
        EditPrepromptDialog(
            preprompt = preprompt,
            onDismiss = { showEditDialog = false },
            onUpdate = { title, description, category ->
                viewModel.updatePreprompt(preprompt.id, title, description, category)
                showEditDialog = false
            },
            onDelete = {
                viewModel.deletePreprompt(preprompt.id)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPrepromptDialog(
    viewModel: MarketplaceViewModel,
    onDismiss: () -> Unit
) {
    var selectedPreprompt by remember { mutableStateOf<Preprompt?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PrepromptCategory.GENERAL) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var localPreprompts by remember { mutableStateOf<List<Preprompt>>(emptyList()) }

    LaunchedEffect(Unit) {
        localPreprompts = viewModel.getLocalPreprompts()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Preprompt to Marketplace") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Select a preprompt to share:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))

                if (localPreprompts.isEmpty()) {
                    Text(
                        "No preprompts found. Create some preprompts first!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    localPreprompts.forEach { preprompt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = { selectedPreprompt = preprompt },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPreprompt == preprompt)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    preprompt.trigger,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    preprompt.instruction.take(60) + "...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (selectedPreprompt != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Title:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Display name for marketplace") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Description:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Describe what this command does...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Category:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = showCategoryMenu,
                        onExpandedChange = { showCategoryMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.getDisplayName(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            PrepromptCategory.values().forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.getDisplayName()) },
                                    onClick = {
                                        selectedCategory = category
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedPreprompt?.let { preprompt ->
                        viewModel.uploadPreprompt(
                            preprompt = preprompt,
                            title = title,
                            description = description,
                            category = selectedCategory
                        )
                        onDismiss()
                    }
                },
                enabled = selectedPreprompt != null && title.isNotBlank() && description.isNotBlank()
            ) {
                Text("Upload")
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
fun UploadOptionDialog(
    onDismiss: () -> Unit,
    onUploadExisting: () -> Unit,
    onCreateNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Preprompt") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Choose how you want to upload:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onUploadExisting
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Upload Existing",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Choose from your saved preprompts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCreateNew
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Create,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Create & Upload",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Create a new preprompt to share",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SelectPrepromptDialog(
    viewModel: MarketplaceViewModel,
    onDismiss: () -> Unit,
    onSelect: (Preprompt) -> Unit
) {
    var localPreprompts by remember { mutableStateOf<List<Preprompt>>(emptyList()) }
    var selectedPreprompt by remember { mutableStateOf<Preprompt?>(null) }

    LaunchedEffect(Unit) {
        localPreprompts = viewModel.getLocalPreprompts()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Preprompt") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (localPreprompts.isEmpty()) {
                    Text(
                        "No preprompts found. Create some preprompts first!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        "Choose a preprompt to share:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    localPreprompts.forEach { preprompt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = { selectedPreprompt = preprompt },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPreprompt == preprompt)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    preprompt.trigger,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    preprompt.instruction.take(80) + if (preprompt.instruction.length > 80) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedPreprompt?.let { onSelect(it) }
                },
                enabled = selectedPreprompt != null
            ) {
                Text("Next")
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
fun CreatePrepromptDialog(
    onDismiss: () -> Unit,
    onCreate: (Preprompt) -> Unit
) {
    var trigger by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Preprompt") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Trigger (e.g., @mytrigger):",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    placeholder = { Text("@mytrigger") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    "Instruction:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    placeholder = { Text("Enter the AI instruction...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    "Example (optional):",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    placeholder = { Text("Example usage...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (trigger.isNotBlank() && instruction.isNotBlank()) {
                        onCreate(
                            Preprompt(
                                trigger = trigger.trim(),
                                instruction = instruction.trim(),
                                example = example.trim(),
                                isDefault = false
                            )
                        )
                    }
                },
                enabled = trigger.isNotBlank() && instruction.isNotBlank()
            ) {
                Text("Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDetailsDialog(
    preprompt: Preprompt,
    viewModel: MarketplaceViewModel,
    onDismiss: () -> Unit,
    onUpload: (String, String, PrepromptCategory) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PrepromptCategory.GENERAL) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Details") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Selected: ${preprompt.trigger}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            preprompt.instruction.take(60) + if (preprompt.instruction.length > 60) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Title:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Display name for marketplace") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    "Description:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Describe what this command does...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    "Category:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = showCategoryMenu,
                    onExpandedChange = { showCategoryMenu = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.getDisplayName(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        PrepromptCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.getDisplayName()) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpload(title, description, selectedCategory)
                    onDismiss()
                },
                enabled = title.isNotBlank() && description.isNotBlank()
            ) {
                Text("Upload")
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
fun AccountDialog(
    currentUser: UserProfile?,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Account") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                if (currentUser?.isAnonymous == false) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(currentUser.displayName, style = MaterialTheme.typography.titleMedium)
                                currentUser.email?.let { email ->
                                    Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Your uploads will show your name", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Anonymous User", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            Text("Uploads will show as 'Anonymous'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (currentUser?.isAnonymous != false) {
                    Text("Sign in to:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("• Show your name on uploads", style = MaterialTheme.typography.bodySmall)
                    Text("• Save favorites across devices", style = MaterialTheme.typography.bodySmall)
                    Text("• Manage your shared preprompts", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (currentUser?.isAnonymous != false) {
                Button(onClick = onSignIn) {
                    Text("Sign In")
                }
            }
        },
        dismissButton = {
            if (currentUser?.isAnonymous == false) {
                TextButton(onClick = onSignOut) {
                    Text("Sign Out")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
fun PrepromptDetailsDialog(
    preprompt: SharedPreprompt,
    onDismiss: () -> Unit,
    onInstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                preprompt.title.ifBlank { preprompt.trigger },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Category Badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        preprompt.category.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Trigger
                Text("Trigger:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    preprompt.trigger,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                Spacer(Modifier.height(12.dp))

                // Instruction
                Text("Instruction:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    preprompt.instruction,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (preprompt.example.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Example:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            preprompt.example,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                androidx.compose.material3.HorizontalDivider()

                Spacer(Modifier.height(12.dp))

                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Author", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(preprompt.authorName, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Downloads", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${preprompt.downloads}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onInstall) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    currentFilter: MarketplaceFilter,
    onDismiss: () -> Unit,
    onApply: (MarketplaceFilter) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(currentFilter.category) }
    var minDownloads by remember { mutableStateOf(currentFilter.minDownloads.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Preprompts") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))

                // Category chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                    PrepromptCategory.values().forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.getDisplayName()) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Minimum Downloads", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = minDownloads,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            minDownloads = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Min downloads") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        MarketplaceFilter(
                            category = selectedCategory,
                            minDownloads = minDownloads.toIntOrNull() ?: 0
                        )
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

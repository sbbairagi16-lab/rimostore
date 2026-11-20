package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.AppListing
import com.example.data.DeveloperProfile
import com.example.data.InstalledApp
import com.example.data.StoreRepository
import com.example.data.UiState
import com.example.ui.components.iconForPreset
import com.example.ui.screens.AppDetailScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CatalogBrowseScreen
import com.example.ui.screens.DeveloperStudioScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PublishAppScreen
import com.example.ui.theme.PulseStoreTheme
import com.example.viewmodel.StoreViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseStoreTheme {
                PulseStoreRoot()
            }
        }
    }
}

private enum class StoreTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    DISCOVER(R.string.tab_discover, Icons.Filled.Explore, Icons.Outlined.Explore, "tab_discover"),
    GAMES(R.string.tab_games, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, "tab_games"),
    APPS(R.string.tab_apps, Icons.Filled.Apps, Icons.Outlined.Apps, "tab_apps"),
    LIBRARY(R.string.tab_library, Icons.Filled.DownloadDone, Icons.Outlined.DownloadDone, "tab_library"),
    STUDIO(R.string.tab_studio, Icons.Filled.Code, Icons.Outlined.Code, "tab_studio")
}

@Composable
private fun PulseStoreRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* Reactive AuthStateListener updates currentUser */ })
    } else {
        key(user.uid) {
            PulseStoreShell(user = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PulseStoreShell(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StoreViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StoreViewModel(
                    repository = StoreRepository(db),
                    currentUserId = user.uid,
                    currentUserDisplayName = user.displayName ?: "PulseStore Member",
                    currentUserEmail = user.email ?: ""
                )
            }
        }
    )

    val publishedAppsState by viewModel.publishedAppsState.collectAsStateWithLifecycle()
    val myDeveloperAppsState by viewModel.myDeveloperAppsState.collectAsStateWithLifecycle()
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val myDeveloperProfileState by viewModel.myDeveloperProfileState.collectAsStateWithLifecycle()
    val myPrivateAccount by viewModel.myPrivateDeveloperAccount.collectAsStateWithLifecycle()
    val selectedAppReviewsState by viewModel.selectedAppReviewsState.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGameCategory by viewModel.selectedGameCategory.collectAsStateWithLifecycle()
    val selectedAppCategory by viewModel.selectedAppCategory.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val viewedStudioProfile by viewModel.viewedStudioProfile.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    var activeTab by rememberSaveable { mutableStateOf(StoreTab.DISCOVER) }
    var openDetailAppId by rememberSaveable { mutableStateOf<String?>(null) }
    var isPublishingScreenOpen by rememberSaveable { mutableStateOf(false) }
    var editingAppId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }

    val publishedApps = (publishedAppsState as? UiState.Success)?.data.orEmpty()
    val myDevApps = (myDeveloperAppsState as? UiState.Success)?.data.orEmpty()
    val libraryApps = (libraryState as? UiState.Success)?.data.orEmpty()

    val catalogById = remember(publishedApps, myDevApps) {
        val map = LinkedHashMap<String, AppListing>()
        publishedApps.forEach { map[it.id] = it }
        myDevApps.forEach { map[it.id] = it }
        map
    }
    val installedById = remember(libraryApps) {
        libraryApps.associateBy { it.appId }
    }
    val pendingUpdatesCount = remember(libraryApps, catalogById) {
        libraryApps.count { installed ->
            val latest = catalogById[installed.appId]
            latest != null && latest.versionName != installed.installedVersion
        }
    }

    fun launchInstalledSoftware(title: String, downloadUrl: String?) {
        if (!downloadUrl.isNullOrBlank() && downloadUrl.startsWith("https://")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
            runCatching { context.startActivity(intent) }
                .onFailure { viewModel.postMessage("Opened $title from PulseStore Library") }
        } else {
            viewModel.postMessage("Launching $title from your PulseStore Library…")
        }
    }

    fun openDetail(appId: String) {
        openDetailAppId = appId
        viewModel.selectAppForDetail(appId)
    }

    fun closeDetail() {
        openDetailAppId = null
        viewModel.selectAppForDetail(null)
    }

    // Account Dialog
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("PulseStore Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UserAvatarBadge(photoUrl = user.photoUrl?.toString(), name = user.displayName)
                        Column {
                            Text(
                                text = user.displayName ?: "PulseStore Member",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = user.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    val devProfile = (myDeveloperProfileState as? UiState.Success)?.data
                    if (devProfile != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Developer Studio: ${devProfile.studioName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccountDialog = false
                        signOutUser(credentialManager, scope)
                    },
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Public Developer Studio Preview Modal
    viewedStudioProfile?.let { studio ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissStudioProfileSheet() }
        ) {
            DeveloperProfileSheetContent(
                profile = studio,
                onVisitWebsite = { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    runCatching { context.startActivity(intent) }
                },
                onClose = { viewModel.dismissStudioProfileSheet() }
            )
        }
    }

    // Secondary Full-Screen Routes: Publish / Edit Software & App Detail
    val activeDetailApp = openDetailAppId?.let { catalogById[it] }
    val activeStudioProfile = (myDeveloperProfileState as? UiState.Success)?.data

    when {
        isPublishingScreenOpen -> {
            val editingApp = editingAppId?.let { catalogById[it] }
            PublishAppScreen(
                studioName = activeStudioProfile?.studioName ?: user.displayName ?: "Indie Studio",
                editingApp = editingApp,
                isBusy = isBusy,
                onBack = {
                    isPublishingScreenOpen = false
                    editingAppId = null
                },
                onSubmit = { draft ->
                    val devName = activeStudioProfile?.studioName ?: user.displayName ?: "Indie Studio"
                    viewModel.saveSoftwareListing(draft, devName) { savedId ->
                        isPublishingScreenOpen = false
                        editingAppId = null
                        openDetail(savedId)
                    }
                }
            )
        }

        activeDetailApp != null -> {
            AppDetailScreen(
                app = activeDetailApp,
                currentUserId = user.uid,
                installedApp = installedById[activeDetailApp.id],
                downloadProgress = downloadProgress[activeDetailApp.id],
                reviewsState = selectedAppReviewsState,
                isBusy = isBusy,
                onBack = { closeDetail() },
                onInstall = { viewModel.startDownloadOrUpdate(activeDetailApp, isUpdate = false) },
                onUpdate = { viewModel.startDownloadOrUpdate(activeDetailApp, isUpdate = true) },
                onUninstall = { viewModel.uninstallFromLibrary(activeDetailApp.id, activeDetailApp.title) },
                onLaunch = { launchInstalledSoftware(activeDetailApp.title, activeDetailApp.downloadUrl) },
                onInspectDeveloper = {
                    viewModel.inspectDeveloperStudio(activeDetailApp.developerId, activeDetailApp.developerName)
                },
                onSubmitReview = { rating, comment, existing ->
                    viewModel.submitReview(activeDetailApp.id, rating, comment, existing)
                },
                onDeleteReview = { review ->
                    viewModel.deleteReview(activeDetailApp.id, review)
                },
                onEditIfOwner = if (activeDetailApp.developerId == user.uid && !activeDetailApp.starterCatalog) {
                    {
                        editingAppId = activeDetailApp.id
                        isPublishingScreenOpen = true
                    }
                } else null
            )
        }

        else -> {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 720.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            StoreTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { activeTab = tab },
                                    icon = {
                                        TabIconWithBadge(
                                            tab = tab,
                                            selected = selected,
                                            badgeCount = if (tab == StoreTab.LIBRARY) pendingUpdatesCount else 0
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    Scaffold(
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_pulsestore_mark_1795157579887),
                                            contentDescription = "PulseStore logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                        )
                                        Text(
                                            text = when (activeTab) {
                                                StoreTab.DISCOVER -> stringResource(R.string.app_name)
                                                StoreTab.GAMES -> "Games"
                                                StoreTab.APPS -> "Apps"
                                                StoreTab.LIBRARY -> "My Library"
                                                StoreTab.STUDIO -> "Developer Studio"
                                            },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                actions = {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .clip(CircleShape)
                                            .clickable { showAccountDialog = true }
                                            .testTag("account_avatar_button")
                                    ) {
                                        UserAvatarBadge(
                                            photoUrl = user.photoUrl?.toString(),
                                            name = user.displayName
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        },
                        bottomBar = {
                            if (!useNavRail) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp
                                ) {
                                    StoreTab.entries.forEach { tab ->
                                        val selected = activeTab == tab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { activeTab = tab },
                                            icon = {
                                                TabIconWithBadge(
                                                    tab = tab,
                                                    selected = selected,
                                                    badgeCount = if (tab == StoreTab.LIBRARY) pendingUpdatesCount else 0
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(tab.labelRes),
                                                    maxLines = 1
                                                )
                                            },
                                            modifier = Modifier.testTag(tab.tag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (activeTab) {
                                StoreTab.DISCOVER -> DiscoverScreen(
                                    appsState = publishedAppsState,
                                    installedById = installedById,
                                    downloadProgress = downloadProgress,
                                    searchQuery = searchQuery,
                                    isBusy = isBusy,
                                    onSearchChange = viewModel::updateSearchQuery,
                                    onAppClick = { openDetail(it.id) },
                                    onInstall = { viewModel.startDownloadOrUpdate(it, isUpdate = false) },
                                    onUpdate = { viewModel.startDownloadOrUpdate(it, isUpdate = true) },
                                    onOpen = { launchInstalledSoftware(it.title, it.downloadUrl) },
                                    onCategoryQuickJump = { cat, isGame ->
                                        if (isGame) {
                                            viewModel.selectGameCategory(cat)
                                            activeTab = StoreTab.GAMES
                                        } else {
                                            viewModel.selectAppCategory(cat)
                                            activeTab = StoreTab.APPS
                                        }
                                    },
                                    onNavigateToGames = { activeTab = StoreTab.GAMES },
                                    onNavigateToApps = { activeTab = StoreTab.APPS },
                                    onNavigateToStudio = { activeTab = StoreTab.STUDIO },
                                    onSeedCatalog = viewModel::seedStarterCatalog
                                )

                                StoreTab.GAMES -> CatalogBrowseScreen(
                                    isGamesMode = true,
                                    appsState = publishedAppsState,
                                    installedById = installedById,
                                    downloadProgress = downloadProgress,
                                    selectedCategory = selectedGameCategory,
                                    sortOption = sortOption,
                                    onSelectCategory = viewModel::selectGameCategory,
                                    onSelectSort = viewModel::updateSortOption,
                                    onAppClick = { openDetail(it.id) },
                                    onInstall = { viewModel.startDownloadOrUpdate(it, isUpdate = false) },
                                    onUpdate = { viewModel.startDownloadOrUpdate(it, isUpdate = true) },
                                    onOpen = { launchInstalledSoftware(it.title, it.downloadUrl) },
                                    onNavigateToStudio = { activeTab = StoreTab.STUDIO }
                                )

                                StoreTab.APPS -> CatalogBrowseScreen(
                                    isGamesMode = false,
                                    appsState = publishedAppsState,
                                    installedById = installedById,
                                    downloadProgress = downloadProgress,
                                    selectedCategory = selectedAppCategory,
                                    sortOption = sortOption,
                                    onSelectCategory = viewModel::selectAppCategory,
                                    onSelectSort = viewModel::updateSortOption,
                                    onAppClick = { openDetail(it.id) },
                                    onInstall = { viewModel.startDownloadOrUpdate(it, isUpdate = false) },
                                    onUpdate = { viewModel.startDownloadOrUpdate(it, isUpdate = true) },
                                    onOpen = { launchInstalledSoftware(it.title, it.downloadUrl) },
                                    onNavigateToStudio = { activeTab = StoreTab.STUDIO }
                                )

                                StoreTab.LIBRARY -> LibraryScreen(
                                    libraryState = libraryState,
                                    catalogById = catalogById,
                                    downloadProgress = downloadProgress,
                                    onOpenDetail = { openDetail(it) },
                                    onUpdateApp = { viewModel.startDownloadOrUpdate(it, isUpdate = true) },
                                    onLaunchApp = { item: InstalledApp ->
                                        launchInstalledSoftware(item.title, item.downloadUrl)
                                    },
                                    onUninstall = { item ->
                                        viewModel.uninstallFromLibrary(item.appId, item.title)
                                    },
                                    onBrowseStore = { activeTab = StoreTab.DISCOVER }
                                )

                                StoreTab.STUDIO -> DeveloperStudioScreen(
                                    developerProfileState = myDeveloperProfileState,
                                    privateAccount = myPrivateAccount,
                                    myAppsState = myDeveloperAppsState,
                                    defaultUserName = user.displayName ?: "",
                                    defaultUserEmail = user.email ?: "",
                                    isBusy = isBusy,
                                    onSaveDeveloperAccount = { input, isUpdate, onDone ->
                                        viewModel.saveDeveloperAccount(input, isUpdate, onDone)
                                    },
                                    onUploadNewSoftware = {
                                        editingAppId = null
                                        isPublishingScreenOpen = true
                                    },
                                    onEditSoftware = { app ->
                                        editingAppId = app.id
                                        isPublishingScreenOpen = true
                                    },
                                    onPreviewSoftware = { app ->
                                        openDetail(app.id)
                                    },
                                    onTogglePublished = viewModel::toggleAppPublished,
                                    onDeleteSoftware = viewModel::deleteSoftwareListing
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: StoreTab,
    selected: Boolean,
    badgeCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge { Text(badgeCount.toString()) }
            }
        ) {
            Icon(imageVector = icon, contentDescription = stringResource(tab.labelRes))
        }
    } else {
        Icon(imageVector = icon, contentDescription = stringResource(tab.labelRes))
    }
}

@Composable
private fun UserAvatarBadge(photoUrl: String?, name: String?) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.size(38.dp)
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name ?: "Account profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Account profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DeveloperProfileSheetContent(
    profile: DeveloperProfile,
    onVisitWebsite: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForPreset(profile.avatarPreset),
                    contentDescription = profile.studioName,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = profile.studioName,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified Developer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = profile.specialty,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (profile.tagline.isNotBlank()) {
            Text(
                text = profile.tagline,
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (profile.bio.isNotBlank()) {
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!profile.websiteUrl.isNullOrBlank()) {
            OutlinedButton(
                onClick = { onVisitWebsite(profile.websiteUrl) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Visit Studio Website")
            }
        }
        Button(
            onClick = onClose,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

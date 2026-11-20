package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppListing
import com.example.data.DeveloperProfile
import com.example.data.DeveloperRegistrationInput
import com.example.data.PrivateDeveloperAccount
import com.example.data.StoreConstants
import com.example.data.UiState
import com.example.ui.components.AppBadgeIcon
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.formatDownloads
import com.example.ui.components.formatRating
import com.example.ui.components.iconForPreset
import com.example.ui.theme.StarAmber

@Composable
fun DeveloperStudioScreen(
    developerProfileState: UiState<DeveloperProfile?>,
    privateAccount: PrivateDeveloperAccount?,
    myAppsState: UiState<List<AppListing>>,
    defaultUserName: String,
    defaultUserEmail: String,
    isBusy: Boolean,
    onSaveDeveloperAccount: (DeveloperRegistrationInput, Boolean, () -> Unit) -> Unit,
    onUploadNewSoftware: () -> Unit,
    onEditSoftware: (AppListing) -> Unit,
    onPreviewSoftware: (AppListing) -> Unit,
    onTogglePublished: (AppListing) -> Unit,
    onDeleteSoftware: (AppListing) -> Unit
) {
    var showEditProfileForm by remember { mutableStateOf(false) }
    var pendingDeleteApp by remember { mutableStateOf<AppListing?>(null) }

    pendingDeleteApp?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteApp = null },
            title = { Text("Delete ${target.title}?") },
            text = {
                Text("This permanently removes ${target.title} from PulseStore. Users who already downloaded it will no longer receive updates.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSoftware(target)
                        pendingDeleteApp = null
                    },
                    modifier = Modifier.testTag("confirm_delete_app_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteApp = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    when (developerProfileState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(modifier = Modifier.padding(16.dp)) {
                EmptyStatePanel(
                    icon = Icons.Filled.RocketLaunch,
                    title = "Developer Studio Unavailable",
                    subtitle = developerProfileState.message
                )
            }
        }

        is UiState.Success -> {
            val profile = developerProfileState.data
            if (profile == null || showEditProfileForm) {
                DeveloperAccountForm(
                    existingProfile = profile,
                    existingPrivateAccount = privateAccount,
                    defaultUserName = defaultUserName,
                    defaultUserEmail = defaultUserEmail,
                    isBusy = isBusy,
                    onCancel = if (profile != null) ({ showEditProfileForm = false }) else null,
                    onSubmit = { input ->
                        onSaveDeveloperAccount(input, profile != null) {
                            showEditProfileForm = false
                        }
                    }
                )
            } else {
                DeveloperDashboardContent(
                    profile = profile,
                    myAppsState = myAppsState,
                    onEditStudioProfile = { showEditProfileForm = true },
                    onUploadNewSoftware = onUploadNewSoftware,
                    onEditSoftware = onEditSoftware,
                    onPreviewSoftware = onPreviewSoftware,
                    onTogglePublished = onTogglePublished,
                    onRequestDelete = { pendingDeleteApp = it }
                )
            }
        }
    }
}

@Composable
private fun DeveloperAccountForm(
    existingProfile: DeveloperProfile?,
    existingPrivateAccount: PrivateDeveloperAccount?,
    defaultUserName: String,
    defaultUserEmail: String,
    isBusy: Boolean,
    onCancel: (() -> Unit)?,
    onSubmit: (DeveloperRegistrationInput) -> Unit
) {
    val isUpdate = existingProfile != null
    var studioName by remember(existingProfile) {
        mutableStateOf(
            existingProfile?.studioName
                ?: if (defaultUserName.isNotBlank()) "$defaultUserName Studio" else ""
        )
    }
    var tagline by remember(existingProfile) {
        mutableStateOf(existingProfile?.tagline ?: "Building focused indie software")
    }
    var bio by remember(existingProfile) {
        mutableStateOf(existingProfile?.bio ?: "")
    }
    var specialty by remember(existingProfile) {
        mutableStateOf(existingProfile?.specialty ?: StoreConstants.specialties.first())
    }
    var avatarPreset by remember(existingProfile) {
        mutableStateOf(existingProfile?.avatarPreset ?: "rocket")
    }
    var contactEmail by remember(existingPrivateAccount) {
        mutableStateOf(existingPrivateAccount?.contactEmail ?: defaultUserEmail)
    }
    var websiteUrl by remember(existingProfile) {
        mutableStateOf(existingProfile?.websiteUrl ?: "")
    }
    var termsAccepted by remember(isUpdate) { mutableStateOf(isUpdate) }
    var formError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("developer_account_form"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "banner") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dev_studio_1795157581135),
                        contentDescription = "Developer Studio",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0x330A101D), Color(0xEE0A101D))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isUpdate) "Edit Studio Profile" else "Create Developer Account",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = if (isUpdate) {
                                "Update your public studio identity and developer contact details."
                            } else {
                                "Publish games and apps to PulseStore, track live installs, and ship version updates."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.86f)
                        )
                    }
                }
            }
        }

        item(key = "fields") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = studioName,
                        onValueChange = { studioName = it.take(60); formError = null },
                        label = { Text("Developer / Studio Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_studio_name_input")
                    )

                    OutlinedTextField(
                        value = tagline,
                        onValueChange = { tagline = it.take(120); formError = null },
                        label = { Text("Studio Tagline") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_tagline_input")
                    )

                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it.take(120); formError = null },
                        label = { Text("Private Support Email * (Owner Only)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_email_input")
                    )

                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it.take(300); formError = null },
                        label = { Text("Studio Website (Optional, https://...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it.take(600); formError = null },
                        label = { Text("About Your Studio") },
                        minLines = 3,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Primary Focus",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StoreConstants.specialties) { item ->
                            FilterChip(
                                selected = specialty == item,
                                onClick = { specialty = item },
                                label = { Text(item) }
                            )
                        }
                    }

                    Text(
                        text = "Studio Emblem",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StoreConstants.iconPresets.take(8)) { preset ->
                            val selected = avatarPreset == preset
                            Surface(
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { avatarPreset = preset }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = iconForPreset(preset),
                                        contentDescription = preset,
                                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!isUpdate) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { termsAccepted = !termsAccepted }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = termsAccepted,
                                onCheckedChange = { termsAccepted = it; formError = null },
                                modifier = Modifier.testTag("dev_terms_checkbox")
                            )
                            Text(
                                text = "I agree to publish original software that follows PulseStore safety & distribution guidelines.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (formError != null) {
                        Text(
                            text = formError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onCancel != null) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Cancel")
                            }
                        }
                        Button(
                            onClick = {
                                val trimmedName = studioName.trim()
                                val trimmedEmail = contactEmail.trim()
                                val trimmedWebsite = websiteUrl.trim()
                                when {
                                    trimmedName.length < 2 ->
                                        formError = "Studio name must be at least 2 characters."
                                    trimmedEmail.length < 3 || !trimmedEmail.contains("@") ->
                                        formError = "Enter a valid developer contact email."
                                    trimmedWebsite.isNotEmpty() && !trimmedWebsite.startsWith("https://") ->
                                        formError = "Website URL must start with https://"
                                    !termsAccepted ->
                                        formError = "Accept the developer guidelines to continue."
                                    else -> {
                                        onSubmit(
                                            DeveloperRegistrationInput(
                                                studioName = trimmedName,
                                                tagline = tagline.trim(),
                                                bio = bio.trim(),
                                                specialty = specialty,
                                                avatarPreset = avatarPreset,
                                                websiteUrl = trimmedWebsite.takeIf { it.isNotEmpty() },
                                                contactEmail = trimmedEmail,
                                                supportUrl = null
                                            )
                                        )
                                    }
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1.4f)
                                .height(50.dp)
                                .testTag("dev_register_submit_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(if (isUpdate) "Save Profile" else "Activate Developer Account")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperDashboardContent(
    profile: DeveloperProfile,
    myAppsState: UiState<List<AppListing>>,
    onEditStudioProfile: () -> Unit,
    onUploadNewSoftware: () -> Unit,
    onEditSoftware: (AppListing) -> Unit,
    onPreviewSoftware: (AppListing) -> Unit,
    onTogglePublished: (AppListing) -> Unit,
    onRequestDelete: (AppListing) -> Unit
) {
    val personalReleases = remember(myAppsState) {
        (myAppsState as? UiState.Success)?.data?.filter { !it.starterCatalog } ?: emptyList()
    }
    val totalDownloads = personalReleases.sumOf { it.downloadsCount }
    val publishedCount = personalReleases.count { it.published }
    val ratedReleases = personalReleases.filter { it.ratingCount > 0L }
    val avgRating = if (ratedReleases.isEmpty()) 0.0
    else ratedReleases.sumOf { it.ratingSum }.toDouble() / ratedReleases.sumOf { it.ratingCount }.toDouble()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("developer_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Studio Header Card
        item(key = "studio_card") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_dev_studio_1795157581135),
                            contentDescription = "Studio header",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0x440A101D), Color(0xDD0A101D))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconForPreset(profile.avatarPreset),
                                        contentDescription = profile.studioName,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = profile.studioName,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Verified,
                                            contentDescription = "Verified Developer",
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = profile.specialty,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onEditStudioProfile,
                                modifier = Modifier.testTag("edit_studio_profile_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Studio Profile",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // KPI Stats Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StudioMetricCell(value = "$publishedCount/${personalReleases.size}", label = "Live Apps")
                        StudioMetricCell(value = formatDownloads(totalDownloads), label = "Downloads")
                        StudioMetricCell(
                            value = formatRating(avgRating, if (ratedReleases.isEmpty()) 0L else 1L),
                            label = "Avg Rating"
                        )
                    }
                }
            }
        }

        // Primary Upload Software CTA
        item(key = "upload_cta") {
            Button(
                onClick = onUploadNewSoftware,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("upload_software_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Upload,
                    contentDescription = "Upload Software",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upload New Software",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item(key = "my_releases_header") {
            SectionHeaderRow(
                title = "Your Software Releases (${personalReleases.size})"
            )
        }

        when (myAppsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is UiState.Error -> {
                item {
                    EmptyStatePanel(
                        icon = Icons.Filled.RocketLaunch,
                        title = "Unable to load releases",
                        subtitle = myAppsState.message
                    )
                }
            }

            is UiState.Success -> {
                if (personalReleases.isEmpty()) {
                    item(key = "no_uploads") {
                        EmptyStatePanel(
                            icon = Icons.Filled.Add,
                            title = "Ship Your First Release",
                            subtitle = "Upload a game or application with version notes, custom badge artwork, and optional release links so other users can download and review it.",
                            actionLabel = "Upload Software",
                            onAction = onUploadNewSoftware
                        )
                    }
                } else {
                    items(personalReleases, key = { "dev_app_${it.id}" }) { app ->
                        DeveloperReleaseCard(
                            app = app,
                            onEdit = { onEditSoftware(app) },
                            onPreview = { onPreviewSoftware(app) },
                            onTogglePublish = { onTogglePublished(app) },
                            onDelete = { onRequestDelete(app) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DeveloperReleaseCard(
    app: AppListing,
    onEdit: () -> Unit,
    onPreview: () -> Unit,
    onTogglePublish: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dev_release_card_${app.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppBadgeIcon(
                    accentHex = app.accentColorHex,
                    iconPreset = app.iconPreset,
                    iconUrl = app.iconUrl,
                    title = app.title,
                    size = 54.dp,
                    cornerRadius = 15.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = app.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            color = if (app.published) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (app.published) "LIVE" else "DRAFT",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (app.published) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${app.category} • v${app.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatDownloads(app.downloadsCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = StarAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatRating(app.averageRating, app.ratingCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("dev_delete_${app.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete ${app.title}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTogglePublish,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dev_toggle_publish_${app.id}")
                ) {
                    Icon(
                        imageVector = if (app.published) Icons.Filled.PublicOff else Icons.Filled.Public,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (app.published) "Unpublish" else "Publish")
                }
                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dev_edit_${app.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit / Update")
                }
            }

            TextButton(
                onClick = onPreview,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Preview Store Listing →")
            }
        }
    }
}

@Composable
private fun StudioMetricCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

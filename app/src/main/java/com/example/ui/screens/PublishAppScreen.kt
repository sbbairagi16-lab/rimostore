package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AppDraftInput
import com.example.data.AppListing
import com.example.data.StoreConstants
import com.example.ui.components.AppBadgeIcon
import com.example.ui.components.iconForPreset
import com.example.ui.components.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishAppScreen(
    studioName: String,
    editingApp: AppListing?,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSubmit: (AppDraftInput) -> Unit
) {
    BackHandler(onBack = onBack)
    val isEdit = editingApp != null

    var appType by remember(editingApp) { mutableStateOf(editingApp?.appType ?: StoreConstants.TYPE_GAME) }
    var category by remember(editingApp) {
        mutableStateOf(
            editingApp?.category ?: StoreConstants.categoriesForType(StoreConstants.TYPE_GAME).first()
        )
    }
    var title by remember(editingApp) { mutableStateOf(editingApp?.title ?: "") }
    var tagline by remember(editingApp) { mutableStateOf(editingApp?.tagline ?: "") }
    var description by remember(editingApp) { mutableStateOf(editingApp?.description ?: "") }
    var versionName by remember(editingApp) { mutableStateOf(editingApp?.versionName ?: "1.0.0") }
    var sizeMbText by remember(editingApp) {
        mutableStateOf(editingApp?.sizeMb?.toInt()?.toString() ?: "48")
    }
    var ageRating by remember(editingApp) { mutableStateOf(editingApp?.ageRating ?: "Everyone") }
    var accentColorHex by remember(editingApp) {
        mutableStateOf(editingApp?.accentColorHex ?: StoreConstants.accentPaletteHex.first())
    }
    var iconPreset by remember(editingApp) {
        mutableStateOf(editingApp?.iconPreset ?: "gamepad")
    }
    var iconUrl by remember(editingApp) { mutableStateOf(editingApp?.iconUrl ?: "") }
    var downloadUrl by remember(editingApp) { mutableStateOf(editingApp?.downloadUrl ?: "") }
    var packageName by remember(editingApp) { mutableStateOf(editingApp?.packageName ?: "") }
    var tagsText by remember(editingApp) {
        mutableStateOf(editingApp?.tags?.joinToString(", ") ?: "")
    }
    var releaseNotes by remember(editingApp) {
        mutableStateOf(editingApp?.releaseNotes ?: "Initial release on PulseStore.")
    }
    var publishImmediately by remember(editingApp) {
        mutableStateOf(editingApp?.published ?: true)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val availableCategories = StoreConstants.categoriesForType(appType)
    if (category !in availableCategories) {
        category = availableCategories.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Software Release" else "Upload Software") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("publish_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("publish_app_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Storefront Preview Card
            item(key = "live_preview") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppBadgeIcon(
                            accentHex = accentColorHex,
                            iconPreset = iconPreset,
                            iconUrl = iconUrl.trim().takeIf { it.startsWith("https://") },
                            title = title.ifBlank { "Preview" },
                            size = 64.dp,
                            cornerRadius = 18.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STOREFRONT PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = title.ifBlank { "Your Software Name" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$studioName • $category • v${versionName.ifBlank { "1.0.0" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Software Type Toggle
            item(key = "type_toggle") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Software Type", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = appType == StoreConstants.TYPE_GAME,
                            onClick = {
                                appType = StoreConstants.TYPE_GAME
                                if (category !in StoreConstants.gameCategories) {
                                    category = StoreConstants.gameCategories.first()
                                }
                            },
                            label = { Text("Game") },
                            leadingIcon = {
                                Icon(Icons.Filled.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("type_game_chip")
                        )
                        FilterChip(
                            selected = appType == StoreConstants.TYPE_APP,
                            onClick = {
                                appType = StoreConstants.TYPE_APP
                                if (category !in StoreConstants.appCategories) {
                                    category = StoreConstants.appCategories.first()
                                }
                            },
                            label = { Text("Application") },
                            leadingIcon = {
                                Icon(Icons.Filled.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("type_app_chip")
                        )
                    }
                }
            }

            // Category Selection
            item(key = "category_picker") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableCategories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            // Core Metadata Inputs
            item(key = "core_fields") {
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
                            value = title,
                            onValueChange = { title = it.take(50); validationError = null },
                            label = { Text("Title * (2–50 chars)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("publish_title_input")
                        )
                        OutlinedTextField(
                            value = tagline,
                            onValueChange = { tagline = it.take(120); validationError = null },
                            label = { Text("Short Tagline * (4–120 chars)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("publish_tagline_input")
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it.take(2000); validationError = null },
                            label = { Text("Description * (10–2000 chars)") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("publish_description_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = versionName,
                                onValueChange = { versionName = it.take(24); validationError = null },
                                label = { Text("Version *") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("publish_version_input")
                            )
                            OutlinedTextField(
                                value = sizeMbText,
                                onValueChange = { raw ->
                                    sizeMbText = raw.filter { it.isDigit() || it == '.' }.take(6)
                                    validationError = null
                                },
                                label = { Text("Size (MB) *") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("publish_size_input")
                            )
                        }
                        OutlinedTextField(
                            value = releaseNotes,
                            onValueChange = { releaseNotes = it.take(600); validationError = null },
                            label = { Text("What's New / Release Notes") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = tagsText,
                            onValueChange = { tagsText = it.take(120) },
                            label = { Text("Discovery Tags (comma-separated, up to 6)") },
                            placeholder = { Text("indie, offline, arcade") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Branding & Badge Customization
            item(key = "branding") {
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
                        Text("Badge Accent Color", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(StoreConstants.accentPaletteHex) { hex ->
                                val color = parseHexColor(hex)
                                val selected = accentColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.onBackground else Color.White.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable { accentColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected color",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text("Badge Symbol", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(StoreConstants.iconPresets) { preset ->
                                val selected = iconPreset == preset
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { iconPreset = preset }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = iconForPreset(preset),
                                            contentDescription = preset,
                                            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text("Age Rating", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(StoreConstants.ageRatings) { rating ->
                                FilterChip(
                                    selected = ageRating == rating,
                                    onClick = { ageRating = rating },
                                    label = { Text(rating) }
                                )
                            }
                        }
                    }
                }
            }

            // Distribution Metadata (Optional URLs & Package)
            item(key = "distribution") {
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
                        Text("Distribution Details (Optional)", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = packageName,
                            onValueChange = { packageName = it.take(120) },
                            label = { Text("Package Identifier (e.g. com.studio.game)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = downloadUrl,
                            onValueChange = { downloadUrl = it.take(500); validationError = null },
                            label = { Text("Release / Download Portal URL (https://...)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = iconUrl,
                            onValueChange = { iconUrl = it.take(500); validationError = null },
                            label = { Text("Custom Icon Image URL (https://...)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Publish to Storefront",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Make immediately discoverable & downloadable by all users.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = publishImmediately,
                                onCheckedChange = { publishImmediately = it },
                                modifier = Modifier.testTag("publish_visibility_switch")
                            )
                        }
                    }
                }
            }

            // Error & Submit Button
            item(key = "submit") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (validationError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val cleanTitle = title.trim()
                            val cleanTagline = tagline.trim()
                            val cleanDesc = description.trim()
                            val cleanVer = versionName.trim()
                            val parsedSize = sizeMbText.toDoubleOrNull()
                            val cleanIconUrl = iconUrl.trim()
                            val cleanDownloadUrl = downloadUrl.trim()

                            when {
                                cleanTitle.length !in 2..50 ->
                                    validationError = "Title must be between 2 and 50 characters."
                                cleanTagline.length !in 4..120 ->
                                    validationError = "Tagline must be between 4 and 120 characters."
                                cleanDesc.length !in 10..2000 ->
                                    validationError = "Description must be at least 10 characters."
                                cleanVer.isEmpty() ->
                                    validationError = "Version name is required (e.g. 1.0.0)."
                                parsedSize == null || parsedSize < 1.0 || parsedSize > 4096.0 ->
                                    validationError = "Package size must be between 1 and 4096 MB."
                                cleanIconUrl.isNotEmpty() && !cleanIconUrl.startsWith("https://") ->
                                    validationError = "Custom icon URL must start with https://"
                                cleanDownloadUrl.isNotEmpty() && !cleanDownloadUrl.startsWith("https://") ->
                                    validationError = "Release portal URL must start with https://"
                                else -> {
                                    val parsedTags = tagsText.split(",")
                                        .map { it.trim().lowercase() }
                                        .filter { it.isNotEmpty() }
                                        .distinct()
                                        .take(6)
                                    onSubmit(
                                        AppDraftInput(
                                            existingId = editingApp?.id,
                                            title = cleanTitle,
                                            tagline = cleanTagline,
                                            description = cleanDesc,
                                            appType = appType,
                                            category = category,
                                            versionName = cleanVer,
                                            sizeMb = parsedSize,
                                            ageRating = ageRating,
                                            accentColorHex = accentColorHex,
                                            iconPreset = iconPreset,
                                            iconUrl = cleanIconUrl.takeIf { it.isNotEmpty() },
                                            downloadUrl = cleanDownloadUrl.takeIf { it.isNotEmpty() },
                                            packageName = packageName.trim().takeIf { it.isNotEmpty() },
                                            tags = parsedTags,
                                            releaseNotes = releaseNotes.trim(),
                                            published = publishImmediately
                                        )
                                    )
                                }
                            }
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("publish_submit_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isEdit -> "Save & Ship Release"
                                    publishImmediately -> "Publish to PulseStore"
                                    else -> "Save Draft"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

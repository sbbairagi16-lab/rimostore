package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppListing
import com.example.data.InstalledApp
import com.example.data.StoreConstants
import com.example.data.UiState
import com.example.ui.components.AppRowCard
import com.example.ui.components.AppSpotlightCard
import com.example.ui.components.EditorialHeroCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.iconForCategory

@Composable
fun DiscoverScreen(
    appsState: UiState<List<AppListing>>,
    installedById: Map<String, InstalledApp>,
    downloadProgress: Map<String, Float>,
    searchQuery: String,
    isBusy: Boolean,
    onSearchChange: (String) -> Unit,
    onAppClick: (AppListing) -> Unit,
    onInstall: (AppListing) -> Unit,
    onUpdate: (AppListing) -> Unit,
    onOpen: (AppListing) -> Unit,
    onCategoryQuickJump: (String, Boolean) -> Unit,
    onNavigateToGames: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onSeedCatalog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("discover_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Search Bar
        item(key = "search_bar") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("discover_search_input")
            )
        }

        when (appsState) {
            is UiState.Loading -> {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is UiState.Error -> {
                item(key = "error") {
                    EmptyStatePanel(
                        icon = Icons.Filled.Storefront,
                        title = "Unable to load storefront",
                        subtitle = appsState.message,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            is UiState.Success -> {
                val allApps = appsState.data
                val trimmedQuery = searchQuery.trim()

                if (trimmedQuery.isNotEmpty()) {
                    val matches = allApps.filter { app ->
                        app.title.contains(trimmedQuery, ignoreCase = true) ||
                            app.tagline.contains(trimmedQuery, ignoreCase = true) ||
                            app.category.contains(trimmedQuery, ignoreCase = true) ||
                            app.developerName.contains(trimmedQuery, ignoreCase = true) ||
                            app.tags.any { it.contains(trimmedQuery, ignoreCase = true) }
                    }
                    item(key = "search_header") {
                        SectionHeaderRow(
                            title = "Results (${matches.size})",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    if (matches.isEmpty()) {
                        item(key = "search_empty") {
                            EmptyStatePanel(
                                icon = Icons.Filled.Search,
                                title = "No matching software",
                                subtitle = "Try searching another category, tag, or studio name.",
                                actionLabel = "Clear Search",
                                onAction = { onSearchChange("") },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(matches, key = { "search_${it.id}" }) { app ->
                            AppRowCard(
                                app = app,
                                installedApp = installedById[app.id],
                                downloadProgress = downloadProgress[app.id],
                                onClick = { onAppClick(app) },
                                onInstall = { onInstall(app) },
                                onUpdate = { onUpdate(app) },
                                onOpen = { onOpen(app) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    // Editorial Hero Carousel
                    item(key = "hero_carousel") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                EditorialHeroCard(
                                    drawableRes = R.drawable.img_hero_arcade_1795157581052,
                                    eyebrow = "Featured Games",
                                    headline = "Next-Gen Indie Arcade",
                                    subtitle = "Drift racing, tactical mechs & zen puzzles",
                                    ctaLabel = "Explore",
                                    onClick = onNavigateToGames,
                                    modifier = Modifier
                                        .width(316.dp)
                                        .testTag("hero_games_banner")
                                )
                            }
                            item {
                                EditorialHeroCard(
                                    drawableRes = R.drawable.img_hero_indie_1795157581872,
                                    eyebrow = "Creator Spotlight",
                                    headline = "Essential Studio Apps",
                                    subtitle = "Synths, raw photo labs & focus decks",
                                    ctaLabel = "Browse",
                                    onClick = onNavigateToApps,
                                    modifier = Modifier
                                        .width(316.dp)
                                        .testTag("hero_apps_banner")
                                )
                            }
                            item {
                                EditorialHeroCard(
                                    drawableRes = R.drawable.img_dev_studio_1795157581135,
                                    eyebrow = "For Developers",
                                    headline = "Ship Your Own Software",
                                    subtitle = "Open a Studio account & publish live",
                                    ctaLabel = "Studio",
                                    onClick = onNavigateToStudio,
                                    modifier = Modifier
                                        .width(316.dp)
                                        .testTag("hero_studio_banner")
                                )
                            }
                        }
                    }

                    // Quick Browse Categories Strip
                    item(key = "categories_strip") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeaderRow(
                                title = "Browse Categories",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(StoreConstants.allCategories, key = { it }) { cat ->
                                    val isGameCat = cat in StoreConstants.gameCategories
                                    FilterChip(
                                        selected = false,
                                        onClick = { onCategoryQuickJump(cat, isGameCat) },
                                        label = { Text(cat) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = iconForCategory(cat),
                                                contentDescription = cat,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.testTag("quick_cat_$cat")
                                    )
                                }
                            }
                        }
                    }

                    if (allApps.isEmpty()) {
                        item(key = "empty_catalog_seed") {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = "Launch catalog",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Text(
                                            text = "Welcome to PulseStore",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "Your cloud storefront is brand new. Load 8 curated starter games & apps right away, or head to Developer Studio to upload your first software!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = onSeedCatalog,
                                            enabled = !isBusy,
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier.testTag("seed_catalog_button")
                                        ) {
                                            if (isBusy) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text("Load Starter Catalog")
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier
                                                .clickable(onClick = onNavigateToStudio)
                                                .testTag("empty_open_studio_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.RocketLaunch,
                                                    contentDescription = "Open Studio",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Publish Software",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val games = allApps.filter { it.isGame }
                        val apps = allApps.filter { !it.isGame }
                        val communityReleases = allApps.filter { !it.starterCatalog }

                        if (communityReleases.isNotEmpty()) {
                            item(key = "community_header") {
                                SectionHeaderRow(
                                    title = "Fresh from Indie Developers",
                                    actionLabel = "Publish",
                                    onActionClick = onNavigateToStudio,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            item(key = "community_row") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(communityReleases.take(10), key = { "comm_${it.id}" }) { app ->
                                        AppSpotlightCard(
                                            app = app,
                                            installedApp = installedById[app.id],
                                            downloadProgress = downloadProgress[app.id],
                                            onClick = { onAppClick(app) },
                                            onInstall = { onInstall(app) },
                                            onUpdate = { onUpdate(app) },
                                            onOpen = { onOpen(app) }
                                        )
                                    }
                                }
                            }
                        }

                        if (games.isNotEmpty()) {
                            item(key = "games_header") {
                                SectionHeaderRow(
                                    title = "Trending Games",
                                    actionLabel = "See all",
                                    onActionClick = onNavigateToGames,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            item(key = "games_carousel") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(games.take(8), key = { "game_spot_${it.id}" }) { app ->
                                        AppSpotlightCard(
                                            app = app,
                                            installedApp = installedById[app.id],
                                            downloadProgress = downloadProgress[app.id],
                                            onClick = { onAppClick(app) },
                                            onInstall = { onInstall(app) },
                                            onUpdate = { onUpdate(app) },
                                            onOpen = { onOpen(app) }
                                        )
                                    }
                                }
                            }
                        }

                        if (apps.isNotEmpty()) {
                            item(key = "apps_header") {
                                SectionHeaderRow(
                                    title = "Top Applications",
                                    actionLabel = "See all",
                                    onActionClick = onNavigateToApps,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            itemsIndexed(apps.take(6), key = { _, app -> "top_app_${app.id}" }) { idx, app ->
                                AppRowCard(
                                    app = app,
                                    rank = idx + 1,
                                    installedApp = installedById[app.id],
                                    downloadProgress = downloadProgress[app.id],
                                    onClick = { onAppClick(app) },
                                    onInstall = { onInstall(app) },
                                    onUpdate = { onUpdate(app) },
                                    onOpen = { onOpen(app) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

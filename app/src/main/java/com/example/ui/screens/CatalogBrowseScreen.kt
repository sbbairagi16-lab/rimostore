package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AppListing
import com.example.data.InstalledApp
import com.example.data.StoreConstants
import com.example.data.UiState
import com.example.ui.components.AppRowCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.iconForCategory
import com.example.viewmodel.CatalogSortOption
import com.google.firebase.Timestamp

@Composable
fun CatalogBrowseScreen(
    isGamesMode: Boolean,
    appsState: UiState<List<AppListing>>,
    installedById: Map<String, InstalledApp>,
    downloadProgress: Map<String, Float>,
    selectedCategory: String?,
    sortOption: CatalogSortOption,
    onSelectCategory: (String?) -> Unit,
    onSelectSort: (CatalogSortOption) -> Unit,
    onAppClick: (AppListing) -> Unit,
    onInstall: (AppListing) -> Unit,
    onUpdate: (AppListing) -> Unit,
    onOpen: (AppListing) -> Unit,
    onNavigateToStudio: () -> Unit
) {
    val categories = if (isGamesMode) StoreConstants.gameCategories else StoreConstants.appCategories
    val targetType = if (isGamesMode) StoreConstants.TYPE_GAME else StoreConstants.TYPE_APP

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(if (isGamesMode) "games_browse_screen" else "apps_browse_screen")
    ) {
        // Category Filter Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "cat_all") {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text(if (isGamesMode) "All Games" else "All Apps") },
                    modifier = Modifier.testTag("filter_cat_all")
                )
            }
            items(categories, key = { it }) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        onSelectCategory(if (selectedCategory == category) null else category)
                    },
                    label = { Text(category) },
                    leadingIcon = {
                        Icon(
                            imageVector = iconForCategory(category),
                            contentDescription = category,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_cat_$category")
                )
            }
        }

        // Sort Selector Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CatalogSortOption.entries, key = { it.name }) { option ->
                FilterChip(
                    selected = sortOption == option,
                    onClick = { onSelectSort(option) },
                    label = {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("sort_${option.name.lowercase()}")
                )
            }
        }

        when (appsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStatePanel(
                        icon = if (isGamesMode) Icons.Filled.SportsEsports else Icons.Filled.Apps,
                        title = "Catalog unavailable",
                        subtitle = appsState.message
                    )
                }
            }

            is UiState.Success -> {
                val filtered = remember(appsState.data, targetType, selectedCategory, sortOption) {
                    val base = appsState.data.filter { app ->
                        app.appType == targetType &&
                            (selectedCategory == null || app.category == selectedCategory)
                    }
                    when (sortOption) {
                        CatalogSortOption.POPULAR -> base.sortedByDescending { it.downloadsCount }
                        CatalogSortOption.TOP_RATED -> base.sortedWith(
                            compareByDescending<AppListing> { it.averageRating }
                                .thenByDescending { it.ratingCount }
                        )
                        CatalogSortOption.NEWEST -> base.sortedByDescending {
                            it.createdAt ?: Timestamp(0, 0)
                        }
                        CatalogSortOption.SMALLEST -> base.sortedBy { it.sizeMb }
                    }
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStatePanel(
                            icon = if (isGamesMode) Icons.Filled.SportsEsports else Icons.Filled.Apps,
                            title = if (selectedCategory != null) {
                                "No $selectedCategory titles yet"
                            } else {
                                "No ${if (isGamesMode) "games" else "apps"} published yet"
                            },
                            subtitle = "Be the first developer to release software in this category!",
                            actionLabel = "Open Developer Studio",
                            onAction = onNavigateToStudio
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 28.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "count_label") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filtered.size} ${if (isGamesMode) "games" else "applications"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Sorted by ${sortOption.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        itemsIndexed(filtered, key = { _, app -> app.id }) { index, app ->
                            AppRowCard(
                                app = app,
                                rank = index + 1,
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
        }
    }
}

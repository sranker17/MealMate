package com.sranker.mealmate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sranker.mealmate.data.MealWithTags
import com.sranker.mealmate.data.TagEntity
import com.sranker.mealmate.ui.components.EmptyState
import com.sranker.mealmate.ui.viewmodel.PlannerViewModel
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.graphics.Color

/**
 * Planner screen — the main interactive screen of the app.
 *
 * Manages the recommendation engine, pinned meals, menu acceptance,
 * and completion tracking.
 *
 * Layout:
 * - Scrollable column with header, filter chips, recommendation card,
 *   action buttons, pinned meal list, and menu lifecycle controls.
 *
 * @param viewModel The [PlannerViewModel] providing UI state.
 * @param windowWidthSizeClass The [WindowWidthSizeClass] of the screen.
 * @param onNavigateToMeals Called to navigate to the meal list (empty state guidance).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onNavigateToMeals: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasAnyMeal = state.allTags.isNotEmpty() ||
            state.activeMenu != null ||
            state.recommendedMeal != null

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Heti Tervező",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showFilterSheet = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Szűrők",
                    tint = if (state.selectedTagIds.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!hasAnyMeal) {
            EmptyState(
                icon = Icons.Default.Restaurant,
                message = "Még nincs étel az adatbázisban\nAdj hozzá ételeket az Ételek menüpontban, hogy elkezdhesd a tervezést!"
            )
        } else if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) {
            // Tablet layout: side-by-side panes
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Left pane: Recommendation card + action buttons
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.selectedTagIds.isNotEmpty()) {
                        FilterChipRow(
                            allTags = state.allTags,
                            selectedTagIds = state.selectedTagIds,
                            onTagToggled = viewModel::onTagFilterToggled
                        )
                    }
                    RecommendationCard(
                        recommendedMeal = state.recommendedMeal,
                        isRecommending = state.isRecommending,
                        onRecommend = viewModel::recommendMeal,
                        onPin = viewModel::pinMeal,
                        onSkip = viewModel::skipMeal
                    )
                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Right pane: Pinned meals + lifecycle buttons
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val pinnedMeals = state.activeMenu?.meals ?: emptyList()
                    if (pinnedMeals.isNotEmpty()) {
                        Text(
                            text = "Kitűzött ételek",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        pinnedMeals.forEach { meal ->
                            val crossRef = state.activeMenuCrossRefs.find { it.mealId == meal.id }
                            val isCompleted = crossRef?.isCompleted ?: false
                            PinnedMealCard(
                                mealName = meal.name,
                                isCompleted = isCompleted,
                                isAccepted = state.isAccepted,
                                onToggleComplete = { viewModel.onMealCompletedToggled(meal.id) },
                                onUnpin = { viewModel.unpinMeal(meal.id) }
                            )
                        }
                    }

                    if (!state.isAccepted) {
                        if (pinnedMeals.isNotEmpty()) {
                            Button(
                                onClick = viewModel::acceptMenu,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menü elfogadása")
                            }
                        }
                    } else {
                        val allCompleted = pinnedMeals.isNotEmpty() &&
                                pinnedMeals.all { meal ->
                                    state.activeMenuCrossRefs.any { it.mealId == meal.id && it.isCompleted }
                                }
                        Button(
                            onClick = viewModel::finishMenu,
                            enabled = allCompleted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menü befejezése")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            // Phone layout: scrollable column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active filter chips
                if (state.selectedTagIds.isNotEmpty()) {
                    FilterChipRow(
                        allTags = state.allTags,
                        selectedTagIds = state.selectedTagIds,
                        onTagToggled = viewModel::onTagFilterToggled
                    )
                }

                // Recommendation card
                RecommendationCard(
                    recommendedMeal = state.recommendedMeal,
                    isRecommending = state.isRecommending,
                    onRecommend = viewModel::recommendMeal,
                    onPin = viewModel::pinMeal,
                    onSkip = viewModel::skipMeal
                )

                // Error message
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Pinned meals section
                val pinnedMeals = state.activeMenu?.meals ?: emptyList()
                if (pinnedMeals.isNotEmpty()) {
                    Text(
                        text = "Kitűzött ételek",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    pinnedMeals.forEach { meal ->
                        val crossRef = state.activeMenuCrossRefs.find { it.mealId == meal.id }
                        val isCompleted = crossRef?.isCompleted ?: false
                        PinnedMealCard(
                            mealName = meal.name,
                            isCompleted = isCompleted,
                            isAccepted = state.isAccepted,
                            onToggleComplete = { viewModel.onMealCompletedToggled(meal.id) },
                            onUnpin = { viewModel.unpinMeal(meal.id) }
                        )
                    }
                }

                // Action buttons
                if (!state.isAccepted) {
                    if (pinnedMeals.isNotEmpty()) {
                        Button(
                            onClick = viewModel::acceptMenu,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menü elfogadása")
                        }
                    }
                } else {
                    val allCompleted = pinnedMeals.isNotEmpty() &&
                            pinnedMeals.all { meal ->
                                state.activeMenuCrossRefs.any { it.mealId == meal.id && it.isCompleted }
                            }
                    Button(
                        onClick = viewModel::finishMenu,
                        enabled = allCompleted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Menü befejezése")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Szűrés címkék szerint",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.selectedTagIds.isNotEmpty()) {
                        TextButton(onClick = viewModel::onClearFilters) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Törlés")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.allTags, key = { it.id }) { tag ->
                        val isSelected = tag.id in state.selectedTagIds
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onTagFilterToggled(tag.id) },
                            label = { Text(tag.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Horizontal row of filter chips showing currently active tag filters.
 * Each chip can be tapped to remove the filter.
 */
@Composable
private fun FilterChipRow(
    allTags: List<TagEntity>,
    selectedTagIds: Set<Long>,
    onTagToggled: (Long) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(allTags.filter { it.id in selectedTagIds }, key = { it.id }) { tag ->
            FilterChip(
                selected = true,
                onClick = { onTagToggled(tag.id) },
                label = { Text(tag.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * Large card showing the currently recommended meal with action buttons.
 *
 * When [recommendedMeal] is non-null: displays meal name, tags, serving size, Skip and Pin buttons.
 * When null: shows a placeholder icon with "Ajánlás" and "Véletlenszerű" buttons.
 */
@Composable
private fun RecommendationCard(
    recommendedMeal: MealWithTags?,
    isRecommending: Boolean,
    onRecommend: () -> Unit,
    onPin: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (recommendedMeal != null) {
                Text(
                    text = recommendedMeal.meal.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                if (recommendedMeal.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        recommendedMeal.tags.forEach { tag ->
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                recommendedMeal.meal.servingSize?.let { size ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$size fő",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onSkip,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kihagyás")
                    }
                    Button(
                        onClick = onPin,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kitűzés")
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isRecommending) "Ajánlás keresése…" else "Nincs megjeleníthető ajánlás",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRecommend,
                    enabled = !isRecommending,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajánlás")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRecommend,
                    enabled = !isRecommending,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Véletlenszerű")
                }
            }
        }
    }
}

/**
 * A single pinned meal card in the active menu.
 *
 * Pre-acceptance: shows pin icon, meal name, unpin button, and supports swipe-to-unpin.
 * Post-acceptance: shows checkbox for completion tracking, meal name, and check icon when done.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinnedMealCard(
    mealName: String,
    isCompleted: Boolean,
    isAccepted: Boolean,
    onToggleComplete: () -> Unit,
    onUnpin: () -> Unit
) {
    if (!isAccepted) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onUnpin()
                    true
                } else {
                    false
                }
            }
        )
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Kitűzés visszavonása",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true
        ) {
            PinnedMealCardContent(
                mealName = mealName,
                isCompleted = isCompleted,
                isAccepted = false,
                onToggleComplete = onToggleComplete,
                onUnpin = onUnpin
            )
        }
    } else {
        PinnedMealCardContent(
            mealName = mealName,
            isCompleted = isCompleted,
            isAccepted = true,
            onToggleComplete = onToggleComplete,
            onUnpin = onUnpin
        )
    }
}

@Composable
private fun PinnedMealCardContent(
    mealName: String,
    isCompleted: Boolean,
    isAccepted: Boolean,
    onToggleComplete: () -> Unit,
    onUnpin: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAccepted) {
                Checkbox(checked = isCompleted, onCheckedChange = { onToggleComplete() })
            } else {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Text(
                text = mealName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Elkészítve",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            if (!isAccepted) {
                IconButton(onClick = onUnpin) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Kitűzés visszavonása",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sranker.mealmate.R
import com.sranker.mealmate.data.MealWithTags
import com.sranker.mealmate.data.TagEntity
import com.sranker.mealmate.ui.components.EmptyState
import com.sranker.mealmate.ui.viewmodel.PlannerViewModel
import com.sranker.mealmate.ui.viewmodel.PlannerEvent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.launch

/**
 * Planner screen — the main interactive screen of the app.
 *
 * Manages the recommendation engine, pinned meals, menu acceptance,
 * and completion tracking.
 *
 * Layout:
 * - Phone: Scrollable column with recommend button, pinned meals (recommended first), lifecycle buttons.
 * - Tablet: Side-by-side panes.
 *
 * @param viewModel The [PlannerViewModel] providing UI state.
 * @param windowWidthSizeClass The [WindowWidthSizeClass] of the screen.
 * @param onNavigateToMeals Called to navigate to the meal list (empty state guidance).
 * @param onMealClick Called with the meal ID when a pinned meal card is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onNavigateToMeals: () -> Unit,
    onMealClick: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    // Capture strings before LaunchedEffect (not a composable context)
    val mealUnpinnedText = stringResource(R.string.planner_meal_unpinned)
    val undoText = stringResource(R.string.planner_undo)
    val noRecommendationText = stringResource(R.string.planner_no_recommendation_toast)

    // Consume one-shot events (snackbar)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PlannerEvent.MealUnpinned -> {
                    val result = snackbarHostState.showSnackbar(
                        message = mealUnpinnedText,
                        actionLabel = undoText,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoUnpinMeal(event.mealId)
                    }
                }
                is PlannerEvent.NoRecommendationAvailable -> {
                    snackbarHostState.showSnackbar(
                        message = noRecommendationText,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    val hasAnyMeal = state.allTags.isNotEmpty() ||
            state.activeMenu != null ||
            state.recommendedMeal != null

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.planner_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showFilterSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.planner_filter),
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
                    message = stringResource(R.string.planner_empty_title) + "\n" +
                            stringResource(R.string.planner_empty_message)
                )
            } else if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) {
                // Tablet layout: side-by-side panes
                PlannerContent(
                    state = state,
                    viewModel = viewModel,
                    onMealClick = onMealClick,
                    isTablet = true
                )
            } else {
                // Phone layout: scrollable column
                PlannerContent(
                    state = state,
                    viewModel = viewModel,
                    onMealClick = onMealClick,
                    isTablet = false
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Shared planner content composable used by both phone and tablet layouts.
 */
@Composable
private fun PlannerContent(
    state: com.sranker.mealmate.ui.viewmodel.PlannerUiState,
    viewModel: PlannerViewModel,
    onMealClick: (Long) -> Unit,
    isTablet: Boolean
) {
    if (isTablet) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Left pane: filter chips + recommend button
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

                // Recommend button - only when not accepted
                RecommendButton(
                    isRecommending = state.isRecommending,
                    onRecommend = viewModel::recommendMeal,
                    isVisible = !state.isAccepted && !state.hasOutstandingRecommendation
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

                // Show recommended meal as a card with Pin/Skip
                state.recommendedMeal?.let { recommended ->
                    RecommendedMealCard(
                        meal = recommended,
                        onPin = viewModel::pinMeal,
                        onSkip = viewModel::skipMeal
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Right pane: Pinned meals + lifecycle buttons
            PinnedMealsSection(
                state = state,
                viewModel = viewModel,
                onMealClick = onMealClick,
                showAcceptButton = !state.isAccepted && !state.hasOutstandingRecommendation
            )
        }
    } else {
        // Phone layout
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

            // Recommend button - only when not accepted and no outstanding recommendation
            if (!state.isAccepted) {
                RecommendButton(
                    isRecommending = state.isRecommending,
                    onRecommend = viewModel::recommendMeal,
                    isVisible = !state.hasOutstandingRecommendation
                )
            }

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

            // Show recommended meal as a card in the pinned section
            state.recommendedMeal?.let { recommended ->
                RecommendedMealCard(
                    meal = recommended,
                    onPin = viewModel::pinMeal,
                    onSkip = viewModel::skipMeal
                )
            }

            // Pinned meals section
            PinnedMealsSection(
                state = state,
                viewModel = viewModel,
                onMealClick = onMealClick,
                showAcceptButton = !state.isAccepted && !state.hasOutstandingRecommendation
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Recommend button - only visible when appropriate (not accepted, no outstanding recommendation).
 */
@Composable
private fun RecommendButton(
    isRecommending: Boolean,
    onRecommend: () -> Unit,
    isVisible: Boolean
) {
    if (isVisible) {
        Button(
            onClick = onRecommend,
            enabled = !isRecommending,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isRecommending) stringResource(R.string.planner_recommending)
                else stringResource(R.string.planner_recommend)
            )
        }
    }
}

/**
 * Card showing the recommended meal with Pin and Skip action buttons.
 */
@Composable
private fun RecommendedMealCard(
    meal: MealWithTags,
    onPin: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.planner_recommended),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = meal.meal.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (meal.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    meal.tags.forEach { tag ->
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            meal.meal.servingSize?.let { size ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$size ${stringResource(R.string.meal_detail_servings_unit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onSkip,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.planner_skip))
                }
                Button(
                    onClick = onPin,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.planner_pin))
                }
            }
        }
    }
}

/**
 * Pinned meals section with lifecycle buttons.
 */
@Composable
private fun PinnedMealsSection(
    state: com.sranker.mealmate.ui.viewmodel.PlannerUiState,
    viewModel: PlannerViewModel,
    onMealClick: (Long) -> Unit,
    showAcceptButton: Boolean
) {
    val pinnedMeals = state.activeMenu?.meals ?: emptyList()

    if (pinnedMeals.isNotEmpty()) {
        Text(
            text = stringResource(R.string.planner_pinned_meals),
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
                onUnpin = { viewModel.unpinMeal(meal.id) },
                onMealClick = { onMealClick(meal.id) }
            )
        }
    }

    // Accept button - only shown when not accepted, has pinned meals, no outstanding recommendation
    if (showAcceptButton && pinnedMeals.isNotEmpty()) {
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
            Text(stringResource(R.string.planner_accept_menu))
        }
    }

    // Finish button - only shown after acceptance
    if (state.isAccepted) {
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
            Text(stringResource(R.string.planner_finish_menu))
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
 * A single pinned meal card in the active menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinnedMealCard(
    mealName: String,
    isCompleted: Boolean,
    isAccepted: Boolean,
    onToggleComplete: () -> Unit,
    onUnpin: () -> Unit,
    onMealClick: () -> Unit
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
                        contentDescription = stringResource(R.string.planner_unpin),
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
                onUnpin = onUnpin,
                onMealClick = onMealClick
            )
        }
    } else {
        PinnedMealCardContent(
            mealName = mealName,
            isCompleted = isCompleted,
            isAccepted = true,
            onToggleComplete = onToggleComplete,
            onUnpin = onUnpin,
            onMealClick = onMealClick
        )
    }
}

@Composable
private fun PinnedMealCardContent(
    mealName: String,
    isCompleted: Boolean,
    isAccepted: Boolean,
    onToggleComplete: () -> Unit,
    onUnpin: () -> Unit,
    onMealClick: () -> Unit
) {
    Card(
        onClick = onMealClick,
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
            // No right-side checkmark icon — completion is indicated by card color + checkbox only
            if (!isAccepted) {
                IconButton(onClick = onUnpin) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.planner_unpin),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

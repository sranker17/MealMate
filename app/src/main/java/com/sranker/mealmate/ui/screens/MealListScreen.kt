package com.sranker.mealmate.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sranker.mealmate.R
import com.sranker.mealmate.data.MealWithTags
import com.sranker.mealmate.ui.components.EmptyState
import com.sranker.mealmate.ui.viewmodel.MealListEvent
import com.sranker.mealmate.ui.viewmodel.MealListViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization

/**
 * Meal list screen with search bar, tag filter chips, scrollable meal list,
 * and add-to-plan buttons on each meal.
 *
 * @param viewModel The [MealListViewModel] providing UI state.
 * @param onMealClick Called when a meal card is tapped.
 * @param onAddMealClick Called when the add meal button is tapped.
 * @param onManageTagsClick Called when the manage tags button is tapped.
 */
@Composable
fun MealListScreen(
    viewModel: MealListViewModel,
    onMealClick: (Long) -> Unit,
    onAddMealClick: () -> Unit,
    onManageTagsClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Collect one-shot events for snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                MealListEvent.AddedToPlan -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.meal_added_to_plan))
                }
                MealListEvent.RemovedFromPlan -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.meal_removed_from_plan))
                }
                MealListEvent.AlreadyInPlan -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.meal_already_in_plan))
                }
                MealListEvent.MenuLocked -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.meal_plan_locked))
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.meal_list_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onManageTagsClick) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.meal_list_manage_tags),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onAddMealClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.meal_list_new_meal),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            placeholder = { Text(stringResource(R.string.meal_list_search_hint), style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Sentences
            )
        )

        // Filter chips
        if (state.allTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.allTags.forEach { tag ->
                    FilterChip(
                        selected = tag.id in state.selectedTagIds,
                        onClick = { viewModel.onTagFilterToggled(tag.id) },
                        label = { Text(tag.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Meal list or empty state
        if (state.meals.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Restaurant,
                message = stringResource(R.string.meal_list_empty)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.meals, key = { it.meal.id }) { mealWithTags ->
                    val isInPlan = mealWithTags.meal.id in state.mealIdsInActivePlan
                    MealListItem(
                        mealWithTags = mealWithTags,
                        isInPlan = isInPlan,
                        isMenuLocked = state.isActiveMenuLocked,
                        onClick = { onMealClick(mealWithTags.meal.id) },
                        onAddToPlan = { viewModel.addToActivePlan(mealWithTags.meal.id) }
                    )
                }
            }
        }

        // Snackbar host for add-to-plan feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * A single meal card in the list with add-to-plan button.
 */
@Composable
private fun MealListItem(
    mealWithTags: MealWithTags,
    isInPlan: Boolean,
    isMenuLocked: Boolean,
    onClick: () -> Unit,
    onAddToPlan: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mealWithTags.meal.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (mealWithTags.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        mealWithTags.tags.forEach { tag ->
                            AssistChip(
                                onClick = {},
                                label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
            // Add-to-plan button
            IconButton(onClick = onAddToPlan) {
                Icon(
                    imageVector = if (isInPlan) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                    contentDescription = stringResource(R.string.meal_add_to_plan),
                    tint = if (isInPlan)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

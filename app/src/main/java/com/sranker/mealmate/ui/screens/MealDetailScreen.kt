package com.sranker.mealmate.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sranker.mealmate.R
import com.sranker.mealmate.ui.components.EmptyState
import com.sranker.mealmate.ui.viewmodel.MealDetailEvent
import com.sranker.mealmate.ui.viewmodel.MealDetailViewModel

/**
 * Meal detail screen displaying all properties, stats, tags, ingredients,
 * and a clickable source link.
 *
 * @param viewModel The [MealDetailViewModel] providing UI state.
 * @param onBackClick Called to navigate back.
 * @param onEditClick Called to navigate to the edit screen.
 * @param onDeleteClick Called after the meal is deleted (to navigate back).
 */
@Composable
fun MealDetailScreen(
    viewModel: MealDetailViewModel,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Capture strings before LaunchedEffect (not a composable context)
    val deletedText = stringResource(R.string.meal_list_deleted)
    val undoText = stringResource(R.string.meal_list_undo)

    // Refresh data when screen is composed (handles returning from edit)
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // Consume one-shot events (delete with undo)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MealDetailEvent.MealDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = deletedText,
                        actionLabel = undoText,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleteMeal()
                    } else {
                        onDeleteClick()
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = stringResource(R.string.meal_detail_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                val mealId = state.mealWithTags?.meal?.id
                if (mealId != null) {
                    IconButton(onClick = { onEditClick(mealId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.meal_detail_edit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Only show delete button if meal is not pinned in active menu
                    if (!state.isPinned) {
                        IconButton(onClick = { viewModel.deleteMeal() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.meal_detail_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Content
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.meal_detail_loading),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                state.errorMessage != null || state.errorMessageResId != null -> {
                    EmptyState(
                        icon = Icons.Default.Restaurant,
                        message = when {
                            state.errorMessageResId != null -> stringResource(state.errorMessageResId!!)
                            else -> state.errorMessage!!
                        }
                    )
                }

                else -> {
                    val meal = state.mealWithTags ?: return
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        // Meal name
                        Text(
                            text = meal.meal.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // 1. Recipe (moved up)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = stringResource(R.string.meal_detail_recipe),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = meal.meal.recipe.ifBlank { stringResource(R.string.meal_detail_no_recipe) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (meal.meal.recipe.isBlank())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface
                        )

                        // 2. Ingredients (moved up)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = stringResource(R.string.meal_detail_ingredients),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val ingredients = state.mealWithIngredients?.ingredients
                        if (ingredients.isNullOrEmpty()) {
                            Text(
                                text = stringResource(R.string.meal_detail_no_ingredients),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            ingredients.forEach { ingredient ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = ingredient.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // 3. Tags / Type (in the middle)
                        if (meal.tags.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Text(
                                text = stringResource(R.string.meal_detail_tags),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                meal.tags.forEach { tag ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(tag.name) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.1f
                                            ),
                                            labelColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }

                        // 4. Source link (moved below tags)
                        if (meal.meal.sourceUrl.isNotBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Text(
                                text = stringResource(R.string.meal_detail_source),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val context = LocalContext.current
                            TextButton(
                                onClick = {
                                    val intent =
                                        Intent(Intent.ACTION_VIEW, meal.meal.sourceUrl.toUri())
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = meal.meal.sourceUrl,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }

                        // 5. Stats row (at the very bottom)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            meal.meal.servingSize?.let { size ->
                                StatItem(
                                    icon = Icons.Default.People,
                                    label = stringResource(R.string.meal_detail_serving_size),
                                    value = "$size ${stringResource(R.string.meal_detail_servings_unit)}"
                                )
                            }
                            StatItem(
                                icon = Icons.Default.Star,
                                label = stringResource(R.string.meal_detail_cooked),
                                value = meal.meal.timesCooked.toString()
                            )
                            StatItem(
                                icon = Icons.Default.SkipNext,
                                label = stringResource(R.string.meal_detail_skipped),
                                value = meal.meal.timesSkipped.toString()
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * A small stat item displayed in a row (e.g. times cooked, skipped, serving size).
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

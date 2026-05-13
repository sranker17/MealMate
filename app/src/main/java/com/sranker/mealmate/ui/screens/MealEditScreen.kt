package com.sranker.mealmate.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sranker.mealmate.ui.viewmodel.MealEditViewModel

/**
 * Meal edit/create screen.
 *
 * Shows a form with fields for name, recipe, ingredients, tags, serving size,
 * and source URL. Handles both creating new meals and editing existing ones.
 *
 * @param viewModel The [MealEditViewModel] providing form state.
 * @param onBackClick Called to navigate back.
 * @param onMealSaved Called after the meal is successfully saved, with the new meal ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealEditScreen(
    viewModel: MealEditViewModel,
    onBackClick: () -> Unit,
    onMealSaved: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Navigate back on successful save
    LaunchedEffect(state.savedMealId) {
        state.savedMealId?.let { id ->
            onMealSaved(id)
        }
    }

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
                    contentDescription = "Vissza",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = if (state.isEditing) "Étel szerkesztése" else "Új étel",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.saveMeal() },
                enabled = !state.isSaving
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Mentés",
                    tint = if (state.isSaving)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }

        // Form content
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChanged,
                    label = { Text("Név") },
                    placeholder = { Text("Pl. Csirkepaprikás") },
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Recipe
            item {
                OutlinedTextField(
                    value = state.recipe,
                    onValueChange = viewModel::onRecipeChanged,
                    label = { Text("Recept") },
                    placeholder = { Text("Elkészítési útmutató…") },
                    minLines = 3,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Serving size
            item {
                OutlinedTextField(
                    value = state.servingSize?.toString() ?: "",
                    onValueChange = { value ->
                        viewModel.onServingSizeChanged(value.toIntOrNull())
                    },
                    label = { Text("Adag (fő)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Source URL
            item {
                OutlinedTextField(
                    value = state.sourceUrl,
                    onValueChange = viewModel::onSourceUrlChanged,
                    label = { Text("Forrás link") },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Ingredients section
            item {
                Text(
                    text = "Hozzávalók",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            itemsIndexed(state.ingredients) { index, ingredient ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ingredient.name,
                        onValueChange = { viewModel.onIngredientChanged(index, it) },
                        placeholder = { Text("Új hozzávaló…") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (state.ingredients.size > 1) {
                        IconButton(onClick = { viewModel.onRemoveIngredient(index) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Eltávolítás",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Add ingredient button
            item {
                OutlinedButton(
                    onClick = viewModel::onAddIngredient
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hozzávaló hozzáadása")
                }
            }

            // Tags section
            if (state.allTags.isNotEmpty()) {
                item {
                    Text(
                        text = "Címkék",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.allTags.forEach { tag ->
                            val isSelected = tag.id in state.selectedTagIds
                            if (isSelected) {
                                AssistChip(
                                    onClick = { viewModel.onTagToggled(tag.id) },
                                    label = { Text(tag.name) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            } else {
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.onTagToggled(tag.id) },
                                    label = { Text(tag.name) },
                                    colors = FilterChipDefaults.filterChipColors()
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}


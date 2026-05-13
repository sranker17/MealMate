package com.sranker.mealmate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
 * Meal edit/create screen with elegant, minimalistic styling.
 *
 * Shows a form with fields for name, recipe, ingredients, tags, serving size,
 * and source URL. Handles both creating new meals and editing existing ones.
 *
 * @param viewModel The [MealEditViewModel] providing form state.
 * @param onBackClick Called to navigate back.
 * @param onMealSaved Called after the meal is successfully saved, with the new meal ID.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
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
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                    shape = MaterialTheme.shapes.medium,
                    colors = elegantTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Tags section — always visible, even if empty
            item {
                SectionLabel(text = "Címkék")

                // Existing tags as multi-select chips
                if (state.allTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        state.allTags.forEach { tag ->
                            val isSelected = tag.id in state.selectedTagIds
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onTagToggled(tag.id) },
                                label = { Text(tag.name, style = MaterialTheme.typography.labelMedium) },
                                trailingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Close, contentDescription = "Eltávolítás", modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    Text(
                        text = "Még nincsenek címkék. Hozz létre címkéket a Címkék oldalon.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Recipe
            item {
                SectionLabel(text = "Recept")
                OutlinedTextField(
                    value = state.recipe,
                    onValueChange = viewModel::onRecipeChanged,
                    placeholder = { Text("Elkészítési útmutató…") },
                    minLines = 3,
                    maxLines = 8,
                    shape = MaterialTheme.shapes.medium,
                    colors = elegantTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Serving size
            item {
                SectionLabel(text = "Adag")
                OutlinedTextField(
                    value = state.servingSize?.toString() ?: "",
                    onValueChange = { value ->
                        viewModel.onServingSizeChanged(value.toIntOrNull())
                    },
                    placeholder = { Text("Fő") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = elegantTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Source URL
            item {
                SectionLabel(text = "Forrás")
                OutlinedTextField(
                    value = state.sourceUrl,
                    onValueChange = viewModel::onSourceUrlChanged,
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = elegantTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Ingredients section
            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                SectionLabel(text = "Hozzávalók")
            }

            itemsIndexed(state.ingredients) { index, ingredient ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ingredient.name,
                        onValueChange = { viewModel.onIngredientChanged(index, it) },
                        placeholder = { Text("Pl. 200g liszt") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = elegantTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    if (state.ingredients.size > 1) {
                        IconButton(onClick = { viewModel.onRemoveIngredient(index) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Eltávolítás",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Add ingredient button
            item {
                OutlinedButton(
                    onClick = viewModel::onAddIngredient,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hozzávaló hozzáadása")
                }
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/**
 * Elegant, minimalistic text field colors.
 * Uses a subtle bottom-border-only aesthetic with transparent background.
 */
@Composable
private fun elegantTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
)

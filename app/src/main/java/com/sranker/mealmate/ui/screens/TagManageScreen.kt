package com.sranker.mealmate.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.sranker.mealmate.R
import com.sranker.mealmate.ui.components.EmptyState
import com.sranker.mealmate.ui.viewmodel.TagManageEvent
import com.sranker.mealmate.ui.viewmodel.TagManageViewModel

private val roundedShape = RoundedCornerShape(12.dp)

/**
 * Tag management screen.
 *
 * Displays a list of existing tags with delete functionality and
 * an input field to add new tags.
 *
 * @param viewModel The [TagManageViewModel] providing UI state.
 * @param onBackClick Called to navigate back.
 */
@Composable
fun TagManageScreen(
    viewModel: TagManageViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val tagDeletedText = stringResource(R.string.tag_manage_deleted)
    val undoText = stringResource(R.string.tag_manage_undo)

    // Collect tag delete events for undo snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TagManageEvent.TagDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = tagDeletedText,
                        actionLabel = undoText
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleteTag()
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
                    text = stringResource(R.string.tag_manage_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
            }

            // Add tag input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = state.newTagName,
                    onValueChange = viewModel::onNewTagNameChanged,
                    placeholder = { Text(stringResource(R.string.tag_manage_add_hint)) },
                    isError = state.errorMessage != null || state.errorMessageResId != null,
                    supportingText = when {
                        state.errorMessageResId != null -> {
                            {
                                Text(
                                    stringResource(state.errorMessageResId!!),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        state.errorMessage != null -> {
                            { Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error) }
                        }

                        else -> null
                    },
                    singleLine = true,
                    shape = roundedShape,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = viewModel::addTag,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.tag_manage_add),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tag list or empty state
            if (state.tags.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.Label,
                    message = stringResource(R.string.tag_manage_empty)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.tags, key = { it.id }) { tag ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(tag.name) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        labelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.deleteTag(tag) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

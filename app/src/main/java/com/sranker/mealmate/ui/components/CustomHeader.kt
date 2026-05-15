package com.sranker.mealmate.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sranker.mealmate.R

/**
 * A minimalist header composable with an optional back button and optional action button.
 *
 * @param title The title text displayed in the center of the header.
 * @param onBackClick Optional callback for the back navigation button. Null hides the button.
 * @param actionIcon Optional icon for an action button on the right side.
 * @param onActionClick Optional callback for the action button.
 * @param modifier Optional [Modifier] applied to the root [Box].
 */
@Composable
fun CustomHeader(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Left: Back button
        if (onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Center: Title
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .then(
                    if (onBackClick != null || actionIcon != null) {
                        Modifier.padding(horizontal = 48.dp)
                    } else {
                        Modifier
                    }
                )
        )

        // Right: Action button
        if (actionIcon != null && onActionClick != null) {
            IconButton(
                onClick = onActionClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                actionIcon()
            }
        }
    }
}

package com.michael.vibeboard.ui.memes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.michael.vibeboard.domain.model.MediaSuggestion
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Modal overlay within the current composition (no [android.app.Dialog]). Required for IME:
 * [InputMethodService] has no valid window token for system dialogs.
 */
@Composable
fun MemeGalleryOverlay(
    visible: Boolean,
    title: String,
    items: List<MediaSuggestion>,
    onDismiss: () -> Unit,
    onPick: (MediaSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    loadingMore: Boolean = false,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {},
) {
    if (!visible) return
    val showGiphyAttribution = items.any { it.requireGiphyAttribution }
    Box(modifier.zIndex(1f)) {
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                Text(
                    text = "Tap a meme to insert its label · scroll for more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                MemeGalleryGrid(
                    items = items,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    onItemClick = {
                        onPick(it)
                        onDismiss()
                    },
                    loadingMore = loadingMore,
                    hasMore = hasMore,
                    onLoadMore = onLoadMore,
                )
                if (loadingMore) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.CenterHorizontally),
                        strokeWidth = 2.dp,
                    )
                }
                if (showGiphyAttribution) {
                    Text(
                        text = "Powered by Giphy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun MemeGalleryGrid(
    items: List<MediaSuggestion>,
    modifier: Modifier = Modifier,
    onItemClick: (MediaSuggestion) -> Unit,
    loadingMore: Boolean = false,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {},
) {
    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, items.size, hasMore, loadingMore) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = info.totalItemsCount
            last to total
        }
            .distinctUntilChanged()
            .filter { (_, total) -> total > 0 }
            .collect { (lastVisible, total) ->
                if (hasMore && !loadingMore && lastVisible >= total - 4) {
                    onLoadMore()
                }
            }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 108.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            MemeGalleryTile(
                item = item,
                onClick = { onItemClick(item) },
            )
        }
    }
}

@Composable
private fun MemeGalleryTile(
    item: MediaSuggestion,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            AsyncImage(
                model = item.remoteMediaUrl ?: item.previewDrawableRes,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(item.previewDrawableRes),
                error = painterResource(item.previewDrawableRes),
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                maxLines = 2,
            )
        }
    }
}

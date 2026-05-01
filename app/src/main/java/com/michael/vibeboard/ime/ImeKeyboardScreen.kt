@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.michael.vibeboard.ime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.michael.vibeboard.domain.model.MediaSuggestion
import com.michael.vibeboard.domain.usecase.GetMediaSuggestionsUseCase
import com.michael.vibeboard.domain.usecase.LoadMemeGalleryPageUseCase
import com.michael.vibeboard.presentation.SUGGESTION_DEBOUNCE_MS
import com.michael.vibeboard.ui.memes.MemeGalleryOverlay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val Row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
private val Row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
private val Row3 = listOf("z", "x", "c", "v", "b", "n", "m")

@Composable
fun ImeKeyboardScreen(
    controller: ImeKeyboardController,
    getMediaSuggestions: GetMediaSuggestionsUseCase,
    loadMemeGalleryPage: LoadMemeGalleryPageUseCase,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var memeGalleryOpen by remember { mutableStateOf(false) }
    var memeGalleryItems by remember { mutableStateOf<List<MediaSuggestion>>(emptyList()) }
    var memeGalleryLoading by remember { mutableStateOf(false) }
    var memeGalleryLoadingMore by remember { mutableStateOf(false) }
    var memeGalleryHasMore by remember { mutableStateOf(true) }
    var memeGalleryNextOffset by remember { mutableStateOf(0) }
    var draft by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        controller.draftForSuggestions.collectLatest { draft = it }
    }

    var suggestions by remember { mutableStateOf<List<MediaSuggestion>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        controller.draftForSuggestions
            .debounce(SUGGESTION_DEBOUNCE_MS)
            .distinctUntilChanged()
            .collectLatest { debouncedDraft ->
                loading = true
                try {
                    suggestions = getMediaSuggestions(debouncedDraft)
                } catch (_: Exception) {
                    suggestions = emptyList()
                } finally {
                    loading = false
                }
            }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Box(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "VibeBoard",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(
                    enabled = !memeGalleryLoading,
                    onClick = {
                        scope.launch {
                            memeGalleryLoading = true
                            try {
                                val page = loadMemeGalleryPage(draft, offset = 0, limit = 25)
                                memeGalleryItems = page.items
                                memeGalleryNextOffset = page.nextOffset
                                memeGalleryHasMore = page.hasMore
                                memeGalleryOpen = true
                            } catch (_: Exception) {
                                memeGalleryItems = emptyList()
                                memeGalleryHasMore = false
                                memeGalleryOpen = true
                            } finally {
                                memeGalleryLoading = false
                            }
                        }
                    },
                ) {
                    Text("All memes")
                }
            }

            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(4.dp),
                    strokeWidth = 2.dp,
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            ) {
                items(suggestions, key = { it.id }) { item ->
                    AssistChip(
                        onClick = { controller.commitSuggestionPreview(item.title) },
                        label = { Text(item.title, maxLines = 1) },
                        modifier = Modifier.widthIn(max = 140.dp),
                    )
                }
            }

            KeyRow(keys = Row1, controller = controller)
            KeyRow(keys = Row2, controller = controller)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row3.forEach { key ->
                    ImeKey(label = key, onClick = { controller.commitText(key) })
                }
                FilledTonalButton(
                    onClick = controller::deleteBackward,
                    modifier = Modifier.weight(1.4f),
                ) {
                    Text("⌫")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledTonalButton(
                    onClick = { controller.commitText(" ") },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Space")
                }
                FilledTonalButton(
                    onClick = { controller.commitText(".") },
                    modifier = Modifier.weight(0.35f),
                ) {
                    Text(".")
                }
                FilledTonalButton(
                    onClick = { controller.commitText(",") },
                    modifier = Modifier.weight(0.35f),
                ) {
                    Text(",")
                }
            }
        }

        MemeGalleryOverlay(
            visible = memeGalleryOpen,
            title = "Recommended memes",
            items = memeGalleryItems,
            onDismiss = { memeGalleryOpen = false },
            onPick = { item ->
                controller.commitSuggestionPreview(item.title)
            },
            modifier = Modifier.matchParentSize(),
            loadingMore = memeGalleryLoadingMore,
            hasMore = memeGalleryHasMore,
            onLoadMore = {
                if (memeGalleryHasMore && !memeGalleryLoadingMore && !memeGalleryLoading) {
                    scope.launch {
                        memeGalleryLoadingMore = true
                        try {
                            val page = loadMemeGalleryPage(draft, offset = memeGalleryNextOffset, limit = 25)
                            val existing = memeGalleryItems.map { it.id }.toSet()
                            memeGalleryItems = memeGalleryItems + page.items.filter { it.id !in existing }
                            memeGalleryNextOffset = page.nextOffset
                            memeGalleryHasMore = page.hasMore
                        } catch (_: Exception) {
                            memeGalleryHasMore = false
                        } finally {
                            memeGalleryLoadingMore = false
                        }
                    }
                }
            },
        )
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<String>,
    controller: ImeKeyboardController,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        keys.forEach { key ->
            ImeKey(label = key, onClick = { controller.commitText(key) })
        }
    }
}

@Composable
private fun RowScope.ImeKey(
    label: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
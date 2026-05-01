package com.michael.vibeboard.presentation.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.michael.vibeboard.domain.model.MediaSuggestion
import com.michael.vibeboard.ui.memes.MemeGalleryOverlay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        Text(
            text = "Draft message",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = state.draftText,
            onValueChange = viewModel::onDraftTextChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Type here (keyboard preview)") },
            minLines = 3,
        )

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            text = "Suggestions (from your draft · Giphy when configured)",
            style = MaterialTheme.typography.titleSmall,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(state.suggestions, key = { it.id }) { item ->
                SuggestionCard(item)
            }
        }

        OutlinedButton(
            onClick = viewModel::onOpenMemeGallery,
            enabled = !state.memeGalleryLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.memeGalleryLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .height(20.dp)
                        .width(20.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text("See all memes (GIFs & images)")
        }

        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open keyboard settings (enable VibeBoard)")
        }
        }

        MemeGalleryOverlay(
            visible = state.memeGalleryOpen,
            title = "Recommended memes",
            items = state.memeGalleryItems,
            onDismiss = viewModel::onCloseMemeGallery,
            onPick = { item -> viewModel.onMemePickedFromGallery(item.title) },
            modifier = Modifier.fillMaxSize(),
            loadingMore = state.memeGalleryLoadingMore,
            hasMore = state.memeGalleryHasMore,
            onLoadMore = viewModel::onLoadMoreMemeGallery,
        )
    }
}

@Composable
private fun SuggestionCard(item: MediaSuggestion) {
    Card(modifier = Modifier.width(168.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AsyncImage(
                model = item.remoteMediaUrl ?: item.previewDrawableRes,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(item.previewDrawableRes),
                error = painterResource(item.previewDrawableRes),
            )
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            Text(
                item.tags.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

package com.michael.vibeboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michael.vibeboard.presentation.home.HomeScreen
import com.michael.vibeboard.presentation.home.HomeViewModel
import com.michael.vibeboard.presentation.home.HomeViewModelFactory
import com.michael.vibeboard.ui.theme.VibeBoardTheme

class MainActivity : ComponentActivity() {

    private val appContainer get() = (application as VibeBoardApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val factory = remember {
                HomeViewModelFactory(
                    appContainer.getMediaSuggestionsUseCase,
                    appContainer.loadMemeGalleryPageUseCase,
                )
            }
            val homeViewModel: HomeViewModel = viewModel(factory = factory)
            VibeBoardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        viewModel = homeViewModel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

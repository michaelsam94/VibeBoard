package com.michael.vibeboard.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.michael.vibeboard.VibeBoardApplication
import com.michael.vibeboard.ui.theme.VibeBoardTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [ComposeView] is final, so we host it in a [FrameLayout]. Compose resolves the window recomposer
 * from the IME chrome ([android.R.id.parentPanel]); that lookup walks up from the panel before the
 * child [ComposeView] runs, so we assign [ViewTreeLifecycleOwner] and the saved-state registry
 * owner to this host and its ancestors in [onAttachedToWindow] **before** [FrameLayout] dispatches
 * attach to children.
 */
private class ImeComposeHost(
    private val service: VibeBoardInputMethodService,
    private val compose: ComposeView,
) : FrameLayout(service) {

    init {
        setViewTreeLifecycleOwner(service)
        setViewTreeSavedStateRegistryOwner(service)
        addView(
            compose,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
    }

    override fun onAttachedToWindow() {
        var view: View? = parent as? View
        while (view != null) {
            view.setViewTreeLifecycleOwner(service)
            view.setViewTreeSavedStateRegistryOwner(service)
            view = view.parent as? View
        }
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        val ancestors = buildList {
            var v: View? = parent as? View
            while (v != null) {
                add(v)
                v = v.parent as? View
            }
        }
        super.onDetachedFromWindow()
        ancestors.forEach {
            it.setViewTreeLifecycleOwner(null)
            it.setViewTreeSavedStateRegistryOwner(null)
        }
    }
}

class VibeBoardInputMethodService : InputMethodService(), SavedStateRegistryOwner, ImeKeyboardController {

    private val lifecycleRegistry = LifecycleRegistry(this)

    private val savedStateRegistryController by lazy(LazyThreadSafetyMode.NONE) {
        SavedStateRegistryController.create(this)
    }

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private val _draftForSuggestions = MutableStateFlow("")
    override val draftForSuggestions: StateFlow<String> = _draftForSuggestions.asStateFlow()

    private fun refreshDraftFromEditor() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(512, 0)?.toString().orEmpty()
        _draftForSuggestions.value = before
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        refreshDraftFromEditor()
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        refreshDraftFromEditor()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd,
        )
        refreshDraftFromEditor()
    }

    override fun onCreateInputView(): View {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            }
            if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
        }
        val container = (application as VibeBoardApplication).container
        val owner = this@VibeBoardInputMethodService
        val compose = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VibeBoardTheme {
                    ImeKeyboardScreen(
                        controller = owner,
                        getMediaSuggestions = container.getMediaSuggestionsUseCase,
                        loadMemeGalleryPage = container.loadMemeGalleryPageUseCase,
                    )
                }
            }
        }
        return ImeComposeHost(owner, compose)
    }

    override fun onDestroy() {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            }
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        super.onDestroy()
    }

    override fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
        refreshDraftFromEditor()
    }

    override fun deleteBackward() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        refreshDraftFromEditor()
    }

    override fun commitSuggestionPreview(title: String) {
        currentInputConnection?.commitText("[$title] ", 1)
        refreshDraftFromEditor()
    }
}

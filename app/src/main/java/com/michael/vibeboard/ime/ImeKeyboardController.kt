package com.michael.vibeboard.ime

import kotlinx.coroutines.flow.StateFlow

/**
 * Bridge between [android.inputmethodservice.InputMethodService] and Compose UI.
 */
interface ImeKeyboardController {
    val draftForSuggestions: StateFlow<String>

    fun commitText(text: String)

    fun deleteBackward()

    /** Skeleton: inserts a visible placeholder; replace with image span / commitContent later. */
    fun commitSuggestionPreview(title: String)
}

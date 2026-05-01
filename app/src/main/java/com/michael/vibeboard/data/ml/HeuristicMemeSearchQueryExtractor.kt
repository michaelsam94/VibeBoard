package com.michael.vibeboard.data.ml

import com.michael.vibeboard.domain.ml.MemeSearchQueryExtractor

/**
 * Rule-based placeholder until a local on-device model provides intent → query mapping.
 */
class HeuristicMemeSearchQueryExtractor : MemeSearchQueryExtractor {

    override fun toGiphySearchQuery(userDraftText: String): String {
        val collapsed = userDraftText.trim().replace(Regex("\\s+"), " ")
        if (collapsed.length < 2) return ""
        return collapsed.take(MAX_QUERY_LEN)
    }

    companion object {
        private const val MAX_QUERY_LEN = 200
    }
}

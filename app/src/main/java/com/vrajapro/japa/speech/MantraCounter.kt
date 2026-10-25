package com.vrajapro.japa.speech

import java.text.Normalizer

/**
 * Counts complete Hare Krishna maha-mantras in speech transcripts.
 *
 *   Hare Krishna Hare Krishna Krishna Krishna Hare Hare
 *   Hare Rama    Hare Rama    Rama    Rama    Hare Hare
 *
 * One mantra = 16 recognised mantra words. The word position ([streak]) is
 * carried across transcripts, so pausing mid-mantra does not lose progress.
 * Unrecognised words (noise, breath) are ignored and do not reset the streak.
 *
 * Port of the counting logic in the Japa Meditation Online web app.
 */
class MantraCounter {

    /** Number of mantra words heard toward the current mantra (0..15). */
    var streak: Int = 0
        private set

    fun reset() { streak = 0 }

    /** Advance one word without speech (used by guided chanting). Returns true when a mantra completes. */
    fun advance(): Boolean {
        streak++
        if (streak == WORDS_PER_MANTRA) { streak = 0; return true }
        return false
    }

    /** Feed a final speech transcript; returns the number of mantras completed. */
    fun feed(transcript: String): Int {
        val latin = devanagariToLatin(transcript)
        var found = 0
        for (token in latin.split(WHITESPACE).filter { it.isNotEmpty() }) {
            repeat(extractMantraWords(token).size) {
                if (advance()) found++
            }
        }
        return found
    }

    companion object {
        const val WORDS_PER_MANTRA = 16

        private val WHITESPACE = Regex("\\s+")
        private val PUNCTUATION = Regex("[.,!?;:।॥\"'“”‘’]")
        private val DEVANAGARI = Regex("[\\u0900-\\u097F]")

        private val RAMA_WORDS = setOf("rama", "raam", "rāma", "ram")
        private val HARE_WORDS = setOf("hare", "hari", "hāre", "harry", "hurray", "holly", "hara")
        private val KRISHNA_WORDS = setOf("krishna", "krsna", "krishn", "cristina", "cristine")

        /** Longest first so greedy matching picks "krishna" before "krishn", etc. */
        private val ALL_WORDS: List<String> =
            (RAMA_WORDS + HARE_WORDS + KRISHNA_WORDS).sortedByDescending { it.length }

        private val DEVANAGARI_LATIN = mapOf(
            "हरे" to "hare", "हरि" to "hari", "हरी" to "hari", "हर" to "hare", "हारे" to "hare",
            "राम" to "rama", "रामा" to "rama",
            "कृष्ण" to "krishna", "कृष्णा" to "krishna",
            "हरा" to "hara",
        ).mapKeys { Normalizer.normalize(it.key, Normalizer.Form.NFC) }

        private fun clean(w: String) = w.lowercase().replace(PUNCTUATION, "")

        /** Maps known Devanagari mantra words to Latin; drops other Devanagari tokens. */
        fun devanagariToLatin(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFC)
                .split(WHITESPACE)
                .map { token ->
                    val cleaned = token.replace(PUNCTUATION, "")
                    DEVANAGARI_LATIN[cleaned] ?: if (DEVANAGARI.containsMatchIn(cleaned)) "" else token
                }
                .filter { it.isNotEmpty() }
                .joinToString(" ")

        /** Splits merged tokens such as "harekrishna" into ["hare", "krishna"]. */
        fun extractMantraWords(token: String): List<String> {
            val s = clean(token)
            val found = mutableListOf<String>()
            var i = 0
            while (i < s.length) {
                val match = ALL_WORDS.firstOrNull { s.startsWith(it, i) }
                if (match != null) { found += match; i += match.length } else i++
            }
            return found
        }
    }
}

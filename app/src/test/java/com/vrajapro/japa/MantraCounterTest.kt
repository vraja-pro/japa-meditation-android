package com.vrajapro.japa

import com.vrajapro.japa.speech.MantraCounter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MantraCounterTest {

    private val full = "Hare Krishna Hare Krishna Krishna Krishna Hare Hare " +
        "Hare Rama Hare Rama Rama Rama Hare Hare"

    @Test fun countsOneFullMantra() {
        val c = MantraCounter()
        assertEquals(1, c.feed(full))
        assertEquals(0, c.streak)
    }

    @Test fun carriesStreakAcrossTranscripts() {
        val c = MantraCounter()
        assertEquals(0, c.feed("Hare Krishna Hare Krishna Krishna Krishna Hare Hare"))
        assertEquals(8, c.streak)
        assertEquals(1, c.feed("Hare Rama Hare Rama Rama Rama Hare Hare"))
    }

    @Test fun ignoresNoiseWithoutResettingStreak() {
        val c = MantraCounter()
        c.feed("Hare Krishna um Hare")
        assertEquals(3, c.streak)
    }

    @Test fun splitsMergedWords() {
        assertEquals(listOf("hare", "krishna"), MantraCounter.extractMantraWords("HareKrishna"))
        assertEquals(listOf("hare", "rama"), MantraCounter.extractMantraWords("hare-rama!"))
    }

    @Test fun mapsDevanagari() {
        val c = MantraCounter()
        val hindi = "हरे कृष्ण हरे कृष्ण कृष्ण कृष्ण हरे हरे हरे राम हरे राम राम राम हरे हरे"
        assertEquals(1, c.feed(hindi))
    }

    @Test fun dropsUnknownDevanagari() {
        assertEquals("hare", MantraCounter.devanagariToLatin("हरे नमस्ते"))
    }

    @Test fun countsMultipleMantrasInOneTranscript() {
        val c = MantraCounter()
        assertEquals(2, c.feed("$full $full"))
    }

    @Test fun guidedAdvanceCompletesEvery16Words() {
        val c = MantraCounter()
        repeat(15) { assertFalse(c.advance()) }
        assertTrue(c.advance())
        assertEquals(0, c.streak)
    }
}

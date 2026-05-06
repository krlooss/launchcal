package com.launchcal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {

    @Test
    fun stripAccents_removesAcuteAccents() {
        assertEquals("camara", AppSearch.stripAccents("cámara"))
    }

    @Test
    fun stripAccents_removesTilde() {
        assertEquals("espanol", AppSearch.stripAccents("español"))
    }

    @Test
    fun stripAccents_removesMultipleAccents() {
        assertEquals("electronica", AppSearch.stripAccents("electrónica"))
    }

    @Test
    fun stripAccents_preservesUnaccentedText() {
        assertEquals("hello", AppSearch.stripAccents("hello"))
    }

    @Test
    fun stripAccents_handlesUmlaut() {
        assertEquals("uber", AppSearch.stripAccents("über"))
    }

    @Test
    fun matches_queryWithoutAccentFindsAccentedName() {
        assertTrue(AppSearch.matches("Cámara", "camara"))
    }

    @Test
    fun matches_queryWithAccentFindsAccentedName() {
        assertTrue(AppSearch.matches("Cámara", "cámara"))
    }

    @Test
    fun matches_queryWithAccentFindsUnaccentedName() {
        assertTrue(AppSearch.matches("Camara", "cámara"))
    }

    @Test
    fun matches_caseInsensitive() {
        assertTrue(AppSearch.matches("CÁMARA", "camara"))
    }

    @Test
    fun matches_partialMatch() {
        assertTrue(AppSearch.matches("Cámara de fotos", "camar"))
    }

    @Test
    fun matches_noMatch() {
        assertFalse(AppSearch.matches("Teléfono", "camara"))
    }

    @Test
    fun matches_emptyQuery() {
        assertTrue(AppSearch.matches("Cámara", ""))
    }

    @Test
    fun matches_specialCharactersPreserved() {
        assertTrue(AppSearch.matches("São Paulo", "sao"))
    }

    @Test
    fun matches_frenchAccents() {
        assertTrue(AppSearch.matches("Café", "cafe"))
    }

    @Test
    fun matches_combinedDiacritics() {
        assertTrue(AppSearch.matches("naïve", "naive"))
    }
}

package com.launchcal

import java.text.Normalizer

object AppSearch {
    fun stripAccents(input: String): String =
        Normalizer.normalize(input, Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "")

    fun matches(name: String, query: String): Boolean =
        stripAccents(name).contains(stripAccents(query), ignoreCase = true)
}

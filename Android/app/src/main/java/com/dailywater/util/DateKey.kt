package com.dailywater.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Helper to generate the local yyyy-MM-dd date key.
 * Uses the device's default timezone via LocalDate.now(),
 * so the key always reflects the user's local calendar day.
 */
object DateKey {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun today(): String = LocalDate.now().format(formatter)
}

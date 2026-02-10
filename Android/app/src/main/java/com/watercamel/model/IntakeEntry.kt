package com.watercamel.model

import java.util.UUID

/**
 * A single water intake record.
 */
data class IntakeEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val amount: Double
)

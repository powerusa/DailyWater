package com.watercamel.model

/**
 * Archived summary for one completed day of water tracking.
 */
data class DailyRecord(
    val dateKey: String,       // yyyy-MM-dd
    val totalIntake: Double,
    val goal: Double,
    val unit: String
) {
    /** Progress ratio clamped to [0, 1]. */
    val progress: Double
        get() = if (goal > 0) minOf(totalIntake / goal, 1.0) else 0.0

    /** Whether the goal was met on this day. */
    val goalMet: Boolean
        get() = goal > 0 && totalIntake >= goal
}

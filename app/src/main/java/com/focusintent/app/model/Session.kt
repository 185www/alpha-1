package com.focusintent.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Session entity representing a focus session record.
 * Stored in Room database when session ends or is aborted.
 */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purpose: String,
    val startTime: Long,
    val endTime: Long? = null,
    val durationSec: Long = 0,
    val status: SessionStatus = SessionStatus.ACTIVE
)

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    ABORTED
}

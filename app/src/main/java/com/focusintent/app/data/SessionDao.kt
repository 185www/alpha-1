package com.focusintent.app.data

import androidx.room.*
import com.focusintent.app.model.Session
import com.focusintent.app.model.SessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Session entity.
 * Provides CRUD operations and queries for focus session records.
 */
@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE status != 'ACTIVE' ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): Session?

    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSession(): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}

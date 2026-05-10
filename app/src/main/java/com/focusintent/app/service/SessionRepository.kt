package com.focusintent.app.service

import com.focusintent.app.data.SessionDao
import com.focusintent.app.model.Session
import com.focusintent.app.model.SessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing session data.
 * Provides a clean API for data operations to the rest of the app.
 */
class SessionRepository(
    private val sessionDao: SessionDao
) {
    val allSessions: Flow<List<Session>> = sessionDao.getAllSessions()

    suspend fun getSessionById(id: Long): Session? = sessionDao.getSessionById(id)

    suspend fun getActiveSession(): Session? = sessionDao.getActiveSession()

    suspend fun insert(session: Session): Long = sessionDao.insert(session)

    suspend fun update(session: Session) = sessionDao.update(session)

    suspend fun delete(session: Session) = sessionDao.delete(session)

    suspend fun deleteAll() = sessionDao.deleteAll()
}

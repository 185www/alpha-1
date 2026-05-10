package com.focusintent.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.focusintent.app.R
import com.focusintent.app.data.AppDatabase
import com.focusintent.app.model.Session
import com.focusintent.app.model.SessionStatus
import com.focusintent.app.ui.MainActivity
import kotlinx.coroutines.*
import java.util.*

/**
 * Foreground service that manages the overlay window.
 * Uses WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY for Android 8.0+.
 * Timer logic based on SystemClock.elapsedRealtime() for accuracy.
 */
class OverlayService : Service() {

    private lateinit var sessionRepository: SessionRepository

    private val binder = LocalBinder()
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var isOverlayVisible = false
    
    // Timer state - using elapsedRealtime for accurate timing
    private var startTime: Long = 0
    private var targetDurationSec: Long = 0
    private var isCountUp: Boolean = true
    private var remainingTimeSec: Long = 0
    private var isPaused: Boolean = false
    private var pauseStartTime: Long = 0
    private var totalPausedTime: Long = 0
    
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        const val CHANNEL_ID = "focus_intent_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.focusintent.action.START"
        const val ACTION_PAUSE = "com.focusintent.action.PAUSE"
        const val ACTION_RESUME = "com.focusintent.action.RESUME"
        const val ACTION_END = "com.focusintent.action.END"
        const val EXTRA_PURPOSE = "purpose"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_COUNT_UP = "count_up"
        
        var isActive: Boolean = false
            private set
    }

    inner class LocalBinder : android.os.Binder() {
        fun getService(): OverlayService = this@OverlayService
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize repository manually without dependency injection
        val database = AppDatabase.getDatabase(this)
        sessionRepository = SessionRepository(database.sessionDao())
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val purpose = intent.getStringExtra(EXTRA_PURPOSE) ?: "专注中"
                val duration = intent.getLongExtra(EXTRA_DURATION, 25 * 60)
                val countUp = intent.getBooleanExtra(EXTRA_COUNT_UP, true)
                startSession(purpose, duration, countUp)
            }
            ACTION_PAUSE -> pauseSession()
            ACTION_RESUME -> resumeSession()
            ACTION_END -> endSession(SessionStatus.COMPLETED)
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        timerJob?.cancel()
        scope.cancel()
        isActive = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_service_notification_title))
            .setContentText(getString(R.string.overlay_service_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startSession(purpose: String, durationSec: Long, countUp: Boolean) {
        isActive = true
        startTime = android.os.SystemClock.elapsedRealtime()
        targetDurationSec = durationSec
        isCountUp = countUp
        remainingTimeSec = if (countUp) 0 else durationSec
        isPaused = false
        totalPausedTime = 0
        
        // Save session to database
        scope.launch {
            val session = Session(
                purpose = purpose,
                startTime = System.currentTimeMillis(),
                status = SessionStatus.ACTIVE
            )
            sessionRepository.insert(session)
        }
        
        startForegroundNotification()
        createOverlay(purpose)
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && !isPaused) {
                updateOverlayText()
                
                // Check if timer should end
                if (!isCountUp) {
                    val elapsedSec = (android.os.SystemClock.elapsedRealtime() - startTime + totalPausedTime) / 1000
                    if (elapsedSec >= targetDurationSec) {
                        endSession(SessionStatus.COMPLETED)
                        break
                    }
                }
                
                delay(1000) // 1Hz refresh rate
            }
        }
    }

    private fun updateOverlayText() {
        val textView = overlayView.findViewById<TextView>(R.id.overlay_timer)
        val purposeView = overlayView.findViewById<TextView>(R.id.overlay_purpose)
        
        val elapsedSec = (android.os.SystemClock.elapsedRealtime() - startTime + totalPausedTime) / 1000
        
        if (isCountUp) {
            textView.text = formatTime(elapsedSec)
        } else {
            val remaining = maxOf(0, targetDurationSec - elapsedSec)
            textView.text = formatTime(remaining)
        }
    }

    private fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    private fun createOverlay(purpose: String) {
        // Inflate overlay layout
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_window, null)
        
        val purposeView = overlayView.findViewById<TextView>(R.id.overlay_purpose)
        purposeView.text = purpose
        purposeView.maxLines = 2
        
        updateOverlayText()

        // Layout params for overlay
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 100
            y = 100
        }

        // Handle drag and click
        setupTouchListeners(layoutParams)

        try {
            windowManager.addView(overlayView, layoutParams)
            isOverlayVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupTouchListeners(layoutParams: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        var clickStartTime = 0L

        // Setup pause/resume button
        overlayView.findViewById<TextView>(R.id.btn_pause_resume).setOnClickListener {
            if (isPaused) {
                resumeSession()
            } else {
                pauseSession()
            }
        }

        // Setup end button
        overlayView.findViewById<TextView>(R.id.btn_end).setOnClickListener {
            endSession(SessionStatus.ABORTED)
        }

        overlayView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                event ?: return false
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        clickStartTime = System.currentTimeMillis()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        
                        // If moved significantly, it's a drag
                        if (kotlin.math.abs(dx) > 5 || kotlin.math.abs(dy) > 5) {
                            isDragging = true
                            layoutParams.x = initialX + dx
                            layoutParams.y = initialY + dy
                            windowManager.updateViewLayout(overlayView, layoutParams)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            val clickDuration = System.currentTimeMillis() - clickStartTime
                            // Short click = toggle expand/collapse
                            if (clickDuration < 300) {
                                toggleOverlayExpanded()
                            }
                        }
                    }
                }
                return true
            }
        })
    }

    private var isExpanded = false
    
    private fun toggleOverlayExpanded() {
        isExpanded = !isExpanded
        val controlsView = overlayView.findViewById<View>(R.id.overlay_controls)
        controlsView.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    private fun pauseSession() {
        if (isPaused) return
        isPaused = true
        pauseStartTime = android.os.SystemClock.elapsedRealtime()
        
        val btn = overlayView.findViewById<TextView>(R.id.btn_pause_resume)
        btn.text = getString(R.string.resume)
    }

    private fun resumeSession() {
        if (!isPaused) return
        totalPausedTime += android.os.SystemClock.elapsedRealtime() - pauseStartTime
        isPaused = false
        
        val btn = overlayView.findViewById<TextView>(R.id.btn_pause_resume)
        btn.text = getString(R.string.pause)
        
        startTimer()
    }

    private fun endSession(status: SessionStatus) {
        isActive = false
        timerJob?.cancel()
        
        val endTime = System.currentTimeMillis()
        val elapsedSec = (android.os.SystemClock.elapsedRealtime() - startTime + totalPausedTime) / 1000
        
        scope.launch {
            val activeSession = sessionRepository.getActiveSession()
            activeSession?.let {
                val updated = it.copy(
                    endTime = endTime,
                    durationSec = elapsedSec,
                    status = status
                )
                sessionRepository.update(updated)
            }
        }
        
        removeOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE_TASK)
        stopSelf()
    }

    private fun removeOverlay() {
        try {
            if (isOverlayVisible && ::overlayView.isInitialized) {
                windowManager.removeView(overlayView)
                isOverlayVisible = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

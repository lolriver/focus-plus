// service/TimerService.kt
package com.example.focusplus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.focusplus.FocusPulseApplication
import com.example.focusplus.MainActivity
import com.example.focusplus.R
import com.example.focusplus.domain.model.TimerMode
import com.example.focusplus.domain.model.TimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service for running timer in background
 * 
 * This service ensures the timer continues running even when the app
 * is in the background or the screen is off. It provides notifications
 * and maintains timer state persistence.
 */
@AndroidEntryPoint
class TimerService : Service() {
    
    // Service binder for local binding
    private val binder = TimerBinder()
    
    // Coroutine scope for service operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Timer job
    private var timerJob: Job? = null
    
    // Timer state
    private val _timerState = MutableStateFlow(TimerServiceState())
    val timerState: StateFlow<TimerServiceState> = _timerState.asStateFlow()
    
    // Notification manager
    private lateinit var notificationManager: NotificationManager
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService<NotificationManager>()!!
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> handleStartTimer(intent)
            ACTION_PAUSE_TIMER -> handlePauseTimer()
            ACTION_STOP_TIMER -> handleStopTimer()
            ACTION_COMPLETE_TIMER -> handleCompleteTimer()
        }
        
        return START_STICKY // Restart service if killed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }
    
    // ==================== TIMER CONTROL ====================
    
    /**
     * Start timer with specified parameters
     */
    fun startTimer(
        mode: TimerMode,
        durationSeconds: Long,
        cycleNumber: Int = 1
    ) {
        val currentState = _timerState.value
        
        if (currentState.timerState == TimerState.RUNNING) {
            return // Already running
        }
        
        _timerState.value = currentState.copy(
            timerState = TimerState.RUNNING,
            currentMode = mode,
            totalTimeSeconds = durationSeconds,
            timeRemainingSeconds = if (currentState.timerState == TimerState.PAUSED) {
                currentState.timeRemainingSeconds
            } else {
                durationSeconds
            },
            cycleNumber = cycleNumber,
            startTime = System.currentTimeMillis()
        )
        
        startForegroundService()
        startCountdown()
    }
    
    /**
     * Pause the timer
     */
    fun pauseTimer() {
        val currentState = _timerState.value
        if (currentState.timerState != TimerState.RUNNING) return
        
        timerJob?.cancel()
        
        _timerState.value = currentState.copy(
            timerState = TimerState.PAUSED,
            pauseTime = System.currentTimeMillis()
        )
        
        updateNotification()
    }
    
    /**
     * Resume the timer
     */
    fun resumeTimer() {
        val currentState = _timerState.value
        if (currentState.timerState != TimerState.PAUSED) return
        
        _timerState.value = currentState.copy(
            timerState = TimerState.RUNNING,
            pauseTime = null
        )
        
        startCountdown()
        updateNotification()
    }
    
    /**
     * Stop the timer
     */
    fun stopTimer() {
        timerJob?.cancel()
        
        _timerState.value = TimerServiceState()
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    /**
     * Complete the timer
     */
    fun completeTimer() {
        timerJob?.cancel()
        
        val currentState = _timerState.value
        _timerState.value = currentState.copy(
            timerState = TimerState.COMPLETED,
            timeRemainingSeconds = 0L
        )
        
        showCompletionNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    // ==================== COUNTDOWN LOGIC ====================
    
    /**
     * Start the countdown timer
     */
    private fun startCountdown() {
        timerJob = serviceScope.launch {
            while (_timerState.value.timeRemainingSeconds > 0 && 
                   _timerState.value.timerState == TimerState.RUNNING) {
                
                delay(1000L) // 1 second intervals
                
                val currentState = _timerState.value
                val newTimeRemaining = currentState.timeRemainingSeconds - 1
                
                _timerState.value = currentState.copy(
                    timeRemainingSeconds = newTimeRemaining
                )
                
                // Update notification every 30 seconds or when nearly complete
                if (newTimeRemaining % 30 == 0L || newTimeRemaining <= 10) {
                    updateNotification()
                }
                
                // Check if timer completed
                if (newTimeRemaining <= 0) {
                    completeTimer()
                    break
                }
            }
        }
    }
    
    // ==================== NOTIFICATION MANAGEMENT ====================
    
    /**
     * Create notification channel for timer service
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FocusPulseApplication.SERVICE_NOTIFICATION_CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing timer service notifications"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Start foreground service with notification
     */
    private fun startForegroundService() {
        val notification = createServiceNotification()
        startForeground(FocusPulseApplication.TIMER_SERVICE_NOTIFICATION_ID, notification)
    }
    
    /**
     * Create service notification
     */
    private fun createServiceNotification(): Notification {
        val currentState = _timerState.value
        
        // Create intent to open app
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create pause/resume action
        val actionIntent = if (currentState.timerState == TimerState.RUNNING) {
            Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE_TIMER }
        } else {
            Intent(this, TimerService::class.java).apply { action = ACTION_START_TIMER }
        }
        val actionPendingIntent = PendingIntent.getService(
            this, 1, actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create stop action
        val stopIntent = Intent(this, TimerService::class.java).apply { action = ACTION_STOP_TIMER }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Format time remaining
        val timeRemaining = formatTime(currentState.timeRemainingSeconds)
        val modeText = currentState.currentMode.displayName
        
        val title = when (currentState.timerState) {
            TimerState.RUNNING -> "$modeText - $timeRemaining"
            TimerState.PAUSED -> "$modeText - Paused"
            else -> "FocusPulse Timer"
        }
        
        val content = when (currentState.timerState) {
            TimerState.RUNNING -> "Timer is running"
            TimerState.PAUSED -> "Timer is paused - $timeRemaining remaining"
            else -> "Timer service"
        }
        
        val actionText = if (currentState.timerState == TimerState.RUNNING) "Pause" else "Resume"
        val actionIcon = if (currentState.timerState == TimerState.RUNNING) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }
        
        return NotificationCompat.Builder(this, FocusPulseApplication.SERVICE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(actionIcon, actionText, actionPendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    /**
     * Update the service notification
     */
    private fun updateNotification() {
        val notification = createServiceNotification()
        notificationManager.notify(FocusPulseApplication.TIMER_SERVICE_NOTIFICATION_ID, notification)
    }
    
    /**
     * Show completion notification
     */
    private fun showCompletionNotification() {
        val currentState = _timerState.value
        val modeText = currentState.currentMode.displayName
        
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, FocusPulseApplication.TIMER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("$modeText Complete!")
            .setContentText("Your ${modeText.lowercase()} session has finished.")
            .setSmallIcon(R.drawable.ic_timer_complete)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        notificationManager.notify(FocusPulseApplication.TIMER_COMPLETION_NOTIFICATION_ID, notification)
    }
    
    // ==================== INTENT HANDLERS ====================
    
    private fun handleStartTimer(intent: Intent) {
        val mode = intent.getSerializableExtra(EXTRA_TIMER_MODE) as? TimerMode ?: TimerMode.WORK
        val duration = intent.getLongExtra(EXTRA_DURATION_SECONDS, 25 * 60L)
        val cycle = intent.getIntExtra(EXTRA_CYCLE_NUMBER, 1)
        
        if (_timerState.value.timerState == TimerState.PAUSED) {
            resumeTimer()
        } else {
            startTimer(mode, duration, cycle)
        }
    }
    
    private fun handlePauseTimer() {
        pauseTimer()
    }
    
    private fun handleStopTimer() {
        stopTimer()
    }
    
    private fun handleCompleteTimer() {
        completeTimer()
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Format seconds as MM:SS
     */
    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    // ==================== BINDER CLASS ====================
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    
    companion object {
        // Actions
        const val ACTION_START_TIMER = "com.example.focusplus.START_TIMER"
        const val ACTION_PAUSE_TIMER = "com.example.focusplus.PAUSE_TIMER"
        const val ACTION_STOP_TIMER = "com.example.focusplus.STOP_TIMER"
        const val ACTION_COMPLETE_TIMER = "com.example.focusplus.COMPLETE_TIMER"
        
        // Extras
        const val EXTRA_TIMER_MODE = "timer_mode"
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        const val EXTRA_CYCLE_NUMBER = "cycle_number"
        
        /**
         * Create intent to start timer service
         */
        fun createStartIntent(
            context: Context,
            mode: TimerMode,
            durationSeconds: Long,
            cycleNumber: Int = 1
        ): Intent {
            return Intent(context, TimerService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra(EXTRA_TIMER_MODE, mode)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                putExtra(EXTRA_CYCLE_NUMBER, cycleNumber)
            }
        }
        
        /**
         * Create intent to pause timer service
         */
        fun createPauseIntent(context: Context): Intent {
            return Intent(context, TimerService::class.java).apply {
                action = ACTION_PAUSE_TIMER
            }
        }
        
        /**
         * Create intent to stop timer service
         */
        fun createStopIntent(context: Context): Intent {
            return Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP_TIMER
            }
        }
    }
}

/**
 * State of the timer service
 */
data class TimerServiceState(
    val timerState: TimerState = TimerState.IDLE,
    val currentMode: TimerMode = TimerMode.WORK,
    val timeRemainingSeconds: Long = 0L,
    val totalTimeSeconds: Long = 0L,
    val cycleNumber: Int = 1,
    val startTime: Long? = null,
    val pauseTime: Long? = null
) {
    /**
     * Get progress percentage (0.0 to 1.0)
     */
    val progress: Float
        get() = if (totalTimeSeconds > 0) {
            1f - (timeRemainingSeconds.toFloat() / totalTimeSeconds.toFloat())
        } else 0f
}

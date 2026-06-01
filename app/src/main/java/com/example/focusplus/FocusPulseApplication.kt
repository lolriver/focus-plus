// FocusPulseApplication.kt
package com.example.focusplus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import com.example.focusplus.data.local.DatabaseInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for FocusPulse
 * 
 * This class is responsible for:
 * - Hilt dependency injection setup
 * - Database initialization
 * - Notification channel creation
 * - Global app configuration
 */
@HiltAndroidApp
class FocusPulseApplication : Application() {
    
    @Inject
    lateinit var databaseInitializer: DatabaseInitializer
    
    // Application-scoped coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize app components
        initializeApp()
    }
    
    /**
     * Initialize application components
     */
    private fun initializeApp() {
        applicationScope.launch {
            try {
                // Initialize database
                initializeDatabase()
                
                // Create notification channels
                createNotificationChannels()
                
                // Initialize other components
                initializeOtherComponents()
                
            } catch (e: Exception) {
                // Handle initialization errors
                handleInitializationError(e)
            }
        }
    }
    
    /**
     * Initialize the database
     */
    private suspend fun initializeDatabase() {
        try {
            databaseInitializer.initializeDatabase()
        } catch (e: Exception) {
            // Log error and continue with app startup
            // In a real app, you might want to show an error dialog
            android.util.Log.e("FocusPulse", "Database initialization failed", e)
        }
    }
    
    /**
     * Create notification channels for Android O and above
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService<NotificationManager>()
            
            // Timer completion notification channel
            val timerChannel = NotificationChannel(
                TIMER_NOTIFICATION_CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for timer completion"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            // Background service notification channel
            val serviceChannel = NotificationChannel(
                SERVICE_NOTIFICATION_CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing timer service notifications"
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
            }
            
            // Reminder notification channel
            val reminderChannel = NotificationChannel(
                REMINDER_NOTIFICATION_CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminder notifications to start sessions"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            // Register channels
            notificationManager?.createNotificationChannels(
                listOf(timerChannel, serviceChannel, reminderChannel)
            )
        }
    }
    
    /**
     * Initialize other app components
     */
    private fun initializeOtherComponents() {
        // Initialize analytics (if using)
        // initializeAnalytics()
        
        // Initialize crash reporting (if using)
        // initializeCrashReporting()
        
        // Set up app-wide configurations
        setupAppConfigurations()
    }
    
    /**
     * Setup app-wide configurations
     */
    private fun setupAppConfigurations() {
        // Configure strict mode for development
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }
    
    /**
     * Enable strict mode for development builds
     */
    private fun enableStrictMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
    }
    
    /**
     * Handle application initialization errors
     */
    private fun handleInitializationError(error: Exception) {
        // Log the error
        android.util.Log.e("FocusPulse", "App initialization failed", error)
        
        // In a production app, you might want to:
        // 1. Send crash report to analytics
        // 2. Show user-friendly error message
        // 3. Attempt graceful degradation
        // 4. Provide recovery options
    }
    
    companion object {
        // Notification channel IDs
        const val TIMER_NOTIFICATION_CHANNEL_ID = "timer_notifications"
        const val SERVICE_NOTIFICATION_CHANNEL_ID = "timer_service"
        const val REMINDER_NOTIFICATION_CHANNEL_ID = "reminder_notifications"
        
        // Notification IDs
        const val TIMER_COMPLETION_NOTIFICATION_ID = 1001
        const val TIMER_SERVICE_NOTIFICATION_ID = 1002
        const val REMINDER_NOTIFICATION_ID = 1003
    }
}

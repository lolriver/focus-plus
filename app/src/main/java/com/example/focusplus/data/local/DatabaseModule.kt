// data/local/DatabaseModule.kt
package com.example.focusplus.data.local

import android.content.Context
import androidx.room.Room
import com.example.focusplus.data.local.dao.SessionRecordDao
import com.example.focusplus.data.local.dao.SettingsDao
import com.example.focusplus.data.local.dao.StatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies
 * 
 * This module is responsible for creating and providing:
 * - AppDatabase instance
 * - All DAO instances
 * - Database configuration
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides the main AppDatabase instance
     * 
     * @param context Application context
     * @return Configured AppDatabase instance
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addTypeConverters(Converters())
            .addCallback(DatabaseCallback())
            .addMigrations(*getAllMigrations())
            // Remove fallbackToDestructiveMigration() in production
            .fallbackToDestructiveMigration()
            .build()
    }
    
    /**
     * Provides SessionRecordDao instance
     * 
     * @param database AppDatabase instance
     * @return SessionRecordDao for session operations
     */
    @Provides
    fun provideSessionRecordDao(database: AppDatabase): SessionRecordDao {
        return database.sessionRecordDao()
    }
    
    /**
     * Provides SettingsDao instance
     * 
     * @param database AppDatabase instance
     * @return SettingsDao for settings operations
     */
    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }
    
    /**
     * Provides StatsDao instance
     * 
     * @param database AppDatabase instance
     * @return StatsDao for analytics operations
     */
    @Provides
    fun provideStatsDao(database: AppDatabase): StatsDao {
        return database.statsDao()
    }
    
    /**
     * Get all database migrations
     * Centralized location for all migration definitions
     */
    private fun getAllMigrations(): Array<androidx.room.migration.Migration> {
        return arrayOf(
            // Add migrations here as they are created
            // MIGRATION_1_2,
            // MIGRATION_2_3,
        )
    }
}

/**
 * Database callback for handling database lifecycle events
 */
private class DatabaseCallback : androidx.room.RoomDatabase.Callback() {
    
    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        super.onCreate(db)
        
        // Insert default settings
        insertDefaultSettings(db)
        
        // Create performance indexes
        createPerformanceIndexes(db)
        
        // Insert sample data for development (remove in production)
        if (BuildConfig.DEBUG) {
            insertSampleData(db)
        }
    }
    
    override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        super.onOpen(db)
        
        // Enable foreign key constraints
        db.execSQL("PRAGMA foreign_keys=ON")
        
        // Optimize database performance
        db.execSQL("PRAGMA journal_mode=WAL")
        db.execSQL("PRAGMA synchronous=NORMAL")
        db.execSQL("PRAGMA temp_store=MEMORY")
        db.execSQL("PRAGMA mmap_size=268435456") // 256MB
    }
    
    /**
     * Insert default settings when database is created
     */
    private fun insertDefaultSettings(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("""
            INSERT INTO settings (
                id,
                work_duration_minutes,
                short_break_duration_minutes,
                long_break_duration_minutes,
                sessions_until_long_break,
                is_dark_theme,
                is_notifications_enabled,
                is_sound_enabled,
                is_vibration_enabled,
                auto_start_breaks,
                auto_start_work,
                updated_at
            ) VALUES (
                1,
                25,
                5,
                15,
                4,
                0,
                1,
                1,
                1,
                0,
                0,
                datetime('now')
            )
        """)
    }
    
    /**
     * Create indexes for optimal query performance
     */
    private fun createPerformanceIndexes(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Primary indexes for common queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_session_completed_at ON session_records(completed_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_session_mode ON session_records(mode)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_session_completed ON session_records(was_completed)")
        
        // Composite indexes for complex queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_session_mode_completed_date ON session_records(mode, was_completed, completed_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_session_cycle_date ON session_records(cycle_number, completed_at)")
        
        // Partial indexes for better performance on filtered queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_session_completed_work ON session_records(completed_at) WHERE was_completed = 1 AND mode = 'WORK'")
    }
    
    /**
     * Insert sample data for development and testing
     * This helps with UI development and testing
     */
    private fun insertSampleData(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Insert sample completed work sessions
        val sampleSessions = listOf(
            "('WORK', 25, 25, datetime('now', '-1 day'), 1, 1, 'Focused work session')",
            "('SHORT_BREAK', 5, 5, datetime('now', '-1 day', '+25 minutes'), 1, 1, null)",
            "('WORK', 25, 23, datetime('now', '-1 day', '+30 minutes'), 1, 2, 'Good session')",
            "('WORK', 25, 25, datetime('now', '-2 hours'), 1, 1, 'Morning session')",
            "('SHORT_BREAK', 5, 5, datetime('now', '-1 hour 35 minutes'), 1, 1, null)",
            "('WORK', 25, 20, datetime('now', '-1 hour 30 minutes'), 0, 2, 'Interrupted')"
        )
        
        sampleSessions.forEach { session ->
            db.execSQL("""
                INSERT INTO session_records (
                    mode, planned_duration_minutes, actual_duration_minutes, 
                    completed_at, was_completed, cycle_number, notes, created_at
                ) VALUES $session, datetime('now')
            """)
        }
    }
}

/**
 * Build configuration helper
 */
private object BuildConfig {
    const val DEBUG = true // This should be replaced with actual BuildConfig.DEBUG
}

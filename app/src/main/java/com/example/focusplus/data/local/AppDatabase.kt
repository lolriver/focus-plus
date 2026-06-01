// data/local/AppDatabase.kt
package com.example.focusplus.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.focusplus.data.local.dao.SessionRecordDao
import com.example.focusplus.data.local.dao.SettingsDao
import com.example.focusplus.data.local.dao.StatsDao
import com.example.focusplus.data.local.entity.SessionRecordEntity
import com.example.focusplus.data.local.entity.SettingsEntity

/**
 * Room database for FocusPulse app
 * 
 * This is the main database class that serves as the main access point
 * for the underlying connection to your app's persisted, relational data.
 */
@Database(
    entities = [
        SessionRecordEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = true // Enable schema export for migrations
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // ==================== DAO ABSTRACT METHODS ====================
    
    /**
     * Get SessionRecord DAO for session-related database operations
     */
    abstract fun sessionRecordDao(): SessionRecordDao
    
    /**
     * Get Settings DAO for settings-related database operations
     */
    abstract fun settingsDao(): SettingsDao
    
    /**
     * Get Stats DAO for advanced analytics operations
     */
    abstract fun statsDao(): StatsDao
    
    companion object {
        /**
         * Database name constant
         */
        const val DATABASE_NAME = "focuspulse_database"
        
        /**
         * Create database instance with proper configuration
         * This method is used by Hilt for dependency injection
         */
        fun create(
            context: android.content.Context,
            useInMemory: Boolean = false
        ): AppDatabase {
            val databaseBuilder = if (useInMemory) {
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            } else {
                Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            }
            
            return databaseBuilder
                .addTypeConverters(Converters())
                .addCallback(DatabaseCallback()) // Add callback for initial data
                .addMigrations(*getAllMigrations()) // Add all migrations
                .fallbackToDestructiveMigration() // Only for development - remove in production
                .build()
        }
        
        /**
         * Get all database migrations
         * Add new migrations here as the database schema evolves
         */
        private fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                // Future migrations will be added here
                // MIGRATION_1_2,
                // MIGRATION_2_3,
                // etc.
            )
        }
    }
}

/**
 * Database callback for handling database creation and opening events
 */
private class DatabaseCallback : RoomDatabase.Callback() {
    
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        // Insert default settings when database is first created
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
        
        // Create indexes for better query performance
        createIndexes(db)
    }
    
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        
        // Enable foreign key constraints
        db.execSQL("PRAGMA foreign_keys=ON")
        
        // Optimize database performance
        db.execSQL("PRAGMA journal_mode=WAL") // Write-Ahead Logging for better concurrency
        db.execSQL("PRAGMA synchronous=NORMAL") // Balance between safety and performance
    }
    
    /**
     * Create database indexes for optimal query performance
     */
    private fun createIndexes(db: SupportSQLiteDatabase) {
        // Index on completed_at for date-based queries (most common)
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_session_completed_at 
            ON session_records(completed_at)
        """)
        
        // Index on mode for filtering by session type
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_session_mode 
            ON session_records(mode)
        """)
        
        // Index on was_completed for filtering completed sessions
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_session_completed 
            ON session_records(was_completed)
        """)
        
        // Composite index for common query patterns
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_session_mode_completed_date 
            ON session_records(mode, was_completed, completed_at)
        """)
        
        // Index on cycle_number for cycle-based queries
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_session_cycle 
            ON session_records(cycle_number)
        """)
    }
}

/**
 * Example migration from version 1 to 2
 * This is a template for future schema changes
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Example: Add a new column to session_records table
        // database.execSQL("ALTER TABLE session_records ADD COLUMN new_column TEXT")
        
        // Example: Create a new table
        // database.execSQL("""
        //     CREATE TABLE IF NOT EXISTS new_table (
        //         id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        //         name TEXT NOT NULL
        //     )
        // """)
        
        // Example: Create index for new column
        // database.execSQL("CREATE INDEX IF NOT EXISTS idx_new_column ON session_records(new_column)")
    }
}

/**
 * Migration utilities for common database operations
 */
object MigrationUtils {
    
    /**
     * Add a column to an existing table safely
     */
    fun addColumn(
        database: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
        columnType: String,
        defaultValue: String? = null
    ) {
        val defaultClause = defaultValue?.let { " DEFAULT $it" } ?: ""
        database.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnType$defaultClause")
    }
    
    /**
     * Create an index safely (IF NOT EXISTS)
     */
    fun createIndex(
        database: SupportSQLiteDatabase,
        indexName: String,
        tableName: String,
        columns: String
    ) {
        database.execSQL("CREATE INDEX IF NOT EXISTS $indexName ON $tableName($columns)")
    }
    
    /**
     * Drop an index safely (IF EXISTS)
     */
    fun dropIndex(database: SupportSQLiteDatabase, indexName: String) {
        database.execSQL("DROP INDEX IF EXISTS $indexName")
    }
    
    /**
     * Rename a table safely
     */
    fun renameTable(database: SupportSQLiteDatabase, oldName: String, newName: String) {
        database.execSQL("ALTER TABLE $oldName RENAME TO $newName")
    }
}

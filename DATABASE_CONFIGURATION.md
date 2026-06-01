# FocusPulse - Database Configuration

## Overview
This document describes the Room database configuration for the FocusPulse app, including setup, migrations, performance optimizations, and maintenance procedures.

## Database Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │    Hilt     │  │ Application │  │   Initialization    │ │
│  │   Module    │  │    Class    │  │     Helper          │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    ROOM DATABASE                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ AppDatabase │  │    DAOs     │  │   Type Converters   │ │
│  │             │  │             │  │                     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   SQLite DATABASE                           │
│  ┌─────────────┐  ┌─────────────┐                          │
│  │session_     │  │  settings   │                          │
│  │records      │  │             │                          │
│  └─────────────┘  └─────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

## Database Configuration

### **AppDatabase.kt**
```kotlin
@Database(
    entities = [SessionRecordEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase()
```

**Key Features:**
- **Version 1**: Initial schema version
- **Export Schema**: Enables schema export for migration testing
- **Type Converters**: Handles LocalDateTime and enum conversions
- **Multiple DAOs**: SessionRecord, Settings, and Stats operations

### **Database Creation**
```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
    .addTypeConverters(Converters())
    .addCallback(DatabaseCallback())
    .addMigrations(*getAllMigrations())
    .fallbackToDestructiveMigration() // Development only
    .build()
```

## Hilt Integration

### **DatabaseModule.kt**
Provides dependency injection for:
- `AppDatabase` (Singleton)
- `SessionRecordDao`
- `SettingsDao` 
- `StatsDao`

### **Injection Pattern**
```kotlin
@Inject
lateinit var sessionDao: SessionRecordDao

@Inject  
lateinit var settingsDao: SettingsDao
```

## Database Initialization

### **DatabaseInitializer.kt**
Handles:
- **First-time setup**: Creates default settings
- **Data validation**: Ensures database integrity
- **Migration support**: Data transformations between versions
- **Error recovery**: Graceful handling of initialization failures

### **Initialization Flow**
1. Check if settings exist
2. Create default settings if needed
3. Perform data migrations
4. Validate database integrity
5. Handle errors with recovery

### **Usage in Application**
```kotlin
@HiltAndroidApp
class FocusPulseApplication : Application() {
    @Inject lateinit var databaseInitializer: DatabaseInitializer
    
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            databaseInitializer.initializeDatabase()
        }
    }
}
```

## Performance Optimizations

### **Database Indexes**
```sql
-- Primary indexes for common queries
CREATE INDEX idx_session_completed_at ON session_records(completed_at);
CREATE INDEX idx_session_mode ON session_records(mode);
CREATE INDEX idx_session_completed ON session_records(was_completed);

-- Composite indexes for complex queries
CREATE INDEX idx_session_mode_completed_date ON session_records(mode, was_completed, completed_at);

-- Partial indexes for filtered queries
CREATE INDEX idx_session_completed_work ON session_records(completed_at) 
WHERE was_completed = 1 AND mode = 'WORK';
```

### **SQLite Optimizations**
```sql
PRAGMA foreign_keys=ON;           -- Enable foreign key constraints
PRAGMA journal_mode=WAL;          -- Write-Ahead Logging for concurrency
PRAGMA synchronous=NORMAL;        -- Balance safety and performance
PRAGMA temp_store=MEMORY;         -- Store temp tables in memory
PRAGMA mmap_size=268435456;       -- 256MB memory mapping
```

### **Query Optimization Guidelines**
1. **Use indexes**: Design queries to leverage existing indexes
2. **Limit results**: Always use `LIMIT` for potentially large result sets
3. **Proper ordering**: Include `ORDER BY` for consistent results
4. **Null handling**: Use `COALESCE()` for safe aggregations
5. **Flow usage**: Use `Flow<T>` for reactive queries to avoid loading all data

## Migration Strategy

### **Schema Migrations**
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new column
        database.execSQL("ALTER TABLE session_records ADD COLUMN new_column TEXT")
        
        // Create new index
        database.execSQL("CREATE INDEX idx_new_column ON session_records(new_column)")
    }
}
```

### **Migration Utilities**
```kotlin
// Add column safely
MigrationUtils.addColumn(db, "session_records", "new_column", "TEXT", "'default'")

// Create index safely  
MigrationUtils.createIndex(db, "idx_new", "session_records", "new_column")
```

### **Testing Migrations**
1. **Export schema**: Enable `exportSchema = true`
2. **Test migrations**: Use Room's migration testing utilities
3. **Validate data**: Ensure data integrity after migration
4. **Rollback plan**: Have a rollback strategy for failed migrations

## Default Data Setup

### **Settings Initialization**
```sql
INSERT INTO settings (
    id, work_duration_minutes, short_break_duration_minutes,
    long_break_duration_minutes, sessions_until_long_break,
    is_dark_theme, is_notifications_enabled, is_sound_enabled,
    is_vibration_enabled, auto_start_breaks, auto_start_work,
    updated_at
) VALUES (
    1, 25, 5, 15, 4, 0, 1, 1, 1, 0, 0, datetime('now')
);
```

### **Sample Data (Development)**
- Sample work sessions for UI testing
- Various session types and completion states
- Realistic timestamps for trend testing

## Database Health Monitoring

### **DatabaseHealthMonitor.kt**
Provides:
- **Health checks**: Regular database integrity validation
- **Status reporting**: Current database state information
- **Error detection**: Identify and report database issues
- **Recovery suggestions**: Automated recovery procedures

### **Health Check Results**
- `Healthy`: Database is functioning normally
- `NeedsInitialization`: First-time setup required
- `InvalidSettings`: Settings data is corrupted
- `Error`: Specific error encountered

## Error Handling

### **Common Error Scenarios**
1. **Database corruption**: Handle with recovery procedures
2. **Migration failures**: Rollback to previous version
3. **Initialization errors**: Reset to defaults
4. **Constraint violations**: Validate data before insertion

### **Recovery Strategies**
```kotlin
// Reset to defaults
databaseInitializer.resetToDefaults()

// Validate and fix settings
if (!settings.isValid()) {
    settingsDao.resetToDefaults()
}

// Handle migration errors
.fallbackToDestructiveMigration() // Development only
```

## Testing Configuration

### **Test Database Setup**
```kotlin
// In-memory database for tests
Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
    .allowMainThreadQueries() // Test only
    .build()
```

### **Testing Best Practices**
1. **Use in-memory database**: Faster and isolated tests
2. **Test migrations**: Verify schema changes work correctly
3. **Test constraints**: Ensure data integrity rules are enforced
4. **Test concurrent access**: Verify thread safety
5. **Test error scenarios**: Handle database errors gracefully

## Production Considerations

### **Release Configuration**
```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
    .addMigrations(*getAllMigrations())
    // Remove fallbackToDestructiveMigration() in production
    .build()
```

### **Monitoring and Maintenance**
1. **Performance monitoring**: Track query execution times
2. **Storage usage**: Monitor database size growth
3. **Error tracking**: Log and report database errors
4. **Backup strategy**: Consider user data backup options
5. **Analytics**: Track database usage patterns

### **Security Considerations**
1. **Data encryption**: Consider SQLCipher for sensitive data
2. **Access control**: Limit database access to authorized components
3. **Data validation**: Validate all input data
4. **Audit logging**: Track sensitive data access

## Troubleshooting

### **Common Issues**
1. **Migration failures**: Check migration scripts and test thoroughly
2. **Performance issues**: Review indexes and query patterns
3. **Data corruption**: Implement regular integrity checks
4. **Memory usage**: Monitor database size and query efficiency

### **Debug Tools**
1. **Database Inspector**: Android Studio's database inspector
2. **Query logging**: Enable SQL query logging for debugging
3. **Performance profiling**: Use Android profiler for database operations
4. **Schema export**: Review exported schema files

### **Recovery Procedures**
1. **Backup restoration**: Restore from known good backup
2. **Data migration**: Move data to new database instance
3. **Selective reset**: Reset only corrupted portions
4. **User notification**: Inform users of data recovery procedures

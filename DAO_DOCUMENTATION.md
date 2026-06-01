# FocusPulse - DAO Documentation

## Overview
This document describes the Data Access Object (DAO) interfaces for the FocusPulse app, providing comprehensive database operations with reactive programming support.

## DAO Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    DAO LAYER                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │SessionRecord│  │  Settings   │  │      Stats          │ │
│  │    DAO      │  │    DAO      │  │      DAO            │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ (Room Database)
┌─────────────────────────────────────────────────────────────┐
│                   SQLite DATABASE                           │
│  ┌─────────────┐  ┌─────────────┐                          │
│  │session_     │  │  settings   │                          │
│  │records      │  │             │                          │
│  └─────────────┘  └─────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

## SessionRecordDao

### **Purpose**
Handles all database operations for Pomodoro session records including CRUD operations, statistics queries, and analytics.

### **Key Features**
- **Reactive Queries**: All queries return `Flow<T>` for real-time UI updates
- **Comprehensive Statistics**: Built-in queries for analytics and reporting
- **Optimized Performance**: Efficient queries with proper indexing considerations
- **Date Range Filtering**: Support for today, this week, and custom date ranges

### **Operation Categories**

#### **CRUD Operations**
```kotlin
suspend fun insertSession(session: SessionRecordEntity): Long
suspend fun updateSession(session: SessionRecordEntity)
suspend fun deleteSession(session: SessionRecordEntity)
fun getAllSessions(): Flow<List<SessionRecordEntity>>
```

#### **Statistics Queries**
```kotlin
fun getTotalSessionCount(): Flow<Int>
fun getSessionsCompletedToday(todayStart: String, todayEnd: String): Flow<Int>
fun getWorkMinutesToday(todayStart: String, todayEnd: String): Flow<Int>
fun getCompletionRate(): Flow<Double>
```

#### **Advanced Filtering**
```kotlin
fun getSessionsByMode(mode: String): Flow<List<SessionRecordEntity>>
fun getSessionsInDateRange(startDate: String, endDate: String): Flow<List<SessionRecordEntity>>
fun getCompletedSessions(): Flow<List<SessionRecordEntity>>
```

### **Query Performance Considerations**
- Date queries use ISO string format for consistency
- `COALESCE()` functions handle null values in aggregations
- `LIMIT` clauses prevent excessive data loading
- Proper `ORDER BY` clauses for consistent results

## SettingsDao

### **Purpose**
Manages user settings with a single-row approach (ID always = 1) for simplicity and performance.

### **Key Features**
- **Single Row Design**: Always uses ID = 1 for settings storage
- **Granular Updates**: Individual setting updates without full object replacement
- **Bulk Operations**: Update multiple related settings atomically
- **Reactive Settings**: Flow-based queries for real-time settings updates

### **Operation Categories**

#### **Basic Operations**
```kotlin
suspend fun upsertSettings(settings: SettingsEntity) // Insert or update
fun getSettings(): Flow<SettingsEntity?> // Reactive settings
suspend fun getSettingsSync(): SettingsEntity? // Synchronous for initialization
```

#### **Individual Setting Updates**
```kotlin
suspend fun updateWorkDuration(workDurationMinutes: Int)
suspend fun updateTheme(isDarkTheme: Boolean)
suspend fun updateNotifications(isNotificationsEnabled: Boolean)
```

#### **Bulk Updates**
```kotlin
suspend fun updateAllDurations(work: Int, shortBreak: Int, longBreak: Int, sessions: Int)
suspend fun updateNotificationSettings(notifications: Boolean, sound: Boolean, vibration: Boolean)
```

#### **Specific Getters**
```kotlin
fun getWorkDuration(): Flow<Int?>
fun getThemePreference(): Flow<Boolean?>
fun getNotificationPreference(): Flow<Boolean?>
```

### **Design Benefits**
- **Atomic Updates**: Individual settings can be updated without affecting others
- **Audit Trail**: `updated_at` field tracks when settings were last modified
- **Default Handling**: `resetToDefaults()` function for easy reset
- **Type Safety**: Separate functions prevent incorrect value assignments

## StatsDao (Advanced Analytics)

### **Purpose**
Provides complex analytical queries separated from basic CRUD operations for better organization and performance.

### **Key Features**
- **Trend Analysis**: Daily, weekly, and monthly trends
- **Productivity Insights**: Hourly patterns and performance metrics
- **Streak Calculations**: Data for calculating consecutive day streaks
- **Goal Tracking**: Progress monitoring against user-defined goals

### **Analytics Categories**

#### **Productivity Analytics**
```kotlin
fun getDailySessionCounts(thirtyDaysAgo: String): Flow<List<DailySessionCount>>
fun getHourlyProductivityPattern(): Flow<List<HourlyProductivity>>
fun getWeeklyTrends(twelveWeeksAgo: String): Flow<List<WeeklyStats>>
```

#### **Performance Insights**
```kotlin
fun getSessionModeDistribution(): Flow<List<ModeDistribution>>
fun getCompletionRateByDayOfWeek(): Flow<List<DayOfWeekStats>>
fun getBestPerformingDays(limit: Int): Flow<List<DailyPerformance>>
```

#### **Goal Tracking**
```kotlin
fun getDailyGoalProgress(todayStart: String, todayEnd: String, dailyGoal: Int): Flow<GoalProgress?>
fun getMonthlySummary(monthStart: String, monthEnd: String): Flow<MonthlySummary?>
```

### **Custom Result Classes**
Each complex query returns a custom data class for type safety:
- `DailySessionCount` - Daily session statistics
- `HourlyProductivity` - Hourly productivity patterns
- `WeeklyStats` - Weekly trend data
- `ModeDistribution` - Session type distribution
- `GoalProgress` - Goal achievement tracking

## Query Optimization Strategies

### **Indexing Recommendations**
```sql
-- For date-based queries (most common)
CREATE INDEX idx_session_completed_at ON session_records(completed_at);

-- For mode filtering
CREATE INDEX idx_session_mode ON session_records(mode);

-- For completion status filtering
CREATE INDEX idx_session_completed ON session_records(was_completed);

-- Composite index for common queries
CREATE INDEX idx_session_mode_completed_date ON session_records(mode, was_completed, completed_at);
```

### **Query Performance Tips**
1. **Date Formatting**: Use consistent ISO string format for date comparisons
2. **Aggregation Functions**: Use `COALESCE()` to handle null values in `SUM()` and `AVG()`
3. **Limit Results**: Always use `LIMIT` for queries that might return large datasets
4. **Proper Ordering**: Include `ORDER BY` clauses for consistent, predictable results

### **Memory Considerations**
- Use `Flow<T>` for reactive queries to avoid loading all data at once
- Implement pagination for large result sets (e.g., session history)
- Consider using `Paging 3` library for infinite scrolling scenarios

## Usage Examples

### **Basic Session Operations**
```kotlin
// Insert a new session
val sessionId = sessionDao.insertSession(sessionEntity)

// Get today's sessions reactively
sessionDao.getSessionsCompletedToday(todayStart, todayEnd)
    .collect { count -> 
        // Update UI with session count
    }
```

### **Settings Management**
```kotlin
// Update work duration
settingsDao.updateWorkDuration(30)

// Get settings reactively
settingsDao.getSettings()
    .collect { settings ->
        // Update UI with current settings
    }
```

### **Statistics and Analytics**
```kotlin
// Get productivity trends
statsDao.getWeeklyTrends(twelveWeeksAgo)
    .collect { trends ->
        // Display trend chart
    }

// Check daily goal progress
statsDao.getDailyGoalProgress(todayStart, todayEnd, dailyGoal = 8)
    .collect { progress ->
        // Update progress indicator
    }
```

## Error Handling

### **Common Scenarios**
1. **Settings Not Found**: `getSettings()` returns null for first-time users
2. **Empty Results**: Statistics queries may return empty lists for new users
3. **Date Parsing**: Ensure consistent ISO date format across all queries

### **Recommended Practices**
```kotlin
// Handle null settings
settingsDao.getSettings().collect { settings ->
    val workDuration = settings?.workDurationMinutes ?: 25 // Default fallback
}

// Handle empty statistics
statsDao.getDailySessionCounts(thirtyDaysAgo).collect { counts ->
    if (counts.isEmpty()) {
        // Show "No data available" message
    } else {
        // Display chart
    }
}
```

## Testing Considerations

### **Unit Testing DAOs**
- Use in-memory Room database for testing
- Test both success and edge cases
- Verify Flow emissions for reactive queries
- Test transaction behavior for bulk operations

### **Integration Testing**
- Test DAO operations with actual SQLite database
- Verify query performance with realistic data volumes
- Test concurrent access scenarios
- Validate data integrity constraints

## Migration Strategy

### **Schema Changes**
When adding new columns or tables:
1. Create Room migration classes
2. Update DAO interfaces with new queries
3. Maintain backward compatibility
4. Test migration with existing data

### **Query Evolution**
- Add new queries without breaking existing ones
- Use versioned query methods if needed
- Deprecate old queries gradually
- Document breaking changes clearly

# FocusPulse - Data Models Documentation

## Overview
This document describes the data models used in the FocusPulse app, following Clean Architecture principles with separation between domain models and data layer entities.

## Architecture Pattern

```
┌─────────────────────────────────────────────────────────────┐
│                    DOMAIN MODELS                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ SessionRecord│  │  Settings   │  │   TimerMode/State   │ │
│  │             │  │             │  │                     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ (Mapping)
┌─────────────────────────────────────────────────────────────┐
│                    ROOM ENTITIES                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │SessionRecord│  │ Settings    │  │   Type Converters   │ │
│  │   Entity    │  │  Entity     │  │                     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Domain Models (Pure Kotlin - No Android Dependencies)

### **TimerMode.kt**
```kotlin
enum class TimerMode(val displayName: String, val defaultDurationMinutes: Int)
```
- **Purpose**: Represents different timer modes (Work, Short Break, Long Break)
- **Features**: 
  - Default durations for each mode
  - Helper properties (`isBreak`, `isWork`)
  - Display names for UI

### **TimerState.kt**
```kotlin
enum class TimerState
```
- **Purpose**: Represents current timer state (Idle, Running, Paused, Completed)
- **Features**:
  - State validation helpers (`canStart`, `canPause`, `canReset`)
  - Active state checking

### **SessionRecord.kt**
```kotlin
data class SessionRecord(
    val id: Long,
    val mode: TimerMode,
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int,
    val completedAt: LocalDateTime,
    val wasCompleted: Boolean,
    val cycleNumber: Int,
    val notes: String?
)
```
- **Purpose**: Represents a completed Pomodoro session
- **Features**:
  - Success calculation (80% completion threshold)
  - Completion percentage calculation
  - Date-based filtering (`isToday()`, `isThisWeek()`)
  - Formatted duration display

### **Settings.kt**
```kotlin
data class Settings(
    val workDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val sessionsUntilLongBreak: Int = 4,
    // ... notification and theme settings
)
```
- **Purpose**: User preferences and configuration
- **Features**:
  - Duration settings for each mode
  - Notification preferences
  - Theme settings
  - Auto-start options
  - Validation with error messages
  - Predefined setting templates (Standard, Deep Focus, Short Bursts)

### **SessionStats.kt**
```kotlin
data class SessionStats(
    val sessionsToday: Int,
    val sessionsThisWeek: Int,
    val totalSessions: Int,
    val workMinutesToday: Int,
    // ... more statistics
)
```
- **Purpose**: Aggregated session statistics
- **Features**:
  - Time-based statistics (today, week, total)
  - Streak tracking
  - Completion rate calculation
  - Productivity level assessment
  - Formatted time display

## Room Entities (Data Layer - Android Dependencies)

### **SessionRecordEntity.kt**
```kotlin
@Entity(tableName = "session_records")
data class SessionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "mode") val mode: String,
    @ColumnInfo(name = "planned_duration_minutes") val plannedDurationMinutes: Int,
    // ... other fields
)
```
- **Purpose**: Database representation of session records
- **Features**:
  - Auto-generated primary key
  - String storage for enums (avoids serialization issues)
  - ISO string storage for dates
  - Audit fields (`createdAt`)
  - Bidirectional mapping with domain model

### **SettingsEntity.kt**
```kotlin
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1, // Fixed ID for single row
    @ColumnInfo(name = "work_duration_minutes") val workDurationMinutes: Int,
    // ... other settings
)
```
- **Purpose**: Database representation of user settings
- **Features**:
  - Single-row approach (ID always = 1)
  - Column name mapping for readability
  - Audit field (`updatedAt`)
  - Default factory methods

## Type Converters

### **Converters.kt**
```kotlin
class Converters {
    @TypeConverter fun fromLocalDateTime(dateTime: LocalDateTime?): String?
    @TypeConverter fun toLocalDateTime(dateTimeString: String?): LocalDateTime?
    @TypeConverter fun fromTimerMode(mode: TimerMode?): String?
    @TypeConverter fun toTimerMode(modeName: String?): TimerMode?
}
```
- **Purpose**: Handle complex types in Room database
- **Supported Types**:
  - `LocalDateTime` ↔ ISO String
  - `TimerMode` enum ↔ String

## Database Schema

### **session_records Table**
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PRIMARY KEY, AUTOINCREMENT | Unique session ID |
| mode | TEXT | NOT NULL | Timer mode (WORK/SHORT_BREAK/LONG_BREAK) |
| planned_duration_minutes | INTEGER | NOT NULL | Intended session length |
| actual_duration_minutes | INTEGER | NOT NULL | Actual session length |
| completed_at | TEXT | NOT NULL | ISO datetime of completion |
| was_completed | INTEGER | NOT NULL | Boolean: session finished successfully |
| cycle_number | INTEGER | DEFAULT 1 | Pomodoro cycle number |
| notes | TEXT | NULL | Optional user notes |
| created_at | TEXT | NOT NULL | Audit: record creation time |

### **settings Table**
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PRIMARY KEY | Always 1 (single row) |
| work_duration_minutes | INTEGER | NOT NULL | Work session duration |
| short_break_duration_minutes | INTEGER | NOT NULL | Short break duration |
| long_break_duration_minutes | INTEGER | NOT NULL | Long break duration |
| sessions_until_long_break | INTEGER | NOT NULL | Cycles before long break |
| is_dark_theme | INTEGER | NOT NULL | Boolean: dark theme enabled |
| is_notifications_enabled | INTEGER | NOT NULL | Boolean: notifications on |
| is_sound_enabled | INTEGER | NOT NULL | Boolean: sound alerts on |
| is_vibration_enabled | INTEGER | NOT NULL | Boolean: vibration on |
| auto_start_breaks | INTEGER | NOT NULL | Boolean: auto-start breaks |
| auto_start_work | INTEGER | NOT NULL | Boolean: auto-start work |
| updated_at | TEXT | NOT NULL | Audit: last update time |

## Data Validation

### **Settings Validation Rules**
- Work duration: 1-120 minutes
- Short break: 1-60 minutes  
- Long break: 1-120 minutes
- Sessions until long break: 1-10 sessions

### **Session Validation**
- Planned duration > 0
- Actual duration ≥ 0
- Completion threshold: 80% of planned duration
- Valid completion timestamp

## Usage Examples

### **Creating a Session Record**
```kotlin
val session = SessionRecord(
    mode = TimerMode.WORK,
    plannedDurationMinutes = 25,
    actualDurationMinutes = 24,
    completedAt = LocalDateTime.now(),
    wasCompleted = true,
    cycleNumber = 1
)
```

### **Updating Settings**
```kotlin
val newSettings = currentSettings.copy(
    workDurationMinutes = 30,
    isDarkTheme = true
)
```

### **Checking Statistics**
```kotlin
val stats = SessionStats(sessionsToday = 5, workMinutesToday = 120)
println(stats.productivityLevel) // HIGH
println(stats.formattedWorkTimeToday) // "2h"
```

## Benefits of This Design

1. **Clean Architecture**: Domain models are pure Kotlin without Android dependencies
2. **Testability**: Easy to unit test business logic
3. **Type Safety**: Strong typing with validation
4. **Maintainability**: Clear separation of concerns
5. **Flexibility**: Easy to change database schema without affecting business logic
6. **Performance**: Efficient Room queries with proper indexing

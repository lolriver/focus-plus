# FocusPulse - Repository Layer Documentation

## Overview
This document describes the Repository layer implementation for the FocusPulse app, following Clean Architecture principles with clear separation between domain interfaces and data implementations.

## Repository Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                             │
│  ┌─────────────┐  ┌─────────────┐                          │
│  │ Session     │  │  Settings   │                          │
│  │ Repository  │  │ Repository  │                          │
│  │ Interface   │  │ Interface   │                          │
│  └─────────────┘  └─────────────┘                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ (Implementation)
┌─────────────────────────────────────────────────────────────┐
│                     DATA LAYER                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ Session     │  │  Settings   │  │    Hilt Module      │ │
│  │ Repository  │  │ Repository  │  │                     │ │
│  │    Impl     │  │    Impl     │  │                     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ (Data Access)
┌─────────────────────────────────────────────────────────────┐
│                      DAO LAYER                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │SessionRecord│  │  Settings   │  │      Stats          │ │
│  │    DAO      │  │    DAO      │  │      DAO            │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Clean Architecture Benefits

### **Separation of Concerns**
- **Domain Layer**: Pure business logic, no Android dependencies
- **Data Layer**: Android-specific implementations, database access
- **Interface Segregation**: Each repository has a focused responsibility

### **Dependency Inversion**
- Domain layer depends on abstractions (interfaces)
- Data layer provides concrete implementations
- Easy to swap implementations for testing or different data sources

### **Testability**
- Domain interfaces can be easily mocked
- Business logic can be tested without Android dependencies
- Repository implementations can be tested with in-memory databases

## SessionRepository

### **Domain Interface (SessionRepository.kt)**
```kotlin
interface SessionRepository {
    // CRUD Operations
    suspend fun insertSession(session: SessionRecord): Long
    suspend fun updateSession(session: SessionRecord)
    suspend fun deleteSession(session: SessionRecord)
    
    // Query Operations
    fun getAllSessions(): Flow<List<SessionRecord>>
    fun getSessionsByMode(mode: TimerMode): Flow<List<SessionRecord>>
    
    // Statistics
    fun getSessionStats(): Flow<SessionStats>
    fun getCurrentStreak(): Flow<Int>
    fun getLongestStreak(): Flow<Int>
    
    // Analytics
    fun getDailySessionCounts(days: Int): Flow<Map<LocalDateTime, Int>>
    fun getHourlyProductivityPattern(): Flow<Map<Int, Int>>
}
```

### **Data Implementation (SessionRepositoryImpl.kt)**
```kotlin
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionRecordDao,
    private val statsDao: StatsDao
) : SessionRepository {
    
    override suspend fun insertSession(session: SessionRecord): Long {
        val entity = SessionRecordEntity.fromDomainModel(session)
        return sessionDao.insertSession(entity)
    }
    
    override fun getSessionStats(): Flow<SessionStats> {
        return combine(
            getSessionsCompletedToday(),
            getSessionsCompletedThisWeek(),
            getTotalSessionCount(),
            // ... other statistics
        ) { /* combine statistics */ }
    }
}
```

### **Key Features**
- **Entity Mapping**: Automatic conversion between domain models and database entities
- **Reactive Queries**: All queries return `Flow<T>` for real-time UI updates
- **Complex Statistics**: Combines multiple DAO queries into comprehensive statistics
- **Streak Calculation**: Advanced algorithms for calculating current and longest streaks
- **Date Handling**: Proper LocalDateTime handling with timezone considerations

## SettingsRepository

### **Domain Interface (SettingsRepository.kt)**
```kotlin
interface SettingsRepository {
    // Basic Operations
    fun getSettings(): Flow<Settings>
    suspend fun saveSettings(settings: Settings)
    suspend fun resetToDefaults()
    
    // Specific Updates
    suspend fun updateWorkDuration(durationMinutes: Int)
    suspend fun updateTheme(isDarkTheme: Boolean)
    
    // Bulk Updates
    suspend fun updateAllDurations(work: Int, shortBreak: Int, longBreak: Int, sessions: Int)
    
    // Getters
    fun getDurationForMode(mode: TimerMode): Flow<Int>
    fun getThemePreference(): Flow<Boolean>
    
    // Validation
    fun validateSettings(settings: Settings): Boolean
    fun getValidationErrors(settings: Settings): List<String>
    
    // Presets
    suspend fun applyPomodoroStandard()
    suspend fun applyDeepFocus()
    suspend fun applyShortBursts()
}
```

### **Data Implementation (SettingsRepositoryImpl.kt)**
```kotlin
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {
    
    override fun getSettings(): Flow<Settings> {
        return settingsDao.getSettings().map { entity ->
            entity?.toDomainModel() ?: Settings.DEFAULT
        }
    }
    
    override suspend fun updateWorkDuration(durationMinutes: Int) {
        if (isValidDuration(durationMinutes, 1, 120)) {
            settingsDao.updateWorkDuration(durationMinutes)
        } else {
            throw IllegalArgumentException("Invalid duration")
        }
    }
}
```

### **Key Features**
- **Input Validation**: All updates include validation with descriptive error messages
- **Fallback Handling**: Graceful fallback to default settings when data is missing
- **Granular Updates**: Individual setting updates without full object replacement
- **Preset Management**: Built-in presets for common Pomodoro configurations
- **Type Safety**: Strong typing prevents invalid setting combinations

## Dependency Injection

### **RepositoryModule.kt**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        sessionRepositoryImpl: SessionRepositoryImpl
    ): SessionRepository
    
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
```

### **Usage in ViewModels**
```kotlin
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    val settings = settingsRepository.getSettings()
    val sessionStats = sessionRepository.getSessionStats()
    
    suspend fun saveSession(session: SessionRecord) {
        sessionRepository.insertSession(session)
    }
}
```

## Data Flow Examples

### **Session Creation Flow**
1. **UI Layer**: User completes a timer session
2. **ViewModel**: Creates `SessionRecord` domain model
3. **Repository**: Converts to `SessionRecordEntity`
4. **DAO**: Inserts into database
5. **Repository**: Returns generated ID
6. **ViewModel**: Updates UI state

### **Settings Update Flow**
1. **UI Layer**: User changes work duration
2. **ViewModel**: Calls `updateWorkDuration(30)`
3. **Repository**: Validates duration (1-120 minutes)
4. **DAO**: Updates database record
5. **Repository**: Emits new settings via Flow
6. **UI Layer**: Automatically updates with new duration

### **Statistics Calculation Flow**
1. **UI Layer**: Requests session statistics
2. **Repository**: Combines multiple DAO queries
3. **DAO**: Executes optimized SQL queries
4. **Repository**: Calculates streaks and aggregations
5. **Repository**: Returns `SessionStats` domain model
6. **UI Layer**: Displays formatted statistics

## Error Handling

### **Repository-Level Error Handling**
```kotlin
override suspend fun updateWorkDuration(durationMinutes: Int) {
    if (!isValidDuration(durationMinutes, 1, 120)) {
        throw IllegalArgumentException("Work duration must be between 1 and 120 minutes")
    }
    
    try {
        settingsDao.updateWorkDuration(durationMinutes)
    } catch (e: Exception) {
        throw SettingsUpdateException("Failed to update work duration", e)
    }
}
```

### **Common Error Scenarios**
1. **Validation Errors**: Invalid input values
2. **Database Errors**: Connection or constraint violations
3. **Mapping Errors**: Entity to domain model conversion failures
4. **Concurrency Issues**: Multiple simultaneous updates

### **Error Recovery Strategies**
- **Graceful Degradation**: Return default values when data is unavailable
- **Retry Logic**: Automatic retry for transient failures
- **User Feedback**: Meaningful error messages for validation failures
- **Logging**: Comprehensive error logging for debugging

## Testing Strategy

### **Repository Testing Utilities (RepositoryTestUtils.kt)**
```kotlin
object RepositoryTestUtils {
    fun createSampleWorkSession(): SessionRecord { /* ... */ }
    fun createSampleSettings(): Settings { /* ... */ }
    fun createStreakTestSessions(streakDays: Int): List<SessionRecord> { /* ... */ }
    fun generateRealisticSessionData(days: Int): List<SessionRecord> { /* ... */ }
}
```

### **Unit Testing Approach**
1. **Mock DAOs**: Use Mockito or MockK to mock DAO interfaces
2. **Test Business Logic**: Focus on repository logic, not database operations
3. **Validate Mappings**: Ensure correct entity ↔ domain model conversion
4. **Test Error Scenarios**: Verify proper error handling and validation

### **Integration Testing**
1. **In-Memory Database**: Use Room's in-memory database for integration tests
2. **End-to-End Flows**: Test complete data flows from repository to database
3. **Performance Testing**: Verify query performance with realistic data volumes
4. **Concurrency Testing**: Test thread safety and concurrent access

## Performance Considerations

### **Query Optimization**
- **Flow Usage**: Reactive queries prevent loading all data at once
- **Lazy Loading**: Load data only when needed
- **Efficient Aggregations**: Use SQL aggregations instead of in-memory calculations
- **Proper Indexing**: Ensure database indexes support repository queries

### **Memory Management**
- **Entity Mapping**: Minimize object creation during mapping
- **Flow Operators**: Use efficient Flow operators for data transformation
- **Caching Strategy**: Consider caching frequently accessed data
- **Lifecycle Awareness**: Ensure proper cleanup of Flow subscriptions

### **Scalability**
- **Pagination**: Implement pagination for large datasets
- **Background Processing**: Use coroutines for heavy computations
- **Database Optimization**: Regular database maintenance and optimization
- **Monitoring**: Track repository performance metrics

## Best Practices

### **Repository Design**
1. **Single Responsibility**: Each repository handles one domain entity
2. **Interface Segregation**: Keep interfaces focused and minimal
3. **Dependency Inversion**: Depend on abstractions, not concretions
4. **Immutable Models**: Use immutable domain models for thread safety

### **Error Handling**
1. **Fail Fast**: Validate inputs early and throw meaningful exceptions
2. **Graceful Degradation**: Provide fallbacks for non-critical failures
3. **Consistent Errors**: Use consistent error types and messages
4. **Logging**: Log errors with sufficient context for debugging

### **Testing**
1. **Test Interfaces**: Test against repository interfaces, not implementations
2. **Mock External Dependencies**: Mock DAOs and other external dependencies
3. **Test Edge Cases**: Include tests for error scenarios and edge cases
4. **Integration Tests**: Verify end-to-end functionality with real database

### **Documentation**
1. **Interface Documentation**: Clearly document repository interface contracts
2. **Error Documentation**: Document possible exceptions and error conditions
3. **Usage Examples**: Provide clear usage examples for complex operations
4. **Performance Notes**: Document performance characteristics and limitations

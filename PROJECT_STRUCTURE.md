# FocusPulse - Project Structure

## Architecture Overview
This project follows **Clean Architecture** principles with **MVVM** pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Compose   │  │ ViewModels  │  │    Navigation       │ │
│  │   Screens   │  │             │  │                     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     DOMAIN LAYER                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ Repository  │  │   Models    │  │    Use Cases        │ │
│  │ Interfaces  │  │             │  │   (Optional)        │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      DATA LAYER                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │    Room     │  │ Repository  │  │    DataStore        │ │
│  │  Database   │  │ Impl        │  │   Preferences       │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Package Structure

```
com.example.focusplus/
├── 📁 data/                          # Data Layer
│   ├── 📁 local/                     # Local data sources
│   │   ├── 📁 dao/                   # Room DAO interfaces
│   │   │   ├── SessionRecordDao.kt   # Session CRUD operations
│   │   │   └── SettingsDao.kt        # Settings CRUD operations
│   │   ├── 📁 entity/                # Room entities
│   │   │   ├── SessionRecordEntity.kt # Session data model
│   │   │   └── SettingsEntity.kt     # Settings data model
│   │   └── AppDatabase.kt            # Room database configuration
│   └── 📁 repository/                # Repository implementations
│       ├── SessionRepositoryImpl.kt  # Session data operations
│       └── SettingsRepositoryImpl.kt # Settings data operations
│
├── 📁 domain/                        # Domain Layer
│   ├── 📁 model/                     # Domain models
│   │   ├── SessionRecord.kt          # Session domain model
│   │   ├── Settings.kt               # Settings domain model
│   │   └── TimerMode.kt              # Timer mode enum
│   └── 📁 repository/                # Repository interfaces
│       ├── SessionRepository.kt      # Session operations contract
│       └── SettingsRepository.kt     # Settings operations contract
│
├── 📁 presentation/                  # Presentation Layer
│   ├── 📁 timer/                     # Timer feature
│   │   ├── TimerScreen.kt            # Timer UI screen
│   │   ├── TimerViewModel.kt         # Timer state management
│   │   └── components/               # Timer UI components
│   │       ├── TimerDisplay.kt       # Large timer display
│   │       ├── TimerControls.kt      # Start/Pause/Reset buttons
│   │       └── ModeIndicator.kt      # Work/Break indicator
│   ├── 📁 settings/                  # Settings feature
│   │   ├── SettingsScreen.kt         # Settings UI screen
│   │   ├── SettingsViewModel.kt      # Settings state management
│   │   └── components/               # Settings UI components
│   │       ├── DurationPicker.kt     # Duration selection
│   │       └── ThemeToggle.kt        # Theme switcher
│   ├── 📁 stats/                     # Statistics feature
│   │   ├── StatsScreen.kt            # Stats UI screen
│   │   ├── StatsViewModel.kt         # Stats state management
│   │   └── components/               # Stats UI components
│   │       ├── StatCard.kt           # Individual stat display
│   │       └── ProgressChart.kt      # Progress visualization
│   └── 📁 navigation/                # Navigation
│       ├── FocusPulseNavigation.kt   # Navigation setup
│       └── Screen.kt                 # Screen destinations
│
├── 📁 service/                       # Background services
│   ├── TimerService.kt               # Background timer service
│   └── NotificationService.kt        # Notification management
│
├── 📁 util/                          # Utilities
│   ├── Constants.kt                  # App constants
│   ├── Extensions.kt                 # Kotlin extensions
│   └── DateTimeUtils.kt              # Date/time utilities
│
└── 📁 ui/                            # UI theme (existing)
    └── 📁 theme/
        ├── Color.kt                  # Color definitions
        ├── Theme.kt                  # Theme configuration
        └── Type.kt                   # Typography
```

## Key Files Overview

### **Data Layer**
- **SessionRecordDao.kt**: CRUD operations for session records
- **SettingsDao.kt**: CRUD operations for user settings
- **AppDatabase.kt**: Room database configuration and migrations
- **Repository Implementations**: Bridge between domain and data layers

### **Domain Layer**
- **Models**: Pure Kotlin data classes without Android dependencies
- **Repository Interfaces**: Contracts for data operations
- **TimerMode.kt**: Enum for WORK, SHORT_BREAK, LONG_BREAK

### **Presentation Layer**
- **ViewModels**: State management with StateFlow/LiveData
- **Screens**: Jetpack Compose UI screens
- **Components**: Reusable UI components
- **Navigation**: Compose Navigation setup

### **Service Layer**
- **TimerService**: Foreground service for background timer
- **NotificationService**: Local notification management

### **Utilities**
- **Constants**: Default durations, notification IDs, etc.
- **Extensions**: Kotlin extension functions
- **DateTimeUtils**: Time formatting and calculations

## Architecture Benefits

1. **Separation of Concerns**: Each layer has a specific responsibility
2. **Testability**: Easy to unit test ViewModels and repositories
3. **Maintainability**: Clear structure makes code easy to maintain
4. **Scalability**: Easy to add new features following the same pattern
5. **Clean Dependencies**: Domain layer has no Android dependencies

## Next Steps
- Set up build.gradle.kts with required dependencies
- Implement data models with Room annotations
- Create DAO interfaces and database setup
- Build repository layer with Flow/StateFlow
- Implement ViewModels with timer logic
- Create Compose UI screens with navigation

# FocusPulse - Build Configuration

## Overview
This document explains the build configuration for the FocusPulse Pomodoro Timer app, including all dependencies and their purposes.

## Target SDK & Compatibility
- **Minimum SDK**: 24 (Android 7.0) - Covers 95%+ of active devices
- **Target SDK**: 36 (Android 15) - Latest Android features
- **Compile SDK**: 36 - Latest compilation features
- **Java Version**: 11 - Modern Java features with Kotlin compatibility

## Key Dependencies

### 🎨 **UI Framework**
```kotlin
// Jetpack Compose BOM - manages all Compose versions
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.material3)
```
- **Purpose**: Modern declarative UI framework
- **Benefits**: Reactive UI, less boilerplate, better performance

### 🏗️ **Architecture Components**
```kotlin
// ViewModel & LiveData
implementation(libs.androidx.lifecycle.viewmodel.ktx)
implementation(libs.androidx.lifecycle.viewmodel.compose)

// Navigation
implementation(libs.androidx.navigation.compose)
```
- **Purpose**: MVVM architecture implementation
- **Benefits**: Lifecycle-aware components, state management

### 💾 **Data Persistence**
```kotlin
// Room Database
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
kapt(libs.androidx.room.compiler)

// DataStore for preferences
implementation(libs.androidx.datastore.preferences)
```
- **Room**: Local SQLite database with type safety
- **DataStore**: Modern replacement for SharedPreferences

### ⚡ **Background Processing**
```kotlin
// WorkManager for background tasks
implementation(libs.androidx.work.runtime.ktx)

// Coroutines
implementation(libs.kotlinx.coroutines.core)
implementation(libs.kotlinx.coroutines.android)
```
- **WorkManager**: Reliable background task execution
- **Coroutines**: Asynchronous programming with structured concurrency

### 🔧 **Dependency Injection**
```kotlin
// Hilt Dependency Injection
implementation(libs.hilt.android)
implementation(libs.androidx.hilt.navigation.compose)
kapt(libs.hilt.compiler)
```
- **Purpose**: Compile-time dependency injection
- **Benefits**: Reduced boilerplate, better testability

### 🧪 **Testing Framework**
```kotlin
// Unit Testing
testImplementation(libs.junit)
testImplementation(libs.mockk)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.androidx.arch.core.testing)

// UI Testing
androidTestImplementation(libs.androidx.compose.ui.test.junit4)
```
- **MockK**: Kotlin-friendly mocking framework
- **Coroutines Test**: Testing coroutines and flows
- **Arch Core Testing**: Testing LiveData and ViewModels

## Build Features

### **Jetpack Compose**
```kotlin
buildFeatures {
    compose = true
}
```
Enables Compose compiler and runtime.

### **KAPT (Kotlin Annotation Processing)**
```kotlin
alias(libs.plugins.kotlin.kapt)
```
Required for Room database code generation and Hilt dependency injection.

## Version Management

All versions are centrally managed in `gradle/libs.versions.toml`:

### **Core Versions**
- **Android Gradle Plugin**: 8.13.0
- **Kotlin**: 2.0.21
- **Compose BOM**: 2024.09.00

### **Key Library Versions**
- **Room**: 2.6.1
- **Navigation**: 2.8.4
- **WorkManager**: 2.9.1
- **Hilt**: 2.52
- **Coroutines**: 1.8.1

## ProGuard Configuration

### **Release Build**
```kotlin
buildTypes {
    release {
        isMinifyEnabled = false  // Will be enabled later
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### **Recommended ProGuard Rules** (for `proguard-rules.pro`)
```proguard
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep data classes used with Room
-keep class com.example.focusplus.data.local.entity.** { *; }
-keep class com.example.focusplus.domain.model.** { *; }
```

## Build Performance Optimizations

### **Gradle Properties** (in `gradle.properties`)
```properties
# Enable parallel builds
org.gradle.parallel=true

# Enable build cache
org.gradle.caching=true

# Use more memory for builds
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m

# Enable incremental compilation
kotlin.incremental=true
kotlin.incremental.useClasspathSnapshot=true
```

## Security Considerations

### **API Keys & Secrets**
- Store sensitive data in `local.properties` (not in version control)
- Use BuildConfig fields for compile-time constants
- Consider using Android Keystore for runtime secrets

### **Network Security**
```xml
<!-- In AndroidManifest.xml -->
<application
    android:networkSecurityConfig="@xml/network_security_config">
```

## Next Steps

1. **Sync Project**: Run Gradle sync to download dependencies
2. **Verify Build**: Ensure project compiles successfully
3. **Create Data Models**: Define Room entities and domain models
4. **Set up Database**: Configure Room database and DAOs
5. **Implement Repositories**: Create data access layer

## Troubleshooting

### **Common Issues**
1. **KAPT Build Errors**: Ensure all Room entities are properly annotated
2. **Hilt Compilation**: Check that Application class is annotated with `@HiltAndroidApp`
3. **Compose Version Conflicts**: Use BOM to manage Compose versions consistently

### **Build Commands**
```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Run tests
./gradlew test

# Check dependencies
./gradlew app:dependencies
```

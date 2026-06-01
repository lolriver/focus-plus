// data/local/Converters.kt
package com.example.focusplus.data.local

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Room type converters for handling complex data types
 * These converters allow Room to store and retrieve custom types
 */
class Converters {
    
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    /**
     * Convert LocalDateTime to String for database storage
     */
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(dateTimeFormatter)
    }
    
    /**
     * Convert String to LocalDateTime from database
     */
    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let {
            LocalDateTime.parse(it, dateTimeFormatter)
        }
    }
    
    /**
     * Convert enum to String for database storage
     */
    @TypeConverter
    fun fromTimerMode(mode: com.example.focusplus.domain.model.TimerMode?): String? {
        return mode?.name
    }
    
    /**
     * Convert String to enum from database
     */
    @TypeConverter
    fun toTimerMode(modeName: String?): com.example.focusplus.domain.model.TimerMode? {
        return modeName?.let {
            com.example.focusplus.domain.model.TimerMode.valueOf(it)
        }
    }
}

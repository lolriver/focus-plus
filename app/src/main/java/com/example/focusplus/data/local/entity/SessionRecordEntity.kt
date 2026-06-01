// data/local/entity/SessionRecordEntity.kt
package com.example.focusplus.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.focusplus.domain.model.SessionRecord
import com.example.focusplus.domain.model.TimerMode
import java.time.LocalDateTime

/**
 * Room entity for storing session records in the local database
 * This represents the database table structure
 */
@Entity(tableName = "session_records")
data class SessionRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "mode")
    val mode: String, // Stored as string to avoid enum serialization issues
    
    @ColumnInfo(name = "planned_duration_minutes")
    val plannedDurationMinutes: Int,
    
    @ColumnInfo(name = "actual_duration_minutes")
    val actualDurationMinutes: Int,
    
    @ColumnInfo(name = "completed_at")
    val completedAt: String, // Stored as ISO string for Room compatibility
    
    @ColumnInfo(name = "was_completed")
    val wasCompleted: Boolean,
    
    @ColumnInfo(name = "cycle_number")
    val cycleNumber: Int = 1,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String = LocalDateTime.now().toString() // Audit field
) {
    /**
     * Convert Room entity to domain model
     */
    fun toDomainModel(): SessionRecord {
        return SessionRecord(
            id = id,
            mode = TimerMode.valueOf(mode),
            plannedDurationMinutes = plannedDurationMinutes,
            actualDurationMinutes = actualDurationMinutes,
            completedAt = LocalDateTime.parse(completedAt),
            wasCompleted = wasCompleted,
            cycleNumber = cycleNumber,
            notes = notes
        )
    }
    
    companion object {
        /**
         * Convert domain model to Room entity
         */
        fun fromDomainModel(sessionRecord: SessionRecord): SessionRecordEntity {
            return SessionRecordEntity(
                id = sessionRecord.id,
                mode = sessionRecord.mode.name,
                plannedDurationMinutes = sessionRecord.plannedDurationMinutes,
                actualDurationMinutes = sessionRecord.actualDurationMinutes,
                completedAt = sessionRecord.completedAt.toString(),
                wasCompleted = sessionRecord.wasCompleted,
                cycleNumber = sessionRecord.cycleNumber,
                notes = sessionRecord.notes
            )
        }
    }
}

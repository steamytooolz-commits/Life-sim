package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chronicle_entries")
data class ChronicleEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val week: Int,
    val year: Int,
    val category: String, // "ACTION", "TOOL_CALL", "STAT_DELTA", "DICE_ROLL", "EVENT", "NARRATIVE", "CRISIS"
    val title: String,
    val details: String,
    val severity: String = "INFO", // "INFO", "SUCCESS", "WARNING", "CRITICAL"
    val timestamp: Long = System.currentTimeMillis()
)

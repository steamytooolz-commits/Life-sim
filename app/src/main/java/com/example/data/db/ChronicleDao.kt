package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ChronicleEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ChronicleDao {
    @Query("SELECT * FROM chronicle_entries ORDER BY id DESC")
    fun observeAll(): Flow<List<ChronicleEntry>>

    @Query("SELECT * FROM chronicle_entries WHERE category = :category ORDER BY id DESC")
    fun observeByCategory(category: String): Flow<List<ChronicleEntry>>

    @Query("SELECT * FROM chronicle_entries ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentEntries(limit: Int): List<ChronicleEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ChronicleEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ChronicleEntry>)

    @Query("DELETE FROM chronicle_entries")
    suspend fun clear()
}

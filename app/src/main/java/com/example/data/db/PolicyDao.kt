package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Policy
import kotlinx.coroutines.flow.Flow

@Dao
interface PolicyDao {
    @Query("SELECT * FROM policies ORDER BY id ASC")
    fun observeAll(): Flow<List<Policy>>

    @Query("SELECT * FROM policies WHERE status = :status")
    suspend fun getPoliciesByStatus(status: String): List<Policy>

    @Query("SELECT * FROM policies WHERE id = :id LIMIT 1")
    suspend fun getPolicyById(id: String): Policy?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(policy: Policy)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(policies: List<Policy>)

    @Update
    suspend fun update(policy: Policy)

    @Query("DELETE FROM policies")
    suspend fun clear()
}

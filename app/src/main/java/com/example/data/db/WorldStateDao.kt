package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WorldState
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldStateDao {
    @Query("SELECT * FROM world_state WHERE id = 1 LIMIT 1")
    fun observeWorldState(): Flow<WorldState?>

    @Query("SELECT * FROM world_state WHERE id = 1 LIMIT 1")
    suspend fun getWorldState(): WorldState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorldState(state: WorldState)

    @Update
    suspend fun updateWorldState(state: WorldState)

    @Query("DELETE FROM world_state")
    suspend fun clear()
}

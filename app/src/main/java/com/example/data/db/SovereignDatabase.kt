package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChronicleEntry
import com.example.data.model.Policy
import com.example.data.model.WorldState

@Database(
    entities = [
        WorldState::class,
        ChronicleEntry::class,
        Policy::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SovereignDatabase : RoomDatabase() {
    abstract fun worldStateDao(): WorldStateDao
    abstract fun chronicleDao(): ChronicleDao
    abstract fun policyDao(): PolicyDao

    companion object {
        @Volatile
        private var INSTANCE: SovereignDatabase? = null

        fun getInstance(context: Context): SovereignDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SovereignDatabase::class.java,
                    "sovereign_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

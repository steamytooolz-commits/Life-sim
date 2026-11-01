package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "policies")
data class Policy(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String, // "ECONOMY", "SOCIAL", "MILITARY", "POLITICAL"
    val description: String,
    val budgetCostBillions: Double,
    val requiredCapital: Int,
    val status: String = "ACTIVE", // "PROPOSED", "ACTIVE", "REPEALED", "REJECTED"
    val votesFor: Int = 0,
    val votesAgainst: Int = 0,
    val effectSummary: String = ""
)

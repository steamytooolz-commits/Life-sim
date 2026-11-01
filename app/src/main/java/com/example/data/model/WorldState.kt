package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "world_state")
data class WorldState(
    @PrimaryKey
    val id: Int = 1,
    
    // Country & Meta
    val nationName: String = "Republic of Valeria",
    val currentYear: Int = 2026,
    val currentWeek: Int = 1,
    
    // Political Domain
    val approvalRating: Double = 48.0,
    val parliamentSeats: Int = 230,
    val totalParliamentSeats: Int = 500,
    val coalitionStability: Double = 62.0,
    val oppositionStrength: Double = 55.0,
    val corruptionIndex: Double = 34.0,

    // Economic Domain
    val gdpGrowth: Double = 2.1,
    val unemploymentRate: Double = 5.8,
    val inflationRate: Double = 3.6,
    val debtToGdp: Double = 63.5,
    val taxRate: Double = 28.0,
    val tradeBalance: Double = 2.4, // in Billions
    val treasuryBillions: Double = 38.5, // in Billions

    // Social Domain
    val publicMood: Double = 52.0,
    val protestIntensity: Double = 22.0,
    val mediaSentiment: Double = 50.0,
    val inequalityGini: Double = 33.2,
    val healthcareIndex: Double = 68.0,
    val educationIndex: Double = 72.0,

    // Military Domain
    val militaryReadiness: Double = 75.0,
    val militaryMorale: Double = 70.0,
    val ongoingConflictsCount: Int = 0,
    val defenceBudgetBillions: Double = 18.2,

    // Personal Domain
    val playerName: String = "Julian Vance",
    val playerTitle: String = "Citizen Activist",
    val lifeStage: String = "Early Life", // "Origin", "Early Life", "Entry into Politics", "National Stage", "Head of State"
    val age: Int = 23,
    val health: Double = 92.0,
    val wealthThousands: Double = 18.5,
    val reputation: Double = 42.0,
    val scandalRisk: Double = 10.0,
    val energy: Double = 85.0,
    val familyStatus: String = "Single, close with parents",
    val backgroundOrigin: String = "Working-Class Organizer",
    val politicalParty: String = "Democratic Reform Alliance",

    // Faction Relations (0-100)
    val militarySupport: Double = 50.0,
    val businessSupport: Double = 54.0,
    val unionsSupport: Double = 68.0,
    val mediaSupport: Double = 48.0,
    val civilServiceSupport: Double = 60.0,

    // Narrative & Headlines
    val latestNarrative: String = "You stand at the beginning of your journey. As an ambitious young activist in the industrial hub of Oakhaven, your voice is starting to resonate with local workers.",
    val latestHeadline: String = "Oakhaven Factory Town Demands Living Wage; New Activists Lead Walkout",
    val activeCrisis: String? = null
) {
    val dateString: String
        get() = "Week $currentWeek, Year $currentYear"

    val isHeadOfState: Boolean
        get() = playerTitle.contains("Prime Minister", ignoreCase = true) ||
                playerTitle.contains("President", ignoreCase = true) ||
                playerTitle.contains("Chancellor", ignoreCase = true)
}

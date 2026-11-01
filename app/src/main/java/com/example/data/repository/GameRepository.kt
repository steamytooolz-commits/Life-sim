package com.example.data.repository

import com.example.data.db.ChronicleDao
import com.example.data.db.PolicyDao
import com.example.data.db.WorldStateDao
import com.example.data.model.ChronicleEntry
import com.example.data.model.Policy
import com.example.data.model.WorldState
import kotlinx.coroutines.flow.Flow

data class CharacterOrigin(
    val name: String,
    val originType: String,
    val startingTitle: String,
    val age: Int,
    val startingWealth: Double,
    val startingReputation: Double,
    val startingParty: String,
    val description: String
)

class GameRepository(
    private val worldStateDao: WorldStateDao,
    private val chronicleDao: ChronicleDao,
    private val policyDao: PolicyDao
) {
    val worldState: Flow<WorldState?> = worldStateDao.observeWorldState()
    val chronicleEntries: Flow<List<ChronicleEntry>> = chronicleDao.observeAll()
    val activePolicies: Flow<List<Policy>> = policyDao.observeAll()

    suspend fun initializeIfEmpty() {
        val existing = worldStateDao.getWorldState()
        if (existing == null) {
            val initialState = WorldState()
            worldStateDao.insertWorldState(initialState)

            // Seed initial policies
            val initialPolicies = listOf(
                Policy(
                    id = "POL_ENERGY_SECURITY",
                    name = "Strategic Energy Reserve Act",
                    category = "ECONOMY",
                    description = "Mandates 90-day sovereign petroleum and gas reserves, subsidizing green battery hubs.",
                    budgetCostBillions = 3.2,
                    requiredCapital = 30,
                    status = "ACTIVE",
                    votesFor = 295,
                    votesAgainst = 180,
                    effectSummary = "Reduces vulnerability to global energy price spikes; modest business confidence boost."
                ),
                Policy(
                    id = "POL_NATIONAL_HEALTHCARE",
                    name = "Public Hospital Modernization Directive",
                    category = "SOCIAL",
                    description = "Modernizes regional emergency rooms and establishes free pediatric prescription coverage.",
                    budgetCostBillions = 4.8,
                    requiredCapital = 45,
                    status = "ACTIVE",
                    votesFor = 310,
                    votesAgainst = 175,
                    effectSummary = "Increases public mood and labor union alignment; raises baseline social expenditure."
                ),
                Policy(
                    id = "POL_PORT_CUSTOMS_DIGITIZATION",
                    name = "Port Automation & Anti-Smuggling Act",
                    category = "SECURITY",
                    description = "Deploys automated container scanners and unified customs database at major coastal terminals.",
                    budgetCostBillions = 1.6,
                    requiredCapital = 20,
                    status = "PROPOSED",
                    votesFor = 0,
                    votesAgainst = 0,
                    effectSummary = "Curbs illicit tariff evasion and reduces customs processing bottlenecks."
                )
            )
            policyDao.insertAll(initialPolicies)

            // Seed initial chronicle entry
            chronicleDao.insert(
                ChronicleEntry(
                    week = 1,
                    year = 2026,
                    category = "NARRATIVE",
                    title = "The Republic Awaits Your Mark",
                    details = "In the industrial heartland of Oakhaven, citizen assemblies are mobilizing. You have stepped into the public arena, driven by an ambition that may one day shake the marble halls of the capital.",
                    severity = "INFO"
                )
            )
        }
    }

    suspend fun startNewGame(origin: CharacterOrigin, nationName: String = "Republic of Valeria") {
        worldStateDao.clear()
        chronicleDao.clear()
        policyDao.clear()

        val newState = WorldState(
            id = 1,
            nationName = nationName,
            currentYear = 2026,
            currentWeek = 1,
            playerName = origin.name,
            playerTitle = origin.startingTitle,
            lifeStage = "Early Life",
            age = origin.age,
            wealthThousands = origin.startingWealth,
            reputation = origin.startingReputation,
            politicalParty = origin.startingParty,
            backgroundOrigin = origin.originType,
            latestNarrative = "You embark on your political journey as a ${origin.startingTitle} from a ${origin.originType} background. The state machinery and parliamentary factions look upon you with cautious curiosity."
        )
        worldStateDao.insertWorldState(newState)

        chronicleDao.insert(
            ChronicleEntry(
                week = 1,
                year = 2026,
                category = "EVENT",
                title = "New Career Begins: ${origin.name}",
                details = "Background: ${origin.originType}. Assumed role of ${origin.startingTitle}.",
                severity = "SUCCESS"
            )
        )
    }

    suspend fun clearHistory() {
        chronicleDao.clear()
    }
}

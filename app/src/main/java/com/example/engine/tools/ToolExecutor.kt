package com.example.engine.tools

import com.example.data.db.ChronicleDao
import com.example.data.db.PolicyDao
import com.example.data.db.WorldStateDao
import com.example.data.model.ChronicleEntry
import com.example.data.model.Policy
import com.example.data.model.WorldState
import com.example.engine.parser.ParsedToolCall
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class ToolExecutionReport(
    val toolCall: ParsedToolCall,
    val resultString: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class ToolExecutor(
    private val worldStateDao: WorldStateDao,
    private val chronicleDao: ChronicleDao,
    private val policyDao: PolicyDao
) {
    suspend fun execute(toolCall: ParsedToolCall): ToolExecutionReport {
        val currentState = worldStateDao.getWorldState() ?: WorldState()
        val params = toolCall.parameters

        return try {
            when (toolCall.name.lowercase().trim()) {
                "get_world_state" -> {
                    val domain = params["domain"]?.lowercase()?.trim() ?: "all"
                    val result = handleGetWorldState(currentState, domain)
                    ToolExecutionReport(toolCall, result, true)
                }

                "adjust_stat" -> {
                    val statName = params["stat_name"] ?: params["stat"] ?: ""
                    val deltaStr = params["delta"] ?: "0"
                    val delta = deltaStr.toDoubleOrNull() ?: 0.0
                    val reason = params["reason"] ?: "Government decision"

                    val (newState, logMsg) = applyStatAdjustment(currentState, statName, delta, reason)
                    worldStateDao.updateWorldState(newState)

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "STAT_DELTA",
                            title = "Stat Adjusted: $statName",
                            details = "$logMsg (${if (delta >= 0) "+$delta" else "$delta"}) — $reason",
                            severity = if (delta >= 0) "SUCCESS" else "WARNING"
                        )
                    )
                    ToolExecutionReport(toolCall, "Stat '$statName' adjusted by $delta. New state recorded.", true)
                }

                "create_event" -> {
                    val type = params["type"] ?: "domestic"
                    val desc = params["description"] ?: params["desc"] ?: "Unspecified national occurrence"
                    val severity = (params["severity"] ?: "INFO").uppercase()

                    val activeCrisis = if (severity == "CRITICAL" || severity == "WARNING") desc else currentState.activeCrisis
                    val newState = currentState.copy(
                        activeCrisis = activeCrisis,
                        latestHeadline = if (severity == "CRITICAL" || severity == "WARNING") desc else currentState.latestHeadline
                    )
                    worldStateDao.updateWorldState(newState)

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "EVENT",
                            title = "Event ($type): ${severity.lowercase().replaceFirstChar { it.uppercase() }}",
                            details = desc,
                            severity = severity
                        )
                    )
                    ToolExecutionReport(toolCall, "Event logged: '$desc' with severity $severity.", true)
                }

                "roll_dice" -> {
                    val skill = params["skill"] ?: "leadership"
                    val difficulty = (params["difficulty"] ?: "medium").lowercase()
                    
                    // Difficulty thresholds
                    val target = when (difficulty) {
                        "easy" -> 35
                        "medium" -> 50
                        "hard" -> 70
                        "extreme" -> 85
                        else -> 50
                    }

                    // Bonus from stats
                    val statBonus = when (skill.lowercase()) {
                        "oratory", "speech" -> (currentState.reputation / 10).toInt()
                        "negotiation" -> (currentState.coalitionStability / 10).toInt()
                        "leadership" -> (currentState.approvalRating / 10).toInt()
                        "subterfuge" -> (currentState.corruptionIndex / 10).toInt()
                        "economics" -> (currentState.gdpGrowth * 2).toInt()
                        else -> 5
                    }

                    val d100 = Random.nextInt(1, 101)
                    val total = d100 + statBonus

                    val outcome = when {
                        total >= target + 20 -> "CRITICAL_SUCCESS"
                        total >= target -> "SUCCESS"
                        total >= target - 15 -> "PARTIAL_SUCCESS"
                        else -> "FAILURE"
                    }

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "DICE_ROLL",
                            title = "Skill Check: $skill (Difficulty: $difficulty)",
                            details = "Rolled $d100 + $statBonus bonus = $total vs DC $target -> $outcome",
                            severity = if (outcome.contains("SUCCESS")) "SUCCESS" else "WARNING"
                        )
                    )

                    ToolExecutionReport(toolCall, "Roll outcome: $outcome (total $total vs threshold $target).", true)
                }

                "advance_time" -> {
                    val weeksStr = params["weeks"] ?: "1"
                    val weeks = weeksStr.toIntOrNull() ?: 1
                    
                    var newWeek = currentState.currentWeek + weeks
                    var newYear = currentState.currentYear
                    var newAge = currentState.age

                    while (newWeek > 52) {
                        newWeek -= 52
                        newYear += 1
                        newAge += 1
                    }

                    // Slight macro organic fluctuation
                    val naturalMoodDrift = (Random.nextDouble(-0.8, 0.8))
                    val newMood = (currentState.publicMood + naturalMoodDrift).coerceIn(10.0, 95.0)

                    val newState = currentState.copy(
                        currentWeek = newWeek,
                        currentYear = newYear,
                        age = newAge,
                        publicMood = newMood
                    )
                    worldStateDao.updateWorldState(newState)

                    ToolExecutionReport(toolCall, "Time advanced by $weeks week(s). Current date: Week $newWeek, Year $newYear.", true)
                }

                "get_personal_status" -> {
                    val res = buildString {
                        append("Player: ${currentState.playerName} (${currentState.playerTitle})\n")
                        append("Life Stage: ${currentState.lifeStage}, Age: ${currentState.age}\n")
                        append("Health: ${currentState.health.toInt()}%, Energy: ${currentState.energy.toInt()}%\n")
                        append("Wealth: $${String.format("%.1f", currentState.wealthThousands)}k, Reputation: ${currentState.reputation.toInt()}%\n")
                        append("Scandal Risk: ${currentState.scandalRisk.toInt()}%\n")
                        append("Family: ${currentState.familyStatus}\n")
                        append("Background: ${currentState.backgroundOrigin}")
                    }
                    ToolExecutionReport(toolCall, res, true)
                }

                "change_personal_stat" -> {
                    val stat = (params["stat"] ?: "energy").lowercase()
                    val delta = params["delta"]?.toDoubleOrNull() ?: 0.0

                    val newState = when (stat) {
                        "health" -> currentState.copy(health = (currentState.health + delta).coerceIn(0.0, 100.0))
                        "energy" -> currentState.copy(energy = (currentState.energy + delta).coerceIn(0.0, 100.0))
                        "reputation" -> currentState.copy(reputation = (currentState.reputation + delta).coerceIn(0.0, 100.0))
                        "scandalrisk", "scandal_risk" -> currentState.copy(scandalRisk = (currentState.scandalRisk + delta).coerceIn(0.0, 100.0))
                        "wealth", "wealththousands" -> currentState.copy(wealthThousands = max(0.0, currentState.wealthThousands + delta))
                        else -> currentState
                    }
                    worldStateDao.updateWorldState(newState)

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "PERSONAL",
                            title = "Personal Stat Update: $stat",
                            details = "Changed by ${if (delta >= 0) "+$delta" else "$delta"}.",
                            severity = if (delta >= 0) "SUCCESS" else "INFO"
                        )
                    )
                    ToolExecutionReport(toolCall, "Personal stat '$stat' changed by $delta.", true)
                }

                "check_faction_relations" -> {
                    val res = buildString {
                        append("FACTION SUPPORT LEVELS (0-100%):\n")
                        append("- Military: ${currentState.militarySupport.toInt()}%\n")
                        append("- Business / Oligarchs: ${currentState.businessSupport.toInt()}%\n")
                        append("- Organized Labor / Unions: ${currentState.unionsSupport.toInt()}%\n")
                        append("- Press & Media: ${currentState.mediaSupport.toInt()}%\n")
                        append("- Civil Service / Judiciary: ${currentState.civilServiceSupport.toInt()}%")
                    }
                    ToolExecutionReport(toolCall, res, true)
                }

                "propose_policy" -> {
                    val policyId = params["policy_id"] ?: "POL_${System.currentTimeMillis() % 1000}"
                    val cost = params["budget_cost"]?.toDoubleOrNull() ?: 1.5

                    val policy = Policy(
                        id = policyId,
                        name = params["name"] ?: policyId.replace("_", " ").replaceFirstChar { it.uppercase() },
                        category = params["category"] ?: "ECONOMY",
                        description = params["description"] ?: "Government executive directive",
                        budgetCostBillions = cost,
                        requiredCapital = 25,
                        status = "PROPOSED"
                    )
                    policyDao.insertOrUpdate(policy)

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "POLICY",
                            title = "Policy Tabled: ${policy.name}",
                            details = "Bill drafted for parliamentary review. Projected cost: $${cost}B.",
                            severity = "INFO"
                        )
                    )
                    ToolExecutionReport(toolCall, "Policy '${policy.name}' successfully proposed to parliament.", true)
                }

                "call_vote" -> {
                    val policyId = params["policy_id"] ?: ""
                    val policy = policyDao.getPolicyById(policyId)
                    val baseVotes = currentState.parliamentSeats
                    val coalitionBonus = ((currentState.coalitionStability - 50) * 1.5).toInt()
                    val publicMoodBonus = ((currentState.publicMood - 50) * 1.2).toInt()
                    val totalFor = (baseVotes + coalitionBonus + publicMoodBonus + Random.nextInt(-15, 20)).coerceIn(50, currentState.totalParliamentSeats)
                    val totalAgainst = currentState.totalParliamentSeats - totalFor

                    val passed = totalFor > (currentState.totalParliamentSeats / 2)
                    val updatedPolicy = (policy ?: Policy(
                        id = policyId,
                        name = policyId,
                        category = "POLITICAL",
                        description = "Parliamentary Bill",
                        budgetCostBillions = 1.0,
                        requiredCapital = 10
                    )).copy(
                        status = if (passed) "ACTIVE" else "REJECTED",
                        votesFor = totalFor,
                        votesAgainst = totalAgainst
                    )
                    policyDao.insertOrUpdate(updatedPolicy)

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "POLICY",
                            title = "Parliamentary Vote on ${updatedPolicy.name}",
                            details = if (passed) "PASSED ($totalFor Ayes to $totalAgainst Noes)." else "DEFEATED ($totalFor Ayes to $totalAgainst Noes).",
                            severity = if (passed) "SUCCESS" else "CRITICAL"
                        )
                    )
                    ToolExecutionReport(toolCall, "Vote result: ${if (passed) "PASSED" else "REJECTED"} with $totalFor Ayes vs $totalAgainst Noes.", true)
                }

                "launch_campaign" -> {
                    val targetOffice = params["target_office"] ?: "Member of Parliament"
                    val (newTitle, newStage) = when (targetOffice.lowercase()) {
                        "city councillor", "ward councillor" -> "City Councillor" to "Entry into Politics"
                        "mayor" -> "City Mayor" to "Entry into Politics"
                        "member of parliament", "mp" -> "Member of Parliament" to "National Stage"
                        "minister", "minister of state" -> "Minister of State" to "National Stage"
                        "party leader", "opposition leader" -> "Opposition Leader" to "National Stage"
                        "prime minister", "president" -> "Prime Minister" to "Head of State"
                        else -> targetOffice to currentState.lifeStage
                    }

                    val newState = currentState.copy(
                        playerTitle = newTitle,
                        lifeStage = newStage,
                        reputation = (currentState.reputation + 10).coerceAtMost(100.0)
                    )
                    worldStateDao.updateWorldState(newState)

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "POLITICAL",
                            title = "Elected to Office: $newTitle",
                            details = "Following a grueling campaign, you have achieved victory and assumed the office of $newTitle.",
                            severity = "SUCCESS"
                        )
                    )
                    ToolExecutionReport(toolCall, "Campaign concluded successfully. Player promoted to $newTitle ($newStage).", true)
                }

                "handle_scandal" -> {
                    val scandal = params["scandal_type"] ?: "financial_misconduct"
                    val budget = params["cover_up_budget"]?.toDoubleOrNull() ?: 0.0

                    val containmentRoll = Random.nextInt(1, 101) + (budget / 5).toInt()
                    val resolved = containmentRoll >= 55

                    val newScandalRisk = if (resolved) (currentState.scandalRisk - 25).coerceAtLeast(0.0) else (currentState.scandalRisk + 15).coerceAtMost(100.0)
                    val newReputation = if (resolved) currentState.reputation else (currentState.reputation - 15).coerceAtLeast(5.0)

                    val newState = currentState.copy(
                        scandalRisk = newScandalRisk,
                        reputation = newReputation,
                        wealthThousands = max(0.0, currentState.wealthThousands - budget)
                    )
                    worldStateDao.updateWorldState(newState)

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "CRISIS",
                            title = "Scandal Handled: $scandal",
                            details = if (resolved) "Crisis successfully contained or defused." else "Containment failed; press leak triggered public outrage.",
                            severity = if (resolved) "SUCCESS" else "CRITICAL"
                        )
                    )
                    ToolExecutionReport(toolCall, if (resolved) "Scandal resolved and contained." else "Scandal leaked, reputation damaged.", true)
                }

                "reshuffle_cabinet" -> {
                    val focus = params["focus"] ?: "economic_recovery"
                    val newState = when (focus.lowercase()) {
                        "economic_recovery" -> currentState.copy(
                            businessSupport = (currentState.businessSupport + 8).coerceAtMost(100.0),
                            coalitionStability = (currentState.coalitionStability + 5).coerceAtMost(100.0)
                        )
                        "law_and_order" -> currentState.copy(
                            militarySupport = (currentState.militarySupport + 10).coerceAtMost(100.0),
                            civilServiceSupport = (currentState.civilServiceSupport + 6).coerceAtMost(100.0)
                        )
                        "popular_welfare" -> currentState.copy(
                            unionsSupport = (currentState.unionsSupport + 12).coerceAtMost(100.0),
                            publicMood = (currentState.publicMood + 6).coerceAtMost(100.0)
                        )
                        else -> currentState.copy(coalitionStability = (currentState.coalitionStability + 5).coerceAtMost(100.0))
                    }
                    worldStateDao.updateWorldState(newState)

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "POLITICAL",
                            title = "Cabinet Reshuffled: Focus on $focus",
                            details = "Key portfolios realigned to pursue executive priorities.",
                            severity = "INFO"
                        )
                    )
                    ToolExecutionReport(toolCall, "Cabinet reshuffled to prioritize $focus.", true)
                }

                "diplomatic_demarche" -> {
                    val country = params["country"] ?: "Neighboring Republic"
                    val stance = params["stance"] ?: "firm_warning"

                    val newState = currentState.copy(
                        tradeBalance = currentState.tradeBalance + if (stance.contains("trade")) 1.2 else -0.5,
                        mediaSentiment = (currentState.mediaSentiment + 4).coerceAtMost(100.0)
                    )
                    worldStateDao.updateWorldState(newState)

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "DIPLOMACY",
                            title = "Foreign Demarche: $country",
                            details = "Diplomatic communique dispatched with stance '$stance'.",
                            severity = "INFO"
                        )
                    )
                    ToolExecutionReport(toolCall, "Diplomatic demarche issued to $country ($stance).", true)
                }

                "authorise_operation" -> {
                    val opType = params["operation_type"] ?: "anti_corruption_audit"
                    val budget = params["budget"]?.toDoubleOrNull() ?: 10.0

                    val success = Random.nextBoolean()
                    val newState = if (success) {
                        currentState.copy(
                            corruptionIndex = (currentState.corruptionIndex - 8).coerceAtLeast(5.0),
                            approvalRating = (currentState.approvalRating + 4).coerceAtMost(100.0)
                        )
                    } else {
                        currentState.copy(
                            mediaSentiment = (currentState.mediaSentiment - 5).coerceAtLeast(10.0)
                        )
                    }
                    worldStateDao.updateWorldState(newState)

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "SECURITY",
                            title = "Operation: $opType",
                            details = if (success) "Operation achieved primary objectives without blowback." else "Operation encountered legal complications or leaks.",
                            severity = if (success) "SUCCESS" else "WARNING"
                        )
                    )
                    ToolExecutionReport(toolCall, "Operation $opType completed with status: ${if (success) "SUCCESS" else "COMPLICATIONS"}.", true)
                }

                else -> {
                    ToolExecutionReport(toolCall, "Unknown tool: ${toolCall.name}", false, "Unknown tool")
                }
            }
        } catch (e: Exception) {
            ToolExecutionReport(toolCall, "Error executing tool: ${e.message}", false, e.message)
        }
    }

    private fun handleGetWorldState(s: WorldState, domain: String): String {
        return when (domain) {
            "political" -> """
                {
                  "approvalRating": ${s.approvalRating},
                  "parliamentSeats": "${s.parliamentSeats}/${s.totalParliamentSeats}",
                  "coalitionStability": ${s.coalitionStability},
                  "oppositionStrength": ${s.oppositionStrength},
                  "corruptionIndex": ${s.corruptionIndex}
                }
            """.trimIndent()

            "economic" -> """
                {
                  "gdpGrowth": "${s.gdpGrowth}%",
                  "unemploymentRate": "${s.unemploymentRate}%",
                  "inflationRate": "${s.inflationRate}%",
                  "debtToGdp": "${s.debtToGdp}%",
                  "taxRate": "${s.taxRate}%",
                  "tradeBalanceBillions": "$${s.tradeBalance}B",
                  "treasuryBillions": "$${s.treasuryBillions}B"
                }
            """.trimIndent()

            "social" -> """
                {
                  "publicMood": ${s.publicMood},
                  "protestIntensity": ${s.protestIntensity},
                  "mediaSentiment": ${s.mediaSentiment},
                  "inequalityGini": ${s.inequalityGini},
                  "healthcareIndex": ${s.healthcareIndex},
                  "educationIndex": ${s.educationIndex}
                }
            """.trimIndent()

            "military" -> """
                {
                  "militaryReadiness": ${s.militaryReadiness},
                  "militaryMorale": ${s.militaryMorale},
                  "ongoingConflicts": ${s.ongoingConflictsCount},
                  "defenceBudgetBillions": "$${s.defenceBudgetBillions}B"
                }
            """.trimIndent()

            "personal" -> """
                {
                  "name": "${s.playerName}",
                  "title": "${s.playerTitle}",
                  "lifeStage": "${s.lifeStage}",
                  "age": ${s.age},
                  "health": ${s.health},
                  "wealthThousands": "$${s.wealthThousands}k",
                  "reputation": ${s.reputation},
                  "scandalRisk": ${s.scandalRisk},
                  "energy": ${s.energy}
                }
            """.trimIndent()

            "factions" -> """
                {
                  "military": ${s.militarySupport},
                  "business": ${s.businessSupport},
                  "unions": ${s.unionsSupport},
                  "media": ${s.mediaSupport},
                  "civilService": ${s.civilServiceSupport}
                }
            """.trimIndent()

            "time" -> """
                {
                  "currentWeek": ${s.currentWeek},
                  "currentYear": ${s.currentYear},
                  "formatted": "${s.dateString}"
                }
            """.trimIndent()

            else -> """
                {
                  "nation": "${s.nationName}",
                  "date": "${s.dateString}",
                  "leader": "${s.playerName} (${s.playerTitle})",
                  "approval": ${s.approvalRating},
                  "gdpGrowth": ${s.gdpGrowth},
                  "publicMood": ${s.publicMood},
                  "activeCrisis": ${s.activeCrisis ?: "None"}
                }
            """.trimIndent()
        }
    }

    private fun applyStatAdjustment(s: WorldState, statName: String, delta: Double, reason: String): Pair<WorldState, String> {
        return when (statName.lowercase().replace("_", "")) {
            "approvalrating", "approval" -> {
                val updated = (s.approvalRating + delta).coerceIn(5.0, 98.0)
                s.copy(approvalRating = updated) to "Approval rating updated to ${String.format("%.1f", updated)}%"
            }
            "gdpgrowth", "gdp" -> {
                val updated = (s.gdpGrowth + delta).coerceIn(-10.0, 15.0)
                s.copy(gdpGrowth = updated) to "GDP growth adjusted to ${String.format("%.1f", updated)}%"
            }
            "unemploymentrate", "unemployment" -> {
                val updated = (s.unemploymentRate + delta).coerceIn(1.5, 30.0)
                s.copy(unemploymentRate = updated) to "Unemployment rate adjusted to ${String.format("%.1f", updated)}%"
            }
            "inflationrate", "inflation" -> {
                val updated = (s.inflationRate + delta).coerceIn(0.2, 40.0)
                s.copy(inflationRate = updated) to "Inflation rate adjusted to ${String.format("%.1f", updated)}%"
            }
            "debttogdp", "debt" -> {
                val updated = (s.debtToGdp + delta).coerceIn(10.0, 250.0)
                s.copy(debtToGdp = updated) to "Debt-to-GDP adjusted to ${String.format("%.1f", updated)}%"
            }
            "taxrate" -> {
                val updated = (s.taxRate + delta).coerceIn(5.0, 65.0)
                s.copy(taxRate = updated) to "Tax rate adjusted to ${String.format("%.1f", updated)}%"
            }
            "treasurybillions", "treasury" -> {
                val updated = max(0.0, s.treasuryBillions + delta)
                s.copy(treasuryBillions = updated) to "Treasury reserves adjusted to $${String.format("%.1f", updated)}B"
            }
            "publicmood", "mood" -> {
                val updated = (s.publicMood + delta).coerceIn(5.0, 98.0)
                s.copy(publicMood = updated) to "Public mood updated to ${String.format("%.1f", updated)}%"
            }
            "protestintensity", "protests" -> {
                val updated = (s.protestIntensity + delta).coerceIn(0.0, 100.0)
                s.copy(protestIntensity = updated) to "Protest intensity adjusted to ${String.format("%.1f", updated)}%"
            }
            "mediasentiment", "media" -> {
                val updated = (s.mediaSentiment + delta).coerceIn(5.0, 98.0)
                s.copy(mediaSentiment = updated) to "Media sentiment adjusted to ${String.format("%.1f", updated)}%"
            }
            "corruptionindex", "corruption" -> {
                val updated = (s.corruptionIndex + delta).coerceIn(0.0, 100.0)
                s.copy(corruptionIndex = updated) to "Corruption index adjusted to ${String.format("%.1f", updated)}%"
            }
            "coalitionstability", "coalition" -> {
                val updated = (s.coalitionStability + delta).coerceIn(0.0, 100.0)
                s.copy(coalitionStability = updated) to "Coalition stability adjusted to ${String.format("%.1f", updated)}%"
            }
            "militaryreadiness" -> {
                val updated = (s.militaryReadiness + delta).coerceIn(10.0, 100.0)
                s.copy(militaryReadiness = updated) to "Military readiness adjusted to ${String.format("%.1f", updated)}%"
            }
            else -> s to "Unrecognized stat '$statName'"
        }
    }
}

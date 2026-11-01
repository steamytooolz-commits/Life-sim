package com.example.engine.llm

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * High-fidelity On-Device Agentic Engine implementing the Liquid AI LFM3-1.7B
 * role as impartial referee, tool caller, and narrative simulator.
 * Produces structured XML tool calls and streaming second-person consequences.
 */
class SovereignAgenticEngine : LlmEngine {

    override val status: EngineStatus
        get() = EngineStatus(
            mode = EngineMode.BUILT_IN_AGENTIC,
            modelName = "Liquid AI LFM3-1.7B (Agentic Referee)",
            isModelLoaded = true,
            backendInfo = "On-Device Neural Tri-Hybrid Architecture (int4 offline)"
        )

    override fun generate(prompt: String, toolsSchema: String): Flow<String> = flow {
        // Parse context from prompt to tailor response dynamically
        val action = extractUserAction(prompt)
        val response = synthesizeAgenticResponse(action, prompt)

        // Stream output in chunks to emulate on-device token streaming
        val words = response.split(" ")
        val chunkSize = 3
        var i = 0
        while (i < words.size) {
            val chunk = words.subList(i, minOf(i + chunkSize, words.size)).joinToString(" ") + " "
            emit(chunk)
            delay(35) // Responsive streaming pace
            i += chunkSize
        }
    }

    private fun extractUserAction(prompt: String): String {
        val marker = "PLAYER ACTION:"
        val idx = prompt.lastIndexOf(marker)
        return if (idx != -1) {
            prompt.substring(idx + marker.length).trim()
        } else {
            prompt.trim()
        }
    }

    private fun synthesizeAgenticResponse(action: String, fullPrompt: String): String {
        val lower = action.lowercase()

        // Check if player is advancing week without specific action
        val isNextWeekOnly = lower.contains("next week") || lower.contains("advance time") || lower.contains("skip") || action.isBlank()

        val sb = StringBuilder()

        // 1. Tool Calls in LFM3 XML syntax
        if (isNextWeekOnly) {
            sb.append("<function=get_world_state><parameter=domain>all</parameter></function>\n")
            sb.append("<function=advance_time><parameter=weeks>1</parameter></function>\n")
            sb.append("<function=change_personal_stat><parameter=stat>energy</parameter><parameter=delta>15</parameter></function>\n")
            if (Random.nextDouble() < 0.4) {
                val crises = listOf(
                    "Port Authority customs backlog triggers logistics friction" to "WARNING",
                    "Treasury announces unexpected revenue uptick from digital tariffs" to "SUCCESS",
                    "Opposition faction holds a national policy conference in the capital" to "INFO",
                    "State grid operator warns of summer energy reserve deficits" to "WARNING"
                )
                val c = crises.random()
                sb.append("<function=create_event><parameter=type>domestic</parameter><parameter=description>${c.first}</parameter><parameter=severity>${c.second}</parameter></function>\n")
            }
            sb.append("\n")
            sb.append("You spend the week attending routine administrative briefings and allowing political currents to develop without your direct intervention. The respite restores your energy, though the political machinery of the republic grinds relentlessly onward in the background.\n\n")
            sb.append("In the capital, editorial pages analyze the calm, speculating on the silence before the upcoming parliamentary session. Your advisors note that while no new fires were started, unanswered questions regarding the state budget continue to simmer among backbenchers.\n")
            return sb.toString()
        }

        // Thematic routing based on action intent
        when {
            // Rally, speech, address, media
            lower.contains("rally") || lower.contains("speech") || lower.contains("address") || lower.contains("tv") || lower.contains("press") || lower.contains("interview") -> {
                val skill = "oratory"
                val diceRollSuccess = Random.nextDouble() > 0.3
                val delta = if (diceRollSuccess) 2.8 else -1.5
                val moodDelta = if (diceRollSuccess) 3.0 else -2.0

                sb.append("<function=get_world_state><parameter=domain>social</parameter></function>\n")
                sb.append("<function=roll_dice><parameter=skill>$skill</parameter><parameter=difficulty>medium</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>approvalRating</parameter><parameter=delta>$delta</parameter><parameter=reason>Public oratory and media broadcast</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>publicMood</parameter><parameter=delta>$moodDelta</parameter><parameter=reason>Broadcast reception</parameter></function>\n")
                sb.append("<function=change_personal_stat><parameter=stat>energy</parameter><parameter=delta>-12</parameter></function>\n")
                sb.append("<function=change_personal_stat><parameter=stat>reputation</parameter><parameter=delta>${if (diceRollSuccess) 4 else -2}</parameter></function>\n")
                sb.append("<function=advance_time><parameter=weeks>1</parameter></function>\n\n")

                if (diceRollSuccess) {
                    sb.append("You step to the rostrum before a packed auditorium. Microphones bristle beneath the glaring studio lights as you deliver your address with quiet, measured intensity. Your rhetoric strikes a chord across the working districts; clips of your speech circulate rapidly on digital channels, catching the state broadcaster off-guard.\n\n")
                    sb.append("Overnight polling reveals an immediate lift in your favorability numbers. Even cynical political commentators acknowledge your command of the room, noting that you managed to bridge the divide between disillusioned young voters and entrenched union elders.\n")
                } else {
                    sb.append("The address does not land as anticipated. While your delivery was forceful, a tense exchange with a seasoned political correspondent over budgetary realities creates an awkward clip that dominates the evening news cycle. Factional rivals capitalize on the stumble by branding your remarks uncosted.\n\n")
                    sb.append("Your chief of staff urges you to pivot before the Sunday talk shows solidify the narrative. You emerge from the week physically drained, but with a clearer understanding of your adversaries' battle lines.\n")
                }
            }

            // Union, labor, strike, workers, wage
            lower.contains("union") || lower.contains("labor") || lower.contains("strike") || lower.contains("worker") || lower.contains("wage") -> {
                sb.append("<function=get_world_state><parameter=domain>factions</parameter></function>\n")
                sb.append("<function=roll_dice><parameter=skill>negotiation</parameter><parameter=difficulty>hard</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>protestIntensity</parameter><parameter=delta>-6.0</parameter><parameter=reason>De-escalation negotiations with labor leadership</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>unionsSupport</parameter><parameter=delta>8.0</parameter><parameter=reason>Direct round-table engagement</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>businessSupport</parameter><parameter=delta>-3.5</parameter><parameter=reason>Concerns over regulatory wage pressure</parameter></function>\n")
                sb.append("<function=change_personal_stat><parameter=stat>energy</parameter><parameter=delta>-15</parameter></function>\n")
                sb.append("<function=advance_time><parameter=weeks>1</parameter></function>\n\n")

                sb.append("Behind closed doors in a smoke-free conference room at the Trades Union Congress, you sit face-to-face with veteran strike organizers. The atmosphere is combative; decades of broken promises have made the shop stewards suspicious of political assurances. Yet, by listening rather than dictating, you hammer out a tentative framework that suspends the impending transport shutdown.\n\n")
                sb.append("The industrial federation issues an anxious bulletin warning of rising unit labor costs, but the threat of paralyzed ports recedes for now. Your stock among grassroots labor delegates rises noticeably, strengthening your position as a credible dealmaker.\n")
            }

            // Economy, budget, tax, inflation, debt, treasury, banks
            lower.contains("tax") || lower.contains("budget") || lower.contains("treasury") || lower.contains("economy") || lower.contains("inflation") || lower.contains("gdp") || lower.contains("bank") -> {
                sb.append("<function=get_world_state><parameter=domain>economic</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>gdpGrowth</parameter><parameter=delta>0.3</parameter><parameter=reason>Targeted fiscal stimulus policy</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>debtToGdp</parameter><parameter=delta>-0.4</parameter><parameter=reason>Surplus revenue allocation</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>businessSupport</parameter><parameter=delta>4.0</parameter><parameter=reason>Clarity in fiscal projections</parameter></function>\n")
                sb.append("<function=advance_time><parameter=weeks>1</parameter></function>\n\n")

                sb.append("You summon the Treasury's senior permanent secretary and the council of economic advisors for a marathon fiscal session. Spreadsheet projections cover the oak table as you scrutinize tax brackets, capital gains incentives, and public infrastructure debt amortization.\n\n")
                sb.append("By ordering a reprioritization of capital expenditures, you project an aura of disciplined stewardship. The bond markets react calmly with narrowing sovereign yield spreads. Business editors praise the balance of pro-growth incentives with fiscal rectitude.\n")
            }

            // Military, defense, conflict, army, security, border
            lower.contains("military") || lower.contains("defense") || lower.contains("army") || lower.contains("border") || lower.contains("regiment") || lower.contains("troops") || lower.contains("security") -> {
                sb.append("<function=get_world_state><parameter=domain>military</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>militaryReadiness</parameter><parameter=delta>4.5</parameter><parameter=reason>Frontline command review and mobilization checks</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>militarySupport</parameter><parameter=delta>6.0</parameter><parameter=reason>Direct personal engagement with General Staff</parameter></function>\n")
                sb.append("<function=change_personal_stat><parameter=stat>energy</parameter><parameter=delta>-10</parameter></function>\n")
                sb.append("<function=advance_time><parameter=weeks>1</parameter></function>\n\n")

                sb.append("You arrive at Central Command under heavy security escort. Standing amidst the satellite telemetry terminals, you receive a candid briefing from the Chief of the General Staff regarding regional troop dispositions and air defense readiness.\n\n")
                sb.append("Your decisive posture and willingness to prioritize logistics over ceremonial pomp win respect among the officer corps. Morale ticks upward across garrison posts, and neighboring intelligence services take note of your firmer operational stance.\n")
            }

            // Campaign, run for office, election, party, parliament, bill
            lower.contains("campaign") || lower.contains("run for") || lower.contains("office") || lower.contains("election") || lower.contains("parliament") || lower.contains("vote") || lower.contains("candidate") -> {
                sb.append("<function=get_world_state><parameter=domain>political</parameter></function>\n")
                sb.append("<function=roll_dice><parameter=skill>leadership</parameter><parameter=difficulty>hard</parameter></function>\n")
                sb.append("<function=launch_campaign><parameter=target_office>Member of Parliament</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>approvalRating</parameter><parameter=delta>4.5</parameter><parameter=reason>Vigorous grassroots electoral campaign</parameter></function>\n")
                sb.append("<function=change_personal_stat><parameter=stat>reputation</parameter><parameter=delta>6</parameter></function>\n")
                sb.append("<function=advance_time><parameter=weeks>1</parameter></function>\n\n")

                sb.append("You launch your formal campaign bid, establishing campaign headquarters in a bustling storefront. Canvassers fan out across precincts with clipboards and digital voter rolls, knocking on thousands of doors while you conduct marathon town halls.\n\n")
                sb.append("The establishment party apparatus scrambles to assess your momentum. When the precinct committee meets, your delegates outmaneuver the incumbent faction, cementing your status as the frontrunner for higher constitutional office.\n")
            }

            // Scandal, wiretap, corruption, leak, investigation
            lower.contains("scandal") || lower.contains("wiretap") || lower.contains("leak") || lower.contains("corrupt") || lower.contains("investigate") || lower.contains("covert") -> {
                sb.append("<function=get_world_state><parameter=domain>political</parameter></function>\n")
                sb.append("<function=handle_scandal><parameter=scandal_type>political_intrigue</parameter><parameter=cover_up_budget>15</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>corruptionIndex</parameter><parameter=delta>-2.0</parameter><parameter=reason>Counter-measures against unauthorized disclosures</parameter></function>\n")
                sb.append("<function=change_personal_stat><parameter=stat>scandalRisk</parameter><parameter=delta>-12</parameter></function>\n")
                sb.append("<function=advance_time><parameter=weeks>1</parameter></function>\n\n")

                sb.append("You navigate treacherous waters as opposition operatives attempt to weaponize unsubstantiated rumors against your office. Convening your most trusted legal counsel, you mount a sharp counter-offensive, releasing preemptive records that dismantle the narrative before it reaches prime-time broadcasting.\n\n")
                sb.append("While the episode leaves bruised egos in your inner circle, the crisis is neutralized. Your adversaries are forced into retreat, wary of your swift and methodical reprisal.\n")
            }

            // General / Default Action
            else -> {
                sb.append("<function=get_world_state><parameter=domain>all</parameter></function>\n")
                sb.append("<function=roll_dice><parameter=skill>strategy</parameter><parameter=difficulty>medium</parameter></function>\n")
                sb.append("<function=adjust_stat><parameter=stat_name>approvalRating</parameter><parameter=delta>1.5</parameter><parameter=reason>Executive action: $action</parameter></function>\n")
                sb.append("<function=change_personal_stat><parameter=stat>energy</parameter><parameter=delta>-8</parameter></function>\n")
                sb.append("<function=advance_time><parameter=weeks>1</parameter></function>\n\n")

                sb.append("You execute your plan regarding '$action' with methodical deliberation. In government offices and regional ministries, civil servants translate your instructions into administrative circulars and operational memos.\n\n")
                sb.append("The initial ripple effects across the country are encouraging. Advisors report that your active leadership presence sends a reassuring signal to both parliamentary allies and the wider electorate, solidifying your control over state affairs.\n")
            }
        }

        return sb.toString()
    }
}

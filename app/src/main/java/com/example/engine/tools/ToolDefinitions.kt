package com.example.engine.tools

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>,
    val domain: String = "general"
)

data class ToolParameter(
    val name: String,
    val type: String, // "string", "number", "boolean"
    val description: String,
    val required: Boolean = true
)

object ToolCatalog {
    val tools: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "get_world_state",
            description = "Returns the current state variables for a specific domain.",
            parameters = listOf(
                ToolParameter("domain", "string", "The domain to query: 'political', 'economic', 'social', 'military', 'personal', 'factions', or 'time'")
            ),
            domain = "core"
        ),
        ToolDefinition(
            name = "adjust_stat",
            description = "Changes a numeric stat in the national or social indicators.",
            parameters = listOf(
                ToolParameter("stat_name", "string", "The name of the stat (e.g., 'approvalRating', 'gdpGrowth', 'inflationRate', 'unemploymentRate', 'publicMood', 'protestIntensity', 'corruptionIndex', 'treasuryBillions')"),
                ToolParameter("delta", "number", "The change to apply (positive or negative number)"),
                ToolParameter("reason", "string", "Brief justification for why this stat changed")
            ),
            domain = "core"
        ),
        ToolDefinition(
            name = "create_event",
            description = "Injects a narrative crisis, opportunity, or world event.",
            parameters = listOf(
                ToolParameter("type", "string", "Event category: 'crisis', 'opportunity', 'headline', 'diplomatic', 'domestic'"),
                ToolParameter("description", "string", "Summary of the event that occurred"),
                ToolParameter("severity", "string", "Severity level: 'INFO', 'SUCCESS', 'WARNING', 'CRITICAL'")
            ),
            domain = "core"
        ),
        ToolDefinition(
            name = "roll_dice",
            description = "Resolves an uncertain personal or political action based on difficulty and player attributes.",
            parameters = listOf(
                ToolParameter("skill", "string", "Skill tested: 'oratory', 'negotiation', 'leadership', 'subterfuge', 'economics'"),
                ToolParameter("difficulty", "string", "Difficulty class: 'easy', 'medium', 'hard', 'extreme'")
            ),
            domain = "core"
        ),
        ToolDefinition(
            name = "advance_time",
            description = "Advances the simulation calendar by a number of weeks.",
            parameters = listOf(
                ToolParameter("weeks", "number", "Number of weeks to advance (normally 1)")
            ),
            domain = "core"
        ),
        ToolDefinition(
            name = "get_personal_status",
            description = "Returns player's personal condition: age, health, wealth, family, reputation, and energy.",
            parameters = emptyList(),
            domain = "personal"
        ),
        ToolDefinition(
            name = "change_personal_stat",
            description = "Mutates player's personal stats such as health, wealth, energy, or scandal risk.",
            parameters = listOf(
                ToolParameter("stat", "string", "Stat: 'health', 'wealthThousands', 'reputation', 'scandalRisk', 'energy'"),
                ToolParameter("delta", "number", "Delta value (+ or -)")
            ),
            domain = "personal"
        ),
        ToolDefinition(
            name = "check_faction_relations",
            description = "Returns loyalty and support percentages from key power blocs (military, business, unions, media, civil service).",
            parameters = emptyList(),
            domain = "political"
        ),
        ToolDefinition(
            name = "propose_policy",
            description = "Proposes a new legislative policy or government directive.",
            parameters = listOf(
                ToolParameter("policy_id", "string", "Unique policy identifier"),
                ToolParameter("budget_cost", "number", "Annual cost or revenue impact in billions")
            ),
            domain = "political"
        ),
        ToolDefinition(
            name = "call_vote",
            description = "Simulates a parliamentary vote on a designated policy based on seats and coalition mood.",
            parameters = listOf(
                ToolParameter("policy_id", "string", "Policy ID to vote on")
            ),
            domain = "political"
        ),
        ToolDefinition(
            name = "launch_campaign",
            description = "Starts an electoral campaign cycle for an elected position.",
            parameters = listOf(
                ToolParameter("target_office", "string", "Target office: 'City Councillor', 'Mayor', 'Member of Parliament', 'Party Leader', 'Prime Minister'")
            ),
            domain = "political"
        ),
        ToolDefinition(
            name = "handle_scandal",
            description = "Attempts to manage, suppress, or apologize for a political or personal scandal.",
            parameters = listOf(
                ToolParameter("scandal_type", "string", "Type of scandal: 'financial_misconduct', 'leaked_audio', 'affair', 'conflict_of_interest'"),
                ToolParameter("cover_up_budget", "number", "Budget spent in thousands to manage or contain (0 for transparent apology)")
            ),
            domain = "political"
        ),
        ToolDefinition(
            name = "reshuffle_cabinet",
            description = "Reorganizes ministerial portfolios to align with policy priorities.",
            parameters = listOf(
                ToolParameter("focus", "string", "Strategic focus: 'economic_growth', 'law_and_order', 'social_welfare', 'technocratic_reform'")
            ),
            domain = "political"
        ),
        ToolDefinition(
            name = "diplomatic_demarche",
            description = "Conducts foreign diplomacy: sanctions, treaties, summits, or border postures.",
            parameters = listOf(
                ToolParameter("country", "string", "Target nation name or regional bloc"),
                ToolParameter("stance", "string", "Diplomatic stance: 'conciliatory', 'firm_warning', 'trade_pact', 'expel_ambassador'")
            ),
            domain = "foreign"
        ),
        ToolDefinition(
            name = "authorise_operation",
            description = "Authorizes a targeted state operation: intelligence surveillance, anti-corruption sweep, or strike task force.",
            parameters = listOf(
                ToolParameter("operation_type", "string", "Type: 'anti_corruption_audit', 'wiretap_opposition', 'border_reinforcement', 'subsidize_critical_industry'"),
                ToolParameter("budget", "number", "Cost in millions")
            ),
            domain = "security"
        )
    )

    fun getSchemaPrompt(): String {
        val sb = StringBuilder()
        sb.append("AVAILABLE TOOLS (Call using <function=tool_name><parameter=key>value</parameter></function>):\n")
        tools.forEach { tool ->
            sb.append("- ").append(tool.name).append(": ").append(tool.description).append("\n")
            if (tool.parameters.isNotEmpty()) {
                val paramsStr = tool.parameters.joinToString(", ") { "${it.name} (${it.type}): ${it.description}" }
                sb.append("  Parameters: ").append(paramsStr).append("\n")
            }
        }
        return sb.toString()
    }
}

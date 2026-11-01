package com.example.engine.orchestrator

import com.example.data.db.ChronicleDao
import com.example.data.db.PolicyDao
import com.example.data.db.WorldStateDao
import com.example.data.model.ChronicleEntry
import com.example.data.model.WorldState
import com.example.engine.llm.EngineStatus
import com.example.engine.llm.Lfm3Engine
import com.example.engine.parser.ParsedToolCall
import com.example.engine.parser.ToolCall
import com.example.engine.parser.ToolCallParseException
import com.example.engine.parser.ToolCallParser
import com.example.engine.prompt.PromptTemplates
import com.example.engine.tools.ToolCatalog
import com.example.engine.tools.ToolExecutionReport
import com.example.engine.tools.ToolExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed interface TurnState {
    object Idle : TurnState
    data class Generating(val streamedText: String) : TurnState
    data class ExecutingTools(val toolCount: Int, val completed: Int, val currentTool: String) : TurnState
    data class Completed(val narrative: String, val toolReports: List<ToolExecutionReport>) : TurnState
    data class Error(val message: String) : TurnState
}

data class TurnResult(
    val narrative: String,
    val toolResults: List<ToolExecutionReport>
)

class GameOrchestrator(
    private val worldStateDao: WorldStateDao,
    private val chronicleDao: ChronicleDao,
    private val policyDao: PolicyDao,
    modelDir: File
) {
    private val toolExecutor = ToolExecutor(worldStateDao, chronicleDao, policyDao)
    private val lfm3Engine = Lfm3Engine(modelDir)

    private val _turnState = MutableStateFlow<TurnState>(TurnState.Idle)
    val turnState: StateFlow<TurnState> = _turnState.asStateFlow()

    val engineStatus: EngineStatus
        get() = EngineStatus(
            modelName = "Liquid AI LFM3-1.7B (q4f16 ONNX)",
            isModelLoaded = lfm3Engine != null,
            backendInfo = "On-Device Neural Tri-Hybrid (arm64-v8a GenAI)"
        )

    suspend fun executePlayerAction(action: String): TurnResult {
        if (_turnState.value is TurnState.Generating || _turnState.value is TurnState.ExecutingTools) {
            return TurnResult("Engine busy", emptyList())
        }

        try {
            val currentState = worldStateDao.getWorldState() ?: WorldState()

            // 1. Log player action
            chronicleDao.insert(
                ChronicleEntry(
                    week = currentState.currentWeek,
                    year = currentState.currentYear,
                    category = "ACTION",
                    title = "Directive Issued",
                    details = action.ifBlank { "Advance time (Observe and consolidate)" },
                    severity = "INFO"
                )
            )

            // 2. Build ChatML prompt with summarized recent history
            val recentEntries = chronicleDao.getRecentEntries(4)
            val historySummary = if (recentEntries.isNotEmpty()) {
                recentEntries.reversed().joinToString("\n") { "· [Wk ${it.week}] ${it.title}: ${it.details.take(90)}" }
            } else {
                "Beginning of simulation. Year ${currentState.currentYear}, Week ${currentState.currentWeek}."
            }

            val prompt = PromptTemplates.buildUserTurn(historySummary, action.ifBlank { "Observe state affairs and advance one week." })
            _turnState.value = TurnState.Generating("Referee analyzing political consequences...")

            // 3. Generate raw model response
            var rawOutput = lfm3Engine.generate(prompt, maxTokens = 512, temperature = 0.1)

            _turnState.value = TurnState.Generating(rawOutput)

            // 4. Parse XML Tool Calls with retry on failure
            var toolCalls: List<ToolCall> = emptyList()
            try {
                toolCalls = ToolCallParser.parse(rawOutput)
            } catch (e: ToolCallParseException) {
                // Corrective system retry with temperature 0.0
                val correctivePrompt = PromptTemplates.buildCorrectivePrompt(rawOutput, e.message ?: "Invalid tool syntax")
                rawOutput = lfm3Engine.generate(correctivePrompt, maxTokens = 512, temperature = 0.0)
                try {
                    toolCalls = ToolCallParser.parse(rawOutput)
                } catch (_: Exception) {
                    toolCalls = emptyList()
                }
            }

            // 5. Execute each tool deterministically against Room SQLite
            val toolReports = mutableListOf<ToolExecutionReport>()
            val totalTools = toolCalls.size

            if (totalTools > 0) {
                _turnState.value = TurnState.ExecutingTools(totalTools, 0, toolCalls.first().name)
                for ((idx, tool) in toolCalls.withIndex()) {
                    _turnState.value = TurnState.ExecutingTools(totalTools, idx, tool.name)
                    val parsed = ParsedToolCall(tool.name, tool.args, tool.rawBlock)
                    val report = toolExecutor.execute(parsed)
                    toolReports.add(report)

                    chronicleDao.insert(
                        ChronicleEntry(
                            week = currentState.currentWeek,
                            year = currentState.currentYear,
                            category = "TOOL_CALL",
                            title = "Tool: <function=${tool.name}>",
                            details = tool.args.entries.joinToString(", ") { "${it.key}=${it.value}" } + "\nResult: ${report.resultString}",
                            severity = if (report.isSuccess) "INFO" else "WARNING"
                        )
                    )
                }
            }

            // 6. Extract clean narrative
            val cleanNarrative = ToolCallParser.extractNarrative(rawOutput).ifBlank {
                "You deliberate on the state of the nation and review incoming advisor dispatches as your directives take effect across the republic."
            }

            val updatedState = worldStateDao.getWorldState() ?: currentState
            worldStateDao.updateWorldState(
                updatedState.copy(latestNarrative = cleanNarrative)
            )

            chronicleDao.insert(
                ChronicleEntry(
                    week = updatedState.currentWeek,
                    year = updatedState.currentYear,
                    category = "NARRATIVE",
                    title = "Weekly Intelligence Briefing",
                    details = cleanNarrative,
                    severity = "INFO"
                )
            )

            _turnState.value = TurnState.Completed(cleanNarrative, toolReports)
            return TurnResult(cleanNarrative, toolReports)
        } catch (e: Exception) {
            _turnState.value = TurnState.Error(e.message ?: "Simulation turn error.")
            return TurnResult("Simulation turn error: ${e.message}", emptyList())
        }
    }

    fun resetTurnState() {
        _turnState.value = TurnState.Idle
    }
}

package com.example.engine.prompt

import com.example.engine.tools.ToolCatalog

object PromptTemplates {

    val TOOL_SCHEMAS_JSON: String = ToolCatalog.getSchemaPrompt()

    val SYSTEM_PROMPT: String = """
<|im_start|>system
You are the game master of a political life simulator called Sovereign.
Rules:
1. Never invent numeric values. Call get_world_state before narrating stat-dependent consequences.
2. For uncertain actions, call roll_dice.
3. Use create_event for crises and opportunities.
4. After tool calls, write 2–4 paragraphs of consequence narrative in second person ("You...").
5. End every response with the current game date obtained via get_world_state("time").
Available tools:
$TOOL_SCHEMAS_JSON
<|im_end|>
    """.trimIndent()

    fun buildUserTurn(recentHistorySummary: String, playerAction: String): String {
        return """
$SYSTEM_PROMPT
<|im_start|>user
[RECENT HISTORY SUMMARY]
$recentHistorySummary

Player action: $playerAction
<|im_end|>
<|im_start|>assistant
        """.trimIndent()
    }

    fun buildCorrectivePrompt(malformedOutput: String, errorReason: String): String {
        return """
<|im_start|>system
Your previous tool call was malformed: $errorReason.
Use strictly:
<tool_call>
<function=NAME>
  <parameter=KEY>VALUE</parameter>
</function>
</tool_call>
Followed by the consequence narrative. Re-emit the correct tool call now.
<|im_end|>
<|im_start|>assistant
        """.trimIndent()
    }
}

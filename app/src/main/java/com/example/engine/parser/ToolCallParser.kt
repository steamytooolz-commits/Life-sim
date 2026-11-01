package com.example.engine.parser

data class ToolCall(
    val name: String,
    val args: Map<String, String>,
    val rawBlock: String
)

class ToolCallParseException(
    message: String,
    val rawText: String,
    cause: Throwable? = null
) : Exception(message, cause)

object ToolCallParser {

    private val toolCallBlockRegex = Regex(
        """<tool_call>(.*?)</tool_call>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    private val functionRegex = Regex(
        """<function\s*=\s*["']?([a-zA-Z0-9_\-]+)["']?\s*>(.*?)</function>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    private val parameterRegex = Regex(
        """<parameter\s*(?:=\s*["']?([a-zA-Z0-9_\-]+)["']?|\s+name\s*=\s*["']?([a-zA-Z0-9_\-]+)["']?)\s*>(.*?)</parameter>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    @Throws(ToolCallParseException::class)
    fun parse(rawOutput: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()

        // Check for malformed opening tags without closing tags
        if (rawOutput.contains("<tool_call", ignoreCase = true) && !rawOutput.contains("</tool_call>", ignoreCase = true)) {
            throw ToolCallParseException("Missing closing </tool_call> tag", rawOutput)
        }
        if (rawOutput.contains("<function", ignoreCase = true) && !rawOutput.contains("</function>", ignoreCase = true)) {
            throw ToolCallParseException("Missing closing </function> tag", rawOutput)
        }

        // 1. Extract from <tool_call> blocks if present
        val toolCallBlocks = toolCallBlockRegex.findAll(rawOutput).map { it.groupValues[1] }.toList()
        val searchScopes = if (toolCallBlocks.isNotEmpty()) toolCallBlocks else listOf(rawOutput)

        for (scope in searchScopes) {
            val funcMatches = functionRegex.findAll(scope).toList()
            for (match in funcMatches) {
                val functionName = match.groupValues[1].trim()
                val body = match.groupValues[2].trim()

                if (functionName.isEmpty()) {
                    throw ToolCallParseException("Function name is empty", match.value)
                }

                if (body.contains("<parameter", ignoreCase = true) && !body.contains("</parameter>", ignoreCase = true)) {
                    throw ToolCallParseException("Missing closing </parameter> tag inside function '$functionName'", body)
                }

                val args = mutableMapOf<String, String>()
                val paramMatches = parameterRegex.findAll(body)
                for (pMatch in paramMatches) {
                    val key = if (pMatch.groupValues[1].isNotEmpty()) pMatch.groupValues[1] else pMatch.groupValues[2]
                    val value = pMatch.groupValues[3].trim()
                    args[key.trim()] = value
                }

                toolCalls.add(
                    ToolCall(
                        name = functionName,
                        args = args,
                        rawBlock = match.value
                    )
                )
            }
        }

        return toolCalls
    }

    /**
     * Extracts pure consequence narrative by stripping all <tool_call> and <function> tags
     */
    fun extractNarrative(rawOutput: String): String {
        var clean = rawOutput.replace(toolCallBlockRegex, "")
        clean = clean.replace(functionRegex, "")
        clean = clean.replace(Regex("""</?(?:tool_call|function|parameter)[^>]*>""", RegexOption.IGNORE_CASE), "")
        // Clean ChatML tokens if present
        clean = clean.replace("<|im_start|>", "").replace("<|im_end|>", "").replace("assistant", "")
        return clean.trim()
    }
}

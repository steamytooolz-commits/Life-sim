package com.example.engine.parser

data class ParsedToolCall(
    val name: String,
    val parameters: Map<String, String>,
    val rawText: String
)

data class ToolParseResult(
    val toolCalls: List<ParsedToolCall>,
    val narrative: String,
    val errors: List<String> = emptyList(),
    val isSuccess: Boolean = errors.isEmpty()
)

object XmlToolParser {
    // Matches <function=NAME> ... </function> or <function="NAME"> ... </function>
    private val functionRegex = Regex(
        """<function\s*=\s*["']?([a-zA-Z0-9_\-]+)["']?\s*>(.*?)</function>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    // Matches <parameter=KEY>VALUE</parameter> or <parameter="KEY">VALUE</parameter>
    // also supports <parameter\s+name=["']KEY["']>VALUE</parameter>
    private val parameterRegex = Regex(
        """<parameter\s*(?:=\s*["']?([a-zA-Z0-9_\-]+)["']?|\s+name\s*=\s*["']?([a-zA-Z0-9_\-]+)["']?)\s*>(.*?)</parameter>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    // Detect unclosed function tag: <function=... without matching </function>
    private val unclosedFunctionRegex = Regex(
        """<function\s*=\s*["']?([a-zA-Z0-9_\-]+)["']?[^>]*>(?!.*?</function>)""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    fun parse(rawOutput: String): ToolParseResult {
        val errors = mutableListOf<String>()
        val toolCalls = mutableListOf<ParsedToolCall>()

        // Check for suspicious unclosed function tags
        if (rawOutput.contains("<function", ignoreCase = true) && !rawOutput.contains("</function>", ignoreCase = true)) {
            errors.add("Malformed tool call: missing closing </function> tag.")
        }

        val functionMatches = functionRegex.findAll(rawOutput).toList()

        for (match in functionMatches) {
            val functionName = match.groupValues[1].trim()
            val innerBody = match.groupValues[2].trim()

            // Check if there's unclosed parameter in innerBody
            if (innerBody.contains("<parameter", ignoreCase = true) && !innerBody.contains("</parameter>", ignoreCase = true)) {
                errors.add("Malformed parameter in function '$functionName': missing closing </parameter> tag.")
            }

            val paramMap = mutableMapOf<String, String>()
            val paramMatches = parameterRegex.findAll(innerBody)
            for (pMatch in paramMatches) {
                val key = if (pMatch.groupValues[1].isNotEmpty()) pMatch.groupValues[1] else pMatch.groupValues[2]
                val value = pMatch.groupValues[3].trim()
                paramMap[key.trim()] = value
            }

            if (functionName.isEmpty()) {
                errors.add("Malformed tool call: empty function name.")
            } else {
                toolCalls.add(
                    ParsedToolCall(
                        name = functionName,
                        parameters = paramMap,
                        rawText = match.value
                    )
                )
            }
        }

        // Clean narrative: remove all <function=...>...</function> blocks
        var cleanedNarrative = rawOutput.replace(functionRegex, "").trim()
        
        // Remove any orphan opening or closing tags if left over
        cleanedNarrative = cleanedNarrative.replace(Regex("""</?function[^>]*>""", RegexOption.IGNORE_CASE), "").trim()
        cleanedNarrative = cleanedNarrative.replace(Regex("""</?parameter[^>]*>""", RegexOption.IGNORE_CASE), "").trim()

        return ToolParseResult(
            toolCalls = toolCalls,
            narrative = cleanedNarrative,
            errors = errors,
            isSuccess = errors.isEmpty()
        )
    }

    /**
     * Formats a tool call into the LFM3 XML syntax
     */
    fun formatCall(functionName: String, params: Map<String, Any>): String {
        val sb = StringBuilder()
        sb.append("<function=").append(functionName).append(">\n")
        params.forEach { (key, value) ->
            sb.append("  <parameter=").append(key).append(">")
                .append(value.toString())
                .append("</parameter>\n")
        }
        sb.append("</function>")
        return sb.toString()
    }
}

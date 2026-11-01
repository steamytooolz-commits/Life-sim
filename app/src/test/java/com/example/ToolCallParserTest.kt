package com.example

import com.example.engine.parser.ToolCallParseException
import com.example.engine.parser.ToolCallParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ToolCallParserTest {

    @Test
    fun testToolCallBlockParsing() {
        val raw = """
            <tool_call>
            <function=get_world_state>
              <parameter=domain>economy</parameter>
            </function>
            </tool_call>
            The national economy exhibits mild inflationary pressure.
        """.trimIndent()

        val calls = ToolCallParser.parse(raw)
        assertEquals(1, calls.size)
        assertEquals("get_world_state", calls[0].name)
        assertEquals("economy", calls[0].args["domain"])

        val narrative = ToolCallParser.extractNarrative(raw)
        assertTrue(narrative.contains("The national economy exhibits mild inflationary pressure."))
        assertFalse(narrative.contains("<tool_call>"))
    }

    @Test
    fun testMultipleToolCallsInsideBlock() {
        val raw = """
            <tool_call>
            <function=roll_dice><parameter=skill>negotiation</parameter><parameter=difficulty>medium</parameter></function>
            <function=adjust_stat><parameter=stat>approvalRating</parameter><parameter=delta>2.5</parameter></function>
            </tool_call>
            You concluded negotiations with regional governors.
        """.trimIndent()

        val calls = ToolCallParser.parse(raw)
        assertEquals(2, calls.size)
        assertEquals("roll_dice", calls[0].name)
        assertEquals("negotiation", calls[0].args["skill"])
        assertEquals("adjust_stat", calls[1].name)
        assertEquals("2.5", calls[1].args["delta"])
    }

    @Test
    fun testMalformedMissingClosingTagThrowsException() {
        val raw = """
            <tool_call>
            <function=adjust_stat>
              <parameter=stat>approvalRating</parameter>
              <parameter=delta>1.0</parameter>
            <!-- Missing closing function and tool_call tags -->
        """.trimIndent()

        try {
            ToolCallParser.parse(raw)
            fail("Expected ToolCallParseException")
        } catch (e: ToolCallParseException) {
            assertTrue(e.message?.contains("Missing closing") == true)
        }
    }
}

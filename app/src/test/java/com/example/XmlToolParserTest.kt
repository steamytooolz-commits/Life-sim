package com.example

import com.example.engine.parser.XmlToolParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XmlToolParserTest {

    @Test
    fun testValidSingleToolCall() {
        val raw = """
            <function=adjust_stat>
              <parameter=stat_name>approvalRating</parameter>
              <parameter=delta>3.5</parameter>
              <parameter=reason>Successful speech</parameter>
            </function>
            You delivered an impassioned address before the crowd.
        """.trimIndent()

        val result = XmlToolParser.parse(raw)

        assertTrue(result.isSuccess)
        assertEquals(1, result.toolCalls.size)
        val call = result.toolCalls[0]
        assertEquals("adjust_stat", call.name)
        assertEquals("approvalRating", call.parameters["stat_name"])
        assertEquals("3.5", call.parameters["delta"])
        assertEquals("Successful speech", call.parameters["reason"])
        assertTrue(result.narrative.contains("You delivered an impassioned address"))
        assertFalse(result.narrative.contains("<function"))
    }

    @Test
    fun testMultipleToolCallsInterspersedWithNarrative() {
        val raw = """
            <function=get_world_state><parameter=domain>political</parameter></function>
            <function=roll_dice><parameter=skill>oratory</parameter><parameter=difficulty>medium</parameter></function>
            <function=change_personal_stat><parameter=stat>energy</parameter><parameter=delta>-10</parameter></function>
            
            You stand before the cameras. Polling in the northern provinces begins to shift in your favor.
        """.trimIndent()

        val result = XmlToolParser.parse(raw)

        assertTrue(result.isSuccess)
        assertEquals(3, result.toolCalls.size)
        assertEquals("get_world_state", result.toolCalls[0].name)
        assertEquals("political", result.toolCalls[0].parameters["domain"])

        assertEquals("roll_dice", result.toolCalls[1].name)
        assertEquals("oratory", result.toolCalls[1].parameters["skill"])
        assertEquals("medium", result.toolCalls[1].parameters["difficulty"])

        assertEquals("change_personal_stat", result.toolCalls[2].name)
        assertEquals("energy", result.toolCalls[2].parameters["stat"])
        assertEquals("-10", result.toolCalls[2].parameters["delta"])

        assertTrue(result.narrative.contains("You stand before the cameras"))
    }

    @Test
    fun testMalformedUnclosedFunction() {
        val raw = """
            <function=adjust_stat>
              <parameter=stat_name>approvalRating</parameter>
              <parameter=delta>2.0</parameter>
            You forgot the closing tag.
        """.trimIndent()

        val result = XmlToolParser.parse(raw)

        assertFalse(result.isSuccess)
        assertTrue(result.errors.any { it.contains("missing closing </function>") })
    }

    @Test
    fun testFormatCallGeneratesValidLfm3Xml() {
        val formatted = XmlToolParser.formatCall(
            "roll_dice",
            mapOf("skill" to "negotiation", "difficulty" to "hard")
        )

        assertTrue(formatted.startsWith("<function=roll_dice>"))
        assertTrue(formatted.contains("<parameter=skill>negotiation</parameter>"))
        assertTrue(formatted.contains("<parameter=difficulty>hard</parameter>"))
        assertTrue(formatted.endsWith("</function>"))

        // Roundtrip parse test
        val roundtrip = XmlToolParser.parse(formatted)
        assertTrue(roundtrip.isSuccess)
        assertEquals(1, roundtrip.toolCalls.size)
        assertEquals("negotiation", roundtrip.toolCalls[0].parameters["skill"])
    }
}

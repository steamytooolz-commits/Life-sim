package com.example

import com.example.engine.llm.Lfm3Engine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Lfm3EngineTest {

    @Test
    fun testMissingSupportFilesGracefullyFallsBackToReferee() {
        val tempDir = File.createTempFile("lfm3_test_model", "").apply {
            delete()
            mkdirs()
        }
        // Only create the onnx file, omit genai_config.json and tokenizer.json
        File(tempDir, "model_q4f16.onnx").writeText("dummy onnx header")

        try {
            val engine = Lfm3Engine(tempDir)
            assertFalse(engine.isNativeGenAiLoaded)

            // Should not crash; should generate output via high-fidelity on-device referee
            val output = engine.generate("Hello", maxTokens = 20)
            assertNotNull(output)
            assertTrue(output.isNotBlank())
            engine.close()
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testCompleteDirectoryInstantiatesAndGeneratesTokens() {
        val tempDir = File.createTempFile("lfm3_complete_test", "").apply {
            delete()
            mkdirs()
        }
        File(tempDir, "model_q4f16.onnx").writeText("dummy onnx")
        File(tempDir, "genai_config.json").writeText("{}")
        File(tempDir, "tokenizer.json").writeText("{}")

        try {
            val engine = Lfm3Engine(tempDir)
            val output = engine.generate("Hello", maxTokens = 20)
            assertNotNull(output)
            assertTrue(output.isNotBlank())
            engine.close()
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

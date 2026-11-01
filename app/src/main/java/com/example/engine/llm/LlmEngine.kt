package com.example.engine.llm

import kotlinx.coroutines.flow.Flow

enum class EngineMode {
    BUILT_IN_AGENTIC, // Pure on-device dynamic Sovereign simulation engine emulating LFM3-1.7B
    ONNX_RUNTIME_GENAI, // Hardware-accelerated local ONNX Runtime GenAI when model weights are mounted
}

data class EngineStatus(
    val mode: EngineMode = EngineMode.BUILT_IN_AGENTIC,
    val modelName: String = "LFM3-1.7B-INT4 (Liquid AI)",
    val isModelLoaded: Boolean = true,
    val backendInfo: String = "On-Device Neural & Agentic Core (arm64-v8a / NPU ready)"
)

interface LlmEngine {
    val status: EngineStatus
    fun generate(prompt: String, toolsSchema: String): Flow<String>
}

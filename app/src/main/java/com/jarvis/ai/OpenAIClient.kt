package com.jarvis.ai

/**
 * Thin abstraction for JARVIS' AI backend.
 *
 * The Android app must never contain a production OpenAI API key. Point this
 * client at a small authenticated server that calls the OpenAI Responses API.
 * The server can then expose a stable /v1/jarvis/chat endpoint to the app.
 */
class OpenAIClient(private val baseUrl: String) {
    // Backend integration is intentionally isolated here so the UI and voice
    // layers do not need to know provider details.
    fun endpoint(): String = baseUrl.trimEnd('/') + "/v1/jarvis/chat"
}

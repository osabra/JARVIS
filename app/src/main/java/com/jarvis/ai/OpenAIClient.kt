package com.jarvis.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenAIClient(private val baseUrl: String) {
    data class Reply(val id: String, val text: String)

    suspend fun chat(messages: List<Pair<String, String>>, previousResponseId: String? = null): Result<Reply> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject()
                .put("messages", JSONArray().apply {
                    messages.takeLast(30).forEach { (role, content) ->
                        put(JSONObject().put("role", role).put("content", content))
                    }
                })
            if (!previousResponseId.isNullOrBlank()) payload.put("previous_response_id", previousResponseId)

            val connection = (URL(endpoint()).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
            try {
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) throw IllegalStateException("JARVIS backend HTTP $code: $body")
                val json = JSONObject(body)
                Reply(json.optString("id"), json.optString("text").ifBlank { "No tengo una respuesta en este momento." })
            } finally {
                connection.disconnect()
            }
        }
    }

    fun endpoint(): String = baseUrl.trimEnd('/') + "/v1/jarvis/chat"
}

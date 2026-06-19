package com.example.smartcarcontrol.data.ai

import com.example.smartcarcontrol.BuildConfig
import com.example.smartcarcontrol.domain.model.CarCommand
import com.example.smartcarcontrol.domain.repository.IAIRepository
import com.example.smartcarcontrol.util.Constants
import javax.inject.Inject

class AIRepositoryImpl @Inject constructor(
    private val geminiApiService: GeminiApiService
) : IAIRepository {

    override suspend fun processVoiceCommand(text: String): CarCommand {
        repeat(3) { attempt ->
            try {
                val prompt = buildPrompt(text)
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(GeminiRequest.Part(prompt))
                        )
                    )
                )
                val response = geminiApiService.generateContent(
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = request
                )

                val rawText = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                android.util.Log.d("AIRepository", "Raw Gemini response: '$rawText'")

                return parseResponse(response)

            } catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    429 -> {
                        val errorBody = e.response()?.errorBody()?.string()
                        android.util.Log.w("AIRepository", "Rate limited on attempt ${attempt + 1}/3")
                        // Wait longer between retries
                        kotlinx.coroutines.delay(5000L * (attempt + 1)) // 5s, 10s, 15s
                    }
                    else -> {
                        android.util.Log.e("AIRepository", "HTTP Error ${e.code()}: ${e.message()}")
                        return CarCommand.UNKNOWN
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AIRepository", "Error calling Gemini: ${e.message}", e)
                return CarCommand.UNKNOWN
            }
        }
        android.util.Log.e("AIRepository", "All retry attempts failed")
        return CarCommand.UNKNOWN
    }

    private fun buildPrompt(userText: String): String {
        return """
            You are a smart car assistant. The user said: "$userText"
            
            Match this to exactly ONE of these commands and respond with ONLY the command name, nothing else:
            - UNLOCK_DOOR
            - LOCK_DOOR
            - OPEN_WINDOW
            - CLOSE_WINDOW
            - OPEN_TRUNK
            - CLOSE_TRUNK
            - UNKNOWN (if nothing matches)
            
            Response must be a single word from the list above. No explanation.
        """.trimIndent()
    }

    private fun parseResponse(response: GeminiResponse): CarCommand {
        val text = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.trim()
            ?.uppercase()
            ?: return CarCommand.UNKNOWN

        return try {
            CarCommand.valueOf(text)
        } catch (e: IllegalArgumentException) {
            CarCommand.UNKNOWN
        }
    }
}
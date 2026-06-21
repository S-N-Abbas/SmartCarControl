package com.example.smartcarcontrol.data.ai

import com.example.smartcarcontrol.BuildConfig
import com.example.smartcarcontrol.domain.model.CarCommand
import com.example.smartcarcontrol.domain.model.GeminiResult
import com.example.smartcarcontrol.domain.repository.IAIRepository
import javax.inject.Inject

class AIRepositoryImpl @Inject constructor(
    private val geminiApiService: GeminiApiService
) : IAIRepository {

    override suspend fun processInput(text: String): GeminiResult {
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
                    ?.trim()

                android.util.Log.d("AIRepository", "Raw Gemini response: '$rawText'")
                return parseResponse(rawText)

            } catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    429 -> {
                        val errorBody = e.response()?.errorBody()?.string()
                        android.util.Log.w("AIRepository", "Rate limited. Body: $errorBody. Attempt ${attempt + 1}/3")
                        kotlinx.coroutines.delay(5000L * (attempt + 1))
                    }
                    else -> {
                        android.util.Log.e("AIRepository", "HTTP Error ${e.code()}: ${e.message()}")
                        return GeminiResult.ChatReply("Sorry, I couldn't connect to AI service.")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AIRepository", "Error calling Gemini: ${e.message}", e)
                return GeminiResult.ChatReply("Sorry, something went wrong.")
            }
        }
        return GeminiResult.ChatReply("Service unavailable. Please try again.")
    }

    private fun buildPrompt(userText: String): String {
        return """
            You are SmartCar AI Assistant built into a car control app.
            The user said: "$userText"
            
            First determine if this is a CAR COMMAND or a CHAT MESSAGE.
            
            CAR COMMANDS are requests to control the car physically:
            - Anything about locking/unlocking doors
            - Anything about windows (open/close/roll up/roll down)
            - Anything about trunk/boot (open/close/pop)
            
            CHAT MESSAGES are everything else:
            - Questions (time, date, weather, math, science, general knowledge)
            - Greetings and conversation
            - Jokes, stories
            - Anything not related to physical car control
            
            If it is a CAR COMMAND, respond with ONLY one of these exact words:
            UNLOCK_DOOR | LOCK_DOOR | OPEN_WINDOW | CLOSE_WINDOW | OPEN_TRUNK | CLOSE_TRUNK
            
            If it is a CHAT MESSAGE, respond with:
            CHAT: followed by your helpful, concise response (max 2-3 sentences)
            
            Examples:
            "unlock my car" → UNLOCK_DOOR
            "what time is it" → CHAT: I don't have access to real-time clock, but you can check your phone's status bar for the current time.
            "what is 25 times 4" → CHAT: 25 times 4 equals 100.
            "tell me a joke" → CHAT: Why don't scientists trust atoms? Because they make up everything!
            "open the windows" → OPEN_WINDOW
            "what is the speed of light" → CHAT: The speed of light is approximately 299,792 kilometers per second in a vacuum.
        """.trimIndent()
    }

    private fun parseResponse(rawText: String?): GeminiResult {
        if (rawText.isNullOrBlank()) {
            return GeminiResult.ChatReply("I didn't get a response. Please try again.")
        }

        // Check if it's a chat response
        if (rawText.startsWith("CHAT:", ignoreCase = true)) {
            val message = rawText.removePrefix("CHAT:").trim()
            return GeminiResult.ChatReply(message)
        }

        // Try to match a car command
        return try {
            val command = CarCommand.valueOf(rawText.uppercase())
            GeminiResult.Command(command)
        } catch (e: IllegalArgumentException) {
            // Gemini returned something else — treat as chat
            GeminiResult.ChatReply(rawText)
        }
    }
}
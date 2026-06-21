package com.example.smartcarcontrol.domain.model

sealed class GeminiResult {
    data class Command(val carCommand: CarCommand) : GeminiResult()
    data class ChatReply(val message: String) : GeminiResult()
}
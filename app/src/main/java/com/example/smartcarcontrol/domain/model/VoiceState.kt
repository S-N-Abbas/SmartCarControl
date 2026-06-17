package com.example.smartcarcontrol.domain.model

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Recognized(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}
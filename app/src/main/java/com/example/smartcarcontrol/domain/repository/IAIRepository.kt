package com.example.smartcarcontrol.domain.repository

import com.example.smartcarcontrol.domain.model.CarCommand

interface IAIRepository {
    suspend fun processVoiceCommand(text: String): CarCommand
}
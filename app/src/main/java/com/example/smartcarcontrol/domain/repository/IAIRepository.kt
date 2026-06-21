package com.example.smartcarcontrol.domain.repository

import com.example.smartcarcontrol.domain.model.GeminiResult

interface IAIRepository {
    suspend fun processInput(text: String): GeminiResult
}
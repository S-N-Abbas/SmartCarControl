package com.example.smartcarcontrol.domain.usecase

import com.example.smartcarcontrol.domain.model.CarCommand
import com.example.smartcarcontrol.domain.repository.IAIRepository
import javax.inject.Inject

class ProcessVoiceCommandUseCase @Inject constructor(
    private val aiRepository: IAIRepository
) {
    suspend operator fun invoke(text: String): CarCommand {
        return aiRepository.processVoiceCommand(text)
    }
}
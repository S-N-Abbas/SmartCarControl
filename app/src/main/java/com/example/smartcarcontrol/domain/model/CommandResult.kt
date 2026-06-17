package com.example.smartcarcontrol.domain.model

data class CommandResult(
    val command: CarCommand,
    val success: Boolean,
    val message: String
)
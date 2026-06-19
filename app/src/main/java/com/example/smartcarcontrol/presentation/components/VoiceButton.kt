package com.example.smartcarcontrol.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.example.smartcarcontrol.domain.model.VoiceState

@Composable
fun VoiceButton(
    voiceState: VoiceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = voiceState is VoiceState.Listening

    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = if (isListening) infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ) else tween(300),
        label = "voiceButtonScale"
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(72.dp)
            .scale(scale)
            .then(
                if (isListening) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                ) else Modifier
            ),
        shape = CircleShape,
        containerColor = if (isListening)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surface,
        elevation = FloatingActionButtonDefaults.elevation(8.dp)
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = "Voice Command",
            tint = if (isListening)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    }
}
package com.ghostpocket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bento Clay Card: Pillowy, tactile 3D clay aesthetic with subtle dual-depth inner/outer highlights.
 */
@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 22.dp,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(shapeRadius)
    val clickModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) { onClick() }
    } else Modifier

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Soft clay gradient
    val clayGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF24242B),
                Color(0xFF19191E)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF1F1F6)
            )
        )
    }

    val topBorderColor = if (isDark) Color(0xFF383844) else Color(0xFFFFFFFF)
    val shadowColor = if (isDark) Color(0x77000000) else Color(0x1F000000)

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(clayGradient)
            .border(1.5.dp, topBorderColor.copy(alpha = if (isDark) 0.35f else 0.8f), shape)
            .then(clickModifier)
            .padding(18.dp)
    ) {
        Column(content = content)
    }
}

/**
 * Tactile Clay Pill Button with 3D press feel
 */
@Composable
fun ClayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val bgBrush = when {
        !enabled -> Brush.verticalGradient(listOf(Color(0xFF888888), Color(0xFF666666)))
        isSecondary -> {
            if (isDark) Brush.verticalGradient(listOf(Color(0xFF2C2C35), Color(0xFF1E1E24)))
            else Brush.verticalGradient(listOf(Color(0xFFF0F0F5), Color(0xFFE4E4EB)))
        }
        else -> {
            // Primary high-contrast clay button
            if (isDark) Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE2E2E8)))
            else Brush.verticalGradient(listOf(Color(0xFF1F1F24), Color(0xFF0F0F12)))
        }
    }

    val textColor = when {
        !enabled -> Color(0xFFAAAAAA)
        isSecondary -> if (isDark) Color(0xFFEAEAEA) else Color(0xFF1A1A1E)
        else -> if (isDark) Color(0xFF111113) else Color(0xFFFFFFFF)
    }

    val elevation = if (isPressed) 1.dp else 4.dp

    Box(
        modifier = modifier
            .shadow(elevation, shape)
            .clip(shape)
            .background(bgBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
        }
    }
}

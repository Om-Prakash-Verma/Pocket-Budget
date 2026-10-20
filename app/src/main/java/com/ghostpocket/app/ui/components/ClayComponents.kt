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
    val shape = remember(shapeRadius) { RoundedCornerShape(shapeRadius) }
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Soft clay gradient
    val clayGradient = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF16161D),
                    Color(0xFF101014)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF1F1F6)
                )
            )
        }
    }

    val topBorderColor = remember(isDark) {
        if (isDark) Color(0xFF2C2C38).copy(alpha = 0.35f) else Color(0xFFFFFFFF).copy(alpha = 0.8f)
    }
    val shadowColor = remember(isDark) {
        if (isDark) Color(0x66000000) else Color(0x18000000)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) { onClick() }
    } else Modifier

    val shadowModifier = if (elevation > 0.dp) {
        Modifier.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
    } else Modifier

    Box(
        modifier = modifier
            .then(shadowModifier)
            .clip(shape)
            .background(clayGradient)
            .border(1.5.dp, topBorderColor, shape)
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
    val shape = remember { RoundedCornerShape(16.dp) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val bgBrush = remember(enabled, isSecondary, isDark) {
        when {
            !enabled -> Brush.verticalGradient(listOf(Color(0xFF555562), Color(0xFF3F3F4A)))
            isSecondary -> {
                if (isDark) Brush.verticalGradient(listOf(Color(0xFF1D1D25), Color(0xFF131318)))
                else Brush.verticalGradient(listOf(Color(0xFFF0F0F5), Color(0xFFE4E4EB)))
            }
            else -> {
                // Primary high-contrast clay button
                if (isDark) Brush.verticalGradient(listOf(Color(0xFFF7F7FA), Color(0xFFDFDFE8)))
                else Brush.verticalGradient(listOf(Color(0xFF1F1F24), Color(0xFF0F0F12)))
            }
        }
    }

    val textColor = when {
        !enabled -> Color(0xFF888894)
        isSecondary -> if (isDark) Color(0xFFEDEDF2) else Color(0xFF1A1A1E)
        else -> if (isDark) Color(0xFF0C0C0E) else Color(0xFFFFFFFF)
    }

    val elevation = if (isPressed) 1.dp else 4.dp
    val buttonBorder = remember(isDark, isSecondary) {
        if (isDark && isSecondary) Color(0xFF2C2C38) else if (!isDark && isSecondary) Color(0xFFE0E0E6) else Color.Transparent
    }

    Box(
        modifier = modifier
            .shadow(elevation, shape)
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, buttonBorder, shape)
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

/**
 * Tactile Clay Icon Button for compact header and action controls
 */
@Composable
fun ClayIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 48.dp,
    shapeRadius: Dp = 16.dp,
    elevation: Dp = 4.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = remember(shapeRadius) { RoundedCornerShape(shapeRadius) }
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val bgBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF181820),
                    Color(0xFF111116)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF1F1F6)
                )
            )
        }
    }

    val topBorderColor = remember(isDark) {
        if (isDark) Color(0xFF2C2C38).copy(alpha = 0.35f) else Color(0xFFFFFFFF).copy(alpha = 0.8f)
    }
    val shadowColor = remember(isDark) {
        if (isDark) Color(0x66000000) else Color(0x18000000)
    }
    val iconColor = if (isDark) Color(0xFFF5F5F8) else Color(0xFF1D1D22)
    val currentElevation = if (isPressed) 1.dp else elevation

    val shadowModifier = if (currentElevation > 0.dp) {
        Modifier.shadow(
            elevation = currentElevation,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .then(shadowModifier)
            .clip(shape)
            .background(bgBrush)
            .border(1.5.dp, topBorderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

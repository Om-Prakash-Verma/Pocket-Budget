package com.ghostpocket.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ============================================================================
 * ClayCard
 * ============================================================================
 * Claymorphic Bento card container with soft 3D shadows and inner lighting gradients.
 *
 * Visual Characteristics:
 * - Rounded corners with customizable radius.
 * - Layered shadows (deep ambient shadow + surface highlight border).
 * - Interactive scale compression when clicked.
 *
 * @param modifier Composable modifier.
 * @param shapeRadius Corner radius.
 * @param elevation Depth elevation.
 * @param onClick Optional click handler.
 * @param content Composable slot content.
 */
@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 24.dp,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.985f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "clay_card_scale"
    )

    val shape: Shape = RoundedCornerShape(shapeRadius)
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val clayBgBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF20202A),
                    Color(0xFF14141C)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF3F3FA)
                )
            )
        }
    }

    val borderColor = remember(isDark) {
        if (isDark) Color(0xFF2C2C3C) else Color(0xFFE2E2EE)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isPressed && onClick != null) elevation / 2 else elevation,
                shape = shape,
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF9090AA).copy(alpha = 0.25f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.9f) else Color(0xFF707090).copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(clayBgBrush)
            .border(1.dp, borderColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(20.dp)
    ) {
        Column {
            content()
        }
    }
}

/**
 * ============================================================================
 * ClayButton
 * ============================================================================
 * Tactile primary & secondary button styled with claymorphic tactile depth.
 *
 * @param text Button label.
 * @param onClick Tap handler.
 * @param modifier Composable modifier.
 * @param icon Optional leading icon.
 * @param enabled Whether button is active.
 * @param isSecondary Styled as a muted secondary button.
 * @param customColor Optional custom accent color override (e.g. green/red for gave/got).
 */
@Composable
fun ClayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    customColor: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "clay_btn_scale"
    )

    val shape = RoundedCornerShape(18.dp)
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val bgBrush = remember(isSecondary, enabled, customColor, isDark) {
        when {
            !enabled -> Brush.verticalGradient(
                listOf(Color(0xFF33333E), Color(0xFF262630))
            )
            customColor != null -> Brush.verticalGradient(
                listOf(customColor.copy(alpha = 0.95f), customColor)
            )
            isSecondary -> {
                if (isDark) {
                    Brush.verticalGradient(listOf(Color(0xFF282836), Color(0xFF1E1E28)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFFEBEBF5), Color(0xFFDEDEEA)))
                }
            }
            else -> {
                // Primary Electric Violet Gradient
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF8B5CF6),
                        Color(0xFF6D28D9)
                    )
                )
            }
        }
    }

    val contentColor = when {
        !enabled -> Color(0xFF6B6B7F)
        customColor != null -> Color.White
        isSecondary -> MaterialTheme.colorScheme.onSurface
        else -> Color.White
    }

    val elevation = if (isPressed || !enabled) 1.dp else 4.dp

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (isSecondary) Color.Transparent else Color(0xFF7C3AED).copy(alpha = 0.35f),
                spotColor = if (isSecondary) Color.Transparent else Color(0xFF7C3AED).copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(bgBrush)
            .border(
                1.dp,
                if (!enabled) Color.Transparent else if (isSecondary) Color(0xFF3B3B4E) else Color(0xFFA78BFA).copy(alpha = 0.6f),
                shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * ============================================================================
 * ClayIconButton
 * ============================================================================
 * Tactile circular or squircle icon button.
 */
@Composable
fun ClayIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shapeRadius: Dp = 14.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "clay_icon_btn_scale"
    )

    val shape = RoundedCornerShape(shapeRadius)
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val bgBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF262634), Color(0xFF1B1B24)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEDEDF6)))
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(if (isPressed) 1.dp else 3.dp, shape)
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, if (isDark) Color(0xFF333344) else Color(0xFFDCDCE8), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

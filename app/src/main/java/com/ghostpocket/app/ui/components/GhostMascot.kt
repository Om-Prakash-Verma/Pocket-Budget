package com.ghostpocket.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GhostMood {
    HAPPY,
    CALM,
    THINKING,
    WAVING,
    SPOOKY_SPOOKED,
    RICH_GHOST
}

/**
 * Spooky, cute, funny clay mascot for GhostPocket.
 * Features bobbing animation, expressive eyes, tiny horns/hat, and tongue out/wink details.
 */
@Composable
fun GhostMascot(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    mood: GhostMood = GhostMood.CALM,
    animate: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ghost_bob")
    val floatOffset by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "float"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val bodyColor = MaterialTheme.colorScheme.onBackground
    val faceColor = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val yShift = floatOffset * (h / 64f)

        // Cute Clay Ghost Body
        val bodyPath = Path().apply {
            moveTo(w * 0.18f, h * 0.44f + yShift)
            // Pillow-soft dome head
            cubicTo(
                w * 0.16f, h * 0.08f + yShift,
                w * 0.84f, h * 0.08f + yShift,
                w * 0.82f, h * 0.44f + yShift
            )
            // Right ghost arm ripple
            cubicTo(
                w * 0.94f, h * 0.54f + yShift,
                w * 0.90f, h * 0.70f + yShift,
                w * 0.82f, h * 0.76f + yShift
            )
            // Bottom floating tentacles (clay-like wavy ripples)
            cubicTo(
                w * 0.74f, h * 0.68f + yShift,
                w * 0.66f, h * 0.88f + yShift,
                w * 0.50f, h * 0.78f + yShift
            )
            cubicTo(
                w * 0.40f, h * 0.68f + yShift,
                w * 0.28f, h * 0.88f + yShift,
                w * 0.18f, h * 0.76f + yShift
            )
            // Left ghost arm ripple
            cubicTo(
                w * 0.10f, h * 0.70f + yShift,
                w * 0.06f, h * 0.54f + yShift,
                w * 0.18f, h * 0.44f + yShift
            )
            close()
        }

        // Draw shadow under floating ghost
        drawOval(
            color = bodyColor.copy(alpha = 0.12f),
            topLeft = Offset(w * 0.22f, h * 0.86f),
            size = Size(w * 0.56f, h * 0.12f)
        )

        // Draw Ghost
        drawPath(
            path = bodyPath,
            color = bodyColor,
            style = Fill
        )

        val eyeRadius = w * 0.045f
        val eyeY = h * 0.42f + yShift
        val leftEyeX = w * 0.38f
        val rightEyeX = w * 0.62f

        when (mood) {
            GhostMood.HAPPY -> {
                // Happy curved eyes (arches ^ ^)
                drawArc(
                    color = faceColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(leftEyeX - eyeRadius, eyeY - eyeRadius),
                    size = Size(eyeRadius * 2, eyeRadius * 2),
                    style = Stroke(width = eyeRadius * 0.9f)
                )
                drawArc(
                    color = faceColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(rightEyeX - eyeRadius, eyeY - eyeRadius),
                    size = Size(eyeRadius * 2, eyeRadius * 2),
                    style = Stroke(width = eyeRadius * 0.9f)
                )
                // Cute open mouth with tongue out
                drawArc(
                    color = faceColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.43f, eyeY + eyeRadius * 1.3f),
                    size = Size(w * 0.14f, h * 0.10f)
                )
            }
            GhostMood.RICH_GHOST -> {
                // Gold coin / rupee sparkle eyes ($ / ₹)
                drawCircle(color = faceColor, radius = eyeRadius * 1.2f, center = Offset(leftEyeX, eyeY))
                drawCircle(color = faceColor, radius = eyeRadius * 1.2f, center = Offset(rightEyeX, eyeY))
                // Tiny top hat
                val hatPath = Path().apply {
                    moveTo(w * 0.38f, h * 0.16f + yShift)
                    lineTo(w * 0.62f, h * 0.16f + yShift)
                    lineTo(w * 0.58f, h * 0.04f + yShift)
                    lineTo(w * 0.42f, h * 0.04f + yShift)
                    close()
                }
                drawPath(path = hatPath, color = faceColor)
                // Grin
                drawArc(
                    color = faceColor,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.44f, eyeY + eyeRadius * 1.2f),
                    size = Size(w * 0.12f, h * 0.06f),
                    style = Stroke(width = eyeRadius * 0.8f)
                )
            }
            GhostMood.SPOOKY_SPOOKED -> {
                // Huge wide shocked eyes (O O)
                drawCircle(color = faceColor, radius = eyeRadius * 1.6f, center = Offset(leftEyeX, eyeY))
                drawCircle(color = faceColor, radius = eyeRadius * 1.6f, center = Offset(rightEyeX, eyeY))
                drawCircle(color = bodyColor, radius = eyeRadius * 0.6f, center = Offset(leftEyeX, eyeY))
                drawCircle(color = bodyColor, radius = eyeRadius * 0.6f, center = Offset(rightEyeX, eyeY))
                // Shocked scream mouth :O
                drawOval(
                    color = faceColor,
                    topLeft = Offset(w * 0.44f, eyeY + eyeRadius * 2f),
                    size = Size(w * 0.12f, h * 0.12f)
                )
            }
            GhostMood.CALM -> {
                drawCircle(color = faceColor, radius = eyeRadius, center = Offset(leftEyeX, eyeY))
                drawCircle(color = faceColor, radius = eyeRadius, center = Offset(rightEyeX, eyeY))
                drawArc(
                    color = faceColor,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.45f, eyeY + eyeRadius),
                    size = Size(w * 0.10f, h * 0.06f),
                    style = Stroke(width = eyeRadius * 0.7f)
                )
            }
            GhostMood.THINKING -> {
                drawCircle(color = faceColor, radius = eyeRadius * 1.3f, center = Offset(leftEyeX, eyeY - 2f))
                drawCircle(color = faceColor, radius = eyeRadius * 0.7f, center = Offset(rightEyeX, eyeY + 1f))
                drawCircle(color = faceColor, radius = eyeRadius * 0.7f, center = Offset(w * 0.50f, eyeY + eyeRadius * 2f))
            }
            GhostMood.WAVING -> {
                drawCircle(color = faceColor, radius = eyeRadius, center = Offset(leftEyeX, eyeY))
                drawCircle(color = faceColor, radius = eyeRadius, center = Offset(rightEyeX, eyeY))
                drawArc(
                    color = faceColor,
                    startAngle = 10f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(w * 0.43f, eyeY + eyeRadius),
                    size = Size(w * 0.14f, h * 0.08f),
                    style = Stroke(width = eyeRadius * 0.8f)
                )
            }
        }
    }
}

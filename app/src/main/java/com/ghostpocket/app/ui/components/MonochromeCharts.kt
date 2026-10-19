package com.ghostpocket.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.CategorySpend
import com.ghostpocket.app.model.MoneyUtils

/**
 * Minimalist monochrome category progress bar list
 */
@Composable
fun MonochromeCategoryBars(
    spends: List<CategorySpend>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        spends.forEach { spend ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(categoryName = spend.category, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = spend.category,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = MoneyUtils.formatPaiseToInr(spend.totalPaise),
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (spend.allocatedBudgetPaise > 0L) {
                            Text(
                                text = if (spend.isOverBudget) "Over budget of ${MoneyUtils.formatPaiseToInr(spend.allocatedBudgetPaise)}! 😱" else "of ${MoneyUtils.formatPaiseToInr(spend.allocatedBudgetPaise)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Monochrome bar: If allocated budget exists, fill represents budget usage fraction, else percentage of total
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                val fillColor = MaterialTheme.colorScheme.onBackground
                val fraction = if (spend.allocatedBudgetPaise > 0L) {
                    spend.budgetUsageFraction.coerceIn(0.02f, 1f)
                } else {
                    spend.percentage.coerceIn(0.02f, 1f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(trackColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(fillColor)
                    )
                }
            }
        }
    }
}

/**
 * Minimalist monochrome sparkline / trend line chart
 */
@Composable
fun MonochromeTrendChart(
    points: List<Float>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.onBackground
    val subtleLineColor = MaterialTheme.colorScheme.outline

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 8.dp)
    ) {
        if (points.size < 2) return@Canvas

        val maxVal = points.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val minVal = 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1)

        // Baseline
        drawLine(
            color = subtleLineColor,
            start = Offset(0f, h),
            end = Offset(w, h),
            strokeWidth = 1.dp.toPx()
        )

        val path = Path()
        points.forEachIndexed { i, value ->
            val x = i * stepX
            val y = h - ((value - minVal) / range) * (h * 0.85f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw discrete minimalist point for latest day
        val lastX = (points.size - 1) * stepX
        val lastY = h - ((points.last() - minVal) / range) * (h * 0.85f)
        drawCircle(
            color = lineColor,
            radius = 3.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}

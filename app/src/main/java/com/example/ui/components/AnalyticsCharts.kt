package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.CategorySpendSlice
import com.example.domain.ExchangeRateModel
import com.example.domain.MonthlyTrendPoint
import com.example.ui.theme.BroughtForwardGold
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyAndBfCalculator
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

/**
 * Interactive Category Donut Chart with touch-to-select slice inspection.
 */
@Composable
fun InteractiveCategoryDonutChart(
    slices: List<CategorySpendSlice>,
    baseCurrency: String,
    rates: Map<String, ExchangeRateModel>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(slices) { mutableIntStateOf(if (slices.isNotEmpty()) 0 else -1) }
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnim = true }
    val sweepProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "donut_sweep"
    )

    val totalSpend = slices.sumOf { it.totalInBaseCurrency }
    val selectedSlice = slices.getOrNull(selectedIndex)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_donut_chart"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (slices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions recorded for this category view yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        Box(
            modifier = Modifier.size(210.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .pointerInput(slices) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            val dist = hypot(dx, dy)
                            val outerRadius = minOf(size.width, size.height).toFloat() / 2f
                            if (dist in (outerRadius * 0.35f)..(outerRadius * 1.15f)) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                angle = (angle + 450f) % 360f // Normalize relative to -90deg top start
                                var cumulative = 0f
                                slices.forEachIndexed { idx, slice ->
                                    val sweep = (slice.sharePercentage / 100f) * 360f
                                    if (angle in cumulative..(cumulative + sweep)) {
                                        selectedIndex = idx
                                        return@detectTapGestures
                                    }
                                    cumulative += sweep
                                }
                            }
                        }
                    }
            ) {
                val strokeWidth = 34.dp.toPx()
                val selectedStrokeWidth = 42.dp.toPx()
                var startAngle = -90f

                slices.forEachIndexed { index, slice ->
                    val rawSweep = (slice.sharePercentage / 100f) * 360f * sweepProgress
                    val isSelected = index == selectedIndex
                    val color = colorFromHex(slice.colorHex)
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = max(rawSweep - 2f, 1f),
                        useCenter = false,
                        style = Stroke(
                            width = if (isSelected) selectedStrokeWidth else strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )
                    startAngle += rawSweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedSlice?.categoryName ?: "Total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyAndBfCalculator.formatCurrency(
                        selectedSlice?.totalInBaseCurrency ?: totalSpend,
                        baseCurrency,
                        rates
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedSlice != null) {
                    Text(
                        text = "%.1f%% of period".format(selectedSlice.sharePercentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorFromHex(selectedSlice.colorHex)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Legend List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slices.forEachIndexed { idx, slice ->
                val isSelected = idx == selectedIndex
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedIndex = idx },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(colorFromHex(slice.colorHex))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${slice.categoryName} (${slice.transactionCount})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        Text(
                            text = "${
                                CurrencyAndBfCalculator.formatCurrency(
                                    slice.totalInBaseCurrency,
                                    baseCurrency,
                                    rates
                                )
                            } (${"%.1f".format(slice.sharePercentage)}%)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive 6-Month Cash Flow (Income vs Expense grouped bars) + Cumulative Carried Forward (C/F)
 * Trend Line Chart. Tapping any month inspects its Opening B/F, Inflow, Outflow, and Closing C/F!
 */
@Composable
fun SixMonthCashFlowAndBfTrendChart(
    points: List<MonthlyTrendPoint>,
    baseCurrency: String,
    rates: Map<String, ExchangeRateModel>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    var selectedMonthIndex by remember(points) { mutableIntStateOf(points.lastIndex) }
    val selectedPoint = points.getOrElse(selectedMonthIndex) { points.last() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("six_month_trend_chart"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "6-Month Cash Flow & Cumulative Carry-Over (C/F) Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap any month to inspect Opening B/F, Inflows, Outflows, and Closing C/F",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Month Callout Strip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = CurrencyAndBfCalculator.formatMonthTitle(selectedPoint.yearMonth),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Opening B/F: ${
                                CurrencyAndBfCalculator.formatCurrency(
                                    selectedPoint.openingBroughtForward,
                                    baseCurrency,
                                    rates
                                )
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroughtForwardGold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "In: +${
                                CurrencyAndBfCalculator.formatCurrency(
                                    selectedPoint.incomeInBase,
                                    baseCurrency,
                                    rates
                                )
                            }  |  Out: -${
                                CurrencyAndBfCalculator.formatCurrency(
                                    selectedPoint.expenseInBase,
                                    baseCurrency,
                                    rates
                                )
                            }",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "Closing C/F: ${
                                CurrencyAndBfCalculator.formatCurrency(
                                    selectedPoint.closingCarriedForward,
                                    baseCurrency,
                                    rates
                                )
                            }",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val maxBarVal = points.maxOfOrNull { max(it.incomeInBase, it.expenseInBase) }?.coerceAtLeast(100.0) ?: 100.0
            val maxCfVal = points.maxOfOrNull { it.closingCarriedForward } ?: 100.0
            val minCfVal = points.minOfOrNull { it.closingCarriedForward } ?: 0.0
            val cfRange = (maxCfVal - minCfVal).coerceAtLeast(100.0)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / points.size.toFloat()
                            val tappedIdx = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                            selectedMonthIndex = tappedIdx
                        }
                    }
            ) {
                val slotWidth = size.width / points.size.toFloat()
                val chartHeight = size.height - 22.dp.toPx()
                val barWidth = (slotWidth * 0.26f).coerceAtMost(18.dp.toPx())

                val cfLinePath = Path()

                points.forEachIndexed { index, pt ->
                    val centerX = slotWidth * index + slotWidth / 2f
                    val isSelected = index == selectedMonthIndex

                    if (isSelected) {
                        drawRoundRect(
                            color = BroughtForwardGold.copy(alpha = 0.12f),
                            topLeft = Offset(slotWidth * index + 4f, 0f),
                            size = Size(slotWidth - 8f, chartHeight),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }

                    val incRatio = (pt.incomeInBase / maxBarVal).toFloat().coerceIn(0.04f, 1f)
                    val expRatio = (pt.expenseInBase / maxBarVal).toFloat().coerceIn(0.04f, 1f)

                    val incHeight = chartHeight * 0.78f * incRatio
                    val expHeight = chartHeight * 0.78f * expRatio

                    // Income Bar
                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = Offset(centerX - barWidth - 2f, chartHeight - incHeight),
                        size = Size(barWidth, incHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )

                    // Expense Bar
                    drawRoundRect(
                        color = ExpenseCoral,
                        topLeft = Offset(centerX + 2f, chartHeight - expHeight),
                        size = Size(barWidth, expHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )

                    // Closing Carried Forward (C/F) Line node
                    val normalizedCf = ((pt.closingCarriedForward - minCfVal) / cfRange).toFloat().coerceIn(0f, 1f)
                    val cfY = chartHeight * 0.82f - (normalizedCf * chartHeight * 0.68f)

                    if (index == 0) {
                        cfLinePath.moveTo(centerX, cfY)
                    } else {
                        cfLinePath.lineTo(centerX, cfY)
                    }
                }

                drawPath(
                    path = cfLinePath,
                    color = BroughtForwardGold,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { index, pt ->
                    val centerX = slotWidth * index + slotWidth / 2f
                    val normalizedCf = ((pt.closingCarriedForward - minCfVal) / cfRange).toFloat().coerceIn(0f, 1f)
                    val cfY = chartHeight * 0.82f - (normalizedCf * chartHeight * 0.68f)
                    drawCircle(
                        color = BroughtForwardGold,
                        radius = if (index == selectedMonthIndex) 6.dp.toPx() else 4.dp.toPx(),
                        center = Offset(centerX, cfY)
                    )
                }
            }

            // Month X-Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                points.forEachIndexed { idx, pt ->
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (idx == selectedMonthIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (idx == selectedMonthIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.clickable { selectedMonthIndex = idx }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendDot(color = IncomeGreen, label = "Period Income")
                ChartLegendDot(color = ExpenseCoral, label = "Period Expense")
                ChartLegendDot(color = BroughtForwardGold, label = "Closing C/F Balance")
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

package com.cashopinion.console.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashopinion.console.R
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes

@Composable
fun GoldPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    iconRes: Int? = null
) {
    val colors = CashTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = Shapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(Shapes.Button)
            .background(
                if (enabled) colors.goldButtonGradient
                else Brush.horizontalGradient(listOf(Color(0xFF3E382A), Color(0xFF2C271D)))
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF140E00),
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = Color(0xFF140E00),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF140E00)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String = "Search...",
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(hint, color = colors.textSecondary) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_document),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Clear",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = Shapes.Input,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.border,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    )
}

@Composable
fun EmptyStateView(
    iconRes: Int = R.drawable.ic_document,
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(Shapes.Circle)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Int = 80
) {
    val colors = CashTheme.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        colors.surface,
        colors.surfaceVariant,
        colors.surface
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim.value - 500f, y = translateAnim.value - 500f),
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(Shapes.Card)
            .background(brush)
            .border(1.dp, colors.border, Shapes.Card)
    )
}

@Composable
fun CountUpText(
    targetValue: Long,
    prefix: String = "",
    suffix: String = "",
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    color: Color = CashTheme.colors.textPrimary
) {
    var displayValue by remember { mutableLongStateOf(0L) }

    LaunchedEffect(targetValue) {
        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis = 800, easing = LinearEasing)
        ) {
            displayValue = this.value.toLong()
        }
        displayValue = targetValue
    }

    Text(
        text = "$prefix$displayValue$suffix",
        style = style,
        color = color
    )
}

/**
 * Section 6.4: Simple Bar Chart drawn on Canvas with numbers on bars, no clutter.
 */
@Composable
fun SimpleBarChart(
    labels: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = CashTheme.colors.primary,
    textColor: Color = CashTheme.colors.textSecondary
) {
    if (labels.isEmpty() || values.isEmpty()) return
    val maxValue = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)

    Card(
        shape = Shapes.Card,
        colors = CardDefaults.cardColors(containerColor = CashTheme.colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CashTheme.colors.border),
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val count = values.size
            val barSpacing = size.width / (count * 2f + 1f)
            val barWidth = barSpacing

            for (i in 0 until count) {
                val value = values[i]
                val normalizedHeight = (value / maxValue) * (size.height * 0.70f)
                val x = barSpacing + (i * 2 * barSpacing)
                val y = size.height - normalizedHeight - 20f

                // Draw Bar
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, normalizedHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Draw Value on Top of Bar
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        this.textSize = 10.sp.toPx()
                        this.textAlign = android.graphics.Paint.Align.CENTER
                        this.isAntiAlias = true
                    }
                    val labelPaint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.GRAY
                        this.textSize = 9.sp.toPx()
                        this.textAlign = android.graphics.Paint.Align.CENTER
                        this.isAntiAlias = true
                    }

                    // Value above bar
                    canvas.nativeCanvas.drawText(
                        value.toInt().toString(),
                        x + barWidth / 2,
                        y - 6f,
                        paint
                    )

                    // Label below bar
                    canvas.nativeCanvas.drawText(
                        labels.getOrElse(i) { "" },
                        x + barWidth / 2,
                        size.height - 2f,
                        labelPaint
                    )
                }
            }
        }
    }
}

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFFDC2626),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_warning),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "No connection. Changes are paused.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

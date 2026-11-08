package com.cashopinion.console.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes

object SoundFx {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 45)
        } catch (_: Exception) {}
    }

    fun playClick() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
        } catch (_: Exception) {}
    }

    fun playSuccess() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 70)
        } catch (_: Exception) {}
    }

    fun playWarning() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 90)
        } catch (_: Exception) {}
    }
}

fun Modifier.bounceClick(onClick: () -> Unit): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitFirstDown(requireUnconsumed = false)
                SoundFx.playClick()
                val upOrCancel = waitForUpOrCancellation()
                if (upOrCancel != null) {
                    onClick()
                }
            }
        }
    }
)

@Composable
fun GoldGlowingBorder(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = Shapes.Card,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "goldGlow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val glowBrush = Brush.linearGradient(
        listOf(
            CashColors.GoldPrimary.copy(alpha = alpha),
            CashColors.AmberSecondary.copy(alpha = alpha * 0.7f),
            CashColors.GoldPrimary.copy(alpha = alpha * 0.4f)
        )
    )

    Box(
        modifier = modifier
            .border(1.5.dp, glowBrush, shape)
            .clip(shape),
        content = content
    )
}

@Composable
fun LuxuryCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isUrgent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CashTheme.colors
    val baseModifier = if (onClick != null) {
        modifier.clickable {
            SoundFx.playClick()
            onClick()
        }
    } else {
        modifier
    }

    val borderColor = if (isUrgent) colors.error else colors.border

    Card(
        modifier = baseModifier
            .fillMaxWidth()
            .clip(Shapes.Card),
        shape = Shapes.Card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val s = status.lowercase()

    val (bgColor, textColor, borderCol) = when {
        s in listOf("paid", "approved", "completed", "published", "active") ->
            Triple(colors.success.copy(alpha = 0.15f), colors.success, colors.success)
        s in listOf("pending", "draft", "in_progress", "warning", "open") ->
            Triple(CashColors.GoldPrimary.copy(alpha = 0.18f), CashColors.GoldPrimary, CashColors.GoldPrimary)
        s in listOf("rejected", "banned", "sla_breach", "closed", "critical") ->
            Triple(colors.error.copy(alpha = 0.18f), colors.error, colors.error)
        s in listOf("reversed") ->
            Triple(CashColors.AmberSecondary.copy(alpha = 0.18f), CashColors.AmberSecondary, CashColors.AmberSecondary)
        else ->
            Triple(colors.surfaceVariant, colors.textSecondary, colors.border)
    }

    Surface(
        modifier = modifier,
        shape = Shapes.Chip,
        color = bgColor,
        border = BorderStroke(1.dp, borderCol.copy(alpha = 0.6f))
    ) {
        Text(
            text = status.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

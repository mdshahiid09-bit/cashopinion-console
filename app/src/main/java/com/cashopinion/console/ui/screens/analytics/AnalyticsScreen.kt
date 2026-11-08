package com.cashopinion.console.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.SimpleBarChart
import com.cashopinion.console.ui.theme.CashTheme

@Composable
fun AnalyticsScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors

    // Mock/derived daily metrics for the last 14 days (Section 6.4)
    val signupLabels = listOf("D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D10", "D11", "D12", "D13", "D14")
    val signupValues = listOf(14f, 22f, 18f, 35f, 29f, 42f, 38f, 50f, 44f, 62f, 58f, 75f, 69f, 85f)

    val withdrawalLabels = listOf("D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D10", "D11", "D12", "D13", "D14")
    val withdrawalValues = listOf(500f, 1200f, 850f, 2100f, 1600f, 2400f, 3100f, 2800f, 4200f, 3900f, 4800f, 5200f, 6100f, 7500f)

    val taskLabels = listOf("T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8")
    val taskValues = listOf(120f, 95f, 82f, 64f, 58f, 42f, 36f, 28f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Analytics & Reports",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Daily Signups (Last 14 Days)",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        SimpleBarChart(
            labels = signupLabels,
            values = signupValues,
            barColor = colors.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Daily Withdrawals Paid INR (Last 14 Days)",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        SimpleBarChart(
            labels = withdrawalLabels,
            values = withdrawalValues,
            barColor = colors.secondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Top Tasks By Completions",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        SimpleBarChart(
            labels = taskLabels,
            values = taskValues,
            barColor = colors.success
        )
    }
}

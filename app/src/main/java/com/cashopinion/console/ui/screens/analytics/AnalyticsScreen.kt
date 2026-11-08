package com.cashopinion.console.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.SimpleBarChart
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors

    val allUsers by consoleRepo.observeUsers().collectAsState(initial = emptyList())
    val allWithdrawals by consoleRepo.observeWithdrawals().collectAsState(initial = emptyList())
    val allTasks by consoleRepo.observeTasks().collectAsState(initial = emptyList())

    // Derive last 14 days signups dynamically from real users
    val (signupLabels, signupValues) = remember(allUsers) {
        val days = 14
        val cal = Calendar.getInstance()
        val labels = mutableListOf<String>()
        val values = mutableListOf<Float>()
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

        for (i in (days - 1) downTo 0) {
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayStart = dayCal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
            val dayEnd = dayCal.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
            labels.add(dateFormat.format(Date(dayStart)))
            val count = allUsers.count { it.createdAt in dayStart..dayEnd }
            values.add(count.toFloat())
        }
        Pair(labels, values)
    }

    // Derive last 14 days paid withdrawals from real withdrawals
    val (withdrawalLabels, withdrawalValues) = remember(allWithdrawals) {
        val days = 14
        val labels = mutableListOf<String>()
        val values = mutableListOf<Float>()
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

        for (i in (days - 1) downTo 0) {
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayStart = dayCal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
            val dayEnd = dayCal.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
            labels.add(dateFormat.format(Date(dayStart)))
            val totalInr = allWithdrawals
                .filter { it.status == "paid" && it.processedAt in dayStart..dayEnd }
                .sumOf { it.inrAmount }
            values.add(totalInr.toFloat())
        }
        Pair(labels, values)
    }

    // Top tasks by reward / completions
    val (taskLabels, taskValues) = remember(allTasks) {
        val sorted = allTasks.sortedByDescending { it.reward }.take(8)
        val labels = sorted.map { it.title.take(10) }
        val values = sorted.map { it.reward.toFloat() }
        Pair(labels, values)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Platform Analytics",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Live User Registrations (Last 14 Days)",
            style = MaterialTheme.typography.titleMedium,
            color = colors.primary,
            fontWeight = FontWeight.Bold
        )
        SimpleBarChart(
            values = signupValues,
            labels = signupLabels,
            barColor = CashColors.GoldPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Completed Payouts Volume INR (Last 14 Days)",
            style = MaterialTheme.typography.titleMedium,
            color = colors.primary,
            fontWeight = FontWeight.Bold
        )
        SimpleBarChart(
            values = withdrawalValues,
            labels = withdrawalLabels,
            barColor = colors.success,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Top Tasks by Reward Tiers",
            style = MaterialTheme.typography.titleMedium,
            color = colors.primary,
            fontWeight = FontWeight.Bold
        )
        SimpleBarChart(
            values = taskValues,
            labels = taskLabels,
            barColor = CashColors.AmberSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

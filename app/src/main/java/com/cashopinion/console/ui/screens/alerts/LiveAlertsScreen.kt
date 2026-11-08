package com.cashopinion.console.ui.screens.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashopinion.console.R
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.EmptyStateView
import com.cashopinion.console.ui.components.LuxuryCard
import com.cashopinion.console.ui.components.StatusBadge
import com.cashopinion.console.ui.theme.CashTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LiveAlertsScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val alerts by consoleRepo.observeLiveAlerts().collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live System Alerts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Real-time monitoring of SLA breaches and risk signals",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            StatusBadge(status = if (alerts.isEmpty()) "HEALTHY" else "${alerts.size} ALERTS")
        }

        if (alerts.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_check_circle,
                message = "All systems operating normally. Zero active SLA breaches or warnings."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alerts, key = { it.id }) { alert ->
                    LuxuryCard(isUrgent = alert.severity == "critical") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (alert.severity == "critical") colors.error else colors.warning
                            )
                            StatusBadge(status = alert.severity.uppercase())
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(alert.timestamp))
                        Text(
                            text = "Detected: $timeStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

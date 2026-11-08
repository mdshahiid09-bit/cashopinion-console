package com.cashopinion.console.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashopinion.console.R
import com.cashopinion.console.data.model.DashboardKpis
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.CountUpText
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes

@Composable
fun HomeScreen(
    consoleRepo: ConsoleRepo,
    onNavigateToWithdrawals: () -> Unit,
    onNavigateToReviews: () -> Unit,
    onNavigateToTickets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val kpis by consoleRepo.observeDashboardKpis().collectAsState(initial = DashboardKpis())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time Platform Metrics",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Urgent Queue Card (Section 6.1)
        val hasUrgent = kpis.urgentWithdrawalsCount > 0 || kpis.urgentReviewsCount > 0
        if (hasUrgent || kpis.openTickets > 0) {
            item {
                Card(
                    shape = Shapes.Card,
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasUrgent) colors.error.copy(alpha = 0.12f) else colors.surfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasUrgent) colors.error.copy(alpha = 0.4f) else colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_warning),
                                contentDescription = null,
                                tint = if (hasUrgent) colors.error else colors.warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Action Required (48h SLA)",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            UrgentChip(
                                label = "Withdrawals >48h",
                                count = kpis.urgentWithdrawalsCount,
                                isUrgent = kpis.urgentWithdrawalsCount > 0,
                                onClick = onNavigateToWithdrawals
                            )
                            UrgentChip(
                                label = "Reviews >48h",
                                count = kpis.urgentReviewsCount,
                                isUrgent = kpis.urgentReviewsCount > 0,
                                onClick = onNavigateToReviews
                            )
                            UrgentChip(
                                label = "Open Tickets",
                                count = kpis.openTickets,
                                isUrgent = false,
                                onClick = onNavigateToTickets
                            )
                        }
                    }
                }
            }
        }

        // Main KPI Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Pending Payouts",
                    value = kpis.pendingWithdrawalsCount.toLong(),
                    subText = "₹${String.format("%.2f", kpis.pendingWithdrawalsInr)}",
                    iconRes = R.drawable.ic_wallet,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Total Users",
                    value = kpis.totalUsers,
                    subText = "Registered Accounts",
                    iconRes = R.drawable.ic_user,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Coins In Circulation",
                    value = kpis.totalCoinsCirculating,
                    subText = "Total User Balances",
                    iconRes = R.drawable.ic_coin,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Open Tickets",
                    value = kpis.openTickets.toLong(),
                    subText = "Support Queue",
                    iconRes = R.drawable.ic_headset,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Storage & Spark Quota Usage Card (Section 6.17)
        item {
            Card(
                shape = Shapes.Card,
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spark Plan Storage Health",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Quota: 1.0 GB",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress bar
                    val usagePercent = 0.28f // ~280 MB estimated
                    LinearProgressIndicator(
                        progress = { usagePercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(Shapes.Chip),
                        color = colors.primary,
                        trackColor = colors.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "~285 MB Used (28.5%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "Status: Healthy",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.success
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: Long,
    subText: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    Card(
        shape = Shapes.Card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            CountUpText(
                targetValue = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun UrgentChip(
    label: String,
    count: Int,
    isUrgent: Boolean,
    onClick: () -> Unit
) {
    val colors = CashTheme.colors
    Surface(
        shape = Shapes.Chip,
        color = if (isUrgent) colors.error.copy(alpha = 0.20f) else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isUrgent) colors.error else colors.border
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isUrgent) colors.error else colors.textPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = colors.textSecondary
            )
        }
    }
}

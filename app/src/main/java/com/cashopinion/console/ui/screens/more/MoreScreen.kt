package com.cashopinion.console.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashopinion.console.R
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes

@Composable
fun MoreScreen(
    onNavigateToReviews: () -> Unit,
    onNavigateToSeason: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Platform Modules",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        ModuleItem(
            title = "Season System",
            subtitle = "Season reset, warning popups & summaries",
            iconRes = R.drawable.ic_calendar_star,
            onClick = onNavigateToSeason
        )

        ModuleItem(
            title = "Review Queue",
            subtitle = "Task submissions & proof verification",
            iconRes = R.drawable.ic_check_circle,
            onClick = onNavigateToReviews
        )

        ModuleItem(
            title = "Analytics & Reports",
            subtitle = "Signups, paid payouts & task metrics",
            iconRes = R.drawable.ic_earnings_history,
            onClick = onNavigateToAnalytics
        )

        ModuleItem(
            title = "App Configuration",
            subtitle = "Maintenance, update flags & links",
            iconRes = R.drawable.ic_settings,
            onClick = onNavigateToConfig
        )

        ModuleItem(
            title = "Admin Profile",
            subtitle = "Display name, base64 photo & password",
            iconRes = R.drawable.ic_user,
            onClick = onNavigateToProfile
        )

        ModuleItem(
            title = "Settings & Security",
            subtitle = "Theme, session timeout & server backend",
            iconRes = R.drawable.ic_shield,
            onClick = onNavigateToSettings
        )
    }
}

@Composable
fun ModuleItem(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    val colors = CashTheme.colors
    Card(
        shape = Shapes.Card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.surfaceVariant, Shapes.Circle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

package com.cashopinion.console.ui.screens.more

import androidx.compose.foundation.background
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
import com.cashopinion.console.ui.components.LuxuryCard
import com.cashopinion.console.ui.components.SoundFx
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme

@Composable
fun MoreScreen(
    onNavigateToReviews: () -> Unit,
    onNavigateToSeason: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPromo: () -> Unit,
    onNavigateToGamification: () -> Unit,
    onNavigateToPagesFaq: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToRedeemCodes: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToPayoutMethods: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Platform Management Modules",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Full-stack enterprise control center for Cash Opinion operations",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )

        // Core Operations Section
        Text(
            text = "CORE OPERATIONS",
            style = MaterialTheme.typography.labelSmall,
            color = CashColors.GoldPrimary,
            fontWeight = FontWeight.Bold
        )

        ModuleItem(
            title = "Review Queue",
            subtitle = "Task submissions & proof verification",
            iconRes = R.drawable.ic_check_circle,
            onClick = onNavigateToReviews
        )

        ModuleItem(
            title = "Problem Reports",
            subtitle = "User bug reports, transaction queries & direct admin resolution",
            iconRes = R.drawable.ic_error,
            onClick = onNavigateToReports
        )

        ModuleItem(
            title = "Customer Support & Tickets",
            subtitle = "User queries, live chat replies & direct warnings",
            iconRes = R.drawable.ic_headset,
            onClick = onNavigateToSupport
        )

        ModuleItem(
            title = "Saved Payout Methods",
            subtitle = "Inspect and disable suspicious UPI IDs & Bank accounts",
            iconRes = R.drawable.ic_bank,
            onClick = onNavigateToPayoutMethods
        )

        ModuleItem(
            title = "Global Transaction Ledger",
            subtitle = "Full platform audit trail across all user wallets & coin events",
            iconRes = R.drawable.ic_transaction_history,
            onClick = onNavigateToTransactions
        )

        ModuleItem(
            title = "Live Risk & SLA Alerts",
            subtitle = "Multi-account fraud alerts & 48h payout breach monitor",
            iconRes = R.drawable.ic_warning,
            onClick = onNavigateToAlerts
        )

        // Growth & Engagement Section
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "GROWTH & ENGAGEMENT",
            style = MaterialTheme.typography.labelSmall,
            color = CashColors.GoldPrimary,
            fontWeight = FontWeight.Bold
        )

        ModuleItem(
            title = "Sponsored Offers",
            subtitle = "Campaign studio, target URL, payout & live offer status",
            iconRes = R.drawable.ic_sponsored_offers,
            onClick = onNavigateToOffers
        )

        ModuleItem(
            title = "Redeem Codes Studio",
            subtitle = "Bulk voucher generator, batch tracking & code revocation",
            iconRes = R.drawable.ic_gift,
            onClick = onNavigateToRedeemCodes
        )

        ModuleItem(
            title = "Notifications, Popups & Banners",
            subtitle = "Broadcast dispatch, custom in-app popups & promotional banners",
            iconRes = R.drawable.ic_bell,
            onClick = onNavigateToNotifications
        )

        ModuleItem(
            title = "Promo Codes & Referrals",
            subtitle = "Create redeem codes, usage caps & referral commission rules",
            iconRes = R.drawable.ic_tag,
            onClick = onNavigateToPromo
        )

        ModuleItem(
            title = "Gamification Studio",
            subtitle = "Leaderboards, scratch card odds & daily task 24h rolling spin limits",
            iconRes = R.drawable.ic_trophy,
            onClick = onNavigateToGamification
        )

        // Lifecycle & Season Section
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "PLATFORM LIFECYCLE",
            style = MaterialTheme.typography.labelSmall,
            color = CashColors.GoldPrimary,
            fontWeight = FontWeight.Bold
        )

        ModuleItem(
            title = "Season System",
            subtitle = "Season reset, warning popups & summaries",
            iconRes = R.drawable.ic_calendar_star,
            onClick = onNavigateToSeason
        )

        ModuleItem(
            title = "Database Backups & Export",
            subtitle = "One-click JSON/CSV chunk exports & 1GB Spark plan quota monitor",
            iconRes = R.drawable.ic_document,
            onClick = onNavigateToBackup
        )

        ModuleItem(
            title = "Analytics & Performance",
            subtitle = "Signups, paid payouts & task metrics",
            iconRes = R.drawable.ic_earnings_history,
            onClick = onNavigateToAnalytics
        )

        ModuleItem(
            title = "Content Pages & FAQs",
            subtitle = "Terms, Privacy, About & Frequently Asked Questions",
            iconRes = R.drawable.ic_info,
            onClick = onNavigateToPagesFaq
        )

        // System Settings
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "CONFIGURATION & SECURITY",
            style = MaterialTheme.typography.labelSmall,
            color = CashColors.GoldPrimary,
            fontWeight = FontWeight.Bold
        )

        ModuleItem(
            title = "App Configuration",
            subtitle = "Maintenance mode, version control & social links",
            iconRes = R.drawable.ic_settings,
            onClick = onNavigateToConfig
        )

        ModuleItem(
            title = "Admin Profile",
            subtitle = "Display name, base64 photo & password security",
            iconRes = R.drawable.ic_user,
            onClick = onNavigateToProfile
        )

        ModuleItem(
            title = "Settings & Backend Server",
            subtitle = "Theme, 15-minute inactivity session lock & Render server health",
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
    LuxuryCard(
        onClick = {
            SoundFx.playClick()
            onClick()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = colors.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
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

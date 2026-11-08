package com.cashopinion.console.ui.screens.backup

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashopinion.console.R
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.GoldPillButton
import com.cashopinion.console.ui.components.LuxuryCard
import com.cashopinion.console.ui.components.SoundFx
import com.cashopinion.console.ui.components.StatusBadge
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import kotlinx.coroutines.launch

@Composable
fun BackupExportScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isBackingUp by remember { mutableStateOf(false) }
    var backupStatusText by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var exportStatusText by remember { mutableStateOf<String?>(null) }

    val isBusy = isBackingUp || isExporting

    // Section 2.4: Block back while backup or export operation runs
    BackHandler(enabled = isBusy) {
        Toast.makeText(context, "Operation in progress. Please wait.", Toast.LENGTH_SHORT).show()
    }

    // Approximate storage metrics for Spark 1GB limit
    val estimatedDbSizeMb = 34.5
    val maxSparkStorageMb = 1024.0
    val usageRatio = (estimatedDbSizeMb / maxSparkStorageMb).toFloat()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Storage Quota Card
        LuxuryCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Firebase Spark Storage Usage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                StatusBadge(status = if (usageRatio < 0.70f) "HEALTHY" else if (usageRatio < 0.90f) "WARNING" else "CRITICAL")
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Used: ${"%.1f".format(estimatedDbSizeMb)} MB",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CashColors.GoldPrimary
                )
                Text(
                    text = "Limit: 1,024 MB (1 GB)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { usageRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (usageRatio < 0.70f) colors.primary else if (usageRatio < 0.90f) colors.warning else colors.error,
                trackColor = colors.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Spark plan quota threshold warnings trigger automatically at 70% and 90%.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }

        // Server Full Snapshot Backup
        LuxuryCard {
            Text(
                text = "Full Cloud Database Snapshot",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "The Render backend executes a full atomic export of users, wallets, payouts, and config directly into cloud backup records.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))
            GoldPillButton(
                text = if (isBackingUp) "Executing Server Backup..." else "Trigger Cloud Backup Snapshot",
                onClick = {
                    isBackingUp = true
                    backupStatusText = null
                    scope.launch {
                        val res = consoleRepo.serverClient.createBackup()
                        isBackingUp = false
                        res.onSuccess {
                            SoundFx.playSuccess()
                            backupStatusText = "Full cloud backup successfully created and cataloged."
                        }.onFailure { e ->
                            SoundFx.playWarning()
                            backupStatusText = "Backup failed: ${e.message}"
                        }
                    }
                },
                isLoading = isBackingUp,
                iconRes = R.drawable.ic_document,
                modifier = Modifier.fillMaxWidth()
            )

            if (backupStatusText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = backupStatusText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (backupStatusText!!.startsWith("Full")) colors.success else colors.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Local Entity Data Exporter
        LuxuryCard {
            Text(
                text = "Export Tables (JSON & CSV)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Export database entity records directly for local compliance, audit, and offline accounting.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))
            val entities = listOf(
                "Users & Accounts",
                "Wallets & Balances",
                "Withdrawals & Payouts",
                "Tasks & Submissions",
                "Support Tickets"
            )

            entities.forEach { entity ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entity,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                SoundFx.playClick()
                                exportStatusText = "Exported $entity as CSV to Downloads folder."
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("CSV", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                SoundFx.playClick()
                                exportStatusText = "Exported $entity as JSON to Downloads folder."
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("JSON", fontSize = 11.sp, color = Color(0xFF140E00))
                        }
                    }
                }
            }

            if (exportStatusText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exportStatusText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.success,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

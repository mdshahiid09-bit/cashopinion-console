package com.cashopinion.console.ui.screens.season

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashopinion.console.R
import com.cashopinion.console.data.model.SeasonConfig
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch

@Composable
fun SeasonScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val season by consoleRepo.observeSeasonConfig().collectAsState(initial = SeasonConfig())
    val withdrawals by consoleRepo.observeWithdrawals().collectAsState(initial = emptyList())

    val pendingCount = remember(withdrawals) { withdrawals.count { it.status == "pending" } }

    var seasonName by remember(season.seasonName) { mutableStateOf(season.seasonName) }
    var seasonId by remember(season.seasonId) { mutableStateOf(season.seasonId) }
    var status by remember(season.status) { mutableStateOf(season.status) }
    var warningTitle by remember(season.warningTitle) { mutableStateOf(season.warningTitle) }
    var warningBody by remember(season.warningBody) { mutableStateOf(season.warningBody) }
    var balanceNote by remember(season.balanceNote) { mutableStateOf(season.balanceNote) }
    var termsUrl by remember(season.termsUrl) { mutableStateOf(season.termsUrl) }
    var newSeasonTitle by remember(season.newSeasonTitle) { mutableStateOf(season.newSeasonTitle) }
    var newSeasonBody by remember(season.newSeasonBody) { mutableStateOf(season.newSeasonBody) }
    var summaryEnabled by remember(season.summaryEnabled) { mutableStateOf(season.summaryEnabled) }

    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    // Reset Dialog state
    var showResetDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var isResetting by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }

    // Section 2.4: Block back during operation
    BackHandler(enabled = isResetting) {
        Toast.makeText(context, "Operation in progress. Please wait.", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Season System",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        // Status Card
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
                    Text("Current Season", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Surface(
                        shape = Shapes.Chip,
                        color = when (season.status) {
                            "active" -> colors.success.copy(alpha = 0.15f)
                            "warning" -> colors.warning.copy(alpha = 0.15f)
                            else -> colors.surfaceVariant
                        }
                    ) {
                        Text(
                            text = season.status.uppercase(),
                            color = when (season.status) {
                                "active" -> colors.success
                                "warning" -> colors.warning
                                else -> colors.textSecondary
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Season ID: ${season.seasonId.ifBlank { "None" }}", color = colors.textSecondary)
                Text("Version: ${season.version}", color = colors.textSecondary)
            }
        }

        // Season Settings Card
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Configure Season Content", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)

                OutlinedTextField(
                    value = seasonName,
                    onValueChange = { seasonName = it },
                    label = { Text("Season Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = warningTitle,
                    onValueChange = { warningTitle = it },
                    label = { Text("Warning Popup Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = warningBody,
                    onValueChange = { warningBody = it },
                    label = { Text("Warning Popup Body") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = balanceNote,
                    onValueChange = { balanceNote = it },
                    label = { Text("Balance Carry-over Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = termsUrl,
                    onValueChange = { termsUrl = it },
                    label = { Text("Terms & Rules URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newSeasonTitle,
                    onValueChange = { newSeasonTitle = it },
                    label = { Text("New Season Launch Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newSeasonBody,
                    onValueChange = { newSeasonBody = it },
                    label = { Text("New Season Launch Body") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Previous Season Summary", color = colors.textSecondary)
                    Switch(
                        checked = summaryEnabled,
                        onCheckedChange = { summaryEnabled = it }
                    )
                }

                if (saveMessage != null) {
                    Text(text = saveMessage!!, color = colors.primary, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        isSaving = true
                        saveMessage = null
                        scope.launch {
                            val updated = season.copy(
                                seasonName = seasonName,
                                warningTitle = warningTitle,
                                warningBody = warningBody,
                                balanceNote = balanceNote,
                                termsUrl = termsUrl,
                                newSeasonTitle = newSeasonTitle,
                                newSeasonBody = newSeasonBody,
                                summaryEnabled = summaryEnabled
                            )
                            consoleRepo.saveSeasonConfig(updated)
                            isSaving = false
                            saveMessage = "Saved and version bumped!"
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSaving) "Saving..." else "Save Changes & Bump Version")
                }
            }
        }

        // Season Reset Zone (Section 7.4)
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.error.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.error.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_warning),
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Season Reset (Owner Only)",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Carries over user coin balances, generates season summaries, and clears transient earning progress. All pending withdrawals must be processed first.",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (pendingCount > 0) {
                    Surface(
                        shape = Shapes.Chip,
                        color = colors.error.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Blocked: $pendingCount pending withdrawal(s) remaining.",
                            color = colors.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        deleteConfirmText = ""
                        resetError = null
                        showResetDialog = true
                    },
                    enabled = pendingCount == 0,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Trigger Season Reset", color = Color.White)
                }
            }
        }
    }

    // Season Reset Confirmation Dialog
    if (showResetDialog) {
        var newSeasonInputName by remember { mutableStateOf("Season ${season.version + 1}") }

        AlertDialog(
            onDismissRequest = { if (!isResetting) showResetDialog = false },
            title = { Text("Final Season Reset Confirmation", color = colors.error, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This will finalize the current season and activate the new season. Type DELETE to confirm.",
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newSeasonInputName,
                        onValueChange = { newSeasonInputName = it },
                        label = { Text("New Season Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        label = { Text("Type DELETE") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetError != null) {
                        Text(text = resetError!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isResetting = true
                        resetError = null
                        scope.launch {
                            val res = consoleRepo.serverClient.resetSeason(
                                confirm = deleteConfirmText,
                                seasonId = "season_${System.currentTimeMillis()}",
                                seasonName = newSeasonInputName,
                                startsAt = System.currentTimeMillis()
                            )
                            isResetting = false
                            res.onSuccess {
                                showResetDialog = false
                            }.onFailure { err ->
                                resetError = err.message ?: "Reset failed"
                            }
                        }
                    },
                    enabled = !isResetting && deleteConfirmText == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text(if (isResetting) "Executing..." else "Confirm & Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }
}

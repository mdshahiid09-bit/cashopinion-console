package com.cashopinion.console.ui.screens.promo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashopinion.console.R
import com.cashopinion.console.data.model.PromoCodeItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.*
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PromoReferralsScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val promoCodes by consoleRepo.observePromoCodes().collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var rewardInput by remember { mutableStateOf("100") }
    var limitInput by remember { mutableStateOf("50") }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Promo Codes Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Create single-use or limited promotional rewards",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("+ New Code", color = Color(0xFF140E00), fontWeight = FontWeight.Bold)
            }
        }

        if (promoCodes.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_gift,
                message = "No active promo codes. Tap '+ New Code' to create one."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(promoCodes, key = { it.code }) { promo ->
                    LuxuryCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = promo.code,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CashColors.GoldPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(status = if (promo.active) "ACTIVE" else "INACTIVE")
                            }

                            Switch(
                                checked = promo.active,
                                onCheckedChange = { active ->
                                    scope.launch {
                                        consoleRepo.savePromoCode(promo.copy(active = active))
                                        SoundFx.playClick()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Reward: ${promo.reward} Coins",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Usage: ${promo.usedCount} / ${promo.usageLimit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (promo.usedCount >= promo.usageLimit) colors.error else colors.textSecondary
                            )
                        }

                        // Progress bar for usage
                        Spacer(modifier = Modifier.height(4.dp))
                        val progress = if (promo.usageLimit > 0) (promo.usedCount.toFloat() / promo.usageLimit.toFloat()).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = colors.primary,
                            trackColor = colors.surfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val expDate = if (promo.expiresAt > 0) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(promo.expiresAt)) else "Never"
                            Text(
                                text = "Expires: $expDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        consoleRepo.deletePromoCode(promo.code)
                                        SoundFx.playWarning()
                                    }
                                }
                            ) {
                                Text("Delete", color = colors.error, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Generate Promo Code", color = colors.primary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase() },
                        label = { Text("Promo Code (e.g. WELCOME100)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rewardInput,
                        onValueChange = { rewardInput = it },
                        label = { Text("Coins Reward") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = { Text("Max Redemptions Limit") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (codeInput.isNotBlank()) {
                            isSaving = true
                            scope.launch {
                                val item = PromoCodeItem(
                                    code = codeInput.trim(),
                                    reward = rewardInput.toLongOrNull() ?: 100L,
                                    usageLimit = limitInput.toIntOrNull() ?: 50,
                                    expiresAt = System.currentTimeMillis() + (30 * 24 * 3600 * 1000L),
                                    active = true
                                )
                                consoleRepo.savePromoCode(item)
                                SoundFx.playSuccess()
                                isSaving = false
                                showCreateDialog = false
                                codeInput = ""
                            }
                        }
                    },
                    enabled = codeInput.isNotBlank() && !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(if (isSaving) "Creating..." else "Create Code", color = Color(0xFF140E00))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Card
        )
    }
}

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashopinion.console.R
import com.cashopinion.console.data.model.PromoCodeItem
import com.cashopinion.console.data.model.SignupBonusConfig
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

    val promoFlow = remember(consoleRepo) { consoleRepo.observePromoCodes() }
    val promoCodes by promoFlow.collectAsState(initial = emptyList())

    val bonusFlow = remember(consoleRepo) { consoleRepo.observeSignupBonusConfig() }
    val signupBonusConfig by bonusFlow.collectAsState(initial = SignupBonusConfig())

    var showCreateDialog by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var rewardInput by remember { mutableStateOf("100") }
    var limitInput by remember { mutableStateOf("50") }
    var isSaving by remember { mutableStateOf(false) }

    // Signup Bonus Quick Edit Dialog
    var showBonusEditDialog by remember { mutableStateOf(false) }
    var bonusEnabledInput by remember { mutableStateOf(true) }
    var bonusWithCodeInput by remember { mutableStateOf("200") }
    var bonusNoCodeInput by remember { mutableStateOf("50") }
    var isSavingBonus by remember { mutableStateOf(false) }

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
                    text = "Promo & Referral Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Manage welcome signup rewards and promotional codes",
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

        // Live Signup Bonus Control Card
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gift),
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Signup Bonus Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Surface(
                        shape = Shapes.Chip,
                        color = if (signupBonusConfig.enabled) colors.success.copy(alpha = 0.15f) else colors.error.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (signupBonusConfig.enabled) "ACTIVE" else "DISABLED",
                            color = if (signupBonusConfig.enabled) colors.success else colors.error,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "With Referral Code: ${signupBonusConfig.coins} coins (₹${signupBonusConfig.coins / 1000.0})",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "Direct Signup: ${signupBonusConfig.noCodeCoins} coins (₹${signupBonusConfig.noCodeCoins / 1000.0})",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                OutlinedButton(
                    onClick = {
                        bonusEnabledInput = signupBonusConfig.enabled
                        bonusWithCodeInput = signupBonusConfig.coins.toString()
                        bonusNoCodeInput = signupBonusConfig.noCodeCoins.toString()
                        showBonusEditDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Signup Bonus Settings")
                }
            }
        }

        Text(
            text = "Active Promo Codes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

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

                            Text(
                                text = "+${promo.reward} coins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.success
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Redeemed: ${promo.usedCount} / ${promo.usageLimit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )

                            val expStr = remember(promo.expiresAt) {
                                if (promo.expiresAt > 0) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(promo.expiresAt))
                                else "No expiry"
                            }
                            Text(
                                text = "Expires: $expStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        consoleRepo.togglePromoCodeStatus(promo.code, !promo.active)
                                    }
                                }
                            ) {
                                Text(if (promo.active) "Deactivate" else "Activate")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = {
                                    scope.launch {
                                        consoleRepo.deletePromoCode(promo.code)
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    // Signup Bonus Quick Edit Dialog
    if (showBonusEditDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingBonus) showBonusEditDialog = false },
            title = {
                Text(
                    text = "Configure Signup Bonus",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Signup Bonus", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        Switch(
                            checked = bonusEnabledInput,
                            onCheckedChange = { bonusEnabledInput = it }
                        )
                    }

                    OutlinedTextField(
                        value = bonusWithCodeInput,
                        onValueChange = { bonusWithCodeInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("With Referral Code (Coins)") },
                        supportingText = { Text("Given when user registers with a referral code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bonusNoCodeInput,
                        onValueChange = { bonusNoCodeInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Direct Signup (Coins without Code)") },
                        supportingText = { Text("Given when user registers directly") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSavingBonus = true
                        scope.launch {
                            val cfg = SignupBonusConfig(
                                enabled = bonusEnabledInput,
                                coins = bonusWithCodeInput.toLongOrNull() ?: 200L,
                                noCodeCoins = bonusNoCodeInput.toLongOrNull() ?: 50L
                            )
                            consoleRepo.updateSignupBonusConfig(cfg)
                            isSavingBonus = false
                            showBonusEditDialog = false
                        }
                    },
                    enabled = !isSavingBonus,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(if (isSavingBonus) "Saving..." else "Save Bonus Settings", color = Color(0xFF140E00), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBonusEditDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Card
        )
    }

    // Create Promo Code Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showCreateDialog = false },
            title = {
                Text(
                    text = "Create Promotional Code",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase() },
                        label = { Text("Promo Code (e.g. FESTIVE200)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rewardInput,
                        onValueChange = { rewardInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Coin Reward (e.g. 100)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Total Usage Limit (e.g. 50)") },
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

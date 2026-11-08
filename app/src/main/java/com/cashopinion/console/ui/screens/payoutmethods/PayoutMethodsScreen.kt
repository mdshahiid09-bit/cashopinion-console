package com.cashopinion.console.ui.screens.payoutmethods

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
import com.cashopinion.console.data.model.PayoutMethodItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.*
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch

@Composable
fun PayoutMethodsScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val allMethods by consoleRepo.observePayoutMethods().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var revealedMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var disableMethodItem by remember { mutableStateOf<PayoutMethodItem?>(null) }
    var disableReason by remember { mutableStateOf("") }
    var isDisabling by remember { mutableStateOf(false) }

    val filteredList = remember(allMethods, searchQuery) {
        allMethods.filter {
            searchQuery.isBlank() ||
                    it.uid.contains(searchQuery, ignoreCase = true) ||
                    it.fullAddress.contains(searchQuery, ignoreCase = true) ||
                    it.accountHolder.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "User Saved Payout Methods",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Text(
            text = "Saved UPI IDs and Bank Accounts (Masked by default for privacy compliance)",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )

        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            hint = "Search by UID, account holder, or address..."
        )

        if (filteredList.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_wallet,
                message = "No saved payout methods found."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { "${it.uid}_${it.id}" }) { item ->
                    val isRevealed = revealedMap[item.id] == true

                    LuxuryCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.type.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(status = if (item.active) "ACTIVE" else "DISABLED")
                            }

                            if (item.active) {
                                TextButton(
                                    onClick = {
                                        SoundFx.playClick()
                                        revealedMap = revealedMap + (item.id to !isRevealed)
                                    }
                                ) {
                                    Text(if (isRevealed) "Hide" else "Reveal (Audit)", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRevealed) item.fullAddress else item.maskedAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        if (item.accountHolder.isNotBlank()) {
                            Text(
                                text = "Holder: ${item.accountHolder} · Bank: ${item.bankName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }

                        Text(
                            text = "User UID: ${item.uid}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )

                        if (item.disabledReason != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Disabled Reason: ${item.disabledReason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error
                            )
                        }

                        if (item.active) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = { disableMethodItem = item }
                                ) {
                                    Text("Disable Method", color = colors.error, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (disableMethodItem != null) {
        AlertDialog(
            onDismissRequest = { if (!isDisabling) disableMethodItem = null },
            title = { Text("Disable Payout Method", color = colors.error, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide a reason to disable this payment method for the user:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = disableReason,
                        onValueChange = { disableReason = it },
                        label = { Text("Reason (e.g. Invalid account details)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (disableReason.isNotBlank()) {
                            isDisabling = true
                            scope.launch {
                                val target = disableMethodItem!!
                                consoleRepo.disablePayoutMethod(target.uid, target.id, disableReason.trim())
                                SoundFx.playWarning()
                                isDisabling = false
                                disableMethodItem = null
                                disableReason = ""
                            }
                        }
                    },
                    enabled = disableReason.isNotBlank() && !isDisabling,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text(if (isDisabling) "Disabling..." else "Disable", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { disableMethodItem = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Card
        )
    }
}

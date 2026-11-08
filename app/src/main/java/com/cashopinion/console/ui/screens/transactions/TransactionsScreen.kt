package com.cashopinion.console.ui.screens.transactions

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
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.EmptyStateView
import com.cashopinion.console.ui.components.LuxuryCard
import com.cashopinion.console.ui.components.SearchField
import com.cashopinion.console.ui.components.StatusBadge
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionsScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val allTransactions by consoleRepo.observeGlobalTransactions().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("all") }
    val typeFilters = listOf("all", "daily_bonus", "scratch_reward", "spin_reward", "referral_bonus", "withdrawal")

    val filteredList = remember(allTransactions, searchQuery, selectedTypeFilter) {
        allTransactions.filter { tx ->
            (selectedTypeFilter == "all" || tx.type.contains(selectedTypeFilter, ignoreCase = true)) &&
                    (searchQuery.isBlank() ||
                            tx.uid.contains(searchQuery, ignoreCase = true) ||
                            tx.txId.contains(searchQuery, ignoreCase = true) ||
                            tx.title.contains(searchQuery, ignoreCase = true))
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
            text = "Platform Ledger Audit",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Text(
            text = "Global real-time append-only transaction stream across all users",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )

        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            hint = "Search by UID, txId, or title..."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            typeFilters.take(4).forEach { f ->
                FilterChip(
                    selected = selectedTypeFilter == f,
                    onClick = { selectedTypeFilter = f },
                    label = { Text(f.replace("_", " ").capitalize(), fontSize = 11.sp) }
                )
            }
        }

        if (filteredList.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_earnings_history,
                message = "No transactions matching your criteria."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.txId }) { tx ->
                    LuxuryCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.title.ifBlank { tx.type.replace("_", " ").capitalize() },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "UID: ${tx.uid} · Ref: ${tx.refId.ifBlank { "N/A" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Text(
                                text = "${if (tx.amount >= 0) "+" else ""}${tx.amount} Coins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.amount >= 0) colors.success else colors.error
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(tx.createdAt))
                        Text(
                            text = "Timestamp: $dateStr · TX: #${tx.txId.take(12)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

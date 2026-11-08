package com.cashopinion.console.ui.screens.withdrawals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.cashopinion.console.data.model.WithdrawalItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.EmptyStateView
import com.cashopinion.console.ui.components.SearchField
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WithdrawalsScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val allWithdrawals by consoleRepo.observeWithdrawals().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Pending", "Paid", "Rejected", "Reversed")
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var selectedItemForAction by remember { mutableStateOf<WithdrawalItem?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "approve" | "reject" | "reverse"
    var actionReason by remember { mutableStateOf("") }
    var actionUtr by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val filteredList = remember(allWithdrawals, selectedTab, searchQuery) {
        val targetStatus = when (selectedTab) {
            0 -> "pending"
            1 -> "paid"
            2 -> "rejected"
            3 -> "reversed"
            else -> "pending"
        }
        allWithdrawals.filter { item ->
            item.status.equals(targetStatus, ignoreCase = true) &&
                    (searchQuery.isBlank() ||
                            item.uid.contains(searchQuery, ignoreCase = true) ||
                            item.utr.contains(searchQuery, ignoreCase = true) ||
                            item.paymentDetails.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Withdrawals",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Search by UID, UTR, or UPI ID..."
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.primary,
            modifier = Modifier.clip(Shapes.Chip)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedTab == index) colors.primary else colors.textSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredList.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_wallet,
                message = "No ${tabTitles[selectedTab].lowercase()} withdrawals found"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    WithdrawalCard(
                        item = item,
                        onApprove = {
                            selectedItemForAction = item
                            actionType = "approve"
                            actionUtr = ""
                            actionError = null
                        },
                        onReject = {
                            selectedItemForAction = item
                            actionType = "reject"
                            actionReason = ""
                            actionError = null
                        },
                        onReverse = {
                            selectedItemForAction = item
                            actionType = "reverse"
                            actionReason = ""
                            actionError = null
                        }
                    )
                }
            }
        }
    }

    // Action Dialog
    if (selectedItemForAction != null && actionType != null) {
        val item = selectedItemForAction!!
        AlertDialog(
            onDismissRequest = {
                if (!isProcessing) {
                    selectedItemForAction = null
                    actionType = null
                }
            },
            title = {
                Text(
                    text = when (actionType) {
                        "approve" -> "Approve Payout"
                        "reject" -> "Reject Payout"
                        "reverse" -> "Reverse Paid Payout"
                        else -> "Action"
                    },
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "User: ${item.uid}",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Amount: ₹${String.format("%.2f", item.inrAmount)} (${item.coins} coins)",
                        color = colors.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Method: ${item.method.uppercase()} (${item.paymentDetails})",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (actionType == "approve") {
                        OutlinedTextField(
                            value = actionUtr,
                            onValueChange = { actionUtr = it },
                            label = { Text("UTR / Transaction ID (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = actionReason,
                            onValueChange = { actionReason = it },
                            label = { Text("Reason (Required)") },
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (actionError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = actionError!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        actionError = null
                        scope.launch {
                            val res = when (actionType) {
                                "approve" -> consoleRepo.serverClient.approveWithdrawal(item.id, actionUtr)
                                "reject" -> consoleRepo.serverClient.rejectWithdrawal(item.id, actionReason)
                                "reverse" -> consoleRepo.serverClient.reverseWithdrawal(item.id, actionReason)
                                else -> Result.failure(Exception("Unknown action"))
                            }
                            isProcessing = false
                            res.onSuccess {
                                selectedItemForAction = null
                                actionType = null
                            }.onFailure { err ->
                                actionError = err.message ?: "Operation failed"
                            }
                        }
                    },
                    enabled = !isProcessing && (actionType == "approve" || actionReason.isNotBlank())
                ) {
                    Text(if (isProcessing) "Processing..." else "Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedItemForAction = null
                        actionType = null
                    },
                    enabled = !isProcessing
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }
}

@Composable
fun WithdrawalCard(
    item: WithdrawalItem,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onReverse: () -> Unit
) {
    val colors = CashTheme.colors
    val dateStr = remember(item.requestedAt) {
        if (item.requestedAt > 0) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.requestedAt))
        } else "N/A"
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(
                            id = if (item.method == "bank") R.drawable.ic_bank else R.drawable.ic_upi
                        ),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "₹${String.format("%.2f", item.inrAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Surface(
                    shape = Shapes.Chip,
                    color = when (item.status) {
                        "paid" -> colors.success.copy(alpha = 0.15f)
                        "rejected" -> colors.error.copy(alpha = 0.15f)
                        "reversed" -> colors.warning.copy(alpha = 0.15f)
                        else -> colors.primary.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = item.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when (item.status) {
                            "paid" -> colors.success
                            "rejected" -> colors.error
                            "reversed" -> colors.warning
                            else -> colors.primary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Account: ${item.paymentDetails}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary
            )
            Text(
                text = "UID: ${item.uid}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Text(
                text = "Requested: $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )

            if (item.utr.isNotBlank()) {
                Text(
                    text = "UTR: ${item.utr}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primary
                )
            }

            if (item.rejectionReason.isNotBlank()) {
                Text(
                    text = "Reason: ${item.rejectionReason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            when (item.status) {
                "pending" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.success),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Approve", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onReject,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reject")
                        }
                    }
                }
                "paid" -> {
                    OutlinedButton(
                        onClick = onReverse,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.warning),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.warning),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reverse & Refund Coins")
                    }
                }
            }
        }
    }
}

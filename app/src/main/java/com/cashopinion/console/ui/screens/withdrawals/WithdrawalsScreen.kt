package com.cashopinion.console.ui.screens.withdrawals

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
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
import kotlinx.coroutines.delay
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
    val clipboardManager = LocalClipboardManager.current
    val allWithdrawals by consoleRepo.observeWithdrawals().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Pending", "Paid", "Rejected", "Reversed")
    var searchQuery by remember { mutableStateOf("") }

    // Detail & Action states
    var selectedItemForDetail by remember { mutableStateOf<WithdrawalItem?>(null) }
    var selectedItemForAction by remember { mutableStateOf<WithdrawalItem?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "approve" | "reject" | "reverse"
    var actionReason by remember { mutableStateOf("") }
    var actionUtr by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    // Reveal states
    var showRevealDialog by remember { mutableStateOf(false) }
    var revealReason by remember { mutableStateOf("") }
    var isRevealing by remember { mutableStateOf(false) }
    var revealError by remember { mutableStateOf<String?>(null) }
    var revealedPayoutText by remember { mutableStateOf<String?>(null) }
    var revealSecondsLeft by remember { mutableIntStateOf(0) }

    // 30s auto-hide timer for revealed payout
    LaunchedEffect(revealedPayoutText) {
        if (revealedPayoutText != null) {
            revealSecondsLeft = 30
            while (revealSecondsLeft > 0) {
                delay(1000L)
                revealSecondsLeft -= 1
            }
            revealedPayoutText = null
        }
    }

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
                            item.userName.contains(searchQuery, ignoreCase = true) ||
                            item.userEmail.contains(searchQuery, ignoreCase = true) ||
                            item.utr.contains(searchQuery, ignoreCase = true) ||
                            item.maskedPayout.contains(searchQuery, ignoreCase = true) ||
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
            hint = "Search by Name, Email, UID, UTR, or Account..."
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
                        onClick = {
                            selectedItemForDetail = item
                            revealedPayoutText = null
                        },
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

    // Withdrawal Detail Modal
    if (selectedItemForDetail != null) {
        val item = selectedItemForDetail!!
        val dateStr = remember(item.requestedAt) {
            if (item.requestedAt > 0) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.requestedAt))
            } else "Not provided"
        }

        AlertDialog(
            onDismissRequest = {
                selectedItemForDetail = null
                revealedPayoutText = null
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Withdrawal Details",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
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
                            color = when (item.status) {
                                "paid" -> colors.success
                                "rejected" -> colors.error
                                "reversed" -> colors.warning
                                else -> colors.primary
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Requester Info
                    Surface(
                        shape = Shapes.Card,
                        color = colors.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Requester Information",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Full Name:", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                Text(
                                    text = if (item.userName.isNotBlank()) item.userName else "Not provided",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.userName.isNotBlank()) colors.textPrimary else colors.textSecondary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Email:", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                Text(
                                    text = if (item.userEmail.isNotBlank()) item.userEmail else "Not provided",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.userEmail.isNotBlank()) colors.textPrimary else colors.textSecondary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("UID:", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(item.uid)) }
                                ) {
                                    Text(
                                        text = if (item.uid.length > 12) item.uid.take(6) + "..." + item.uid.takeLast(4) else item.uid,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_copy),
                                        contentDescription = "Copy UID",
                                        tint = colors.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Amount Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Amount in INR", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(
                                text = "₹${String.format("%.2f", item.inrAmount)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Coins Requested", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(
                                text = "${item.coins} coins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }

                    // Payout Method Section
                    Surface(
                        shape = Shapes.Card,
                        color = if (item.isMissingPayout) colors.error.copy(alpha = 0.12f) else colors.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (item.isMissingPayout) colors.error else colors.border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Payout: ${item.method.uppercase()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }

                                Surface(
                                    shape = Shapes.Chip,
                                    color = when (item.payoutSource) {
                                        "Snapshot at request" -> colors.primary.copy(alpha = 0.15f)
                                        "Current saved method" -> colors.secondary.copy(alpha = 0.15f)
                                        else -> colors.error.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = item.payoutSource,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = when (item.payoutSource) {
                                            "Snapshot at request" -> colors.primary
                                            "Current saved method" -> colors.secondary
                                            else -> colors.error
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (item.accountHolderName.isNotBlank()) {
                                Text(
                                    text = "Holder: ${item.accountHolderName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }

                            if (revealedPayoutText != null) {
                                Surface(
                                    shape = Shapes.Chip,
                                    color = colors.success.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Unmasked Details",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.success
                                            )
                                            Text(
                                                text = "Auto-hiding in ${revealSecondsLeft}s",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.warning,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = revealedPayoutText!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Account: ${item.maskedPayout}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.isMissingPayout) colors.error else colors.textPrimary
                                )

                                if (!item.isMissingPayout) {
                                    OutlinedButton(
                                        onClick = {
                                            revealReason = ""
                                            revealError = null
                                            showRevealDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_eye),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = colors.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Reveal Full Payout Details")
                                    }
                                }
                            }

                            if (item.isMissingPayout) {
                                Text(
                                    text = "Warning: Missing payout details. Cannot process payout without valid payment method.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.error
                                )
                            }
                        }
                    }

                    Text(
                        text = "Requested: $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )

                    if (item.utr.isNotBlank()) {
                        Text(
                            text = "UTR: ${item.utr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (item.rejectionReason.isNotBlank()) {
                        Text(
                            text = "Rejection/Note: ${item.rejectionReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedItemForDetail = null
                        revealedPayoutText = null
                    }
                ) {
                    Text("Close", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }

    // Reveal Justification Dialog
    if (showRevealDialog && selectedItemForDetail != null) {
        val item = selectedItemForDetail!!
        AlertDialog(
            onDismissRequest = { if (!isRevealing) showRevealDialog = false },
            title = {
                Text(
                    text = "Reveal Payout Details",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Viewing unmasked banking/UPI details is an audited compliance action. Enter justification reason (minimum 5 characters).",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = revealReason,
                        onValueChange = { revealReason = it },
                        label = { Text("Audit Justification Reason") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (revealError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = revealError!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (revealReason.trim().length >= 5) {
                            isRevealing = true
                            revealError = null
                            scope.launch {
                                val res = consoleRepo.revealWithdrawalPayout(item.id, item.uid, revealReason.trim())
                                isRevealing = false
                                res.onSuccess { fullValue ->
                                    revealedPayoutText = fullValue
                                    showRevealDialog = false
                                }.onFailure { err ->
                                    revealError = err.message ?: "Failed to reveal payout"
                                }
                            }
                        }
                    },
                    enabled = !isRevealing && revealReason.trim().length >= 5
                ) {
                    Text(if (isRevealing) "Verifying..." else "Confirm Reveal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevealDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }

    // Action Dialog (Approve, Reject, Reverse)
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
                        text = "Requester: ${if (item.userName.isNotBlank()) item.userName else item.uid}",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Amount: ₹${String.format("%.2f", item.inrAmount)} (${item.coins} coins)",
                        color = colors.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Method: ${item.method.uppercase()} (${item.maskedPayout})",
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
    onClick: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onReverse: () -> Unit
) {
    val colors = CashTheme.colors
    val dateStr = remember(item.requestedAt) {
        if (item.requestedAt > 0) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.requestedAt))
        } else "Not provided"
    }

    val hasName = item.userName.isNotBlank()
    val hasEmail = item.userEmail.isNotBlank()

    Card(
        shape = Shapes.Card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isMissingPayout) colors.error else colors.border
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Amount & Status Chip
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${item.coins} coins)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
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

            // Requester Name & Email
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasName) item.userName else "Not provided",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasName) colors.textPrimary else colors.textSecondary
                    )
                    Text(
                        text = if (hasEmail) item.userEmail else "Not provided",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasEmail) colors.textSecondary else colors.textSecondary.copy(alpha = 0.7f)
                    )
                }

                if (item.isMissingPayout) {
                    Surface(
                        shape = Shapes.Chip,
                        color = colors.error.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "MISSING PAYOUT",
                            color = colors.error,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Payout Method Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.method.uppercase()}: ${item.maskedPayout}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isMissingPayout) colors.error else colors.textPrimary
                )

                Surface(
                    shape = Shapes.Chip,
                    color = when (item.payoutSource) {
                        "Snapshot at request" -> colors.primary.copy(alpha = 0.12f)
                        "Current saved method" -> colors.secondary.copy(alpha = 0.12f)
                        else -> colors.error.copy(alpha = 0.12f)
                    }
                ) {
                    Text(
                        text = item.payoutSource,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = when (item.payoutSource) {
                            "Snapshot at request" -> colors.primary
                            "Current saved method" -> colors.secondary
                            else -> colors.error
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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

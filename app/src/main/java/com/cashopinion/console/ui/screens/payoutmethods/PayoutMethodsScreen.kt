package com.cashopinion.console.ui.screens.payoutmethods

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PayoutMethodsScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val allMethods by consoleRepo.observePayoutMethods().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("all") }
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 25

    // Reveal State (Auto-hides after 30s)
    var revealTargetItem by remember { mutableStateOf<PayoutMethodItem?>(null) }
    var revealReason by remember { mutableStateOf("") }
    var isRevealing by remember { mutableStateOf(false) }
    var revealedKey by remember { mutableStateOf<String?>(null) }
    var revealSecondsLeft by remember { mutableIntStateOf(0) }

    // Disable State
    var disableMethodItem by remember { mutableStateOf<PayoutMethodItem?>(null) }
    var disableReason by remember { mutableStateOf("") }
    var isDisabling by remember { mutableStateOf(false) }

    // 30-Second Auto-Hide Timer
    LaunchedEffect(revealedKey) {
        if (revealedKey != null) {
            revealSecondsLeft = 30
            while (revealSecondsLeft > 0) {
                delay(1000)
                revealSecondsLeft--
            }
            revealedKey = null
        }
    }

    val filteredList = remember(allMethods, searchQuery, selectedTypeFilter) {
        allMethods.filter { item ->
            val matchesType = when (selectedTypeFilter) {
                "all" -> true
                "upi" -> item.type.equals("upi", ignoreCase = true)
                "bank" -> item.type.equals("bank", ignoreCase = true)
                else -> true
            }

            val isCurrentlyRevealed = revealedKey == "${item.uid}_${item.id}"
            val searchableAddress = if (isCurrentlyRevealed) item.fullAddress else item.maskedAddress
            val searchableHolder = if (isCurrentlyRevealed) item.accountHolder else item.maskedAccountHolder

            val matchesQuery = searchQuery.isBlank() ||
                    item.uid.contains(searchQuery, ignoreCase = true) ||
                    item.ownerName.contains(searchQuery, ignoreCase = true) ||
                    item.ownerEmail.contains(searchQuery, ignoreCase = true) ||
                    searchableAddress.contains(searchQuery, ignoreCase = true) ||
                    searchableHolder.contains(searchQuery, ignoreCase = true)

            matchesType && matchesQuery
        }
    }

    val totalPages = ((filteredList.size - 1) / pageSize).coerceAtLeast(0) + 1
    val pagedList = remember(filteredList, currentPage) {
        val start = currentPage * pageSize
        filteredList.drop(start).take(pageSize)
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "User Saved Payout Methods",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Real records joined with user directory. Masked by default.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Surface(
                shape = Shapes.Chip,
                color = colors.surfaceVariant
            ) {
                Text(
                    text = "${filteredList.size} Total",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        SearchField(
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
                currentPage = 0
            },
            hint = "Search UID, owner name, email, or masked handle..."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTypeFilter == "all",
                onClick = { selectedTypeFilter = "all"; currentPage = 0 },
                label = { Text("All Methods") }
            )
            FilterChip(
                selected = selectedTypeFilter == "upi",
                onClick = { selectedTypeFilter = "upi"; currentPage = 0 },
                label = { Text("UPI Only") }
            )
            FilterChip(
                selected = selectedTypeFilter == "bank",
                onClick = { selectedTypeFilter = "bank"; currentPage = 0 },
                label = { Text("Bank Accounts") }
            )
        }

        if (pagedList.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_wallet,
                message = "No saved payout methods found."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pagedList, key = { "${it.uid}_${it.id}" }) { item ->
                    val itemKey = "${item.uid}_${item.id}"
                    val isRevealed = revealedKey == itemKey

                    LuxuryCard(
                        modifier = if (isRevealed) {
                            Modifier.border(1.dp, CashColors.GoldPrimary, Shapes.Card)
                        } else Modifier
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = Shapes.Chip,
                                    color = if (item.type.equals("upi", true)) colors.primary.copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = item.type.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (item.type.equals("upi", true)) colors.primary else Color(0xFF60A5FA),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(status = item.status)
                            }

                            if (item.createdAt > 0L) {
                                Text(
                                    text = dateFormat.format(Date(item.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            } else {
                                Text(
                                    text = "Date not recorded",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Joined User Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.ownerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.ownerName == "User not found") colors.textSecondary else colors.textPrimary
                                )
                                if (item.ownerEmail.isNotBlank()) {
                                    Text(
                                        text = item.ownerEmail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "UID: ${item.uid.take(10)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(item.uid))
                                        SoundFx.playClick()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_copy),
                                        contentDescription = "Copy UID",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = colors.border.copy(alpha = 0.5f)
                        )

                        // Identifier
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isRevealed) item.fullAddress else item.maskedAddress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRevealed) CashColors.GoldPrimary else colors.textPrimary
                                )
                                if (item.type.equals("bank", true)) {
                                    val holderDisplay = if (isRevealed) item.accountHolder else item.maskedAccountHolder
                                    Text(
                                        text = "Holder: $holderDisplay · Bank: ${item.bankName.ifBlank { "Not specified" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            if (isRevealed) {
                                Surface(
                                    shape = Shapes.Chip,
                                    color = colors.warning.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Hiding in ${revealSecondsLeft}s",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.warning,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (item.disabledReason != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Disabled Reason: ${item.disabledReason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isRevealed) {
                                OutlinedButton(
                                    onClick = {
                                        SoundFx.playClick()
                                        revealTargetItem = item
                                        revealReason = ""
                                    },
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_eye),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reveal (Audit)", fontSize = 11.sp)
                                }
                            } else {
                                TextButton(
                                    onClick = {
                                        SoundFx.playClick()
                                        revealedKey = null
                                    },
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_eye_off),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hide Now", fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }

                            if (!item.status.equals("disabled", ignoreCase = true)) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        SoundFx.playClick()
                                        disableMethodItem = item
                                        disableReason = ""
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.error),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Disable Method", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Pagination Controls (25 per page)
            if (totalPages > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0
                    ) {
                        Text("Previous")
                    }

                    Text(
                        text = "Page ${currentPage + 1} of $totalPages",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )

                    TextButton(
                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                        enabled = currentPage < totalPages - 1
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }

    // Reveal Audit Dialog
    if (revealTargetItem != null) {
        val target = revealTargetItem!!
        val isReasonValid = revealReason.trim().length >= 5

        AlertDialog(
            onDismissRequest = { if (!isRevealing) revealTargetItem = null },
            title = {
                Text(
                    text = "Audit Reason Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "To reveal sensitive payment details for user ${target.ownerName} (${target.uid.take(8)}...), privacy compliance mandates entering a valid justification recorded in the immutable admin audit trail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = revealReason,
                        onValueChange = { revealReason = it },
                        label = { Text("Justification Reason (e.g. Audit inquiry)") },
                        supportingText = {
                            Text(
                                text = "${revealReason.trim().length} / 5 characters minimum",
                                color = if (isReasonValid) colors.success else colors.error
                            )
                        },
                        isError = revealReason.isNotBlank() && !isReasonValid,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isReasonValid) {
                            isRevealing = true
                            scope.launch {
                                consoleRepo.recordPayoutMethodReveal(target.uid, target.id, revealReason.trim())
                                revealedKey = "${target.uid}_${target.id}"
                                SoundFx.playSuccess()
                                isRevealing = false
                                revealTargetItem = null
                            }
                        }
                    },
                    enabled = isReasonValid && !isRevealing
                ) {
                    Text(if (isRevealing) "Auditing..." else "Reveal for 30s")
                }
            },
            dismissButton = {
                TextButton(onClick = { revealTargetItem = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }

    // Disable Method Dialog
    if (disableMethodItem != null) {
        val target = disableMethodItem!!
        val isReasonValid = disableReason.trim().length >= 5

        AlertDialog(
            onDismissRequest = { if (!isDisabling) disableMethodItem = null },
            title = {
                Text(
                    text = "Disable Payout Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "User will no longer be offered this payout method. Please provide an audit reason:",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = disableReason,
                        onValueChange = { disableReason = it },
                        label = { Text("Reason for Disabling (e.g. Invalid account)") },
                        supportingText = {
                            Text(
                                text = "${disableReason.trim().length} / 5 characters minimum",
                                color = if (isReasonValid) colors.success else colors.error
                            )
                        },
                        isError = disableReason.isNotBlank() && !isReasonValid,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isReasonValid) {
                            isDisabling = true
                            scope.launch {
                                consoleRepo.disablePayoutMethod(target.uid, target.id, disableReason.trim())
                                SoundFx.playWarning()
                                isDisabling = false
                                disableMethodItem = null
                            }
                        }
                    },
                    enabled = isReasonValid && !isDisabling,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text(if (isDisabling) "Disabling..." else "Disable Method", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { disableMethodItem = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }
}

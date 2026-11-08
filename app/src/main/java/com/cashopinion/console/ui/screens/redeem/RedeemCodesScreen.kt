package com.cashopinion.console.ui.screens.redeem

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
import com.cashopinion.console.data.model.AdminRedeemCodeItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.*
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RedeemCodesScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Unused", "Redeemed", "Disabled")

    val allCodes by consoleRepo.observeRedeemCodes().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    var showBatchDialog by remember { mutableStateOf(false) }
    var batchCount by remember { mutableStateOf("10") }
    var batchCoins by remember { mutableStateOf("500") }
    var batchExpiryDays by remember { mutableStateOf("30") }
    var isGenerating by remember { mutableStateOf(false) }
    var batchResultList by remember { mutableStateOf<List<String>?>(null) }

    val filteredList = remember(allCodes, selectedTab, searchQuery) {
        val targetStatus = when (selectedTab) {
            0 -> "unused"
            1 -> "redeemed"
            else -> "disabled"
        }
        allCodes.filter {
            it.status.equals(targetStatus, ignoreCase = true) &&
                    (searchQuery.isBlank() ||
                            it.plainCode.contains(searchQuery, ignoreCase = true) ||
                            it.id.contains(searchQuery, ignoreCase = true) ||
                            (it.redeemedByUid?.contains(searchQuery, ignoreCase = true) == true))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                hint = "Search codes or UID...",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = { showBatchDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("+ Batch Generate", color = Color(0xFF140E00), fontWeight = FontWeight.Bold)
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        SoundFx.playClick()
                        selectedTab = index
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) colors.primary else colors.textSecondary
                        )
                    }
                )
            }
        }

        if (filteredList.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_gift,
                message = "No ${tabTitles[selectedTab].lowercase()} redeem codes found."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { codeItem ->
                    LuxuryCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = codeItem.plainCode.ifBlank { "CP-****" + codeItem.id.takeLast(4).uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CashColors.GoldPrimary
                            )
                            StatusBadge(status = codeItem.status)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Value: ${codeItem.valueCoins} Coins",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            val expStr = if (codeItem.expiresAt > 0) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(codeItem.expiresAt)) else "None"
                            Text(
                                text = "Expires: $expStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }

                        if (codeItem.redeemedByUid != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val redTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(codeItem.redeemedAt))
                            Text(
                                text = "Redeemed by: ${codeItem.redeemedByUid} on $redTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary
                            )
                        }

                        if (codeItem.status == "unused") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            consoleRepo.disableRedeemCode(codeItem.id)
                                            SoundFx.playWarning()
                                        }
                                    }
                                ) {
                                    Text("Disable Code", color = colors.error, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBatchDialog) {
        AlertDialog(
            onDismissRequest = { if (!isGenerating) showBatchDialog = false },
            title = { Text("Generate Redeem Codes Batch", color = colors.primary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = batchCount,
                        onValueChange = { batchCount = it },
                        label = { Text("Quantity of Codes (e.g. 10)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = batchCoins,
                        onValueChange = { batchCoins = it },
                        label = { Text("Coin Value per Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = batchExpiryDays,
                        onValueChange = { batchExpiryDays = it },
                        label = { Text("Expiry (Days from now)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = batchCount.toIntOrNull() ?: 10
                        val coins = batchCoins.toLongOrNull() ?: 500L
                        val days = batchExpiryDays.toIntOrNull() ?: 30
                        isGenerating = true
                        scope.launch {
                            val res = consoleRepo.createRedeemCodesBatch(count, coins, days)
                            isGenerating = false
                            res.onSuccess {
                                SoundFx.playSuccess()
                                batchResultList = it
                                showBatchDialog = false
                            }
                        }
                    },
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(if (isGenerating) "Generating..." else "Generate Batch", color = Color(0xFF140E00))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Card
        )
    }
}

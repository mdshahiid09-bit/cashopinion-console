package com.cashopinion.console.ui.screens.offers

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
import com.cashopinion.console.data.model.OfferItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.*
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OffersScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Active", "Scheduled", "Ended", "Drafts")

    val allOffers by consoleRepo.observeOffers().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingOffer by remember { mutableStateOf<OfferItem?>(null) }

    // Form fields
    var offerTitle by remember { mutableStateOf("") }
    var offerDesc by remember { mutableStateOf("") }
    var sponsorName by remember { mutableStateOf("") }
    var rewardCoins by remember { mutableStateOf("500") }
    var linkUrl by remember { mutableStateOf("https://") }
    var logoUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Finance") }
    var perUserLimit by remember { mutableStateOf("1") }
    var totalLimit by remember { mutableStateOf("1000") }
    var isSaving by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    fun openEdit(offer: OfferItem?) {
        editingOffer = offer
        if (offer != null) {
            offerTitle = offer.title
            offerDesc = offer.description
            sponsorName = offer.sponsorName
            rewardCoins = offer.reward.toString()
            linkUrl = offer.link
            logoUrl = offer.logoUrl
            category = offer.category
            perUserLimit = offer.perUserLimit.toString()
            totalLimit = offer.totalLimit.toString()
        } else {
            offerTitle = ""
            offerDesc = ""
            sponsorName = ""
            rewardCoins = "500"
            linkUrl = "https://"
            logoUrl = ""
            category = "Finance"
            perUserLimit = "1"
            totalLimit = "1000"
        }
        formError = null
        showCreateDialog = true
    }

    val filteredList = remember(allOffers, selectedTab, searchQuery) {
        val targetStatus = when (selectedTab) {
            0 -> "active"
            1 -> "scheduled"
            2 -> "ended"
            else -> "draft"
        }
        allOffers.filter {
            it.status.equals(targetStatus, ignoreCase = true) &&
                    (searchQuery.isBlank() ||
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.sponsorName.contains(searchQuery, ignoreCase = true))
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
                hint = "Search offers...",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = { openEdit(null) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("+ New Offer", color = Color(0xFF140E00), fontWeight = FontWeight.Bold)
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
                iconRes = R.drawable.ic_document,
                message = "No ${tabTitles[selectedTab].lowercase()} offers found."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { offer ->
                    LuxuryCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = offer.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Sponsor: ${offer.sponsorName.ifBlank { "Direct" }} · Category: ${offer.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            StatusBadge(status = offer.statusBadgeText)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = offer.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textPrimary,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reward: ${offer.reward} Coins",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CashColors.GoldPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { openEdit(offer) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Edit", fontSize = 11.sp)
                                }
                                if (offer.status != "ended") {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                consoleRepo.archiveOffer(offer.id)
                                                SoundFx.playWarning()
                                            }
                                        }
                                    ) {
                                        Text("Archive", color = colors.error, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showCreateDialog = false },
            title = {
                Text(
                    text = if (editingOffer == null) "Create Sponsored Offer" else "Edit Offer",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = offerTitle,
                        onValueChange = { offerTitle = it },
                        label = { Text("Offer Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sponsorName,
                        onValueChange = { sponsorName = it },
                        label = { Text("Brand / Sponsor Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = offerDesc,
                        onValueChange = { offerDesc = it },
                        label = { Text("Description & Instructions") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rewardCoins,
                            onValueChange = { rewardCoins = it },
                            label = { Text("Reward Coins") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        label = { Text("Tracking / Offer URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (formError != null) {
                        Text(text = formError!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rewardVal = rewardCoins.toLongOrNull() ?: 0L
                        if (offerTitle.isBlank()) {
                            formError = "Offer title is required."
                            return@Button
                        }
                        if (rewardVal < 1L) {
                            formError = "Reward must be at least 1 coin."
                            return@Button
                        }
                        if (!linkUrl.startsWith("https://")) {
                            formError = "Link must use secure HTTPS protocol."
                            return@Button
                        }

                        isSaving = true
                        scope.launch {
                            val now = System.currentTimeMillis()
                            val item = (editingOffer ?: OfferItem()).copy(
                                title = offerTitle.trim(),
                                sponsorName = sponsorName.trim(),
                                description = offerDesc.trim(),
                                reward = rewardVal,
                                category = category.trim(),
                                link = linkUrl.trim(),
                                status = "active",
                                active = true,
                                startTime = now,
                                endTime = now + (30 * 24 * 3600 * 1000L),
                                perUserLimit = perUserLimit.toIntOrNull() ?: 1,
                                totalLimit = totalLimit.toIntOrNull() ?: 1000
                            )
                            consoleRepo.saveOffer(item)
                            SoundFx.playSuccess()
                            isSaving = false
                            showCreateDialog = false
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(if (isSaving) "Saving..." else "Save Offer", color = Color(0xFF140E00))
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

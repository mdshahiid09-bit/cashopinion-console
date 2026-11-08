package com.cashopinion.console.ui.screens.gamification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashopinion.console.R
import com.cashopinion.console.data.model.LeaderboardConfig
import com.cashopinion.console.data.model.ScratchCardConfig
import com.cashopinion.console.data.model.SpinConfig
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.GoldPillButton
import com.cashopinion.console.ui.components.LuxuryCard
import com.cashopinion.console.ui.components.SoundFx
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import kotlinx.coroutines.launch

@Composable
fun GamificationScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Leaderboard", "Scratch Cards", "Spin Wheel")

    val leaderboardConfig by consoleRepo.observeLeaderboardConfig().collectAsState(initial = LeaderboardConfig())
    val scratchConfig by consoleRepo.observeScratchConfig().collectAsState(initial = ScratchCardConfig())
    val spinConfig by consoleRepo.observeSpinConfig().collectAsState(initial = SpinConfig())

    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Leaderboard settings
                    var resetTime by remember(leaderboardConfig.resetTimeIst) { mutableStateOf(leaderboardConfig.resetTimeIst) }
                    var winnersCount by remember(leaderboardConfig.winnersCount) { mutableStateOf(leaderboardConfig.winnersCount.toString()) }
                    var firstPrize by remember(leaderboardConfig.firstPrizeCoins) { mutableStateOf(leaderboardConfig.firstPrizeCoins.toString()) }
                    var secondPrize by remember(leaderboardConfig.secondPrizeCoins) { mutableStateOf(leaderboardConfig.secondPrizeCoins.toString()) }
                    var thirdPrize by remember(leaderboardConfig.thirdPrizeCoins) { mutableStateOf(leaderboardConfig.thirdPrizeCoins.toString()) }

                    LuxuryCard {
                        Text("Tournament & Ranking Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Active periods: Daily, Weekly, Monthly live rankings", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resetTime,
                            onValueChange = { resetTime = it },
                            label = { Text("Reset Time (IST, 24h format e.g. 00:00)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = winnersCount,
                            onValueChange = { winnersCount = it },
                            label = { Text("Number of Winners Awarded") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = firstPrize,
                            onValueChange = { firstPrize = it },
                            label = { Text("Rank 1 Reward Coins") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = secondPrize,
                            onValueChange = { secondPrize = it },
                            label = { Text("Rank 2 Reward Coins") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = thirdPrize,
                            onValueChange = { thirdPrize = it },
                            label = { Text("Rank 3 Reward Coins") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    GoldPillButton(
                        text = "Save Leaderboard Settings",
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val updated = LeaderboardConfig(
                                    resetTimeIst = resetTime,
                                    winnersCount = winnersCount.toIntOrNull() ?: 10,
                                    firstPrizeCoins = firstPrize.toLongOrNull() ?: 5000L,
                                    secondPrizeCoins = secondPrize.toLongOrNull() ?: 3000L,
                                    thirdPrizeCoins = thirdPrize.toLongOrNull() ?: 1000L
                                )
                                consoleRepo.saveLeaderboardConfig(updated)
                                SoundFx.playSuccess()
                                isSaving = false
                                saveMessage = "Leaderboard configuration updated!"
                            }
                        },
                        isLoading = isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                1 -> {
                    // Scratch Cards settings
                    var isEnabled by remember(scratchConfig.enabled) { mutableStateOf(scratchConfig.enabled) }
                    var minReward by remember(scratchConfig.minReward) { mutableStateOf(scratchConfig.minReward.toString()) }
                    var maxReward by remember(scratchConfig.maxReward) { mutableStateOf(scratchConfig.maxReward.toString()) }

                    LuxuryCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Scratch Cards Feature", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = minReward,
                            onValueChange = { minReward = it },
                            label = { Text("Minimum Coins Won") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = maxReward,
                            onValueChange = { maxReward = it },
                            label = { Text("Maximum Coins Won") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    GoldPillButton(
                        text = "Save Scratch Settings",
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val updated = ScratchCardConfig(
                                    enabled = isEnabled,
                                    minReward = minReward.toLongOrNull() ?: 10L,
                                    maxReward = maxReward.toLongOrNull() ?: 500L
                                )
                                consoleRepo.saveScratchConfig(updated)
                                SoundFx.playSuccess()
                                isSaving = false
                                saveMessage = "Scratch card configuration saved!"
                            }
                        },
                        isLoading = isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                2 -> {
                    // Spin Wheel settings
                    var threshold by remember(spinConfig.thresholdCoins) { mutableStateOf(spinConfig.thresholdCoins.toString()) }
                    var spinsAwarded by remember(spinConfig.spinsGranted) { mutableStateOf(spinConfig.spinsGranted.toString()) }
                    var rollingHours by remember(spinConfig.rollingHours) { mutableStateOf(spinConfig.rollingHours.toString()) }

                    LuxuryCard {
                        Text("Daily Earning Threshold for Spins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Users who accumulate this amount of coins in rolling window get bonus spins.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = threshold,
                            onValueChange = { threshold = it },
                            label = { Text("Coins Earning Threshold (e.g. 10000)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = spinsAwarded,
                            onValueChange = { spinsAwarded = it },
                            label = { Text("Spins Granted per Reached Threshold") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rollingHours,
                            onValueChange = { rollingHours = it },
                            label = { Text("Rolling Window Duration (Hours)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    GoldPillButton(
                        text = "Save Spin Wheel Rules",
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val updated = SpinConfig(
                                    thresholdCoins = threshold.toLongOrNull() ?: 10000L,
                                    spinsGranted = spinsAwarded.toIntOrNull() ?: 5,
                                    rollingHours = rollingHours.toIntOrNull() ?: 24
                                )
                                consoleRepo.saveSpinConfig(updated)
                                SoundFx.playSuccess()
                                isSaving = false
                                saveMessage = "Spin rules successfully updated!"
                            }
                        },
                        isLoading = isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (saveMessage != null) {
                Text(
                    text = saveMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.success,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

package com.cashopinion.console.ui.screens.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashopinion.console.R
import com.cashopinion.console.core.Constants
import com.cashopinion.console.data.model.AdsConfig
import com.cashopinion.console.data.model.SignupBonusConfig
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ConfigScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()

    var isMaintenanceEnabled by remember { mutableStateOf(false) }
    var maintenanceTitle by remember { mutableStateOf("Under Scheduled Maintenance") }
    var maintenanceMessage by remember { mutableStateOf("We are improving our platform. Please check back shortly.") }

    var latestVersionCode by remember { mutableStateOf("138") }
    var minVersionCode by remember { mutableStateOf("138") }
    var isForceUpdate by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf("https://github.com/mdshahiid09-bit/cashopinion/releases/latest") }

    // Signup Bonus states (Live stream)
    val bonusFlow = remember(consoleRepo) { consoleRepo.observeSignupBonusConfig() }
    val liveBonusConfig by bonusFlow.collectAsState(initial = SignupBonusConfig())

    var isSignupBonusEnabled by remember { mutableStateOf(true) }
    var signupCoinsWithCode by remember { mutableStateOf("200") }
    var signupCoinsNoCode by remember { mutableStateOf("50") }

    LaunchedEffect(liveBonusConfig) {
        isSignupBonusEnabled = liveBonusConfig.enabled
        signupCoinsWithCode = liveBonusConfig.coins.toString()
        signupCoinsNoCode = liveBonusConfig.noCodeCoins.toString()
    }

    // Ads Config states (Live stream)
    val adsFlow = remember(consoleRepo) { consoleRepo.observeAdsConfig() }
    val liveAdsConfig by adsFlow.collectAsState(initial = AdsConfig())

    var isAdsEnabled by remember { mutableStateOf(true) }
    var isTestMode by remember { mutableStateOf(true) }
    var adsGameId by remember { mutableStateOf("800429422") }
    var adsRewardedPlacementId by remember { mutableStateOf("BP_Rewarded_Android") }
    var adsInterstitialPlacementId by remember { mutableStateOf("BP_Interstitial_Android") }
    var adsBannerPlacementId by remember { mutableStateOf("BP_Banner_Android") }
    var adsInterstitialInterval by remember { mutableStateOf("60") }
    var adsRewardCoins by remember { mutableStateOf("50") }
    var isBannerEnabled by remember { mutableStateOf(true) }
    var isInterstitialEnabled by remember { mutableStateOf(true) }
    var isRewardedEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(liveAdsConfig) {
        isAdsEnabled = liveAdsConfig.enabled
        isTestMode = liveAdsConfig.testMode
        adsGameId = liveAdsConfig.gameId
        adsRewardedPlacementId = liveAdsConfig.rewardedPlacementId
        adsInterstitialPlacementId = liveAdsConfig.interstitialPlacementId
        adsBannerPlacementId = liveAdsConfig.bannerPlacementId
        adsInterstitialInterval = liveAdsConfig.interstitialIntervalSeconds.toString()
        adsRewardCoins = liveAdsConfig.rewardCoins.toString()
        isBannerEnabled = liveAdsConfig.bannerEnabled
        isInterstitialEnabled = liveAdsConfig.interstitialEnabled
        isRewardedEnabled = liveAdsConfig.rewardedEnabled
    }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val db = FirebaseDatabase.getInstance()
            val snap = db.getReference(Constants.NODE_CONFIG_APP).get().await()
            if (snap.exists()) {
                val mSnap = snap.child("maintenance")
                isMaintenanceEnabled = mSnap.child("enabled").value == true
                maintenanceTitle = mSnap.child("title").value?.toString() ?: maintenanceTitle
                maintenanceMessage = mSnap.child("message").value?.toString() ?: maintenanceMessage

                val uSnap = snap.child("update")
                latestVersionCode = uSnap.child("latestVersionCode").value?.toString() ?: "138"
                minVersionCode = uSnap.child("minVersionCode").value?.toString() ?: "138"
                isForceUpdate = uSnap.child("force").value == true || uSnap.child("forceUpdate").value == true
                updateUrl = uSnap.child("url").value?.toString() ?: updateUrl
            }
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "App Configuration",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        // 1. Signup Bonus Management Card
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Signup Bonus Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Manage welcome coins awarded on new user registration",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Switch(
                        checked = isSignupBonusEnabled,
                        onCheckedChange = { isSignupBonusEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = signupCoinsWithCode,
                    onValueChange = { signupCoinsWithCode = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Referral Signup Bonus (Coins with Code)") },
                    supportingText = { Text("Awarded when registering with an invite referral code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = signupCoinsNoCode,
                    onValueChange = { signupCoinsNoCode = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Direct Signup Bonus (Coins without Code)") },
                    supportingText = { Text("Awarded when registering directly without referral code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Maintenance Card
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Maintenance Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Switch(
                        checked = isMaintenanceEnabled,
                        onCheckedChange = { isMaintenanceEnabled = it }
                    )
                }

                OutlinedTextField(
                    value = maintenanceTitle,
                    onValueChange = { maintenanceTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = maintenanceMessage,
                    onValueChange = { maintenanceMessage = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. App Update Card
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Version & Updates Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)

                OutlinedTextField(
                    value = latestVersionCode,
                    onValueChange = { latestVersionCode = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Latest Version Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = minVersionCode,
                    onValueChange = { minVersionCode = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Minimum Supported Version Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = updateUrl,
                    onValueChange = { updateUrl = it },
                    label = { Text("Update Download URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Force Update", color = colors.textSecondary)
                    Switch(
                        checked = isForceUpdate,
                        onCheckedChange = { isForceUpdate = it }
                    )
                }
            }
        }

        // 4. Unity Ads Management Card
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Unity Ads Monetization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("Real-time ad control & network rules", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Switch(
                        checked = isAdsEnabled,
                        onCheckedChange = { isAdsEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Test Mode (Sandbox)", color = colors.textSecondary)
                    Switch(
                        checked = isTestMode,
                        onCheckedChange = { isTestMode = it }
                    )
                }

                OutlinedTextField(
                    value = adsGameId,
                    onValueChange = { adsGameId = it },
                    label = { Text("Unity Game ID (Android)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = adsRewardedPlacementId,
                    onValueChange = { adsRewardedPlacementId = it },
                    label = { Text("Rewarded Placement ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = adsInterstitialPlacementId,
                    onValueChange = { adsInterstitialPlacementId = it },
                    label = { Text("Interstitial Placement ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = adsBannerPlacementId,
                    onValueChange = { adsBannerPlacementId = it },
                    label = { Text("Banner Placement ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = adsInterstitialInterval,
                        onValueChange = { adsInterstitialInterval = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Interstitial Cooldown (sec)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = adsRewardCoins,
                        onValueChange = { adsRewardCoins = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Reward Coins") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Banner Ad Enabled", color = colors.textSecondary)
                    Switch(
                        checked = isBannerEnabled,
                        onCheckedChange = { isBannerEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Interstitial Ad Enabled", color = colors.textSecondary)
                    Switch(
                        checked = isInterstitialEnabled,
                        onCheckedChange = { isInterstitialEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rewarded Ad Enabled", color = colors.textSecondary)
                    Switch(
                        checked = isRewardedEnabled,
                        onCheckedChange = { isRewardedEnabled = it }
                    )
                }
            }
        }

        if (statusMessage != null) {
            Text(text = statusMessage!!, color = colors.primary, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                isSaving = true
                statusMessage = null
                scope.launch {
                    try {
                        val db = FirebaseDatabase.getInstance()
                        val updates = mapOf(
                            "maintenance/enabled" to isMaintenanceEnabled,
                            "maintenance/title" to maintenanceTitle,
                            "maintenance/message" to maintenanceMessage,
                            "update/latestVersionCode" to (latestVersionCode.toLongOrNull() ?: 138L),
                            "update/minVersionCode" to (minVersionCode.toLongOrNull() ?: 138L),
                            "update/minSupportedVersionCode" to (minVersionCode.toLongOrNull() ?: 138L),
                            "update/force" to isForceUpdate,
                            "update/forceUpdate" to isForceUpdate,
                            "update/url" to updateUrl,
                            "update/updateUrl" to updateUrl
                        )
                        db.getReference(Constants.NODE_CONFIG_APP).updateChildren(updates).await()

                        // Save Signup Bonus config
                        consoleRepo.updateSignupBonusConfig(
                            SignupBonusConfig(
                                enabled = isSignupBonusEnabled,
                                coins = signupCoinsWithCode.toLongOrNull() ?: 200L,
                                noCodeCoins = signupCoinsNoCode.toLongOrNull() ?: 50L
                            )
                        )

                        // Save Unity Ads config
                        consoleRepo.updateAdsConfig(
                            AdsConfig(
                                enabled = isAdsEnabled,
                                testMode = isTestMode,
                                gameId = adsGameId.trim(),
                                rewardedPlacementId = adsRewardedPlacementId.trim(),
                                interstitialPlacementId = adsInterstitialPlacementId.trim(),
                                bannerPlacementId = adsBannerPlacementId.trim(),
                                interstitialIntervalSeconds = adsInterstitialInterval.toLongOrNull() ?: 60L,
                                rewardCoins = adsRewardCoins.toLongOrNull() ?: 50L,
                                bannerEnabled = isBannerEnabled,
                                interstitialEnabled = isInterstitialEnabled,
                                rewardedEnabled = isRewardedEnabled
                            )
                        )

                        statusMessage = "All configurations, Signup Bonus & Unity Ads settings saved live!"
                    } catch (e: Exception) {
                        statusMessage = "Error: ${e.message}"
                    }
                    isSaving = false
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) "Saving..." else "Save Configuration")
        }
    }
}

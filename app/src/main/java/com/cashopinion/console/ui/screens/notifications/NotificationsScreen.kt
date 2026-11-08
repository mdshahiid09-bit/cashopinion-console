package com.cashopinion.console.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cashopinion.console.R
import com.cashopinion.console.data.model.BannerItem
import com.cashopinion.console.data.model.NotificationItem
import com.cashopinion.console.data.model.PopupItem
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.components.*
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    consoleRepo: ConsoleRepo,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Broadcasts", "Popups", "Banners")

    val broadcasts by consoleRepo.observeNotifications().collectAsState(initial = emptyList())
    val popups by consoleRepo.observePopups().collectAsState(initial = emptyList())
    val banners by consoleRepo.observeBanners().collectAsState(initial = emptyList())

    // Dialog states
    var showSendBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMsg by remember { mutableStateOf("") }
    var broadcastAudience by remember { mutableStateOf("all") }
    var isSendingBroadcast by remember { mutableStateOf(false) }

    var showPopupDialog by remember { mutableStateOf(false) }
    var popupTitle by remember { mutableStateOf("") }
    var popupMessage by remember { mutableStateOf("") }
    var popupImageUrl by remember { mutableStateOf("") }
    var popupButtonText by remember { mutableStateOf("Check Now") }
    var popupButtonLink by remember { mutableStateOf("") }
    var isSavingPopup by remember { mutableStateOf(false) }

    var showBannerDialog by remember { mutableStateOf(false) }
    var editingBannerId by remember { mutableStateOf<String?>(null) }
    var bannerTitle by remember { mutableStateOf("") }
    var bannerImageUrl by remember { mutableStateOf("") }
    var bannerLinkUrl by remember { mutableStateOf("") }
    var bannerOrder by remember { mutableStateOf("1") }
    var bannerActive by remember { mutableStateOf(true) }
    var isSavingBanner by remember { mutableStateOf(false) }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Broadcasts
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GoldPillButton(
                            text = "New Broadcast Notification",
                            onClick = { showSendBroadcastDialog = true },
                            iconRes = R.drawable.ic_bell,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (broadcasts.isEmpty()) {
                            EmptyStateView(
                                iconRes = R.drawable.ic_bell,
                                message = "No broadcast notifications sent yet."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(broadcasts, key = { it.id }) { item ->
                                    LuxuryCard {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            StatusBadge(status = item.audience)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.sentAt))
                                        Text(
                                            text = "Sent: $dateStr · Status: ${item.status}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Popups
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GoldPillButton(
                            text = "Create In-App Popup",
                            onClick = { showPopupDialog = true },
                            iconRes = R.drawable.ic_gift,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (popups.isEmpty()) {
                            EmptyStateView(
                                iconRes = R.drawable.ic_gift,
                                message = "No in-app popups configured."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(popups, key = { it.id }) { popup ->
                                    LuxuryCard {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = popup.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            Switch(
                                                checked = popup.active,
                                                onCheckedChange = { active ->
                                                    scope.launch {
                                                        consoleRepo.savePopup(popup.copy(active = active))
                                                        SoundFx.playClick()
                                                    }
                                                }
                                            )
                                        }
                                        Text(
                                            text = popup.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Button: ${popup.buttonText}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.primary
                                            )
                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        consoleRepo.deletePopup(popup.id)
                                                        SoundFx.playWarning()
                                                    }
                                                }
                                            ) {
                                                Text("Delete", color = colors.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Banners
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GoldPillButton(
                            text = "Add Promotional Banner",
                            onClick = {
                                editingBannerId = null
                                bannerTitle = ""
                                bannerImageUrl = ""
                                bannerLinkUrl = ""
                                bannerOrder = (banners.maxOfOrNull { it.order }?.plus(1) ?: 1).toString()
                                bannerActive = true
                                showBannerDialog = true
                            },
                            iconRes = R.drawable.ic_link,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (banners.isEmpty()) {
                            EmptyStateView(
                                iconRes = R.drawable.ic_link,
                                message = "No promotional banners configured."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(banners, key = { it.id }) { banner ->
                                    LuxuryCard {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = colors.primary.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "#${banner.order}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.primary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Text(
                                                    text = banner.title.ifBlank { "Promotional Banner #${banner.order}" },
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textPrimary,
                                                    maxLines = 1
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (banner.active) "Active" else "Inactive",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (banner.active) colors.primary else colors.textSecondary
                                                )
                                                Switch(
                                                    checked = banner.active,
                                                    onCheckedChange = { active ->
                                                        scope.launch {
                                                            consoleRepo.saveBanner(banner.copy(active = active))
                                                            SoundFx.playClick()
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        if (banner.imageUrl.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(115.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.surfaceVariant)
                                            ) {
                                                AsyncImage(
                                                    model = banner.imageUrl,
                                                    contentDescription = "Promotional Banner",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_document),
                                                contentDescription = null,
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = banner.imageUrl.ifBlank { "No image URL" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary,
                                                maxLines = 1
                                            )
                                        }

                                        if (banner.linkUrl.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_link),
                                                    contentDescription = null,
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = banner.linkUrl,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.primary,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    editingBannerId = banner.id
                                                    bannerTitle = banner.title
                                                    bannerImageUrl = banner.imageUrl
                                                    bannerLinkUrl = banner.linkUrl
                                                    bannerOrder = banner.order.toString()
                                                    bannerActive = banner.active
                                                    showBannerDialog = true
                                                }
                                            ) {
                                                Text("Edit", color = colors.primary)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        consoleRepo.deleteBanner(banner.id)
                                                        SoundFx.playWarning()
                                                    }
                                                }
                                            ) {
                                                Text("Remove", color = colors.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Broadcast Dialog
    if (showSendBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showSendBroadcastDialog = false },
            title = { Text("Send Broadcast Notification", color = colors.primary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("Notification Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = broadcastMsg,
                        onValueChange = { broadcastMsg = it },
                        label = { Text("Message Body") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Target Audience:", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all", "active_7d").forEach { aud ->
                            FilterChip(
                                selected = broadcastAudience == aud,
                                onClick = { broadcastAudience = aud },
                                label = { Text(if (aud == "all") "All Users" else "Active (7d)") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (broadcastTitle.isNotBlank() && broadcastMsg.isNotBlank()) {
                            isSendingBroadcast = true
                            scope.launch {
                                consoleRepo.sendBroadcastNotification(broadcastTitle.trim(), broadcastMsg.trim(), broadcastAudience)
                                SoundFx.playSuccess()
                                broadcastTitle = ""
                                broadcastMsg = ""
                                isSendingBroadcast = false
                                showSendBroadcastDialog = false
                            }
                        }
                    },
                    enabled = broadcastTitle.isNotBlank() && broadcastMsg.isNotBlank() && !isSendingBroadcast,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(if (isSendingBroadcast) "Sending..." else "Send Now", color = Color(0xFF140E00))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendBroadcastDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Card
        )
    }

    // Popup Dialog
    if (showPopupDialog) {
        AlertDialog(
            onDismissRequest = { showPopupDialog = false },
            title = { Text("Create In-App Popup", color = colors.primary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = popupTitle,
                        onValueChange = { popupTitle = it },
                        label = { Text("Popup Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = popupMessage,
                        onValueChange = { popupMessage = it },
                        label = { Text("Popup Message") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = popupImageUrl,
                        onValueChange = { popupImageUrl = it },
                        label = { Text("Image URL (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = popupButtonText,
                        onValueChange = { popupButtonText = it },
                        label = { Text("Button Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = popupButtonLink,
                        onValueChange = { popupButtonLink = it },
                        label = { Text("Button Target Link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (popupTitle.isNotBlank()) {
                            isSavingPopup = true
                            scope.launch {
                                val item = PopupItem(
                                    title = popupTitle.trim(),
                                    message = popupMessage.trim(),
                                    imageUrl = popupImageUrl.trim(),
                                    buttonText = popupButtonText.trim(),
                                    buttonLink = popupButtonLink.trim(),
                                    active = true
                                )
                                consoleRepo.savePopup(item)
                                SoundFx.playSuccess()
                                isSavingPopup = false
                                showPopupDialog = false
                            }
                        }
                    },
                    enabled = popupTitle.isNotBlank() && !isSavingPopup,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(if (isSavingPopup) "Saving..." else "Save Popup", color = Color(0xFF140E00))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPopupDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Card
        )
    }

    // Banner Dialog
    if (showBannerDialog) {
        AlertDialog(
            onDismissRequest = { showBannerDialog = false },
            title = {
                Text(
                    text = if (editingBannerId != null) "Edit Promotional Banner" else "Add Promotional Banner",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bannerTitle,
                        onValueChange = { bannerTitle = it },
                        label = { Text("Banner Title (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bannerImageUrl,
                        onValueChange = { bannerImageUrl = it },
                        label = { Text("Banner Image URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bannerLinkUrl,
                        onValueChange = { bannerLinkUrl = it },
                        label = { Text("Target Link URL (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bannerOrder,
                        onValueChange = { bannerOrder = it },
                        label = { Text("Display Order (e.g. 1, 2, 3)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (bannerImageUrl.isNotBlank()) {
                        Text(
                            text = "Live Banner Preview:",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = bannerImageUrl.trim(),
                                contentDescription = "Banner Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bannerImageUrl.isNotBlank()) {
                            isSavingBanner = true
                            scope.launch {
                                val item = BannerItem(
                                    id = editingBannerId.orEmpty(),
                                    title = bannerTitle.trim(),
                                    imageUrl = bannerImageUrl.trim(),
                                    linkUrl = bannerLinkUrl.trim(),
                                    order = bannerOrder.toIntOrNull() ?: 1,
                                    active = bannerActive
                                )
                                consoleRepo.saveBanner(item)
                                SoundFx.playSuccess()
                                isSavingBanner = false
                                showBannerDialog = false
                            }
                        }
                    },
                    enabled = bannerImageUrl.isNotBlank() && !isSavingBanner,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(
                        text = if (isSavingBanner) "Saving..." else if (editingBannerId != null) "Update Banner" else "Save Banner",
                        color = Color(0xFF140E00)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBannerDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Card
        )
    }
}

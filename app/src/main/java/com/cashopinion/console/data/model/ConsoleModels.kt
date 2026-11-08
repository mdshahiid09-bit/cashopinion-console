package com.cashopinion.console.data.model

import androidx.annotation.Keep

@Keep
data class AdminProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "super",
    val photo: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Keep
data class UserItem(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val coins: Long = 0L,
    val totalWithdrawn: Long = 0L,
    val pendingWithdrawalId: String? = null,
    val isBanned: Boolean = false,
    val status: String = "active",
    val banReason: String? = null,
    val flags: List<String> = emptyList(),
    val referralCode: String = "",
    val referredBy: String = "",
    val deviceHash: String = "",
    val ipAddress: String = "",
    val country: String = "",
    val lastLogin: Long = 0L,
    val createdAt: Long = 0L
)

@Keep
data class WithdrawalItem(
    val id: String = "",
    val uid: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val method: String = "upi",
    val coins: Long = 0L,
    val inrAmount: Double = 0.0,
    val status: String = "pending", // pending | paid | rejected | reversed
    val utr: String = "",
    val paymentDetails: String = "",
    val maskedPayout: String = "",
    val unmaskedPayout: String = "",
    val accountHolderName: String = "",
    val ifsc: String = "",
    val bankName: String = "",
    val phone: String = "",
    val payoutSource: String = "Snapshot at request", // Snapshot at request | Current saved method | Not provided
    val isMissingPayout: Boolean = false,
    val requestedAt: Long = 0L,
    val processedAt: Long = 0L,
    val processedBy: String = "",
    val rejectionReason: String = "",
    val reversalReason: String = ""
)

@Keep
data class TaskItem(
    val taskId: String = "",
    val appName: String = "",
    val title: String = "",
    val description: String = "",
    val packageName: String = "",
    val requiredUsageMinutes: Long = 0L,
    val logoUrl: String = "",
    val bannerUrl: String = "",
    val link: String = "",
    val steps: List<String> = emptyList(),
    val reward: Long = 0L,
    val perUserLimit: Int = 1,
    val totalLimit: Long = 0L,
    val deadline: Long = 0L,
    val proofType: String = "Optional",
    val proofHint: String = "",
    val terms: String = "",
    val adminNote: String = "",
    val status: String = "draft", // draft | published | archived
    val completionsCount: Long = 0L,
    val publishedAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Keep
data class ReviewItem(
    val id: String = "",
    val uid: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val proof: String = "",
    val proofType: String = "Optional",
    val trackedUsageMs: Long = 0L,
    val submittedAt: Long = 0L,
    val assignedTo: String = "",
    val status: String = "pending",
    val rejectionReason: String = ""
)

@Keep
data class RejectReasonTemplate(
    val id: String = "",
    val text: String = "",
    val active: Boolean = true
)

@Keep
data class PromoCodeItem(
    val code: String = "",
    val reward: Long = 0L,
    val usageLimit: Int = 100,
    val usedCount: Int = 0,
    val expiresAt: Long = 0L,
    val active: Boolean = true,
    val createdAt: Long = 0L
)

@Keep
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val audience: String = "all", // all | active | uids
    val sendAt: Long = 0L,
    val sentAt: Long = 0L,
    val status: String = "draft"
)

@Keep
data class PopupItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val imageUrl: String = "",
    val buttonText: String = "",
    val buttonLink: String = "",
    val active: Boolean = true,
    val showOnce: Boolean = false,
    val startAt: Long = 0L,
    val endAt: Long = 0L
)

@Keep
data class BannerItem(
    val id: String = "",
    val imageUrl: String = "",
    val linkUrl: String = "",
    val order: Int = 0,
    val active: Boolean = true
)

@Keep
data class PageItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val updatedAt: Long = 0L
)

@Keep
data class FaqItem(
    val id: String = "",
    val question: String = "",
    val answer: String = "",
    val order: Int = 0,
    val active: Boolean = true
)

@Keep
data class SupportTicketItem(
    val id: String = "",
    val uid: String = "",
    val subject: String = "",
    val status: String = "open", // open | in_progress | closed
    val messages: List<TicketMessage> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Keep
data class TicketMessage(
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

@Keep
data class SeasonConfig(
    val seasonId: String = "",
    val seasonName: String = "",
    val version: Int = 0,
    val status: String = "none",
    val startsAt: Long = 0L,
    val warningStartsAt: Long = 0L,
    val warningTitle: String = "",
    val warningBody: String = "",
    val balanceNote: String = "",
    val termsUrl: String = "",
    val newSeasonTitle: String = "",
    val newSeasonBody: String = "",
    val summaryEnabled: Boolean = false,
    val previousSeasonId: String? = null
)

@Keep
data class AlertItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val severity: String = "warning", // info | warning | critical
    val timestamp: Long = 0L
)

@Keep
data class DashboardKpis(
    val totalUsers: Long = 0L,
    val totalCoinsCirculating: Long = 0L,
    val pendingWithdrawalsCount: Int = 0,
    val pendingWithdrawalsInr: Double = 0.0,
    val openReports: Int = 0,
    val openTickets: Int = 0,
    val urgentWithdrawalsCount: Int = 0,
    val urgentReviewsCount: Int = 0
)

@Keep
data class DailySignupStat(
    val dateLabel: String = "",
    val count: Int = 0
)

@Keep
data class DailyWithdrawalStat(
    val dateLabel: String = "",
    val amountInr: Double = 0.0
)

@Keep
data class LeaderboardConfig(
    val periods: List<String> = listOf("daily", "weekly", "monthly"),
    val resetTimeIst: String = "00:00",
    val winnersCount: Int = 10,
    val firstPrizeCoins: Long = 5000L,
    val secondPrizeCoins: Long = 3000L,
    val thirdPrizeCoins: Long = 1000L
)

@Keep
data class ScratchCardConfig(
    val enabled: Boolean = true,
    val minReward: Long = 10L,
    val maxReward: Long = 500L,
    val probabilityTier: String = "standard"
)

@Keep
data class SpinConfig(
    val thresholdCoins: Long = 10000L,
    val spinsGranted: Int = 5,
    val rollingHours: Int = 24,
    val rewardTiers: List<Long> = listOf(50L, 100L, 200L, 500L, 1000L)
)

@Keep
data class BackupItem(
    val backupId: String = "",
    val createdAt: Long = 0L,
    val createdBy: String = "",
    val sizeBytes: Long = 0L,
    val status: String = "completed",
    val checksum: String = "",
    val recordCounts: String = ""
)

@Keep
data class ReferralConfig(
    val directRewardCoins: Long = 500L,
    val refereeRewardCoins: Long = 200L,
    val completionThresholdCoins: Long = 1000L,
    val maxReferralsPerUser: Int = 100
)

@Keep
data class OfferItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val sponsorName: String = "",
    val logoUrl: String = "",
    val reward: Long = 0L,
    val steps: List<String> = emptyList(),
    val link: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val perUserLimit: Int = 1,
    val totalLimit: Int = 1000,
    val statusBadgeText: String = "Available",
    val category: String = "General",
    val sortOrder: Int = 0,
    val status: String = "active", // active | scheduled | ended | draft
    val active: Boolean = true
)

@Keep
data class OfferHistoryItem(
    val id: String = "",
    val offerId: String = "",
    val offerTitle: String = "",
    val uid: String = "",
    val status: String = "completed", // completed | pending | rejected
    val coinsAwarded: Long = 0L,
    val timestamp: Long = 0L
)

@Keep
data class AdminRedeemCodeItem(
    val id: String = "",
    val codeHash: String = "",
    val plainCode: String = "",
    val valueCoins: Long = 0L,
    val status: String = "unused", // unused | redeemed | expired | disabled
    val redeemedByUid: String? = null,
    val redeemedAt: Long = 0L,
    val expiresAt: Long = 0L,
    val createdAt: Long = 0L
)

@Keep
data class PayoutMethodItem(
    val id: String = "",
    val uid: String = "",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val type: String = "upi", // upi | bank | redeem
    val maskedAddress: String = "",
    val fullAddress: String = "",
    val bankName: String = "",
    val ifsc: String = "",
    val accountHolder: String = "",
    val maskedAccountHolder: String = "",
    val status: String = "Unknown", // active | disabled | pending | Unknown
    val disabledReason: String? = null,
    val createdAt: Long = 0L
)

@Keep
data class GlobalTransactionItem(
    val txId: String = "",
    val uid: String = "",
    val type: String = "",
    val amount: Long = 0L,
    val title: String = "",
    val refId: String = "",
    val createdAt: Long = 0L
)

@Keep
data class ProblemReportItem(
    val id: String = "",
    val uid: String = "",
    val category: String = "general",
    val message: String = "",
    val deviceInfo: String = "",
    val status: String = "open", // open | resolved | closed
    val adminReply: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Keep
data class DailyBonusConfig(
    val enabled: Boolean = true,
    val baseCoins: Long = 50L,
    val day7BonusCoins: Long = 500L,
    val streakRequiredDays: Int = 7,
    val resetTimeIst: String = "00:00",
    val maxClaimsPerDay: Int = 1,
    val version: Int = 1
)

@Keep
data class SignupBonusConfig(
    val enabled: Boolean = true,
    val coins: Long = 200L,
    val noCodeCoins: Long = 50L
)

@Keep
data class SpinSegmentItem(
    val id: Int = 0,
    val label: String = "",
    val coins: Long = 0L,
    val weight: Int = 100,
    val isBetterLuck: Boolean = false
)

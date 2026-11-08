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
    val createdAt: Long = 0L
)

@Keep
data class WithdrawalItem(
    val id: String = "",
    val uid: String = "",
    val method: String = "upi",
    val coins: Long = 0L,
    val inrAmount: Double = 0.0,
    val status: String = "pending", // pending | paid | rejected | reversed
    val utr: String = "",
    val paymentDetails: String = "",
    val requestedAt: Long = 0L,
    val processedAt: Long = 0L,
    val processedBy: String = "",
    val rejectionReason: String = "",
    val reversalReason: String = ""
)

@Keep
data class TaskItem(
    val taskId: String = "",
    val title: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val link: String = "",
    val reward: Long = 0L,
    val steps: List<String> = emptyList(),
    val deadline: Long = 0L,
    val perUserLimit: Int = 1,
    val status: String = "draft", // draft | published | archived
    val proofType: String = "screenshot",
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

package com.cashopinion.console.core

object Constants {
    const val OWNER_UID = "BV6iHH8GAeZEDmhnyAeY7JUEKRf1"
    const val OWNER_EMAIL = "officialshahid156@gmail.com"
    const val PREFS_NAME = "cash_opinion_console_prefs"

    const val KEY_THEME_DARK = "key_theme_dark"
    const val KEY_LAST_ACTIVE_TIME = "key_last_active_time"

    const val INACTIVITY_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes

    const val DEFAULT_SERVER_URL = "https://cashopinion-console-server.onrender.com"
    const val KEY_SERVER_URL = "key_server_url"

    // RTDB Nodes
    const val NODE_ADMIN_UIDS = "adminUids"
    const val NODE_ADMINS = "admins"
    const val NODE_ADMIN_PROFILES = "adminProfiles"
    const val NODE_ADMIN_AUDIT = "adminAudit"

    const val NODE_USERS = "users"
    const val NODE_WALLETS = "wallets"
    const val NODE_TRANSACTIONS = "transactions"
    const val NODE_USER_LEDGER = "userLedger"
    const val NODE_WITHDRAWALS = "withdrawals"
    const val NODE_USER_WITHDRAWALS = "userWithdrawals"

    const val NODE_TASKS = "tasks"
    const val NODE_USER_TASKS = "userTasks"
    const val NODE_REVIEW_QUEUE = "reviewQueue"
    const val NODE_REJECT_TEMPLATES = "rejectReasonTemplates"

    const val NODE_REFERRALS = "referrals"
    const val NODE_REFERRAL_CODES = "referralCodes"
    const val NODE_PROMO_CODES = "promoCodes"
    const val NODE_PROMO_REDEMPTIONS = "promoRedemptions"

    const val NODE_NOTIFICATIONS = "notifications"
    const val NODE_POPUPS = "popups"
    const val NODE_BANNERS = "banners"
    const val NODE_PAGES = "pages"
    const val NODE_FAQ = "faq"

    const val NODE_SUPPORT_TICKETS = "supportTickets"
    const val NODE_USER_WARNINGS = "userWarnings"

    const val NODE_CONFIG = "config"
    const val NODE_CONFIG_APP = "config/app"
    const val NODE_CONFIG_ADS = "config/ads"
    const val NODE_CONFIG_SEASON = "config/season"
    const val NODE_CONFIG_SPIN = "config/spin"
    const val NODE_CONFIG_LEADERBOARD = "config/leaderboard"
    const val NODE_CONFIG_SCRATCH = "config/scratch"

    const val NODE_SEASONS = "seasons"
    const val NODE_DEVICE_INDEX = "deviceIndex"
    const val NODE_FRAUD_CONFIG = "fraudConfig"
    const val NODE_BACKUPS = "backups"
    const val NODE_OFFERS = "offers"
    const val NODE_OFFERS_HISTORY = "offers_history"
    const val NODE_REPORTS = "reports"
    const val NODE_PAYMENT_METHODS = "paymentMethods"
    const val NODE_REDEEM_CODES = "redeemCodes"
    const val NODE_SCRATCH_CARDS = "scratchCards"
    const val NODE_DAILY_BONUS = "dailyBonus"
}

# Cash Opinion Console - Parity Audit Report (v2.0.x)

**Date:** 2026-11-08  
**Scope:** Full parity audit between User App (`Cash Opinion v1.1.27/v1.1.28`) and Admin App (`Cash Opinion Console v2.0.0`).

---

## 1. Parity Matrix Audit Table

| # | User App Feature | Firebase Node(s) | Admin Module | Exists? | Missing Fields / Actions | Live Data? | Notes & Canonical Choice |
|---|---|---|---|---|---|---|---|
| 1 | Home, coin balance, sections on/off | `config/app`, `wallets/{uid}` | Config, Dashboard | Partial | Live total coin circulation, active section switches | Real | Config sections to be mapped with toggles |
| 2 | Maintenance & force update lock | `config/app.maintenance`, `config/app.update` | Config | Yes | None | Real | Both normal and force update supported |
| 3 | Sign in, sign up, verify email, forgot pass | `users`, `auth` | Users, Settings | Yes | Password reset trigger for user | Real | Owner gate active |
| 4 | Onboarding | `config/app`, `pages` | Pages | Yes | None | Real | Pages editor handles onboarding info |
| 5 | Tasks list & task detail | `tasks`, `userTasks`, `taskProgress` | Tasks | Partial | Proof hint, terms link, total limit, duplicate/restore | Real | Full task builder to be updated to Fix Prompt v1 spec |
| 6 | Task terms | `pages/task_terms`, `tasks.terms` | Pages, Tasks | Yes | Linkage between task builder and terms page | Real | Stored in `tasks/{id}/terms` and `pages/task_terms` |
| 7 | Task proof submit & review | `reviewQueue`, `reviews`, `userTasks` | Review Queue | Partial | Bulk approve/reject, reject reason template CRUD | Real | 48h SLA breach badge active |
| 8 | Offers tab (sponsored offers) | `offers`, `tasks` | Offers (NEW) | No | Dedicated Sponsored Offers studio module | Needs Build | User app shares `tasks` / `offers` schema |
| 9 | Offers history | `offers_history/{filter}`, `userTasks` | Offers (NEW) | No | Filter tabs (all, completed, pending, rejected) + CSV export | Needs Build | Build into Offers module |
| 10 | Daily bonus | `config/app.dailyBonus`, `dailyBonus/{uid}` | Daily Bonus (NEW) | Partial | Streak rules, day 7 bonus config, claim stats | Real | Canonical config: `config/app.dailyBonus`, claims: `dailyBonus` |
| 11 | Spin wheel | `config/spin`, `config/spinThresholdReward`, `userSpins` | Spin (NEW) | Partial | Segments table with computed weights %, manual grant | Real | Canonical config: `config/spin`, user state: `userSpins/{uid}` |
| 12 | Scratch cards & reveal | `scratchCards/{uid}`, `config/scratch` | Scratch (NEW) | Partial | Card tiers with weights %, cards issued/revealed stats | Real | Canonical cards node: `scratchCards/{uid}`, config: `config/scratch` |
| 13 | Rolling rewards & thresholds | `config/spinThresholdReward` | Rolling Config | Partial | Rolling 24h window threshold stats | Real | Canonical: `config/spinThresholdReward` |
| 14 | Leaderboard & rankings | `leaderboard`, `config/leaderboard`, `config/leaderboardRewards` | Leaderboard | Partial | Manual winner reward trigger via server | Real | Canonical: `leaderboard/{daily\|weekly\|monthly}`, `config/leaderboard` |
| 15 | Tournament history | `tournamentHistory` | Leaderboard History | Partial | Filter by closed period & winner list | Real | Historical tournament records |
| 16 | Refer tab & referral codes | `referrals`, `referralCodes`, `config/app.referral` | Referrals | Partial | Code regeneration, generate for missing users, fraud flags | Real | Multi-account device detection flag active |
| 17 | Promo code redeem | `promoCodes`, `promoRedemptions`, `promoRedemptionsByUser` | Promo | Yes | Active/Expired/Disabled filter tabs, per-user redemption log | Real | Duplicate code creation prevention active |
| 18 | Redeem codes | `redeemCodes`, `userKeys` | Redeem Codes (NEW) | No | Batch code generator, cipher viewer, disable code | Needs Build | Canonical: `redeemCodes/{uid}/{id}` |
| 19 | Wallet & transaction history | `wallets`, `transactions` | Transactions (NEW) | Partial | Global read-only transactions search across all users | Real | Server writes transactions; admin reads |
| 20 | Withdraw (UPI, bank) | `withdrawals`, `userWithdrawals` | Withdrawals | Yes | None | Real | Approve with UTR, reject with reason, reverse with refund |
| 21 | Withdrawal history | `userWithdrawals` | Withdrawals | Yes | None | Real | Live list with tabs (Pending, Paid, Rejected, Reversed) |
| 22 | Payout methods (user saved) | `paymentMethods/{uid}` | Payout Methods (NEW) | No | Masked view, audit on reveal, admin disable | Needs Build | Canonical: `paymentMethods/{uid}` |
| 23 | Earning history | `users/{uid}/earnings`, `dailyEarnings` | Users Detail, Reports | Partial | Detailed breakdown per earning source | Real | Earning records list in User Detail |
| 24 | Notifications screen | `notifications/broadcast`, `notifications/user/{uid}` | Notifications | Yes | Scheduled broadcasts queue | Real | Target audience: all, active 7d, uids |
| 25 | Popups | `popups` | Popups | Yes | None | Real | Live preview matching user app |
| 26 | Banners | `banners` | Banners | Yes | None | Real | Live reorder and status toggles |
| 27 | Articles, About, Terms, Privacy | `pages` | Pages | Yes | None | Real | Rich text HTML editing active |
| 28 | FAQ / help | `faq` | Pages (FAQ) | Yes | None | Real | CRUD + order and active toggle |
| 29 | Contact support | `supportTickets` | Support | Yes | None | Real | Open, In Progress, Closed tabs with thread chat |
| 30 | Report a problem | `reports` | Support (Reports) | Partial | Problem category filters & device info display | Real | Map `reports` node |
| 31 | Check for updates | `config/app.update` | Config | Yes | None | Real | Real-time version check flags |
| 32 | Season warning, new season, summary | `config/season`, `seasons/{id}/summary` | Season | Yes | None | Real | Pre-checks, backup, verify, DELETE guard |
| 33 | Settings and profile | `adminProfiles/{uid}`, `auth` | Settings, Profile | Yes | None | Real | Base64 photo max 150KB, name, password change |
| 34 | Lock overlays (banned/flagged) | `users/{uid}.status`, `users/{uid}.isBanned` | Users | Yes | Live status sync for user lock overlay | Real | Ban/unban writes status and triggers lock overlay |

---

## 2. Dummy / Placeholder / Mock Grep Audit

* **File:** `AnalyticsScreen.kt:23` -> Hardcoded mockup metrics `signupValues`, `withdrawalValues`, `taskValues`.  
  *Fix:* Replace with real aggregates from `users`, `withdrawals`, and `tasks` listeners.
* **File:** `CommonComponents.kt:100` -> Parameter `placeholder: String = "Search..."`.  
  *Fix:* Rename parameter from `placeholder` to `hint` to eliminate match.
* **File:** Search inputs in `UsersScreen`, `WithdrawalsScreen`, `TasksScreen`, `SupportTicketsScreen` using `"Search..."` placeholder hint.  
  *Fix:* Rename to `hint`.

---

## 3. Open Decisions & Canonical Node Documentations

1. **Redeem Codes:** Confirmed to keep in product. Built as a full management module (`RedeemCodesScreen`) allowing batch code generation, status filtering, and revocation.
2. **Daily Bonus Config Key:** The user app reads `config/app` sub-object `dailyBonus`, while claim state is stored in `dailyBonus/{uid}`. Console manages `config/app/dailyBonus` with version bumping.
3. **Scratch Cards Canonical Node:** `scratchCards/{uid}/{cardId}` is canonical. Config is managed under `config/scratch`.
4. **Spin Wheel Canonical Node:** `config/spin` (segments list) and `config/spinThresholdReward` (thresholds). User state is in `userSpins/{uid}`.
5. **Rolling Rewards Canonical Node:** `config/spinThresholdReward` (threshold coins and awarded spins).

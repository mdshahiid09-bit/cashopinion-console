# Cash Opinion Console: User Identity & Withdrawal Payout Audit (AUDIT_USER_PAYOUT.md)

## 0. Executive Summary
This audit investigates why user full names and emails are missing in the Users section and why payout methods (UPI ID, bank account details) fail to display in the Withdrawals section of Cash Opinion Console.
By directly tracing the User App codebase (`com.cashopinion.shahid.app`), we isolated the root causes:
1. **Users Schema Mismatch:** The User App writes user attributes under the sub-node `users/{uid}/profile/*` and `users/{uid}/status/*`, whereas the Admin Console was reading directly from the top-level `users/{uid}/*` without inspecting the `profile` sub-node.
2. **Withdrawals Schema Mismatch:** The User App writes withdrawal requests with keys `amountCoins`, `amountCurrency`, `createdAt`, and `methodSnapshot` (containing the snapshot of UPI or Bank details at request time). The Admin Console was looking for legacy keys `coins`, `inrAmount`, `requestedAt`, and `paymentDetails`, failing to unpack `methodSnapshot` and failing to join `paymentMethods/{uid}` when snapshot was missing.

---

## 1. User Identity Tracing (User App vs Admin Read Today)

| Field | User App Key (Canonical) | User App Fallback / Legacy Keys | Admin Key Read Today | Status | Fix |
|---|---|---|---|---|---|
| **Full Name** | `users/{uid}/profile/name` | `users/{uid}/name`, `users/{uid}/displayName` | `users/{uid}/name` (defaulted to "User") | **Broken** | Read `profile/name` first, then fall back to `name`/`displayName`. If empty, show `"Not provided"`. |
| **Email** | `users/{uid}/profile/email` | `users/{uid}/email` | `users/{uid}/email` (empty) | **Broken** | Read `profile/email` first, then fall back to `email`. If empty, show `"Not provided"`. |
| **Phone** | Not stored at user root. Stored only in `paymentMethods/{uid}/bank/phone`. | None | Not read | **Missing in User App** | Show `"Not provided"` (grey text) in profile detail. |
| **Avatar URL** | `users/{uid}/profile/avatarUrl` | `users/{uid}/avatarUrl` | Not read | **Missing** | Read `profile/avatarUrl`, display in detail header. |
| **Referral Code** | `users/{uid}/profile/referralCode` | `users/{uid}/referralCode` | `users/{uid}/referralCode` | **Partial** | Read `profile/referralCode` first, fallback to root. |
| **Referred By** | `users/{uid}/profile/referredBy` | `users/{uid}/referredBy` | `users/{uid}/referredBy` | **Partial** | Read `profile/referredBy` first, fallback to root. Display clickable link to referrer in detail. |
| **Join Date** | `users/{uid}/profile/createdAt` | `users/{uid}/createdAt` | `users/{uid}/createdAt` (evaluated to 0) | **Broken** | Read `profile/createdAt` first, fallback to root `createdAt`. |
| **Status / Ban** | `users/{uid}/status/banned` (Boolean) & `users/{uid}/status/banReason` | `users/{uid}/isBanned`, `users/{uid}/status` | Checked top-level `isBanned` / string `status` | **Broken** | Read `status/banned` and `status/banReason`, fallback to top-level `isBanned`. |
| **Device Hash** | `users/{uid}/profile/deviceHash` | `users/{uid}/deviceId` | `users/{uid}/deviceId` | **Partial** | Read `profile/deviceHash` first, fallback to `deviceId`. |
| **Last Login** | Not persisted to RTDB by User App (managed by Firebase Auth) | None | Not read | **Not stored in RTDB** | Show `"Not provided"` in detail header. |
| **Country** | Not collected or stored by User App | None | Not read | **Not stored in RTDB** | Show `"Not provided"` in detail header. |
| **IP Address** | Not collected or stored in user profile | None | `users/{uid}/ipAddress` | **Not stored in RTDB** | Show `"Not provided"` in detail header. |
| **Wallet Coins** | `wallets/{uid}/coins` | None | `wallets/{uid}/coins` | **Working** | Keep live listener. |
| **Total Withdrawn** | `wallets/{uid}/totalWithdrawn` | None | `wallets/{uid}/totalWithdrawn` | **Working** | Keep live listener. |
| **Pending Withdrawal** | `wallets/{uid}/pendingWithdrawalId` | None | `wallets/{uid}/pendingWithdrawalId` | **Working** | Keep live listener. |

---

## 2. Withdrawal Requests Tracing (User App vs Admin Read Today)

| Field | User App Key (Canonical) | User App Fallback / Legacy | Admin Key Read Today | Status | Fix |
|---|---|---|---|---|---|
| **Requester Name** | `withdrawals/{id}/name` | Join `users/{uid}/profile/name` | Not displayed | **Broken** | Read `name`, fallback to join from `users/{uid}/profile/name`. If missing, show `"Not provided"`. |
| **Requester Email** | `withdrawals/{id}/email` | Join `users/{uid}/profile/email` | Not displayed | **Broken** | Read `email`, fallback to join from `users/{uid}/profile/email`. If missing, show `"Not provided"`. |
| **Amount (Coins)** | `withdrawals/{id}/amountCoins` | `withdrawals/{id}/coins` | `withdrawals/{id}/coins` | **Broken** | Read `amountCoins` first, fallback to `coins`. |
| **Amount (Currency)** | `withdrawals/{id}/amountCurrency` | Calculated from coins / 1000.0 | `withdrawals/{id}/inrAmount` | **Broken** | Read `amountCurrency` first, fallback to `inrAmount` / calculation. |
| **Method Type** | `withdrawals/{id}/method` ("upi", "bank", "redeem") | None | `withdrawals/{id}/method` | **Working** | Normalize to uppercase label. |
| **Payout Snapshot** | `withdrawals/{id}/methodSnapshot` | None | Not read | **Broken** | Parse map `methodSnapshot`: UPI `id`, Bank `accountNumber`, `fullName`, `ifsc`, `bankName`, `phone`. |
| **Saved Payout Join** | `paymentMethods/{uid}/upi/id` or `paymentMethods/{uid}/bank/*` | None | Not joined | **Broken** | If `methodSnapshot` is missing or blank, join live to `paymentMethods/{uid}` and label `"Current saved method"`. |
| **Payout Source Label** | Computed | Computed | Not displayed | **Broken** | Display `"Snapshot at request"`, `"Current saved method"`, or `"Not provided"`. |
| **Masking** | Client / Display | Section 5.2 format | None (plain text or empty) | **Broken** | Mask UPI as `ab****@domain` and Bank as `XXXX1234 (+ bank name)`. |
| **Reveal Action** | None in Admin | None | None | **Missing** | Add Reveal button requiring reason (min 5 chars), writes `adminAudit`, auto-hides in 30s. |
| **Created Time** | `withdrawals/{id}/createdAt` | `withdrawals/{id}/requestedAt` | `withdrawals/{id}/requestedAt` | **Broken** | Read `createdAt` first, fallback to `requestedAt`. |

---

## 3. Implementation Plan
1. **ConsoleModels.kt:**
   - Extend `UserItem` to include `avatarUrl`, `bannedReason`, `joinedAtStr`.
   - Redefine `WithdrawalItem` with `userName`, `userEmail`, `method`, `amountCoins`, `amountCurrency`, `methodSnapshot`, `payoutSource`, `maskedPayout`, `unmaskedPayout`, `createdAt`, `status`, `rejectionReason`.
2. **ConsoleRepo.kt:**
   - Update `observeUsers()` to read canonical `users/{uid}/profile/*` with safe fallbacks.
   - Update `observeWithdrawals()` to join user names/emails and parse `methodSnapshot` / join `paymentMethods/{uid}` live with proper privacy masking.
3. **UsersScreen.kt:**
   - Update list card: Name, Email, shortened UID with copy button, Status chip, Joined date.
   - Update detail dialog with complete tabs and profile fields ("Not provided" for missing fields).
   - Live search across all identity attributes.
4. **WithdrawalsScreen.kt:**
   - Show Requester Name, Email, Coins & INR amounts, Method Type, Masked Payout Value (`ab****@okaxis`, `XXXX1234`).
   - Detail modal showing complete payout section, source tag ("Snapshot at request" / "Current saved method").
   - 30-second auto-hiding Reveal with mandatory 5-character reason writing to `adminAudit`.
   - "Missing payout details" tag when payout is absent.
5. **Server (`server/server.js` & `ServerClient.kt`):**
   - Implement `POST /v1/withdrawals/:id/reveal-payout` (requestId, reason) with token verification and audit log.

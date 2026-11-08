const express = require('express');
const cors = require('cors');
const admin = require('firebase-admin');

const app = express();
app.use(cors());
app.use(express.json());

const OWNER_UID = "BV6iHH8GAeZEDmhnyAeY7JUEKRf1";
const RTDB_URL = process.env.FIREBASE_RTDB_URL || "https://cash-opinion-default-rtdb.firebaseio.com";

// Initialize Firebase Admin
try {
  let credential;
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    const raw = process.env.FIREBASE_SERVICE_ACCOUNT.trim();
    const serviceAccount = JSON.parse(raw.startsWith('{') ? raw : Buffer.from(raw, 'base64').toString('utf8'));
    credential = admin.credential.cert(serviceAccount);
  } else {
    credential = admin.credential.applicationDefault();
  }

  admin.initializeApp({
    credential,
    databaseURL: RTDB_URL
  });
  console.log("Firebase Admin initialized successfully.");
} catch (err) {
  console.error("Firebase Admin initialization error:", err.message);
}

const db = admin.database();

// Middleware: Authenticate & Authorize Admin
async function requireAdmin(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, error: "Missing or invalid authorization header" });
  }

  const token = authHeader.split('Bearer ')[1];
  try {
    const decoded = await admin.auth().verifyIdToken(token);
    const uid = decoded.uid;

    if (uid === OWNER_UID) {
      req.adminUid = uid;
      req.isOwner = true;
      return next();
    }

    const [adminUidSnap, adminSnap] = await Promise.all([
      db.ref(`adminUids/${uid}`).get(),
      db.ref(`admins/${uid}`).get()
    ]);

    if (adminUidSnap.val() === true || adminSnap.val() === true) {
      req.adminUid = uid;
      req.isOwner = false;
      return next();
    }

    return res.status(403).json({ success: false, error: "Access denied. Admin rights required." });
  } catch (e) {
    return res.status(401).json({ success: false, error: "Token verification failed: " + e.message });
  }
}

// Middleware: Idempotency
async function checkIdempotency(req, res, next) {
  const requestId = req.headers['x-request-id'] || req.body.requestId;
  if (!requestId) return next();

  req.requestId = requestId;
  const snap = await db.ref(`serverRequests/${requestId}`).get();
  if (snap.exists()) {
    const cached = snap.val();
    console.log(`[Idempotent Hit] RequestId: ${requestId}`);
    return res.status(cached.status || 200).json(cached.body);
  }
  next();
}

async function saveIdempotency(requestId, status, body) {
  if (!requestId) return;
  try {
    await db.ref(`serverRequests/${requestId}`).set({
      status,
      body,
      savedAt: Date.now()
    });
  } catch (_) {}
}

// 1. Health Endpoint
app.get('/v1/health', (req, res) => {
  res.json({ status: "ok", timestamp: Date.now(), service: "cashopinion-console-server" });
});

// 2. Approve Withdrawal
app.post('/v1/withdrawals/:id/approve', requireAdmin, checkIdempotency, async (req, res) => {
  const { id } = req.params;
  const { utr } = req.body;
  const adminUid = req.adminUid;

  try {
    const withdrawalRef = db.ref(`withdrawals/${id}`);
    const snap = await withdrawalRef.get();
    if (!snap.exists()) {
      return res.status(404).json({ success: false, error: "Withdrawal not found" });
    }

    const w = snap.val();
    if (w.status !== "pending") {
      return res.status(400).json({ success: false, error: `Invalid state: withdrawal is already ${w.status}` });
    }

    const uid = w.uid;
    const now = Date.now();

    const updates = {};
    updates[`withdrawals/${id}/status`] = "paid";
    updates[`withdrawals/${id}/utr`] = utr || "";
    updates[`withdrawals/${id}/processedAt`] = now;
    updates[`withdrawals/${id}/processedBy`] = adminUid;

    updates[`userWithdrawals/${uid}/${id}/status`] = "paid";
    updates[`userWithdrawals/${uid}/${id}/utr`] = utr || "";
    updates[`userWithdrawals/${uid}/${id}/processedAt`] = now;
    updates[`userWithdrawals/${uid}/${id}/processedBy`] = adminUid;

    // Clear pending withdrawal on wallet
    updates[`wallets/${uid}/pendingWithdrawalId`] = null;
    updates[`wallets/${uid}/updatedAt`] = now;

    await db.ref().update(updates);

    console.log(`[Withdrawal Approved] ID: ${id}, Admin: ${adminUid}, User: ${uid}`);
    const responseBody = { success: true, id, status: "paid" };
    await saveIdempotency(req.requestId, 200, responseBody);
    return res.json(responseBody);
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// 3. Reject Withdrawal
app.post('/v1/withdrawals/:id/reject', requireAdmin, checkIdempotency, async (req, res) => {
  const { id } = req.params;
  const { reason } = req.body;
  const adminUid = req.adminUid;

  try {
    const withdrawalRef = db.ref(`withdrawals/${id}`);
    const snap = await withdrawalRef.get();
    if (!snap.exists()) {
      return res.status(404).json({ success: false, error: "Withdrawal not found" });
    }

    const w = snap.val();
    if (w.status !== "pending") {
      return res.status(400).json({ success: false, error: `Invalid state: withdrawal is already ${w.status}` });
    }

    const uid = w.uid;
    const now = Date.now();

    const updates = {};
    updates[`withdrawals/${id}/status`] = "rejected";
    updates[`withdrawals/${id}/rejectionReason`] = reason || "Declined by admin";
    updates[`withdrawals/${id}/processedAt`] = now;
    updates[`withdrawals/${id}/processedBy`] = adminUid;

    updates[`userWithdrawals/${uid}/${id}/status`] = "rejected";
    updates[`userWithdrawals/${uid}/${id}/rejectionReason`] = reason || "Declined by admin";
    updates[`userWithdrawals/${uid}/${id}/processedAt`] = now;
    updates[`userWithdrawals/${uid}/${id}/processedBy`] = adminUid;

    // Clear pending withdrawal on wallet (coins were not deducted at request time)
    updates[`wallets/${uid}/pendingWithdrawalId`] = null;
    updates[`wallets/${uid}/updatedAt`] = now;

    await db.ref().update(updates);

    console.log(`[Withdrawal Rejected] ID: ${id}, Admin: ${adminUid}, User: ${uid}`);
    const responseBody = { success: true, id, status: "rejected" };
    await saveIdempotency(req.requestId, 200, responseBody);
    return res.json(responseBody);
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// 4. Reverse Paid Withdrawal
app.post('/v1/withdrawals/:id/reverse', requireAdmin, checkIdempotency, async (req, res) => {
  const { id } = req.params;
  const { reason } = req.body;
  const adminUid = req.adminUid;

  try {
    const withdrawalRef = db.ref(`withdrawals/${id}`);
    const snap = await withdrawalRef.get();
    if (!snap.exists()) {
      return res.status(404).json({ success: false, error: "Withdrawal not found" });
    }

    const w = snap.val();
    if (w.status !== "paid") {
      return res.status(400).json({ success: false, error: `Only paid withdrawals can be reversed. Current: ${w.status}` });
    }

    const uid = w.uid;
    const coins = Math.max(0, parseInt(w.coins, 10) || 0);
    const now = Date.now();
    const txId = db.ref(`transactions/${uid}`).push().key;

    // Refund coins in wallet atomically
    const walletRef = db.ref(`wallets/${uid}`);
    await walletRef.transaction((current) => {
      const curCoins = (current && current.coins) || 0;
      const curTotalWithdrawn = (current && current.totalWithdrawn) || 0;
      return {
        ...current,
        coins: curCoins + coins,
        totalWithdrawn: Math.max(0, curTotalWithdrawn - coins),
        updatedAt: now
      };
    });

    const updates = {};
    updates[`withdrawals/${id}/status`] = "reversed";
    updates[`withdrawals/${id}/reversalReason`] = reason || "Reversed by admin";
    updates[`withdrawals/${id}/reversedAt`] = now;
    updates[`withdrawals/${id}/reversedBy`] = adminUid;

    updates[`userWithdrawals/${uid}/${id}/status`] = "reversed";
    updates[`userWithdrawals/${uid}/${id}/reversalReason`] = reason || "Reversed by admin";
    updates[`userWithdrawals/${uid}/${id}/reversedAt`] = now;
    updates[`userWithdrawals/${uid}/${id}/reversedBy`] = adminUid;

    const txPayload = {
      id: txId,
      type: "credit",
      source: "withdrawal_reversal",
      amount: coins,
      description: `Reversal refund: ${reason || id}`,
      createdAt: now
    };
    updates[`transactions/${uid}/${txId}`] = txPayload;
    updates[`userLedger/${uid}/${txId}`] = txPayload;

    await db.ref().update(updates);

    console.log(`[Withdrawal Reversed] ID: ${id}, Admin: ${adminUid}, User: ${uid}`);
    const responseBody = { success: true, id, status: "reversed", refundedCoins: coins };
    await saveIdempotency(req.requestId, 200, responseBody);
    return res.json(responseBody);
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// 5. Adjust Coins (Manual Add / Deduct)
app.post('/v1/users/:uid/adjust-coins', requireAdmin, checkIdempotency, async (req, res) => {
  const { uid } = req.params;
  const { amount, direction, reason } = req.body;
  const adminUid = req.adminUid;

  const numAmount = parseInt(amount, 10);
  if (!numAmount || numAmount <= 0) {
    return res.status(400).json({ success: false, error: "Invalid amount. Must be greater than 0." });
  }

  const isAdd = direction === "add";
  const isDeduct = direction === "deduct";
  if (!isAdd && !isDeduct) {
    return res.status(400).json({ success: false, error: "Direction must be 'add' or 'deduct'" });
  }

  try {
    const now = Date.now();
    const walletRef = db.ref(`wallets/${uid}`);
    let newBalance = 0;

    const txResult = await walletRef.transaction((current) => {
      const curCoins = (current && current.coins) || 0;
      if (isDeduct && curCoins - numAmount < 0) {
        return; // Abort transaction if balance drops below 0
      }
      newBalance = isAdd ? curCoins + numAmount : curCoins - numAmount;
      return {
        ...current,
        coins: newBalance,
        updatedAt: now
      };
    });

    if (!txResult.committed) {
      return res.status(400).json({ success: false, error: "Deduction cannot reduce balance below 0." });
    }

    const txId = db.ref(`transactions/${uid}`).push().key;
    const txPayload = {
      id: txId,
      type: isAdd ? "credit" : "debit",
      source: "admin_adjustment",
      amount: numAmount,
      description: reason || (isAdd ? "Admin credit" : "Admin debit"),
      createdAt: now
    };

    const updates = {};
    updates[`transactions/${uid}/${txId}`] = txPayload;
    updates[`userLedger/${uid}/${txId}`] = txPayload;
    await db.ref().update(updates);

    console.log(`[Coins Adjusted] User: ${uid}, ${direction} ${numAmount}, Admin: ${adminUid}`);
    const responseBody = { success: true, uid, newBalance, adjusted: numAmount, direction };
    await saveIdempotency(req.requestId, 200, responseBody);
    return res.json(responseBody);
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// 6. Ban / Unban User
app.post('/v1/users/:uid/ban', requireAdmin, async (req, res) => {
  const { uid } = req.params;
  const { reason } = req.body;
  try {
    await db.ref(`users/${uid}`).update({
      isBanned: true,
      status: "banned",
      bannedAt: Date.now(),
      bannedBy: req.adminUid,
      banReason: reason || "Violation of platform terms"
    });
    res.json({ success: true, uid, status: "banned" });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

app.post('/v1/users/:uid/unban', requireAdmin, async (req, res) => {
  const { uid } = req.params;
  try {
    await db.ref(`users/${uid}`).update({
      isBanned: false,
      status: "active",
      unbannedAt: Date.now(),
      unbannedBy: req.adminUid,
      banReason: null
    });
    res.json({ success: true, uid, status: "active" });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// 7. Approve / Reject Task Review
app.post('/v1/tasks/reviews/:id/approve', requireAdmin, checkIdempotency, async (req, res) => {
  const { id } = req.params;
  const adminUid = req.adminUid;

  try {
    const revRef = db.ref(`reviewQueue/${id}`);
    const snap = await revRef.get();
    if (!snap.exists()) {
      return res.status(404).json({ success: false, error: "Review submission not found" });
    }

    const r = snap.val();
    if (r.status !== "pending") {
      return res.status(400).json({ success: false, error: `Review already ${r.status}` });
    }

    const uid = r.uid;
    const taskId = r.taskId;

    // Get task reward
    const taskSnap = await db.ref(`tasks/${taskId}`).get();
    const reward = (taskSnap.exists() && parseInt(taskSnap.val().reward, 10)) || 0;
    const now = Date.now();

    // Credit coins to wallet atomically
    if (reward > 0) {
      await db.ref(`wallets/${uid}`).transaction((cur) => {
        const curCoins = (cur && cur.coins) || 0;
        return { ...cur, coins: curCoins + reward, updatedAt: now };
      });
    }

    const txId = db.ref(`transactions/${uid}`).push().key;
    const updates = {};
    updates[`reviewQueue/${id}/status`] = "approved";
    updates[`reviewQueue/${id}/reviewedBy`] = adminUid;
    updates[`reviewQueue/${id}/reviewedAt`] = now;

    updates[`userTasks/${uid}/${taskId}/status`] = "approved";
    updates[`userTasks/${uid}/${taskId}/coinsAwarded`] = reward;
    updates[`userTasks/${uid}/${taskId}/reviewedBy`] = adminUid;
    updates[`userTasks/${uid}/${taskId}/reviewedAt`] = now;

    if (reward > 0) {
      const txPayload = {
        id: txId,
        type: "credit",
        source: "task_reward",
        amount: reward,
        taskId,
        description: `Task reward: ${taskSnap.val().title || taskId}`,
        createdAt: now
      };
      updates[`transactions/${uid}/${txId}`] = txPayload;
      updates[`userLedger/${uid}/${txId}`] = txPayload;
      updates[`users/${uid}/earnings/${txId}`] = {
        amount: reward,
        source: "task",
        timestamp: now
      };
    }

    await db.ref().update(updates);
    const responseBody = { success: true, id, status: "approved", reward };
    await saveIdempotency(req.requestId, 200, responseBody);
    return res.json(responseBody);
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

app.post('/v1/tasks/reviews/:id/reject', requireAdmin, checkIdempotency, async (req, res) => {
  const { id } = req.params;
  const { reason } = req.body;
  const adminUid = req.adminUid;

  try {
    const revRef = db.ref(`reviewQueue/${id}`);
    const snap = await revRef.get();
    if (!snap.exists()) {
      return res.status(404).json({ success: false, error: "Review submission not found" });
    }

    const r = snap.val();
    const uid = r.uid;
    const taskId = r.taskId;
    const now = Date.now();

    const updates = {};
    updates[`reviewQueue/${id}/status`] = "rejected";
    updates[`reviewQueue/${id}/rejectionReason`] = reason || "Proof rejected";
    updates[`reviewQueue/${id}/reviewedBy`] = adminUid;
    updates[`reviewQueue/${id}/reviewedAt`] = now;

    updates[`userTasks/${uid}/${taskId}/status`] = "rejected";
    updates[`userTasks/${uid}/${taskId}/rejectionReason`] = reason || "Proof rejected";
    updates[`userTasks/${uid}/${taskId}/reviewedBy`] = adminUid;
    updates[`userTasks/${uid}/${taskId}/reviewedAt`] = now;

    await db.ref().update(updates);
    const responseBody = { success: true, id, status: "rejected" };
    await saveIdempotency(req.requestId, 200, responseBody);
    return res.json(responseBody);
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// 8. Reward Leaderboard Winners
app.post('/v1/leaderboard/:period/reward', requireAdmin, async (req, res) => {
  const { period } = req.params;
  const { winners } = req.body; // array of { uid, rank, coins }

  if (!Array.isArray(winners) || winners.length === 0) {
    return res.status(400).json({ success: false, error: "Winners list must be a non-empty array" });
  }

  try {
    const now = Date.now();
    for (const w of winners) {
      if (!w.uid || !w.coins) continue;
      const coins = parseInt(w.coins, 10);
      if (coins <= 0) continue;

      await db.ref(`wallets/${w.uid}`).transaction((cur) => {
        const curCoins = (cur && cur.coins) || 0;
        return { ...cur, coins: curCoins + coins, updatedAt: now };
      });

      const txId = db.ref(`transactions/${w.uid}`).push().key;
      const txPayload = {
        id: txId,
        type: "credit",
        source: "leaderboard_prize",
        amount: coins,
        description: `${period} Leaderboard Rank ${w.rank} Prize`,
        createdAt: now
      };

      const updates = {};
      updates[`transactions/${w.uid}/${txId}`] = txPayload;
      updates[`userLedger/${w.uid}/${txId}`] = txPayload;
      updates[`users/${w.uid}/earnings/${txId}`] = {
        amount: coins,
        source: "leaderboard",
        timestamp: now
      };
      await db.ref().update(updates);
    }
    return res.json({ success: true, rewardedCount: winners.length });
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// 9. Season Reset (Owner Only)
app.post('/v1/season/reset', requireAdmin, async (req, res) => {
  if (!req.isOwner) {
    return res.status(403).json({ success: false, error: "Owner-only operation." });
  }

  const { confirm, seasonId, seasonName, startsAt } = req.body;
  if (confirm !== "DELETE") {
    return res.status(400).json({ success: false, error: "Must confirm with 'DELETE'" });
  }

  try {
    // 1. Pre-check: No pending withdrawals allowed
    const withdrawalsSnap = await db.ref('withdrawals').orderByChild('status').equalTo('pending').get();
    if (withdrawalsSnap.exists() && Object.keys(withdrawalsSnap.val()).length > 0) {
      return res.status(400).json({
        success: false,
        error: "Cannot reset season while pending withdrawals exist. Process or reject all pending items first."
      });
    }

    // 2. Fetch current season config
    const currentSeasonSnap = await db.ref('config/season').get();
    const oldSeason = currentSeasonSnap.val() || {};
    const oldSeasonId = oldSeason.seasonId || "season_prev";
    const oldSeasonName = oldSeason.seasonName || "Previous Season";

    // 3. Summaries & Rank generation
    const walletsSnap = await db.ref('wallets').get();
    const walletsData = walletsSnap.val() || {};
    const sortedUsers = Object.keys(walletsData).sort((a, b) => (walletsData[b].coins || 0) - (walletsData[a].coins || 0));

    const summaryUpdates = {};
    sortedUsers.forEach((uid, index) => {
      summaryUpdates[`seasons/${oldSeasonId}/summary/${uid}`] = {
        balance: walletsData[uid].coins || 0,
        rank: index + 1,
        seasonName: oldSeasonName
      };
    });
    await db.ref().update(summaryUpdates);

    // 4. Reset transient state (Keep wallets, user accounts, and profiles!)
    await Promise.all([
      db.ref('userTasks').remove(),
      db.ref('referrals').remove(),
      db.ref('leaderboard').remove(),
      db.ref('dailyEarnings').remove(),
      db.ref('rewardClaims').remove(),
      db.ref('scratchCards').remove(),
      db.ref('userSpins').remove()
    ]);

    // 5. Update config/season
    const newVersion = (oldSeason.version || 0) + 1;
    const now = Date.now();
    await db.ref('config/season').update({
      seasonId: seasonId || `season_${Date.now()}`,
      seasonName: seasonName || "New Season",
      version: newVersion,
      status: "active",
      startsAt: startsAt || now,
      summaryEnabled: true,
      previousSeasonId: oldSeasonId
    });

    // 6. Broadcast notification
    const notifId = db.ref('notifications/broadcast').push().key;
    await db.ref(`notifications/broadcast/${notifId}`).set({
      id: notifId,
      title: "New Season Has Begun!",
      message: `Welcome to ${seasonName || "the new season"}. Your coin balance has carried over!`,
      createdAt: now,
      status: "sent"
    });

    console.log(`[Season Reset Completed] New Season: ${seasonId}`);
    return res.json({ success: true, message: "Season reset complete", seasonId, oldSeasonId });
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// 10. Backup Create Endpoint
app.post('/v1/backup/create', requireAdmin, async (req, res) => {
  if (!req.isOwner) {
    return res.status(403).json({ success: false, error: "Owner-only operation." });
  }

  try {
    const backupId = `backup_${Date.now()}`;
    const [usersSnap, walletsSnap, txSnap, withSnap, tasksSnap, ticketsSnap] = await Promise.all([
      db.ref('users').get(),
      db.ref('wallets').get(),
      db.ref('transactions').get(),
      db.ref('withdrawals').get(),
      db.ref('tasks').get(),
      db.ref('supportTickets').get()
    ]);

    const recordCounts = {
      users: usersSnap.numChildren(),
      wallets: walletsSnap.numChildren(),
      transactions: txSnap.numChildren(),
      withdrawals: withSnap.numChildren(),
      tasks: tasksSnap.numChildren(),
      supportTickets: ticketsSnap.numChildren()
    };

    await db.ref(`backups/${backupId}`).set({
      backupId,
      createdAt: Date.now(),
      createdBy: req.adminUid,
      status: "completed",
      recordCounts
    });

    return res.json({ success: true, backupId, recordCounts });
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// Payout Methods: Reveal with Audit (Requires min 5-char reason)
app.post('/v1/payout-methods/:uid/:methodId/reveal', requireAdmin, checkIdempotency, async (req, res) => {
  const { uid, methodId } = req.params;
  const { reason } = req.body;

  if (!reason || reason.trim().length < 5) {
    return res.status(400).json({ success: false, error: "Valid justification reason (minimum 5 characters) is required for audit compliance." });
  }

  try {
    const snap = await db.ref(`paymentMethods/${uid}/${methodId}`).get();
    if (!snap.exists()) {
      return res.status(404).json({ success: false, error: "Payment method record not found" });
    }

    const methodData = snap.val();

    // Record Immutable Audit Trail
    await db.ref('adminAudit').push({
      action: "payout_method_revealed",
      targetUid: uid,
      methodId,
      reason: reason.trim(),
      adminUid: req.adminUid,
      timestamp: Date.now()
    });

    const responseBody = { success: true, method: methodData };
    if (req.requestId) await saveIdempotency(req.requestId, 200, responseBody);
    return res.json(responseBody);
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// Withdrawals: Reveal Payout Method with Audit
app.post('/v1/withdrawals/:id/reveal-payout', requireAdmin, checkIdempotency, async (req, res) => {
  const { id } = req.params;
  const { reason } = req.body;

  if (!reason || reason.trim().length < 5) {
    return res.status(400).json({ success: false, error: "Valid justification reason (minimum 5 characters) is required for audit compliance." });
  }

  try {
    const wSnap = await db.ref(`withdrawals/${id}`).get();
    if (!wSnap.exists()) {
      return res.status(404).json({ success: false, error: "Withdrawal record not found" });
    }

    const wData = wSnap.val();
    const uid = wData.uid;

    let payoutDetails = null;
    let source = "Not provided";

    if (wData.methodSnapshot && typeof wData.methodSnapshot === 'object' && Object.keys(wData.methodSnapshot).length > 0) {
      payoutDetails = wData.methodSnapshot;
      source = "Snapshot at request";
    } else if (uid) {
      const mSnap = await db.ref(`paymentMethods/${uid}`).get();
      if (mSnap.exists()) {
        const methods = mSnap.val();
        const mKey = (wData.method || "upi").toLowerCase();
        if (methods[mKey]) {
          payoutDetails = methods[mKey];
          source = "Current saved method";
        } else if (methods.upi) {
          payoutDetails = methods.upi;
          source = "Current saved method";
        } else if (methods.bank) {
          payoutDetails = methods.bank;
          source = "Current saved method";
        }
      }
    }

    // Record Immutable Audit Trail
    await db.ref('adminAudit').push({
      action: "withdrawal_payout_revealed",
      withdrawalId: id,
      targetUid: uid,
      reason: reason.trim(),
      adminUid: req.adminUid,
      timestamp: Date.now()
    });

    const responseBody = {
      success: true,
      withdrawalId: id,
      uid,
      source,
      payoutDetails
    };
    if (req.requestId) await saveIdempotency(req.requestId, 200, responseBody);
    return res.json(responseBody);
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// Payout Methods: Disable with Reason
app.post('/v1/payout-methods/:uid/:methodId/disable', requireAdmin, checkIdempotency, async (req, res) => {
  const { uid, methodId } = req.params;
  const { reason } = req.body;

  if (!reason || reason.trim().length < 5) {
    return res.status(400).json({ success: false, error: "Valid reason (minimum 5 characters) is required to disable payout method." });
  }

  try {
    const ref = db.ref(`paymentMethods/${uid}/${methodId}`);
    const snap = await ref.get();
    if (!snap.exists()) {
      return res.status(404).json({ success: false, error: "Payment method record not found" });
    }

    const updates = {
      status: "disabled",
      disabledReason: reason.trim(),
      disabledAt: Date.now(),
      updatedAt: Date.now()
    };

    await ref.update(updates);

    await db.ref('adminAudit').push({
      action: "payout_method_disabled",
      targetUid: uid,
      methodId,
      reason: reason.trim(),
      adminUid: req.adminUid,
      timestamp: Date.now()
    });

    const responseBody = { success: true, status: "disabled" };
    if (req.requestId) await saveIdempotency(req.requestId, 200, responseBody);
    return res.json(responseBody);
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// Tasks: Create Task (Idempotent, Audited)
app.post('/v1/tasks', requireAdmin, checkIdempotency, async (req, res) => {
  const task = req.body.task || req.body;
  if (!task.title || task.title.trim().length < 3) {
    return res.status(400).json({ success: false, error: "Task title must be at least 3 characters" });
  }

  try {
    const taskRef = task.taskId ? db.ref(`tasks/${task.taskId}`) : db.ref('tasks').push();
    const taskId = taskRef.key;
    const now = Date.now();

    const payload = {
      taskId,
      title: task.title.trim(),
      description: (task.description || "").trim(),
      logoUrl: task.logoUrl || "",
      link: task.link || "",
      steps: Array.isArray(task.steps) ? task.steps : [],
      reward: parseInt(task.reward, 10) || 0,
      rewardCoins: parseInt(task.reward, 10) || 0,
      perUserLimit: parseInt(task.perUserLimit, 10) || 1,
      totalLimit: parseInt(task.totalLimit, 10) || 0,
      deadline: parseInt(task.deadline, 10) || 0,
      proofType: task.proofType || "Screenshot",
      proofHint: (task.proofHint || "").trim(),
      terms: (task.terms || "").trim(),
      status: task.status || "draft",
      completionsCount: 0,
      publishedAt: task.status === "published" ? now : (task.publishedAt || 0),
      createdAt: now,
      updatedAt: now
    };

    await taskRef.set(payload);

    await db.ref('adminAudit').push({
      action: "task_created",
      taskId,
      title: payload.title,
      status: payload.status,
      adminUid: req.adminUid,
      timestamp: now
    });

    const responseBody = { success: true, task: payload };
    if (req.requestId) await saveIdempotency(req.requestId, 200, responseBody);
    return res.json(responseBody);
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});

// Tasks: Update Task (Locks key fields if completions > 0)
app.put('/v1/tasks/:id', requireAdmin, checkIdempotency, async (req, res) => {
  const taskId = req.params.id;
  const task = req.body.task || req.body;

  try {
    const taskRef = db.ref(`tasks/${taskId}`);
    const snap = await taskRef.get();
    if (!snap.exists()) {
      return res.status(404).json({ success: false, error: "Task not found" });
    }

    const existing = snap.val();
    const now = Date.now();
    const completions = existing.completionsCount || 0;
    const hasCompletions = completions > 0;

    const updated = {
      ...existing,
      title: task.title ? task.title.trim() : existing.title,
      description: task.description !== undefined ? task.description.trim() : existing.description,
      logoUrl: task.logoUrl !== undefined ? task.logoUrl : existing.logoUrl,
      link: task.link !== undefined ? task.link : existing.link,
      steps: Array.isArray(task.steps) ? task.steps : existing.steps,
      deadline: task.deadline !== undefined ? parseInt(task.deadline, 10) : existing.deadline,
      proofHint: task.proofHint !== undefined ? task.proofHint.trim() : existing.proofHint,
      terms: task.terms !== undefined ? task.terms.trim() : existing.terms,
      status: task.status !== undefined ? task.status : existing.status,
      updatedAt: now
    };

    // If first completion happened, lock reward, limits, and proofType
    if (hasCompletions) {
      updated.reward = existing.reward;
      updated.rewardCoins = existing.reward;
      updated.perUserLimit = existing.perUserLimit;
      updated.totalLimit = existing.totalLimit;
      updated.proofType = existing.proofType;
    } else {
      if (task.reward !== undefined) {
        updated.reward = parseInt(task.reward, 10);
        updated.rewardCoins = parseInt(task.reward, 10);
      }
      if (task.perUserLimit !== undefined) updated.perUserLimit = parseInt(task.perUserLimit, 10);
      if (task.totalLimit !== undefined) updated.totalLimit = parseInt(task.totalLimit, 10);
      if (task.proofType !== undefined) updated.proofType = task.proofType;
    }

    if (updated.status === "published" && (!existing.publishedAt || existing.publishedAt === 0)) {
      updated.publishedAt = now;
    }

    await taskRef.set(updated);

    await db.ref('adminAudit').push({
      action: "task_updated",
      taskId,
      title: updated.title,
      status: updated.status,
      hasLockedFields: hasCompletions,
      adminUid: req.adminUid,
      timestamp: now
    });

    const responseBody = { success: true, task: updated };
    if (req.requestId) await saveIdempotency(req.requestId, 200, responseBody);
    return res.json(responseBody);
  } catch (e) {
    return res.status(500).json({ success: false, error: e.message });
  }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Cash Opinion Console Server running on port ${PORT}`);
});

package com.cashopinion.console.data.repo

import com.cashopinion.console.core.Constants
import com.cashopinion.console.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ConsoleRepo(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth,
    val serverClient: ServerClient
) {
    // 1. Observe Admin Profile
    fun observeAdminProfile(uid: String): Flow<AdminProfile> = callbackFlow {
        val ref = database.getReference(Constants.NODE_ADMIN_PROFILES).child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val p = AdminProfile(
                        uid = uid,
                        name = snapshot.child("name").value?.toString().orEmpty(),
                        email = snapshot.child("email").value?.toString().orEmpty(),
                        role = snapshot.child("role").value?.toString() ?: "super",
                        photo = snapshot.child("photo").value?.toString().orEmpty(),
                        createdAt = snapshot.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L,
                        updatedAt = snapshot.child("updatedAt").value?.toString()?.toLongOrNull() ?: 0L
                    )
                    trySend(p)
                } else {
                    trySend(AdminProfile(uid = uid, email = auth.currentUser?.email.orEmpty()))
                }
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(AdminProfile(uid = uid))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateAdminProfile(name: String, photoBase64: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val updates = mutableMapOf<String, Any>(
                "name" to name,
                "updatedAt" to System.currentTimeMillis()
            )
            if (photoBase64 != null) {
                updates["photo"] = photoBase64
            }
            database.getReference(Constants.NODE_ADMIN_PROFILES).child(uid).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Observe Dashboard KPIs
    fun observeDashboardKpis(): Flow<DashboardKpis> = callbackFlow {
        val usersRef = database.getReference(Constants.NODE_USERS)
        val walletsRef = database.getReference(Constants.NODE_WALLETS)
        val withdrawalsRef = database.getReference(Constants.NODE_WITHDRAWALS)
        val ticketsRef = database.getReference(Constants.NODE_SUPPORT_TICKETS)
        val reviewRef = database.getReference(Constants.NODE_REVIEW_QUEUE)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Periodic aggregate
                } catch (_: Exception) {}
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        // Combine listeners for live dashboard
        val withdrawListener = object : ValueEventListener {
            override fun onDataChange(wSnap: DataSnapshot) {
                var pendingCount = 0
                var pendingInr = 0.0
                var urgentCount = 0
                val now = System.currentTimeMillis()
                val twoDaysAgo = now - (48 * 3600 * 1000L)

                for (child in wSnap.children) {
                    val status = child.child("status").value?.toString()
                    if (status == "pending") {
                        pendingCount++
                        val inr = child.child("inrAmount").value?.toString()?.toDoubleOrNull() ?: 0.0
                        pendingInr += inr
                        val requestedAt = child.child("requestedAt").value?.toString()?.toLongOrNull() ?: 0L
                        if (requestedAt > 0 && requestedAt < twoDaysAgo) {
                            urgentCount++
                        }
                    }
                }

                walletsRef.get().addOnSuccessListener { walletsSnap ->
                    var circulating = 0L
                    for (w in walletsSnap.children) {
                        circulating += w.child("coins").value?.toString()?.toLongOrNull() ?: 0L
                    }

                    usersRef.get().addOnSuccessListener { uSnap ->
                        val totalUsers = uSnap.childrenCount
                        ticketsRef.get().addOnSuccessListener { tSnap ->
                            var openTickets = 0
                            for (t in tSnap.children) {
                                if (t.child("status").value?.toString() == "open") openTickets++
                            }

                            reviewRef.get().addOnSuccessListener { revSnap ->
                                var urgentReviews = 0
                                for (r in revSnap.children) {
                                    val rStatus = r.child("status").value?.toString()
                                    val subAt = r.child("submittedAt").value?.toString()?.toLongOrNull() ?: 0L
                                    if (rStatus == "pending" && subAt > 0 && subAt < twoDaysAgo) {
                                        urgentReviews++
                                    }
                                }

                                trySend(
                                    DashboardKpis(
                                        totalUsers = totalUsers,
                                        totalCoinsCirculating = circulating,
                                        pendingWithdrawalsCount = pendingCount,
                                        pendingWithdrawalsInr = pendingInr,
                                        openReports = 0,
                                        openTickets = openTickets,
                                        urgentWithdrawalsCount = urgentCount,
                                        urgentReviewsCount = urgentReviews
                                    )
                                )
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        withdrawalsRef.addValueEventListener(withdrawListener)
        awaitClose { withdrawalsRef.removeEventListener(withdrawListener) }
    }

    // 3. Observe Withdrawals List
    fun observeWithdrawals(): Flow<List<WithdrawalItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_WITHDRAWALS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<WithdrawalItem>()
                for (child in snapshot.children) {
                    val id = child.key.orEmpty()
                    val uid = child.child("uid").value?.toString().orEmpty()
                    val method = child.child("method").value?.toString() ?: "upi"
                    val coins = child.child("coins").value?.toString()?.toLongOrNull() ?: 0L
                    val inr = child.child("inrAmount").value?.toString()?.toDoubleOrNull() ?: (coins / 1000.0)
                    val status = child.child("status").value?.toString() ?: "pending"
                    val utr = child.child("utr").value?.toString().orEmpty()
                    val paymentDetails = child.child("paymentDetails").value?.toString()
                        ?: child.child("upiId").value?.toString().orEmpty()
                    val requestedAt = child.child("requestedAt").value?.toString()?.toLongOrNull() ?: 0L
                    val processedAt = child.child("processedAt").value?.toString()?.toLongOrNull() ?: 0L
                    val processedBy = child.child("processedBy").value?.toString().orEmpty()
                    val rejectionReason = child.child("rejectionReason").value?.toString().orEmpty()
                    val reversalReason = child.child("reversalReason").value?.toString().orEmpty()

                    list.add(
                        WithdrawalItem(
                            id = id,
                            uid = uid,
                            method = method,
                            coins = coins,
                            inrAmount = inr,
                            status = status,
                            utr = utr,
                            paymentDetails = paymentDetails,
                            requestedAt = requestedAt,
                            processedAt = processedAt,
                            processedBy = processedBy,
                            rejectionReason = rejectionReason,
                            reversalReason = reversalReason
                        )
                    )
                }
                list.sortByDescending { it.requestedAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // 4. Observe Users List
    fun observeUsers(): Flow<List<UserItem>> = callbackFlow {
        val usersRef = database.getReference(Constants.NODE_USERS)
        val walletsRef = database.getReference(Constants.NODE_WALLETS)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                walletsRef.get().addOnSuccessListener { walletsSnap ->
                    val list = mutableListOf<UserItem>()
                    for (child in snapshot.children) {
                        val uid = child.key.orEmpty()
                        val wSnap = walletsSnap.child(uid)
                        val coins = wSnap.child("coins").value?.toString()?.toLongOrNull() ?: 0L
                        val withdrawn = wSnap.child("totalWithdrawn").value?.toString()?.toLongOrNull() ?: 0L
                        val pendingWId = wSnap.child("pendingWithdrawalId").value?.toString()

                        val isBanned = child.child("isBanned").value == true ||
                                child.child("status").value?.toString() == "banned"

                        list.add(
                            UserItem(
                                uid = uid,
                                name = child.child("name").value?.toString() ?: "User",
                                email = child.child("email").value?.toString().orEmpty(),
                                coins = coins,
                                totalWithdrawn = withdrawn,
                                pendingWithdrawalId = pendingWId,
                                isBanned = isBanned,
                                status = if (isBanned) "banned" else "active",
                                banReason = child.child("banReason").value?.toString(),
                                referralCode = child.child("referralCode").value?.toString().orEmpty(),
                                referredBy = child.child("referredBy").value?.toString().orEmpty(),
                                deviceHash = child.child("deviceId").value?.toString().orEmpty(),
                                ipAddress = child.child("ipAddress").value?.toString().orEmpty(),
                                createdAt = child.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L
                            )
                        )
                    }
                    list.sortByDescending { it.createdAt }
                    trySend(list)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }

    // 5. Observe Tasks List
    fun observeTasks(): Flow<List<TaskItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_TASKS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<TaskItem>()
                for (child in snapshot.children) {
                    val id = child.key.orEmpty()
                    val title = child.child("title").value?.toString()
                        ?: child.child("appName").value?.toString().orEmpty()
                    val desc = child.child("description").value?.toString().orEmpty()
                    val logoUrl = child.child("logoUrl").value?.toString()
                        ?: child.child("iconUrl").value?.toString().orEmpty()
                    val link = child.child("link").value?.toString()
                        ?: child.child("playStoreLink").value?.toString()
                        ?: child.child("actionUrl").value?.toString().orEmpty()
                    val reward = child.child("reward").value?.toString()?.toLongOrNull()
                        ?: child.child("rewardCoins").value?.toString()?.toLongOrNull()
                        ?: child.child("prizeCoins").value?.toString()?.toLongOrNull()
                        ?: child.child("coins").value?.toString()?.toLongOrNull() ?: 0L

                    val stepsList = mutableListOf<String>()
                    val stepsSnap = child.child("steps")
                    if (stepsSnap.exists()) {
                        for (s in stepsSnap.children) {
                            val v = s.value?.toString().orEmpty()
                            if (v.isNotBlank()) stepsList.add(v)
                        }
                    }
                    if (stepsList.isEmpty()) {
                        val inst = child.child("instructions").value?.toString()
                            ?: child.child("userInstructions").value?.toString().orEmpty()
                        if (inst.isNotBlank()) {
                            stepsList.addAll(inst.lines().filter { it.isNotBlank() })
                        }
                    }

                    val perUserLimit = child.child("perUserLimit").value?.toString()?.toIntOrNull() ?: 1
                    val totalLimit = child.child("totalLimit").value?.toString()?.toLongOrNull()
                        ?: child.child("slots").value?.toString()?.toLongOrNull() ?: 0L
                    val deadline = child.child("deadline").value?.toString()?.toLongOrNull()
                        ?: child.child("expiryAt").value?.toString()?.toLongOrNull() ?: 0L
                    val proofType = child.child("proofType").value?.toString() ?: "Screenshot"
                    val proofHint = child.child("proofHint").value?.toString().orEmpty()
                    val terms = child.child("terms").value?.toString().orEmpty()
                    val status = child.child("status").value?.toString() ?: "draft"
                    val completionsCount = child.child("completionsCount").value?.toString()?.toLongOrNull() ?: 0L
                    val publishedAt = child.child("publishedAt").value?.toString()?.toLongOrNull() ?: 0L
                    val createdAt = child.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L
                    val updatedAt = child.child("updatedAt").value?.toString()?.toLongOrNull() ?: 0L

                    list.add(
                        TaskItem(
                            taskId = id,
                            title = title,
                            description = desc,
                            logoUrl = logoUrl,
                            link = link,
                            steps = stepsList,
                            reward = reward,
                            perUserLimit = perUserLimit,
                            totalLimit = totalLimit,
                            deadline = deadline,
                            proofType = proofType,
                            proofHint = proofHint,
                            terms = terms,
                            status = status,
                            completionsCount = completionsCount,
                            publishedAt = publishedAt,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                    )
                }
                list.sortByDescending { if (it.updatedAt > 0) it.updatedAt else it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveTask(task: TaskItem): Result<Unit> {
        return try {
            val ref = if (task.taskId.isNotBlank()) {
                database.getReference(Constants.NODE_TASKS).child(task.taskId)
            } else {
                database.getReference(Constants.NODE_TASKS).push()
            }
            val id = ref.key ?: return Result.failure(Exception("Failed to generate task ID"))
            val now = System.currentTimeMillis()
            val payload = hashMapOf<String, Any>(
                "taskId" to id,
                "title" to task.title.trim(),
                "appName" to task.title.trim(),
                "description" to task.description.trim(),
                "logoUrl" to task.logoUrl,
                "iconUrl" to task.logoUrl,
                "link" to task.link.trim(),
                "playStoreLink" to task.link.trim(),
                "actionUrl" to task.link.trim(),
                "steps" to task.steps,
                "reward" to task.reward,
                "rewardCoins" to task.reward,
                "perUserLimit" to task.perUserLimit,
                "totalLimit" to task.totalLimit,
                "slots" to (if (task.totalLimit > 0) task.totalLimit else -1L),
                "deadline" to task.deadline,
                "expiryAt" to task.deadline,
                "proofType" to task.proofType,
                "proofHint" to task.proofHint.trim(),
                "terms" to task.terms.trim(),
                "status" to task.status,
                "active" to (task.status == "published"),
                "enabled" to (task.status == "published"),
                "completionsCount" to task.completionsCount,
                "publishedAt" to (if (task.status == "published" && task.publishedAt == 0L) now else task.publishedAt),
                "createdAt" to (if (task.createdAt > 0) task.createdAt else now),
                "updatedAt" to now
            )
            ref.setValue(payload).await()

            val adminUid = auth.currentUser?.uid.orEmpty()
            database.getReference("adminAudit").push().setValue(
                mapOf(
                    "action" to (if (task.taskId.isNotBlank()) "task_updated" else "task_created"),
                    "taskId" to id,
                    "title" to task.title.trim(),
                    "status" to task.status,
                    "adminUid" to adminUid,
                    "timestamp" to now
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveTask(taskId: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_TASKS).child(taskId).child("status").setValue("archived").await()
            val now = System.currentTimeMillis()
            val adminUid = auth.currentUser?.uid.orEmpty()
            database.getReference("adminAudit").push().setValue(
                mapOf("action" to "task_archived", "taskId" to taskId, "adminUid" to adminUid, "timestamp" to now)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreTask(taskId: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_TASKS).child(taskId).child("status").setValue("published").await()
            val now = System.currentTimeMillis()
            val adminUid = auth.currentUser?.uid.orEmpty()
            database.getReference("adminAudit").push().setValue(
                mapOf("action" to "task_restored", "taskId" to taskId, "adminUid" to adminUid, "timestamp" to now)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_TASKS).child(taskId).removeValue().await()
            val now = System.currentTimeMillis()
            val adminUid = auth.currentUser?.uid.orEmpty()
            database.getReference("adminAudit").push().setValue(
                mapOf("action" to "task_deleted", "taskId" to taskId, "adminUid" to adminUid, "timestamp" to now)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. Observe Review Queue (Supports Proof Optional / Direct Submissions)
    fun observeReviewQueue(): Flow<List<ReviewItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_REVIEW_QUEUE)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ReviewItem>()
                for (child in snapshot.children) {
                    val id = child.key.orEmpty()
                    val uid = child.child("uid").value?.toString().orEmpty()
                    val taskId = child.child("taskId").value?.toString().orEmpty()
                    val taskTitle = child.child("taskTitle").value?.toString()
                        ?: child.child("title").value?.toString().orEmpty()
                    val proof = child.child("proof").value?.toString()
                        ?: child.child("proofText").value?.toString()
                        ?: child.child("screenshotUrl").value?.toString().orEmpty()
                    val proofType = child.child("proofType").value?.toString()
                        ?: (if (proof.isNotBlank()) "Submitted" else "Optional")
                    val trackedUsageMs = child.child("trackedUsageMs").value?.toString()?.toLongOrNull() ?: 0L
                    val status = child.child("status").value?.toString() ?: "pending"
                    val subAt = child.child("submittedAt").value?.toString()?.toLongOrNull() ?: 0L

                    list.add(
                        ReviewItem(
                            id = id,
                            uid = uid,
                            taskId = taskId,
                            taskTitle = taskTitle,
                            proof = proof,
                            proofType = proofType,
                            trackedUsageMs = trackedUsageMs,
                            status = status,
                            submittedAt = subAt
                        )
                    )
                }
                list.sortByDescending { it.submittedAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun approveReview(review: ReviewItem): Result<Unit> {
        return try {
            val sResult = serverClient.approveReview(review.id)
            if (sResult.isSuccess) {
                return Result.success(Unit)
            }

            val now = System.currentTimeMillis()
            val adminUid = auth.currentUser?.uid.orEmpty()

            val taskSnap = database.getReference(Constants.NODE_TASKS).child(review.taskId).get().await()
            val reward = (taskSnap.child("reward").value?.toString()?.toLongOrNull())
                ?: (taskSnap.child("rewardCoins").value?.toString()?.toLongOrNull())
                ?: (taskSnap.child("coins").value?.toString()?.toLongOrNull()) ?: 0L
            val taskTitle = review.taskTitle.ifBlank {
                taskSnap.child("title").value?.toString() ?: review.taskId
            }

            val updates = hashMapOf<String, Any>(
                "${Constants.NODE_REVIEW_QUEUE}/${review.id}/status" to "approved",
                "${Constants.NODE_REVIEW_QUEUE}/${review.id}/reviewedBy" to adminUid,
                "${Constants.NODE_REVIEW_QUEUE}/${review.id}/reviewedAt" to now,
                "${Constants.NODE_USER_TASKS}/${review.uid}/${review.taskId}/status" to "approved",
                "${Constants.NODE_USER_TASKS}/${review.uid}/${review.taskId}/coinsAwarded" to reward,
                "${Constants.NODE_USER_TASKS}/${review.uid}/${review.taskId}/reviewedBy" to adminUid,
                "${Constants.NODE_USER_TASKS}/${review.uid}/${review.taskId}/reviewedAt" to now
            )

            if (reward > 0L) {
                val txRef = database.getReference(Constants.NODE_TRANSACTIONS).child(review.uid).push()
                val txId = txRef.key ?: UUID.randomUUID().toString()
                val txPayload = mapOf(
                    "id" to txId,
                    "type" to "credit",
                    "source" to "task_reward",
                    "amount" to reward,
                    "taskId" to review.taskId,
                    "description" to "Task reward: $taskTitle",
                    "createdAt" to now
                )
                updates["${Constants.NODE_TRANSACTIONS}/${review.uid}/$txId"] = txPayload
                updates["${Constants.NODE_USER_LEDGER}/${review.uid}/$txId"] = txPayload
                updates["${Constants.NODE_USERS}/${review.uid}/earnings/$txId"] = mapOf(
                    "amount" to reward,
                    "source" to "task",
                    "timestamp" to now
                )
                database.getReference(Constants.NODE_WALLETS).child(review.uid).child("coins")
                    .setValue(com.google.firebase.database.ServerValue.increment(reward)).await()
            }

            database.reference.updateChildren(updates).await()

            database.getReference("adminAudit").push().setValue(
                mapOf(
                    "action" to "task_review_approved",
                    "reviewId" to review.id,
                    "uid" to review.uid,
                    "taskId" to review.taskId,
                    "proofType" to review.proofType,
                    "rewardAwarded" to reward,
                    "adminUid" to adminUid,
                    "timestamp" to now
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectReview(review: ReviewItem, reason: String): Result<Unit> {
        return try {
            val sResult = serverClient.rejectReview(review.id, reason)
            if (sResult.isSuccess) {
                return Result.success(Unit)
            }

            val now = System.currentTimeMillis()
            val adminUid = auth.currentUser?.uid.orEmpty()
            val updates = hashMapOf<String, Any>(
                "${Constants.NODE_REVIEW_QUEUE}/${review.id}/status" to "rejected",
                "${Constants.NODE_REVIEW_QUEUE}/${review.id}/rejectionReason" to reason.trim(),
                "${Constants.NODE_REVIEW_QUEUE}/${review.id}/reviewedBy" to adminUid,
                "${Constants.NODE_REVIEW_QUEUE}/${review.id}/reviewedAt" to now,
                "${Constants.NODE_USER_TASKS}/${review.uid}/${review.taskId}/status" to "rejected",
                "${Constants.NODE_USER_TASKS}/${review.uid}/${review.taskId}/rejectionReason" to reason.trim(),
                "${Constants.NODE_USER_TASKS}/${review.uid}/${review.taskId}/reviewedBy" to adminUid,
                "${Constants.NODE_USER_TASKS}/${review.uid}/${review.taskId}/reviewedAt" to now
            )
            database.reference.updateChildren(updates).await()

            database.getReference("adminAudit").push().setValue(
                mapOf(
                    "action" to "task_review_rejected",
                    "reviewId" to review.id,
                    "uid" to review.uid,
                    "taskId" to review.taskId,
                    "reason" to reason.trim(),
                    "adminUid" to adminUid,
                    "timestamp" to now
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 7. Observe Season Config
    fun observeSeasonConfig(): Flow<SeasonConfig> = callbackFlow {
        val ref = database.getReference(Constants.NODE_CONFIG_SEASON)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val c = SeasonConfig(
                        seasonId = snapshot.child("seasonId").value?.toString().orEmpty(),
                        seasonName = snapshot.child("seasonName").value?.toString().orEmpty(),
                        version = snapshot.child("version").value?.toString()?.toIntOrNull() ?: 0,
                        status = snapshot.child("status").value?.toString() ?: "none",
                        startsAt = snapshot.child("startsAt").value?.toString()?.toLongOrNull() ?: 0L,
                        warningStartsAt = snapshot.child("warningStartsAt").value?.toString()?.toLongOrNull() ?: 0L,
                        warningTitle = snapshot.child("warningTitle").value?.toString().orEmpty(),
                        warningBody = snapshot.child("warningBody").value?.toString().orEmpty(),
                        balanceNote = snapshot.child("balanceNote").value?.toString().orEmpty(),
                        termsUrl = snapshot.child("termsUrl").value?.toString().orEmpty(),
                        newSeasonTitle = snapshot.child("newSeasonTitle").value?.toString().orEmpty(),
                        newSeasonBody = snapshot.child("newSeasonBody").value?.toString().orEmpty(),
                        summaryEnabled = snapshot.child("summaryEnabled").value == true,
                        previousSeasonId = snapshot.child("previousSeasonId").value?.toString()
                    )
                    trySend(c)
                } else {
                    trySend(SeasonConfig())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(SeasonConfig())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveSeasonConfig(season: SeasonConfig): Result<Unit> {
        return try {
            val newVersion = season.version + 1
            val updates = mapOf(
                "seasonId" to season.seasonId,
                "seasonName" to season.seasonName,
                "version" to newVersion,
                "status" to season.status,
                "startsAt" to season.startsAt,
                "warningStartsAt" to season.warningStartsAt,
                "warningTitle" to season.warningTitle,
                "warningBody" to season.warningBody,
                "balanceNote" to season.balanceNote,
                "termsUrl" to season.termsUrl,
                "newSeasonTitle" to season.newSeasonTitle,
                "newSeasonBody" to season.newSeasonBody,
                "summaryEnabled" to season.summaryEnabled,
                "previousSeasonId" to season.previousSeasonId
            )
            database.getReference(Constants.NODE_CONFIG_SEASON).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 8. Send In-App Warning to User
    suspend fun sendUserWarning(uid: String, message: String): Result<Unit> {
        return try {
            val ref = database.getReference(Constants.NODE_USER_WARNINGS).child(uid).push()
            val payload = mapOf(
                "id" to ref.key,
                "message" to message,
                "createdBy" to (auth.currentUser?.uid ?: "admin"),
                "createdAt" to System.currentTimeMillis()
            )
            ref.setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 9. Observe Support Tickets
    fun observeSupportTickets(): Flow<List<SupportTicketItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_SUPPORT_TICKETS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<SupportTicketItem>()
                for (child in snapshot.children) {
                    val msgs = mutableListOf<TicketMessage>()
                    val msgsSnap = child.child("messages")
                    for (m in msgsSnap.children) {
                        msgs.add(
                            TicketMessage(
                                sender = m.child("sender").value?.toString().orEmpty(),
                                text = m.child("text").value?.toString().orEmpty(),
                                timestamp = m.child("timestamp").value?.toString()?.toLongOrNull() ?: 0L
                            )
                        )
                    }
                    list.add(
                        SupportTicketItem(
                            id = child.key.orEmpty(),
                            uid = child.child("uid").value?.toString().orEmpty(),
                            subject = child.child("subject").value?.toString().orEmpty(),
                            status = child.child("status").value?.toString() ?: "open",
                            messages = msgs,
                            createdAt = child.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L,
                            updatedAt = child.child("updatedAt").value?.toString()?.toLongOrNull() ?: 0L
                        )
                    )
                }
                trySend(list.sortedByDescending { it.createdAt })
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun replySupportTicket(ticketId: String, text: String): Result<Unit> {
        return try {
            val ref = database.getReference(Constants.NODE_SUPPORT_TICKETS).child(ticketId)
            val msgRef = ref.child("messages").push()
            val now = System.currentTimeMillis()
            msgRef.setValue(
                mapOf(
                    "sender" to "admin",
                    "text" to text,
                    "timestamp" to now
                )
            ).await()
            ref.updateChildren(
                mapOf(
                    "updatedAt" to now,
                    "status" to "in_progress"
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTicketStatus(ticketId: String, status: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_SUPPORT_TICKETS).child(ticketId).updateChildren(
                mapOf(
                    "status" to status,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 10. Broadcast Notifications
    fun observeNotifications(): Flow<List<NotificationItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_NOTIFICATIONS).child("broadcast")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<NotificationItem>()
                for (child in snapshot.children) {
                    list.add(
                        NotificationItem(
                            id = child.key.orEmpty(),
                            title = child.child("title").value?.toString().orEmpty(),
                            message = child.child("message").value?.toString().orEmpty(),
                            audience = child.child("audience").value?.toString() ?: "all",
                            sendAt = child.child("sendAt").value?.toString()?.toLongOrNull() ?: 0L,
                            sentAt = child.child("sentAt").value?.toString()?.toLongOrNull() ?: 0L,
                            status = child.child("status").value?.toString() ?: "sent"
                        )
                    )
                }
                trySend(list.sortedByDescending { it.sendAt })
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun sendBroadcastNotification(title: String, message: String, audience: String): Result<Unit> {
        return try {
            val ref = database.getReference(Constants.NODE_NOTIFICATIONS).child("broadcast").push()
            val now = System.currentTimeMillis()
            val payload = mapOf(
                "title" to title,
                "message" to message,
                "audience" to audience,
                "sendAt" to now,
                "sentAt" to now,
                "status" to "sent"
            )
            ref.setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 11. Popups
    fun observePopups(): Flow<List<PopupItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_POPUPS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<PopupItem>()
                for (child in snapshot.children) {
                    list.add(
                        PopupItem(
                            id = child.key.orEmpty(),
                            title = child.child("title").value?.toString().orEmpty(),
                            message = child.child("message").value?.toString().orEmpty(),
                            imageUrl = child.child("imageUrl").value?.toString().orEmpty(),
                            buttonText = child.child("buttonText").value?.toString().orEmpty(),
                            buttonLink = child.child("buttonLink").value?.toString().orEmpty(),
                            active = child.child("active").value as? Boolean ?: true,
                            showOnce = child.child("showOnce").value as? Boolean ?: false,
                            startAt = child.child("startAt").value?.toString()?.toLongOrNull() ?: 0L,
                            endAt = child.child("endAt").value?.toString()?.toLongOrNull() ?: 0L
                        )
                    )
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun savePopup(item: PopupItem): Result<Unit> {
        return try {
            val ref = if (item.id.isBlank()) database.getReference(Constants.NODE_POPUPS).push()
            else database.getReference(Constants.NODE_POPUPS).child(item.id)
            val payload = mapOf(
                "title" to item.title,
                "message" to item.message,
                "imageUrl" to item.imageUrl,
                "buttonText" to item.buttonText,
                "buttonLink" to item.buttonLink,
                "active" to item.active,
                "showOnce" to item.showOnce,
                "startAt" to item.startAt,
                "endAt" to item.endAt
            )
            ref.setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePopup(id: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_POPUPS).child(id).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 12. Banners
    fun observeBanners(): Flow<List<BannerItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_BANNERS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<BannerItem>()
                for (child in snapshot.children) {
                    list.add(
                        BannerItem(
                            id = child.key.orEmpty(),
                            imageUrl = child.child("imageUrl").value?.toString().orEmpty(),
                            linkUrl = child.child("linkUrl").value?.toString().orEmpty(),
                            order = child.child("order").value?.toString()?.toIntOrNull() ?: 0,
                            active = child.child("active").value as? Boolean ?: true
                        )
                    )
                }
                trySend(list.sortedBy { it.order })
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveBanner(item: BannerItem): Result<Unit> {
        return try {
            val ref = if (item.id.isBlank()) database.getReference(Constants.NODE_BANNERS).push()
            else database.getReference(Constants.NODE_BANNERS).child(item.id)
            val payload = mapOf(
                "imageUrl" to item.imageUrl,
                "linkUrl" to item.linkUrl,
                "order" to item.order,
                "active" to item.active
            )
            ref.setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBanner(id: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_BANNERS).child(id).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 13. Promo Codes & Referrals
    fun observePromoCodes(): Flow<List<PromoCodeItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_PROMO_CODES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<PromoCodeItem>()
                for (child in snapshot.children) {
                    list.add(
                        PromoCodeItem(
                            code = child.key.orEmpty(),
                            reward = child.child("reward").value?.toString()?.toLongOrNull() ?: 0L,
                            usageLimit = child.child("usageLimit").value?.toString()?.toIntOrNull() ?: 100,
                            usedCount = child.child("usedCount").value?.toString()?.toIntOrNull() ?: 0,
                            expiresAt = child.child("expiresAt").value?.toString()?.toLongOrNull() ?: 0L,
                            active = child.child("active").value as? Boolean ?: true,
                            createdAt = child.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L
                        )
                    )
                }
                trySend(list.sortedByDescending { it.createdAt })
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun savePromoCode(item: PromoCodeItem): Result<Unit> {
        return try {
            val codeKey = item.code.trim().uppercase()
            val payload = mapOf(
                "reward" to item.reward,
                "usageLimit" to item.usageLimit,
                "usedCount" to item.usedCount,
                "expiresAt" to item.expiresAt,
                "active" to item.active,
                "createdAt" to if (item.createdAt == 0L) System.currentTimeMillis() else item.createdAt
            )
            database.getReference(Constants.NODE_PROMO_CODES).child(codeKey).setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePromoCode(code: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_PROMO_CODES).child(code).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 14. Gamification Config
    fun observeLeaderboardConfig(): Flow<LeaderboardConfig> = callbackFlow {
        val ref = database.getReference(Constants.NODE_CONFIG_LEADERBOARD)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    LeaderboardConfig(
                        resetTimeIst = snapshot.child("resetTimeIst").value?.toString() ?: "00:00",
                        winnersCount = snapshot.child("winnersCount").value?.toString()?.toIntOrNull() ?: 10,
                        firstPrizeCoins = snapshot.child("firstPrizeCoins").value?.toString()?.toLongOrNull() ?: 5000L,
                        secondPrizeCoins = snapshot.child("secondPrizeCoins").value?.toString()?.toLongOrNull() ?: 3000L,
                        thirdPrizeCoins = snapshot.child("thirdPrizeCoins").value?.toString()?.toLongOrNull() ?: 1000L
                    )
                )
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(LeaderboardConfig())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveLeaderboardConfig(config: LeaderboardConfig): Result<Unit> {
        return try {
            val payload = mapOf(
                "resetTimeIst" to config.resetTimeIst,
                "winnersCount" to config.winnersCount,
                "firstPrizeCoins" to config.firstPrizeCoins,
                "secondPrizeCoins" to config.secondPrizeCoins,
                "thirdPrizeCoins" to config.thirdPrizeCoins
            )
            database.getReference(Constants.NODE_CONFIG_LEADERBOARD).updateChildren(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeScratchConfig(): Flow<ScratchCardConfig> = callbackFlow {
        val ref = database.getReference(Constants.NODE_CONFIG_SCRATCH)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    ScratchCardConfig(
                        enabled = snapshot.child("enabled").value as? Boolean ?: true,
                        minReward = snapshot.child("minReward").value?.toString()?.toLongOrNull() ?: 10L,
                        maxReward = snapshot.child("maxReward").value?.toString()?.toLongOrNull() ?: 500L,
                        probabilityTier = snapshot.child("probabilityTier").value?.toString() ?: "standard"
                    )
                )
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(ScratchCardConfig())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveScratchConfig(config: ScratchCardConfig): Result<Unit> {
        return try {
            val payload = mapOf(
                "enabled" to config.enabled,
                "minReward" to config.minReward,
                "maxReward" to config.maxReward,
                "probabilityTier" to config.probabilityTier
            )
            database.getReference(Constants.NODE_CONFIG_SCRATCH).updateChildren(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSpinConfig(): Flow<SpinConfig> = callbackFlow {
        val ref = database.getReference(Constants.NODE_CONFIG_SPIN)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    SpinConfig(
                        thresholdCoins = snapshot.child("thresholdCoins").value?.toString()?.toLongOrNull() ?: 10000L,
                        spinsGranted = snapshot.child("spinsGranted").value?.toString()?.toIntOrNull() ?: 5,
                        rollingHours = snapshot.child("rollingHours").value?.toString()?.toIntOrNull() ?: 24
                    )
                )
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(SpinConfig())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveSpinConfig(config: SpinConfig): Result<Unit> {
        return try {
            val payload = mapOf(
                "thresholdCoins" to config.thresholdCoins,
                "spinsGranted" to config.spinsGranted,
                "rollingHours" to config.rollingHours
            )
            database.getReference(Constants.NODE_CONFIG_SPIN).updateChildren(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 15. Pages & FAQ
    fun observePages(): Flow<List<PageItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_PAGES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<PageItem>()
                for (child in snapshot.children) {
                    list.add(
                        PageItem(
                            id = child.key.orEmpty(),
                            title = child.child("title").value?.toString().orEmpty(),
                            body = child.child("body").value?.toString().orEmpty(),
                            updatedAt = child.child("updatedAt").value?.toString()?.toLongOrNull() ?: 0L
                        )
                    )
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun savePage(id: String, title: String, body: String): Result<Unit> {
        return try {
            val payload = mapOf(
                "title" to title,
                "body" to body,
                "updatedAt" to System.currentTimeMillis()
            )
            database.getReference(Constants.NODE_PAGES).child(id).setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeFaqs(): Flow<List<FaqItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_FAQ)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FaqItem>()
                for (child in snapshot.children) {
                    list.add(
                        FaqItem(
                            id = child.key.orEmpty(),
                            question = child.child("question").value?.toString().orEmpty(),
                            answer = child.child("answer").value?.toString().orEmpty(),
                            order = child.child("order").value?.toString()?.toIntOrNull() ?: 0,
                            active = child.child("active").value as? Boolean ?: true
                        )
                    )
                }
                trySend(list.sortedBy { it.order })
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveFaq(item: FaqItem): Result<Unit> {
        return try {
            val ref = if (item.id.isBlank()) database.getReference(Constants.NODE_FAQ).push()
            else database.getReference(Constants.NODE_FAQ).child(item.id)
            val payload = mapOf(
                "question" to item.question,
                "answer" to item.answer,
                "order" to item.order,
                "active" to item.active
            )
            ref.setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFaq(id: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_FAQ).child(id).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 16. Live Alerts
    fun observeLiveAlerts(): Flow<List<AlertItem>> = callbackFlow {
        val withdrawalsRef = database.getReference(Constants.NODE_WITHDRAWALS)
        val usersRef = database.getReference(Constants.NODE_USERS)
        val reviewsRef = database.getReference(Constants.NODE_REVIEW_QUEUE)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val alerts = mutableListOf<AlertItem>()
                val now = System.currentTimeMillis()
                val twoDaysAgo = now - (48 * 3600 * 1000L)

                // Check urgent SLA breaches
                for (child in snapshot.children) {
                    val status = child.child("status").value?.toString()
                    val reqAt = child.child("requestedAt").value?.toString()?.toLongOrNull() ?: 0L
                    if (status == "pending" && reqAt in 1..twoDaysAgo) {
                        alerts.add(
                            AlertItem(
                                id = "w_${child.key}",
                                title = "Payout SLA Breach (>48h)",
                                message = "Withdrawal #${child.key} for ₹${child.child("inrAmount").value} is pending over 48 hours.",
                                severity = "critical",
                                timestamp = reqAt
                            )
                        )
                    }
                }
                trySend(alerts)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        withdrawalsRef.addValueEventListener(listener)
        awaitClose { withdrawalsRef.removeEventListener(listener) }
    }

    // 17. Offers & Sponsored Offers
    fun observeOffers(): Flow<List<OfferItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_OFFERS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<OfferItem>()
                for (child in snapshot.children) {
                    val stepsList = mutableListOf<String>()
                    for (s in child.child("steps").children) {
                        stepsList.add(s.value?.toString().orEmpty())
                    }
                    list.add(
                        OfferItem(
                            id = child.key.orEmpty(),
                            title = child.child("title").value?.toString().orEmpty(),
                            description = child.child("description").value?.toString().orEmpty(),
                            sponsorName = child.child("sponsorName").value?.toString().orEmpty(),
                            logoUrl = child.child("logoUrl").value?.toString().orEmpty(),
                            reward = child.child("reward").value?.toString()?.toLongOrNull() ?: 0L,
                            steps = stepsList,
                            link = child.child("link").value?.toString().orEmpty(),
                            startTime = child.child("startTime").value?.toString()?.toLongOrNull() ?: 0L,
                            endTime = child.child("endTime").value?.toString()?.toLongOrNull() ?: 0L,
                            perUserLimit = child.child("perUserLimit").value?.toString()?.toIntOrNull() ?: 1,
                            totalLimit = child.child("totalLimit").value?.toString()?.toIntOrNull() ?: 1000,
                            statusBadgeText = child.child("statusBadgeText").value?.toString() ?: "Available",
                            category = child.child("category").value?.toString() ?: "General",
                            sortOrder = child.child("sortOrder").value?.toString()?.toIntOrNull() ?: 0,
                            status = child.child("status").value?.toString() ?: "active",
                            active = child.child("active").value as? Boolean ?: true
                        )
                    )
                }
                trySend(list.sortedBy { it.sortOrder })
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveOffer(item: OfferItem): Result<Unit> {
        return try {
            val ref = if (item.id.isBlank()) database.getReference(Constants.NODE_OFFERS).push()
            else database.getReference(Constants.NODE_OFFERS).child(item.id)
            val payload = mapOf(
                "title" to item.title,
                "description" to item.description,
                "sponsorName" to item.sponsorName,
                "logoUrl" to item.logoUrl,
                "reward" to item.reward,
                "steps" to item.steps,
                "link" to item.link,
                "startTime" to item.startTime,
                "endTime" to item.endTime,
                "perUserLimit" to item.perUserLimit,
                "totalLimit" to item.totalLimit,
                "statusBadgeText" to item.statusBadgeText,
                "category" to item.category,
                "sortOrder" to item.sortOrder,
                "status" to item.status,
                "active" to item.active
            )
            ref.setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveOffer(id: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_OFFERS).child(id).updateChildren(
                mapOf(
                    "status" to "ended",
                    "active" to false
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeOffersHistory(): Flow<List<OfferHistoryItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_OFFERS_HISTORY)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<OfferHistoryItem>()
                for (child in snapshot.children) {
                    list.add(
                        OfferHistoryItem(
                            id = child.key.orEmpty(),
                            offerId = child.child("offerId").value?.toString().orEmpty(),
                            offerTitle = child.child("offerTitle").value?.toString().orEmpty(),
                            uid = child.child("uid").value?.toString().orEmpty(),
                            status = child.child("status").value?.toString() ?: "completed",
                            coinsAwarded = child.child("coinsAwarded").value?.toString()?.toLongOrNull() ?: 0L,
                            timestamp = child.child("timestamp").value?.toString()?.toLongOrNull() ?: 0L
                        )
                    )
                }
                trySend(list.sortedByDescending { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // 18. Redeem Codes Management
    fun observeRedeemCodes(): Flow<List<AdminRedeemCodeItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_REDEEM_CODES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<AdminRedeemCodeItem>()
                for (userNode in snapshot.children) {
                    for (codeNode in userNode.children) {
                        list.add(
                            AdminRedeemCodeItem(
                                id = codeNode.key.orEmpty(),
                                plainCode = codeNode.child("plainCode").value?.toString().orEmpty(),
                                codeHash = codeNode.child("cipher").value?.toString().orEmpty(),
                                valueCoins = codeNode.child("valueCoins").value?.toString()?.toLongOrNull() ?: 0L,
                                status = codeNode.child("status").value?.toString() ?: "unused",
                                redeemedByUid = codeNode.child("redeemedByUid").value?.toString(),
                                redeemedAt = codeNode.child("redeemedAt").value?.toString()?.toLongOrNull() ?: 0L,
                                expiresAt = codeNode.child("expiresAt").value?.toString()?.toLongOrNull() ?: 0L,
                                createdAt = codeNode.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L
                            )
                        )
                    }
                }
                trySend(list.sortedByDescending { it.createdAt })
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun createRedeemCodesBatch(count: Int, coins: Long, expiryDays: Int): Result<List<String>> {
        return try {
            val generatedCodes = mutableListOf<String>()
            val now = System.currentTimeMillis()
            val expiresAt = now + (expiryDays * 24 * 3600 * 1000L)
            val batchRef = database.getReference(Constants.NODE_REDEEM_CODES).child("unassigned")

            for (i in 1..count) {
                val code = "CP-" + java.util.UUID.randomUUID().toString().take(8).uppercase()
                generatedCodes.add(code)
                val pushRef = batchRef.push()
                val payload = mapOf(
                    "plainCode" to code,
                    "valueCoins" to coins,
                    "status" to "unused",
                    "expiresAt" to expiresAt,
                    "createdAt" to now
                )
                pushRef.setValue(payload).await()
            }
            Result.success(generatedCodes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disableRedeemCode(codeId: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_REDEEM_CODES).child("unassigned").child(codeId)
                .updateChildren(mapOf("status" to "disabled")).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 19. User Payout Methods (Real-time, Joined User Details, Strict Masking)
    fun observePayoutMethods(): Flow<List<PayoutMethodItem>> = callbackFlow {
        val methodsRef = database.getReference(Constants.NODE_PAYMENT_METHODS)
        val usersRef = database.getReference(Constants.NODE_USERS)

        var usersMap = emptyMap<String, Pair<String, String>>() // uid -> (name, email)

        val usersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Pair<String, String>>()
                for (u in snapshot.children) {
                    val uid = u.key.orEmpty()
                    val name = u.child("name").value?.toString() ?: u.child("displayName").value?.toString().orEmpty()
                    val email = u.child("email").value?.toString().orEmpty()
                    map[uid] = Pair(name, email)
                }
                usersMap = map
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        usersRef.addValueEventListener(usersListener)

        val methodsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<PayoutMethodItem>()
                for (userNode in snapshot.children) {
                    val uid = userNode.key.orEmpty()
                    val userPair = usersMap[uid]
                    val ownerName = userPair?.first?.takeIf { it.isNotBlank() } ?: "User not found"
                    val ownerEmail = userPair?.second.orEmpty()

                    for (methodNode in userNode.children) {
                        val mKey = methodNode.key.orEmpty()
                        val mType = methodNode.child("type").value?.toString() ?: mKey
                        val status = methodNode.child("status").value?.toString() ?: "Unknown"
                        val disabledReason = methodNode.child("disabledReason").value?.toString()
                        val createdAt = methodNode.child("createdAt").value?.toString()?.toLongOrNull()
                            ?: methodNode.child("updatedAt").value?.toString()?.toLongOrNull() ?: 0L

                        if (mType.equals("upi", ignoreCase = true) || methodNode.hasChild("id") || methodNode.hasChild("upiId")) {
                            val rawId = methodNode.child("id").value?.toString()
                                ?: methodNode.child("upiId").value?.toString()
                                ?: methodNode.child("address").value?.toString().orEmpty()

                            val masked = if (rawId.contains("@")) {
                                val parts = rawId.split("@", limit = 2)
                                "${parts[0].take(2)}****@${parts.getOrElse(1) { "" }}"
                            } else if (rawId.isNotBlank()) {
                                "${rawId.take(2)}****"
                            } else "****"

                            list.add(
                                PayoutMethodItem(
                                    id = mKey,
                                    uid = uid,
                                    ownerName = ownerName,
                                    ownerEmail = ownerEmail,
                                    type = "upi",
                                    maskedAddress = masked,
                                    fullAddress = rawId,
                                    status = status,
                                    disabledReason = disabledReason,
                                    createdAt = createdAt
                                )
                            )
                        } else if (mType.equals("bank", ignoreCase = true) || methodNode.hasChild("accountNumber")) {
                            val accNo = methodNode.child("accountNumber").value?.toString().orEmpty()
                            val bankName = methodNode.child("bankName").value?.toString().orEmpty()
                            val ifsc = methodNode.child("ifsc").value?.toString().orEmpty()
                            val holder = methodNode.child("fullName").value?.toString()
                                ?: methodNode.child("accountHolder").value?.toString().orEmpty()

                            val last4 = if (accNo.length >= 4) accNo.takeLast(4) else accNo
                            val bankPrefix = if (bankName.isNotBlank() && ifsc.isNotBlank()) {
                                "$bankName (${ifsc.take(4)}) - "
                            } else if (bankName.isNotBlank()) {
                                "$bankName - "
                            } else ""
                            val masked = "${bankPrefix}XXXX$last4"
                            val maskedHolder = if (holder.isNotBlank()) "${holder.take(1)}*****" else "Unknown"

                            list.add(
                                PayoutMethodItem(
                                    id = mKey,
                                    uid = uid,
                                    ownerName = ownerName,
                                    ownerEmail = ownerEmail,
                                    type = "bank",
                                    maskedAddress = masked,
                                    fullAddress = if (bankName.isNotBlank()) "$bankName - A/C: $accNo (IFSC: $ifsc)" else accNo,
                                    bankName = bankName,
                                    ifsc = ifsc,
                                    accountHolder = holder,
                                    maskedAccountHolder = maskedHolder,
                                    status = status,
                                    disabledReason = disabledReason,
                                    createdAt = createdAt
                                )
                            )
                        } else if (mType.equals("redeem", ignoreCase = true) || methodNode.hasChild("email")) {
                            val email = methodNode.child("email").value?.toString().orEmpty()
                            val masked = if (email.contains("@")) {
                                "${email.take(2)}****@${email.substringAfter("@", "")}"
                            } else "****"

                            list.add(
                                PayoutMethodItem(
                                    id = mKey,
                                    uid = uid,
                                    ownerName = ownerName,
                                    ownerEmail = ownerEmail,
                                    type = "redeem",
                                    maskedAddress = masked,
                                    fullAddress = email,
                                    status = status,
                                    disabledReason = disabledReason,
                                    createdAt = createdAt
                                )
                            )
                        }
                    }
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        methodsRef.addValueEventListener(methodsListener)
        awaitClose {
            methodsRef.removeEventListener(methodsListener)
            usersRef.removeEventListener(usersListener)
        }
    }

    suspend fun recordPayoutMethodReveal(uid: String, methodId: String, reason: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val adminUid = auth.currentUser?.uid.orEmpty()
            database.getReference("adminAudit").push().setValue(
                mapOf(
                    "action" to "payout_method_revealed",
                    "targetUid" to uid,
                    "methodId" to methodId,
                    "reason" to reason.trim(),
                    "adminUid" to adminUid,
                    "timestamp" to now
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disablePayoutMethod(uid: String, methodId: String, reason: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val adminUid = auth.currentUser?.uid.orEmpty()
            val updates = mapOf<String, Any>(
                "status" to "disabled",
                "disabledReason" to reason.trim(),
                "disabledAt" to now,
                "updatedAt" to now
            )
            database.getReference(Constants.NODE_PAYMENT_METHODS)
                .child(uid)
                .child(methodId)
                .updateChildren(updates)
                .await()

            database.getReference("adminAudit").push().setValue(
                mapOf(
                    "action" to "payout_method_disabled",
                    "targetUid" to uid,
                    "methodId" to methodId,
                    "reason" to reason.trim(),
                    "adminUid" to adminUid,
                    "timestamp" to now
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 20. Global Transactions Reader
    fun observeGlobalTransactions(): Flow<List<GlobalTransactionItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_TRANSACTIONS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<GlobalTransactionItem>()
                for (userNode in snapshot.children) {
                    val uid = userNode.key.orEmpty()
                    for (txNode in userNode.children) {
                        list.add(
                            GlobalTransactionItem(
                                txId = txNode.key.orEmpty(),
                                uid = uid,
                                type = txNode.child("type").value?.toString().orEmpty(),
                                amount = txNode.child("amount").value?.toString()?.toLongOrNull() ?: 0L,
                                title = txNode.child("title").value?.toString().orEmpty(),
                                refId = txNode.child("refId").value?.toString().orEmpty(),
                                createdAt = txNode.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L
                            )
                        )
                    }
                }
                trySend(list.sortedByDescending { it.createdAt }.take(500))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // 21. Problem Reports
    fun observeProblemReports(): Flow<List<ProblemReportItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_REPORTS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ProblemReportItem>()
                for (child in snapshot.children) {
                    list.add(
                        ProblemReportItem(
                            id = child.key.orEmpty(),
                            uid = child.child("uid").value?.toString().orEmpty(),
                            category = child.child("category").value?.toString() ?: "general",
                            message = child.child("message").value?.toString().orEmpty(),
                            deviceInfo = child.child("deviceInfo").value?.toString().orEmpty(),
                            status = child.child("status").value?.toString() ?: "open",
                            adminReply = child.child("adminReply").value?.toString().orEmpty(),
                            createdAt = child.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L,
                            updatedAt = child.child("updatedAt").value?.toString()?.toLongOrNull() ?: 0L
                        )
                    )
                }
                trySend(list.sortedByDescending { it.createdAt })
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun replyProblemReport(reportId: String, reply: String, status: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_REPORTS).child(reportId).updateChildren(
                mapOf(
                    "adminReply" to reply,
                    "status" to status,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 22. Daily Bonus Config
    fun observeDailyBonusConfig(): Flow<DailyBonusConfig> = callbackFlow {
        val ref = database.getReference(Constants.NODE_CONFIG_APP).child("dailyBonus")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    DailyBonusConfig(
                        enabled = snapshot.child("enabled").value as? Boolean ?: true,
                        baseCoins = snapshot.child("baseCoins").value?.toString()?.toLongOrNull() ?: 50L,
                        day7BonusCoins = snapshot.child("day7BonusCoins").value?.toString()?.toLongOrNull() ?: 500L,
                        streakRequiredDays = snapshot.child("streakRequiredDays").value?.toString()?.toIntOrNull() ?: 7,
                        resetTimeIst = snapshot.child("resetTimeIst").value?.toString() ?: "00:00",
                        version = snapshot.child("version").value?.toString()?.toIntOrNull() ?: 1
                    )
                )
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(DailyBonusConfig())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveDailyBonusConfig(config: DailyBonusConfig): Result<Unit> {
        return try {
            val payload = mapOf(
                "enabled" to config.enabled,
                "baseCoins" to config.baseCoins,
                "day7BonusCoins" to config.day7BonusCoins,
                "streakRequiredDays" to config.streakRequiredDays,
                "resetTimeIst" to config.resetTimeIst,
                "version" to (config.version + 1)
            )
            database.getReference(Constants.NODE_CONFIG_APP).child("dailyBonus").updateChildren(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

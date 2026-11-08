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
                    val title = child.child("title").value?.toString().orEmpty()
                    val desc = child.child("description").value?.toString().orEmpty()
                    val reward = child.child("reward").value?.toString()?.toLongOrNull()
                        ?: child.child("coins").value?.toString()?.toLongOrNull() ?: 0L
                    val status = child.child("status").value?.toString() ?: "published"
                    val link = child.child("link").value?.toString().orEmpty()
                    val deadline = child.child("deadline").value?.toString()?.toLongOrNull() ?: 0L
                    val createdAt = child.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L

                    list.add(
                        TaskItem(
                            taskId = id,
                            title = title,
                            description = desc,
                            reward = reward,
                            status = status,
                            link = link,
                            deadline = deadline,
                            createdAt = createdAt
                        )
                    )
                }
                list.sortByDescending { it.createdAt }
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
            val payload = mapOf(
                "taskId" to id,
                "title" to task.title,
                "description" to task.description,
                "link" to task.link,
                "reward" to task.reward,
                "status" to task.status,
                "deadline" to task.deadline,
                "createdAt" to if (task.createdAt > 0) task.createdAt else now,
                "updatedAt" to now
            )
            ref.setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveTask(taskId: String): Result<Unit> {
        return try {
            database.getReference(Constants.NODE_TASKS).child(taskId).child("status").setValue("archived").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. Observe Review Queue
    fun observeReviewQueue(): Flow<List<ReviewItem>> = callbackFlow {
        val ref = database.getReference(Constants.NODE_REVIEW_QUEUE)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ReviewItem>()
                for (child in snapshot.children) {
                    val id = child.key.orEmpty()
                    val uid = child.child("uid").value?.toString().orEmpty()
                    val taskId = child.child("taskId").value?.toString().orEmpty()
                    val proof = child.child("proof").value?.toString()
                        ?: child.child("proofText").value?.toString()
                        ?: child.child("screenshotUrl").value?.toString().orEmpty()
                    val status = child.child("status").value?.toString() ?: "pending"
                    val subAt = child.child("submittedAt").value?.toString()?.toLongOrNull() ?: 0L

                    list.add(
                        ReviewItem(
                            id = id,
                            uid = uid,
                            taskId = taskId,
                            proof = proof,
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
}

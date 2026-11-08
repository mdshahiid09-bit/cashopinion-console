package com.cashopinion.console.data.repo

import com.cashopinion.console.core.Constants
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepo(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    suspend fun signIn(email: String, pass: String): Result<String> {
        return try {
            val res = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = res.user ?: throw Exception("Login failed. No user object.")
            val uid = user.uid

            // Owner Gate Verification (Section 2.1)
            val isOwner = uid == Constants.OWNER_UID || email.trim().equals(Constants.OWNER_EMAIL, ignoreCase = true)
            if (!isOwner) {
                val adminUidSnap = database.getReference(Constants.NODE_ADMIN_UIDS).child(uid).get().await()
                val adminSnap = database.getReference(Constants.NODE_ADMINS).child(uid).get().await()

                val hasAccess = adminUidSnap.value == true || adminSnap.value == true
                if (!hasAccess) {
                    auth.signOut()
                    return Result.failure(Exception("Access denied. Admin credentials required."))
                }
            }

            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(currentPass: String, newPass: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("Not authenticated")
            val email = user.email ?: throw Exception("No email associated")
            val credential = EmailAuthProvider.getCredential(email, currentPass)
            user.reauthenticate(credential).await()
            user.updatePassword(newPass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    val currentUser get() = auth.currentUser
}

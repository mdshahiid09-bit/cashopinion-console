package com.cashopinion.console.ui.screens.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.cashopinion.console.R
import com.cashopinion.console.core.SessionManager
import com.cashopinion.console.data.model.AdminProfile
import com.cashopinion.console.data.repo.AuthRepo
import com.cashopinion.console.data.repo.ConsoleRepo
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@Composable
fun ProfileScreen(
    consoleRepo: ConsoleRepo,
    authRepo: AuthRepo,
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = authRepo.currentUser?.uid.orEmpty()
    val profile by consoleRepo.observeAdminProfile(uid).collectAsState(initial = AdminProfile(uid = uid))

    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var photoBase64 by remember(profile.photo) { mutableStateOf(profile.photo) }
    var isSaving by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    // Password change state
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isChangingPassword by remember { mutableStateOf(false) }
    var passwordMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(inputStream)
                if (original != null) {
                    val scaled = Bitmap.createScaledBitmap(original, 256, 256, true)
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                    val bytes = baos.toByteArray()
                    if (bytes.size <= 150 * 1024) { // Max 150 KB
                        photoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    } else {
                        saveStatus = "Image is too large. Must be under 150 KB."
                    }
                }
            } catch (e: Exception) {
                saveStatus = "Failed to load image: ${e.message}"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Admin Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        // Photo & Name Card
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(Shapes.Circle)
                        .background(colors.surfaceVariant)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoBase64.isNotBlank()) {
                        val bitmap = remember(photoBase64) {
                            try {
                                val bytes = Base64.decode(photoBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_user),
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_user),
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap photo to change (Max 150 KB)",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Admin Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Role: ${profile.role.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary
                )

                Text(
                    text = "Email: ${profile.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                if (saveStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = saveStatus!!, color = colors.primary, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isSaving = true
                        saveStatus = null
                        scope.launch {
                            val res = consoleRepo.updateAdminProfile(name, photoBase64)
                            isSaving = false
                            saveStatus = if (res.isSuccess) "Profile updated successfully!" else res.exceptionOrNull()?.message
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSaving) "Saving..." else "Save Profile")
                }
            }
        }

        // Change Password Card
        Card(
            shape = Shapes.Card,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Change Password",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (passwordMessage != null) {
                    Text(text = passwordMessage!!, color = colors.primary, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        if (newPassword != confirmNewPassword) {
                            passwordMessage = "New passwords do not match."
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            passwordMessage = "Password must be at least 6 characters."
                            return@Button
                        }

                        isChangingPassword = true
                        passwordMessage = null
                        scope.launch {
                            val res = authRepo.changePassword(currentPassword, newPassword)
                            isChangingPassword = false
                            if (res.isSuccess) {
                                passwordMessage = "Password changed successfully!"
                                currentPassword = ""
                                newPassword = ""
                                confirmNewPassword = ""
                            } else {
                                passwordMessage = res.exceptionOrNull()?.message ?: "Failed to change password"
                            }
                        }
                    },
                    enabled = !isChangingPassword && currentPassword.isNotBlank() && newPassword.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isChangingPassword) "Updating..." else "Change Password")
                }
            }
        }
    }
}

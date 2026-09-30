package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BloodGroup
import com.example.model.UserRole
import com.example.ui.LifeLinkViewModel
import com.example.ui.ScreenRoute
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    viewModel: LifeLinkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isSignUp by viewModel.isSignUpMode.collectAsState()
    val emailOrPhone by viewModel.authEmailOrPhone.collectAsState()
    val password by viewModel.authPassword.collectAsState()
    val confirmPassword by viewModel.authConfirmPassword.collectAsState()
    val name by viewModel.authName.collectAsState()
    val selectedRole by viewModel.authRole.collectAsState()
    val selectedBloodGroup by viewModel.authBloodGroup.collectAsState()
    val city by viewModel.authCity.collectAsState()
    val rememberMe by viewModel.authRememberMe.collectAsState()
    val donorConsent by viewModel.authDonorConsent.collectAsState()
    val showPassword by viewModel.authShowPassword.collectAsState()
    val errorMessage by viewModel.authErrorMessage.collectAsState()
    val otpDialogVisible by viewModel.authOtpDialogVisible.collectAsState()
    val simulatedOtp by viewModel.authSimulatedOtp.collectAsState()
    val enteredOtp by viewModel.authEnteredOtp.collectAsState()

    // If already logged in, show user profile overview with role switch and logout!
    if (currentUser != null) {
        val user = currentUser!!
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(BloodRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        color = BloodRedPrimary
                    )
                }
            }

            item {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${user.email} • Role: ${user.role.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Active Profile Details:", fontWeight = FontWeight.Bold)
                        Text("Registered Blood Group: ${user.bloodGroup?.label ?: "N/A"}")
                        Text("City: ${user.city}")
                        Text("Contact Phone: ${user.phone}")
                        Text("Emergency Consent Granted: ${if (user.consentGranted) "Yes" else "No"}")
                    }
                }
            }

            item {
                Text("Switch Quick Demo Role:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UserRole.entries.forEach { role ->
                        FilterChip(
                            selected = user.role == role,
                            onClick = {
                                viewModel.repository.switchRole(role)
                                when (role) {
                                    UserRole.DONOR -> viewModel.navigateTo(ScreenRoute.DONOR_DASHBOARD)
                                    UserRole.ADMIN -> viewModel.navigateTo(ScreenRoute.ADMIN_PANEL)
                                    UserRole.REQUESTER -> viewModel.navigateTo(ScreenRoute.EMERGENCY_REQUESTS)
                                }
                            },
                            label = { Text(role.displayName, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .testTag("logout_button")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout from LifeLink")
                }
            }
        }
        return
    }

    // Centered Login / Sign-up Card Layout
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // LifeLink Logo Banner
        item {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(BloodRedLight, BloodRedPrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "LifeLink",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "LifeLink Account",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = BloodRedPrimary
            )
            Text(
                text = if (isSignUp) "Create your account to save lives" else "Sign in to manage donations & requests",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Mode Switcher (Login vs Register)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isSignUp) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (!isSignUp) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.isSignUpMode.value = false }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Login",
                        textAlign = TextAlign.Center,
                        fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isSignUp) BloodRedPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSignUp) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isSignUp) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.isSignUpMode.value = true }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Register",
                        textAlign = TextAlign.Center,
                        fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSignUp) BloodRedPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Role Selector (Choose role first as requested!)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select Your Account Role *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UserRole.entries.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { viewModel.authRole.value = role },
                            label = { Text(role.displayName, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Name (if sign up)
        if (isSignUp) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.authName.value = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Email or Phone input
        item {
            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = { viewModel.authEmailOrPhone.value = it },
                label = { Text("Email Address or 10-Digit Mobile Number *") },
                placeholder = { Text("e.g. rahul@example.com or 9812345678") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_or_phone")
            )
        }

        // Password input with visibility toggle
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.authPassword.value = it },
                label = { Text("Password (Min 8 characters) *") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { viewModel.authShowPassword.value = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password")
            )
        }

        // Confirm Password (Sign up only)
        if (isSignUp) {
            item {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { viewModel.authConfirmPassword.value = it },
                    label = { Text("Confirm Password *") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Blood Group & City (For donors only)
            if (selectedRole == UserRole.DONOR) {
                item {
                    Text("Your Blood Group (Donors only) *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BloodGroup.entries.take(4).forEach { group ->
                            FilterChip(
                                selected = selectedBloodGroup == group,
                                onClick = { viewModel.authBloodGroup.value = group },
                                label = { Text(group.label, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BloodGroup.entries.drop(4).forEach { group ->
                            FilterChip(
                                selected = selectedBloodGroup == group,
                                onClick = { viewModel.authBloodGroup.value = group },
                                label = { Text(group.label, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { viewModel.authCity.value = it },
                        label = { Text("City / Location *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Consent Checkbox for Donors
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = donorConsent,
                            onCheckedChange = { viewModel.authDonorConsent.value = it },
                            modifier = Modifier.testTag("auth_donor_consent")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "I consent to sharing contact details during emergency patient requests.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // Remember me and Forgot password row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { viewModel.authRememberMe.value = it },
                            modifier = Modifier.testTag("auth_remember_me")
                        )
                        Text("Remember me", fontSize = 12.sp)
                    }

                    TextButton(onClick = { viewModel.startForgotPasswordFlow() }) {
                        Text("Forgot password?", fontSize = 12.sp, color = BloodRedPrimary)
                    }
                }
            }
        }

        // Error message if any
        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Submit Button
        item {
            Button(
                onClick = { viewModel.executeAuth(context) },
                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_submit_button")
            ) {
                Text(
                    text = if (isSignUp) "CREATE ${selectedRole.displayName.uppercase()} ACCOUNT" else "LOGIN",
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Google Sign In Button
        item {
            OutlinedButton(
                onClick = { viewModel.loginWithGoogle(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("google_login_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = BloodRedPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Login with Google", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Simulated OTP Dialog for Forgot Password
    if (otpDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.authOtpDialogVisible.value = false },
            title = {
                Text("Password Reset OTP", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "A temporary security code has been dispatched to your phone/email: $simulatedOtp",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { viewModel.authEnteredOtp.value = it },
                        label = { Text("Enter 4-Digit OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.verifyOtpAndReset(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                ) {
                    Text("Verify & Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.authOtpDialogVisible.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

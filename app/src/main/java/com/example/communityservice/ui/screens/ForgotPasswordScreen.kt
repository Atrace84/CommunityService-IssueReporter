package com.example.communityservice.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth


private val Emerald50 = Color(0xFFECFDF5)
private val Slate600 = Color(0xFF475569)


private val ALLOWED_EMAIL_PATTERN = Regex("^[a-z0-9._%+-]+@(gmail|yahoo)\\.com$")
private const val EMAIL_ERROR = "Email must be a valid @gmail.com or @yahoo.com address"

private fun isValidEmail(email: String): Boolean =
    email.trim().lowercase().matches(ALLOWED_EMAIL_PATTERN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onBackToSignIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var linkSent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Slate800)
                    }
                    Text(
                        text = "Reset Password",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Emerald950,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!linkSent) {
                Text(
                    text = "Forgot your password?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
                Text(
                    text = "Enter the email address linked to your resident account and we'll send you a link to reset your password.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Slate600
                )

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFB91C1C),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email Address *", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = Emerald700) },
                    placeholder = { Text("e.g. resident@gmail.com", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (email.isBlank()) {
                            errorMessage = "Please enter your email address"
                            return@Button
                        }
                        if (!isValidEmail(email)) {
                            errorMessage = EMAIL_ERROR
                            return@Button
                        }
                        errorMessage = null
                        isSubmitting = true
                        FirebaseAuth.getInstance()
                            .sendPasswordResetEmail(email.trim().lowercase())
                            .addOnCompleteListener { task ->
                                isSubmitting = false
                                if (task.exception is FirebaseNetworkException) {
                                    errorMessage = "No internet connection. Please check your network and try again."
                                } else {
                                    // Same message whether or not the account exists, so nobody can probe for registered emails
                                    linkSent = true
                                }
                            }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Send Reset Link",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                TextButton(
                    onClick = onBackToSignIn,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Back to Sign In", color = Emerald800, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Emerald50),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MarkEmailRead, null, tint = Emerald800, modifier = Modifier.size(32.dp))
                }

                Text(
                    text = "Check your inbox",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
                Text(
                    text = "If an account exists for ${email.trim().lowercase()}, we've sent a link with instructions to reset your password.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Slate600
                )

                Button(
                    onClick = onBackToSignIn,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Back to Sign In", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}
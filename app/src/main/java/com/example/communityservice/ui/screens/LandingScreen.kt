package com.example.communityservice.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.communityservice.data.MUNICIPAL_DISTRICTS
import com.example.communityservice.data.ResidentUser
import com.example.communityservice.ui.viewmodel.IssueViewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.example.communityservice.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.VisualTransformation
// Brand Colors
private val Emerald900 = Color(0xFF064E3B)
private val Emerald100 = Color(0xFFD1FAE5)
private val Amber600 = Color(0xFFD97706)
private val Amber500 = Color(0xFFF59E0B)
private val Slate600 = Color(0xFF475569)

private val ALLOWED_EMAIL_PATTERN = Regex("^[a-z0-9._%+-]+@(gmail|yahoo)\\.com$")
private const val EMAIL_ERROR = "Email must be a valid @gmail.com or @yahoo.com address"

private fun isValidEmail(email: String): Boolean =
    email.trim().lowercase().matches(ALLOWED_EMAIL_PATTERN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    viewModel: IssueViewModel,
    onLoginSuccess: () -> Unit
) {
    var showAuthModal by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    // Controls which full screen is shown: the marketing Landing page, or the
    // standalone Forgot Password screen. Forgot Password is intentionally NOT
    // a dialog/popup - it is its own screen with its own back button.
    var showForgotPassword by remember { mutableStateOf(false) }

    // Form fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()


    val scrollState = rememberScrollState()

    //Logo splash
    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2500)
        showSplash = false
    }

    if (showSplash) {
        // Fade the logo in smoothly
        var logoVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { logoVisible = true }
        val logoAlpha by animateFloatAsState(
            targetValue = if (logoVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 800),
            label = "logoAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Community Service logo",
                modifier = Modifier
                    .size(300.dp)
                    .alpha(logoAlpha)
            )
        }
        return
    }

    if (showForgotPassword) {
        ForgotPasswordScreen(
            onBack = { showForgotPassword = false },
            onBackToSignIn = {
                showForgotPassword = false
                isSignUp = false
                errorMessage = null
                showAuthModal = true
            }
        )
        return
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(37.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = null,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Community Service",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate900,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "-Issue Reporter",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Amber600,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                isSignUp = false
                                errorMessage = null
                                showAuthModal = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Text("Sign In", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        }

                        Button(
                            onClick = {
                                isSignUp = true
                                errorMessage = null
                                showAuthModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Register", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        },
        containerColor = Slate50
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Emerald900, Emerald950)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "CITIZEN CIVIC ACTION • 2026",
                                color = Emerald100,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                letterSpacing = 0.8.sp
                            )
                        }

                        Text(
                            text = "Report Municipal & Neighborhood Faults in Real-Time",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 26.sp
                        )

                        Text(
                            text = "Direct reporting bridge connecting South African communities with municipal services. Snap, submit, track resolution.",
                            color = Emerald100.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = {
                                isSignUp = true
                                errorMessage = null
                                showAuthModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Amber500),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Create Account",
                                color = Slate900,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Category Shortcuts
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Issue Categories",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WaterDrop,
                        title = "Water",
                        bgColor = Slate100,
                        iconColor = Color.Black
                    ) {
                        isSignUp = false
                        showAuthModal = true
                    }
                    CategoryChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        title = "Potholes",
                        bgColor = Slate100,
                        iconColor = Color.Black
                    ) {
                        isSignUp = false
                        showAuthModal = true
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Lightbulb,
                        title = "Streetlights",
                        bgColor = Slate100,
                        iconColor = Color.Black
                    ) {
                        isSignUp = false
                        showAuthModal = true
                    }
                    CategoryChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Delete,
                        title = "Waste Dumping",
                        bgColor = Slate100,
                        iconColor = Color.Black
                    ) {
                        isSignUp = false
                        showAuthModal = true
                    }
                }
            }

            // How It Works / Key Features
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "How Community Service Works",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    FeatureRow(
                        number = "1",
                        title = "Snap Photo & Auto-Locate",
                        desc = "Take a clear picture of the fault with GPS location and select your district."
                    )
                    FeatureRow(
                        number = "2",
                        title = "Real-Time Tracking",
                        desc = "Follow ticket progression from Reported to Assigned and Resolved with timeline logs."
                    )
                    FeatureRow(
                        number = "3",
                        title = "Community Broadcasts",
                        desc = "Receive municipal notices, planned maintenance alerts, and service updates."
                    )
                }
            }

            // Our Vision
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Emerald100,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Our Vision",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald800,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "Turning Everyday Frustrations into Shared Pride",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        lineHeight = 21.sp
                    )

                    Text(
                        text = "Every broken streetlight, leaking pipe, and neglected pothole tells a story of a community waiting for care. When reporting feels like a maze of slow-moving hotlines and lost tickets, neighbors become silent observers. We believe that caring for our streets should never be a bureaucratic puzzle.",
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = Slate600
                    )

                    Text(
                        text = "By turning simple observations into immediate reports, we connect active citizens directly with local city dispatch crews. When we bring issues into the open, we transform collective concerns into targeted municipal resolutions—putting civic improvement directly into your hands.",
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = Slate600
                    )
                }
            }

            // About Us Strip
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "WHO WE ARE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Emerald700,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "A Direct Bridge to a Better Neighborhood",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "We empower everyday citizens to act as the eyes and ears of their municipal wards. By fostering open tracking, active visual reporting, and cooperative feedback, we help neighbors, wards, and city administrators build clean, secure, and resilient communities together.",
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = Slate600
                    )
                }
            }

            // Emergency Helplines
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "EMERGENCY COMMUNITY HELPLINES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate600,
                        letterSpacing = 0.5.sp
                    )
                    HelplineItem(service = "Water", number = "0800 200 300")
                    HelplineItem(service = "Electricity", number = "0800 400 500")
                    HelplineItem(service = "Roads & Traffic", number = "0800 600 700")
                }
            }

            // Footer
            Text(
                text = "Copyright © 2026 Community Service - Issue Reporter",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Slate500,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }

    // Auth Dialog (Create Account / Sign In)
    if (showAuthModal) {
        Dialog(onDismissRequest = { showAuthModal = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Modal Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Emerald800, Emerald950)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        IconButton(
                            onClick = { showAuthModal = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isSignUp) "Create Resident Account" else "Resident Sign In",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Community Service - Issue Reporter",
                                color = Emerald100,
                                fontSize = 11.sp
                            )

                            // Tabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSignUp) Color.White else Color.Transparent)
                                        .clickable { isSignUp = true; errorMessage = null }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Create Account",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSignUp) Emerald950 else Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (!isSignUp) Color.White else Color.Transparent)
                                        .clickable { isSignUp = false; errorMessage = null }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Sign In",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isSignUp) Emerald950 else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Form Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                    modifier = Modifier.padding(8.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (isSignUp) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it; errorMessage = null },
                                label = { Text("Full Name *", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = Emerald700) },
                                placeholder = { Text("e.g. Thabo Mokoena", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it; errorMessage = null },
                                label = { Text("Mobile Phone Number *", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Phone, null, tint = Emerald700) },
                                placeholder = { Text("10 Digits (e.g. 0821234567)", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it; errorMessage = null },
                                label = { Text("Residential Street Address *", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Home, null, tint = Emerald700) },
                                placeholder = { Text("e.g. 74 Pretorius Street", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // District Selector Dropdown with all South African Municipal Districts
                            var districtExpanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = districtExpanded,
                                onExpandedChange = { districtExpanded = !districtExpanded }
                            ) {
                                OutlinedTextField(
                                    value = if (selectedDistrict.isEmpty()) "Select Municipal District *" else selectedDistrict,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Municipal Ward / District *", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Map, null, tint = Emerald700) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedTextColor = if (selectedDistrict.isEmpty()) Slate500 else Slate900
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = districtExpanded,
                                    onDismissRequest = { districtExpanded = false },
                                    modifier = Modifier.heightIn(max = 280.dp)
                                ) {
                                    MUNICIPAL_DISTRICTS.forEach { district ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = district,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (district == selectedDistrict) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (district == selectedDistrict) Emerald800 else Slate900
                                                )
                                            },
                                            onClick = {
                                                selectedDistrict = district
                                                districtExpanded = false
                                                errorMessage = null
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            label = { Text("Email Address *", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Emerald700) },
                            placeholder = { Text("e.g. resident@gmail.com or @yahoo.com", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("Password *", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Emerald700) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Emerald700
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (!isSignUp) {
                            Text(
                                text = "Forgot Password?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald800,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clickable {
                                        showAuthModal = false
                                        errorMessage = null
                                        showForgotPassword = true
                                    }
                            )
                        }


                        Button(
                            onClick = {
                                if (isSignUp) {
                                    // Capitalise every word: "john smith" -> "John Smith"
                                    val formattedName = name.trim()
                                        .split(Regex("\\s+"))
                                        .joinToString(" ") { word ->
                                            word.lowercase().replaceFirstChar { it.uppercase() }
                                        }

                                    val nameParts = formattedName.split(" ")
                                    if (nameParts.size < 2 || nameParts[0].isEmpty() || nameParts[1].isEmpty()) {
                                        errorMessage = "Please enter your Full Name (at least two names)"
                                        return@Button
                                    }

                                    // Email must end in @gmail.com or @yahoo.com
                                    if (!isValidEmail(email)) {
                                        errorMessage = EMAIL_ERROR
                                        return@Button
                                    }

                                    val digitsOnly = phone.filter { it.isDigit() }

                                    if (digitsOnly.length != 10 || !digitsOnly.startsWith("0")) {
                                        errorMessage = "Please enter a valid 10-digit number starting with 0"
                                        return@Button
                                    }

                                    if (address.isBlank()) {
                                        errorMessage = "Please enter your residential street address"
                                        return@Button
                                    }

                                    if (selectedDistrict.isEmpty()) {
                                        errorMessage = "Please select your Municipal District"
                                        return@Button
                                    }

                                    if (password.length < 8) {
                                        errorMessage = "Password must be a minimum of 8 characters"
                                        return@Button
                                    }

                                    val hasLetter = password.any { it.isLetter() }
                                    val hasDigit = password.any { it.isDigit() }
                                    val hasSpecial = password.any { !it.isLetterOrDigit() }
                                    if (!hasLetter || !hasDigit || !hasSpecial) {
                                        errorMessage = "Password must include a letter, a digit and a special character"
                                        return@Button
                                    }

                                    isLoading = true
                                    scope.launch {
                                        val problem = viewModel.register(
                                            name = formattedName,
                                            email = email.trim().lowercase(),
                                            phone = phone.trim(),
                                            address = address.trim(),
                                            ward = selectedDistrict,
                                            password = password
                                        )
                                        isLoading = false
                                        if (problem != null) {
                                            errorMessage = problem
                                        } else {
                                            showAuthModal = false
                                            onLoginSuccess()
                                        }
                                    }
                                } else {
                                    if (email.isBlank()) {
                                        errorMessage = "Please enter your email address"
                                        return@Button
                                    }

                                    // Email must end in @gmail.com or @yahoo.com
                                    if (!isValidEmail(email)) {
                                        errorMessage = EMAIL_ERROR
                                        return@Button
                                    }

                                    if (password.isEmpty()) {
                                        errorMessage = "Please enter your password"
                                        return@Button
                                    }

                                    isLoading = true
                                    scope.launch {
                                        val problem = viewModel.signIn(email.trim().lowercase(), password)
                                        isLoading = false
                                        if (problem != null) {
                                            errorMessage = problem
                                        } else {
                                            showAuthModal = false
                                            onLoginSuccess()
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (isSignUp) "Create Account" else "Sign In",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    bgColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )
        }
    }
}

@Composable
private fun FeatureRow(
    number: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Emerald800),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Slate600,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun HelplineItem(service: String, number: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = Emerald800,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = service,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Slate800
            )
        }
        Text(
            text = number,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Emerald800
        )
    }
}
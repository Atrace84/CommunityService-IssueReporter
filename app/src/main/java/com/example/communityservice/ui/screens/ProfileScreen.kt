package com.example.communityservice.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.communityservice.ui.viewmodel.IssueViewModel
import com.example.communityservice.ui.theme.Slate50
import com.example.communityservice.ui.theme.Slate500
import com.example.communityservice.ui.theme.Slate900
import com.example.communityservice.ui.theme.Slate100
import com.example.communityservice.ui.theme.Slate800
import com.example.communityservice.ui.theme.Emerald800
import com.example.communityservice.ui.theme.Emerald950
import com.example.communityservice.ui.theme.Emerald700

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: IssueViewModel,
    onLogout: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val scrollState = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    onLogout()
                }) {
                    Text("Log Out", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Slate700)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(end = 48.dp)) {
                        Text("Resident Account", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Emerald950)
                        Text("Profile Settings", fontSize = 11.sp, color = Emerald700)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Menu", tint = Slate800)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Pinned to the bottom of the screen so it is always visible,
            // no matter how much content is above it.
            Surface(color = Color.White, shadowElevation = 4.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFB91C1C))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate100)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Avatar Simulation
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Emerald800.copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, Emerald800, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser?.name?.split(" ")?.mapNotNull { it.firstOrNull() }?.joinToString("").orEmpty().ifEmpty { "R" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Emerald800
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentUser?.name.orEmpty(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = currentUser?.id.orEmpty(),
                            fontSize = 12.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentUser?.ward.orEmpty(),
                            color = Emerald950,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Contact Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate100)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Contact Information", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)

                    ProfileDetailItem(icon = Icons.Default.Email, label = "Email Address", value = currentUser?.email.orEmpty())
                    ProfileDetailItem(icon = Icons.Default.Phone, label = "Phone Number", value = currentUser?.phone.orEmpty())
                    ProfileDetailItem(icon = Icons.Default.Home, label = "Residential Address", value = currentUser?.address.orEmpty())
                }
            }

            // Ward Information Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate100)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ward Cllr Office", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)

                    ProfileDetailItem(icon = Icons.Default.Person, label = "Ward Councillor", value = "Cllr. Arthur Vance")
                    ProfileDetailItem(icon = Icons.Default.Phone, label = "Office Hot-Line", value = "+27 (0)11 555 0100")
                    ProfileDetailItem(icon = Icons.Default.Business, label = "Ward Hub Office", value = "Central Municipal Depot, Building C, Room 12")
                }
            }

            // Application version info
            Text(
                text = "Copyright © 2026 Community Service - Issue Reporter\nKotlin SDK Target 36 • Material 3",
                fontSize = 11.sp,
                color = Slate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun ProfileDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Slate50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Emerald800, modifier = Modifier.size(16.dp))
        }

        Column {
            Text(label, fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 12.sp, color = Slate900, fontWeight = FontWeight.SemiBold)
        }
    }
}
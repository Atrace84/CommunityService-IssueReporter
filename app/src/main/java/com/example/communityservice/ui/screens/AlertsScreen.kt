package com.example.communityservice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.communityservice.data.NotificationItem
import com.example.communityservice.ui.viewmodel.IssueViewModel
import com.example.communityservice.Slate500
import com.example.communityservice.Slate900
import com.example.communityservice.Emerald800
import com.example.communityservice.Amber500
import com.example.communityservice.Slate700
import com.example.communityservice.ui.theme.Slate50
import com.example.communityservice.ui.theme.Slate800
import com.example.communityservice.ui.theme.Slate100
import com.example.communityservice.ui.theme.Emerald700
import com.example.communityservice.Emerald950


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: IssueViewModel,
    onReportClick: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val alerts by viewModel.alerts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(end = 48.dp)) {
                        Text("Announcements & Alerts", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Emerald950)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Menu", tint = Slate800)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats indicator
            val unreadCount = alerts.count { !it.isRead }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Official Notifications",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(Amber500, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("$unreadCount New", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsNone, null, tint = Slate500, modifier = Modifier.size(48.dp))
                        Text("No active alerts at this time", color = Slate500, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alerts) { item ->
                        AlertItemCard(
                            item = item,
                            onClick = {
                                viewModel.markAlertAsRead(item.id)
                                if (item.reportId != null) {
                                    onReportClick(item.reportId)
                                } else {
                                    viewModel.showToast("Broadcasting announcement detail: ${item.title}")
                                }
                            }
                        )
                    }
                }
            }

            // Copyright Footer always present
            Text(
                text = "Copyright © 2026 Community Service - Issue Reporter",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Slate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AlertItemCard(
    item: NotificationItem,
    onClick: () -> Unit
) {
    val icon = when (item.type) {
        "status_update" -> Icons.Default.CheckCircle
        "assignment" -> Icons.Default.Engineering
        else -> Icons.Default.Campaign
    }

    val iconColor = when (item.type) {
        "status_update" -> Color(0xFF10B981)
        "assignment" -> Color(0xFF3B82F6)
        else -> Color(0xFFF59E0B)
    }

    val containerBg = if (item.isRead) Color.White else Color(0xFFECFDF5)
    val borderStrokeColor = if (item.isRead) Slate100 else Color(0xFFA7F3D0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderStrokeColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1
                    )
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Amber500, CircleShape)
                        )
                    }
                }
                Text(
                    text = item.message,
                    fontSize = 12.sp,
                    color = Slate700,
                    lineHeight = 16.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.timestamp,
                        fontSize = 10.sp,
                        color = Slate500
                    )
                    if (item.reportId != null) {
                        Text(
                            text = "View Report ${item.reportId} →",
                            fontSize = 11.sp,
                            color = Emerald800,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

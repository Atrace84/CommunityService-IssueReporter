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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.communityservice.data.IssueCategory
import com.example.communityservice.data.IssuePriority
import com.example.communityservice.data.IssueReport
import com.example.communityservice.data.IssueStatus
import com.example.communityservice.ui.viewmodel.IssueViewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.example.communityservice.R
private val Emerald900 = Color(0xFF064E3B)
private val Emerald600 = Color(0xFF059669)
private val Emerald100 = Color(0xFFD1FAE5)
private val Emerald50 = Color(0xFFECFDF5)
private val Amber600 = Color(0xFFD97706)
private val Amber500 = Color(0xFFF59E0B)
private val Amber400 = Color(0xFFFBBF24)
private val Amber50 = Color(0xFFFFFBEB)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: IssueViewModel,
    onReportIssueClick: (IssueCategory?) -> Unit,
    onViewReportsClick: () -> Unit,
    onSelectReportClick: (String) -> Unit = {},
    onOpenDrawer: () -> Unit,
    onOpenAlerts: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val alerts by viewModel.alerts.collectAsState()

    val submittedCount = reports.count { it.status == IssueStatus.REPORTED }
    val inProgressCount = reports.count { it.status == IssueStatus.IN_PROGRESS || it.status == IssueStatus.ASSIGNED }
    val resolvedCount = reports.count { it.status == IssueStatus.RESOLVED }

    val unreadAlerts = alerts.filter { !it.isRead }
    val recentReports = reports.take(3)
    val scrollState = rememberScrollState()

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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onOpenDrawer) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp)
                        )                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Community Service",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Emerald950
                        )
                        Text(
                            text = "-Issue Reporter",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Amber600
                        )
                    }

                    Box {
                        IconButton(onClick = onOpenAlerts) {
                            Icon(Icons.Default.Notifications, "Alerts", tint = Slate800)
                        }
                        if (unreadAlerts.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(16.dp)
                                    .background(Color(0xFFDC2626), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unreadAlerts.size.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcomee Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Emerald800, Emerald700, Emerald900)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                )
                            }

                            Column {
                                Surface(
                                    color = Emerald600.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Amber400, CircleShape)
                                        )
                                        Text(
                                            text = "Resident Portal • ${currentUser?.ward ?: "Ward 4"}",
                                            color = Emerald100,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Welcome, ${currentUser?.name?.split(" ")?.firstOrNull() ?: "Resident"}!",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = "Help keep our community better. Report municipal issues such as water leaks, broken streetlights, or potholes in your area.",
                            color = Emerald100.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onReportIssueClick(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = Amber400),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Slate900,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Report Issue",
                                    color = Slate900,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = onViewReportsClick,
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "My Reports",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Resident Dashboard Metrics
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DASHBOARD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Total reports: ${reports.size}",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        iconBg = Color(0xFFFFFFFF),
                        iconColor = Amber600,
                        count = submittedCount,
                        label = "Submitted",
                        subLabel = "Awaiting dispatch",
                        onClick = onViewReportsClick
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Schedule,
                        iconBg = Color(0xFFFFFFFF),
                        iconColor = Color(0xFF2563EB),
                        count = inProgressCount,
                        label = "In Progress",
                        subLabel = "Assigned / Active",
                        onClick = onViewReportsClick
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        iconBg = Color(0xFFFFFFFF),
                        iconColor = Emerald600,
                        count = resolvedCount,
                        label = "Resolved",
                        subLabel = "Repaired & closed",
                        onClick = onViewReportsClick
                    )
                }
            }

            // Quick Report Categories
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUICK REPORT CATEGORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "All Categories →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald800,
                        modifier = Modifier.clickable { onReportIssueClick(null) }                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickCategoryTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Delete,
                        title = "Bins",
                        desc = "Waste & collection",
                        onClick = { onReportIssueClick(IssueCategory.SANITATION) }
                    )
                    QuickCategoryTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Lightbulb,
                        title = "Lights",
                        desc = "Outage & damage",
                        onClick = { onReportIssueClick(IssueCategory.STREETLIGHT) }
                    )
                    QuickCategoryTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WaterDrop,
                        title = "Water",
                        desc = "Burst pipes & leaks",
                        onClick = { onReportIssueClick(IssueCategory.WATER) }
                    )
                    QuickCategoryTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        title = "Potholes",
                        desc = "Road hazards",
                        onClick = { onReportIssueClick(IssueCategory.POTHOLE) }
                    )
                }
            }

            // Recent Reports List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Reports",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onViewReportsClick() }
                    ) {
                        Text(
                            text = "View All",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald800
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Emerald800,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (recentReports.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "No reports submitted yet. Tap 'Report Issue' to file your first ticket!",
                            color = Slate500,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    recentReports.forEach { report ->
                        RecentReportItemCard(
                            report = report,
                            onClick = { onSelectReportClick(report.id) }
                        )
                    }
                }
            }

            // Copyright Footer
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
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    count: Int,
    label: String,
    subLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Slate900
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate700
            )
            Text(
                text = subLabel,
                fontSize = 9.sp,
                color = Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickCategoryTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Slate900,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                textAlign = TextAlign.Center
            )
            Text(
                text = desc,
                fontSize = 8.sp,
                color = Slate500,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentReportItemCard(
    report: IssueReport,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Category Icon Box
            val (icon, bg, fg) = when (report.category) {
                IssueCategory.WATER -> Triple(Icons.Default.WaterDrop, Color(0xFFF5F5F5), Color.Black)
                IssueCategory.POTHOLE -> Triple(Icons.Default.Warning, Color(0xFFF5F5F5), Color.Black)
                IssueCategory.STREETLIGHT -> Triple(Icons.Default.Lightbulb, Color(0xFFF5F5F5), Color.Black)
                IssueCategory.SANITATION -> Triple(Icons.Default.Delete, Color(0xFFF5F5F5), Color.Black)
                IssueCategory.OTHER -> Triple(Icons.Default.MoreHoriz, Color(0xFFF5F5F5), Color.Black)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Report Details Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = report.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "#${report.id}",
                        fontSize = 10.sp,
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${report.areaWard} • ${report.reportedAt}",
                        fontSize = 10.sp,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Status Badge Pill
            val (statusText, statusBg, statusColor) = when (report.status) {
                IssueStatus.REPORTED -> Triple("Reported", Color(0xFFFEF3C7), Amber600)
                IssueStatus.ASSIGNED -> Triple("Assigned", Color(0xFFEFF6FF), Color(0xFF2563EB))
                IssueStatus.IN_PROGRESS -> Triple("In Progress", Color(0xFFEFF6FF), Color(0xFF2563EB))
                IssueStatus.RESOLVED -> Triple("Resolved", Color(0xFFECFDF5), Emerald600)
            }

            Surface(
                color = statusBg,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

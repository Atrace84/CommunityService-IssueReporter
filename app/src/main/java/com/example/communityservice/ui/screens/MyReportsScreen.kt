package com.example.communityservice.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.communityservice.data.IssueReport
import com.example.communityservice.data.IssueStatus
import com.example.communityservice.data.IssuePriority
import com.example.communityservice.ui.viewmodel.IssueViewModel

val Slate500 = Color(0xFF64748B)
val Slate100 = Color(0xFFF1F5F9)
val Slate700 = Color(0xFF334155)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)

val Slate50 = Color(0xFFF8FAFC)
val Emerald700 = Color(0xFF047857)
val Emerald800 = Color(0xFF065F46)
val Emerald950 = Color(0xFF022C22)






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    viewModel: IssueViewModel,
    onSelectReport: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val reports by viewModel.reports.collectAsState()
    var searchKeyword by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<IssueStatus?>(null) }

    val filteredReports = reports.filter { report ->
        val matchesSearch = report.title.contains(searchKeyword, ignoreCase = true) ||
                report.id.contains(searchKeyword, ignoreCase = true) ||
                report.description.contains(searchKeyword, ignoreCase = true)
        val matchesStatus = selectedStatusFilter == null || report.status == selectedStatusFilter
        matchesSearch && matchesStatus
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(end = 48.dp)) {
                        Text("My Submitted Issues", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Emerald950)
                        Text("Ward tracking", fontSize = 11.sp, color = Emerald700)
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
            // Search Input
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                placeholder = { Text("Search by ID, keyword, description...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate500, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            // Status filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusFilterChip(
                    label = "All",
                    isSelected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null }
                )
                StatusFilterChip(
                    label = "Reported",
                    isSelected = selectedStatusFilter == IssueStatus.REPORTED,
                    onClick = { selectedStatusFilter = IssueStatus.REPORTED }
                )
                StatusFilterChip(
                    label = "Active",
                    isSelected = selectedStatusFilter == IssueStatus.IN_PROGRESS || selectedStatusFilter == IssueStatus.ASSIGNED,
                    onClick = { selectedStatusFilter = IssueStatus.IN_PROGRESS }
                )
                StatusFilterChip(
                    label = "Resolved",
                    isSelected = selectedStatusFilter == IssueStatus.RESOLVED,
                    onClick = { selectedStatusFilter = IssueStatus.RESOLVED }
                )
            }

            // Reports count
            Text(
                text = "Found ${filteredReports.size} items",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500
            )

            if (filteredReports.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, null, tint = Slate500, modifier = Modifier.size(48.dp))
                        Text("No reports found.", color = Slate500, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredReports) { report ->
                        ReportCardItem(
                            report = report,
                            onClick = { onSelectReport(report.id) }
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
fun StatusFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) Emerald800 else Color.White,
                RoundedCornerShape(8.dp)
            )
            .border(1.dp, if (isSelected) Emerald800 else Slate100, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Slate700
        )
    }
}

@Composable
fun ReportCardItem(
    report: IssueReport,
    onClick: () -> Unit
) {
    val badgeBg = when (report.status) {
        IssueStatus.RESOLVED -> Color(0xFFD1FAE5)
        IssueStatus.IN_PROGRESS -> Color(0xFFFEF3C7)
        IssueStatus.ASSIGNED -> Color(0xFFDBEAFE)
        IssueStatus.REPORTED -> Color(0xFFF1F5F9)
    }

    val badgeText = when (report.status) {
        IssueStatus.RESOLVED -> Color(0xFF065F46)
        IssueStatus.IN_PROGRESS -> Color(0xFF92400E)
        IssueStatus.ASSIGNED -> Color(0xFF1E40AF)
        IssueStatus.REPORTED -> Color(0xFF334155)
    }

    val priorityColor = when (report.priority) {
        IssuePriority.CRITICAL -> Color(0xFFDC2626)
        IssuePriority.MAJOR -> Color(0xFFF43F5E)
        IssuePriority.INCONVENIENCE -> Color(0xFFD97706)
        IssuePriority.IMPROVEMENT -> Color(0xFF059669)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate100, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberIssuePainter(report.photoBefore),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .background(Slate100, RoundedCornerShape(8.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.id,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = report.status.display,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }
                }

                Text(
                    text = report.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1
                )

                Text(
                    text = report.locationAddress,
                    fontSize = 11.sp,
                    color = Slate500,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Priority: ${report.priority.title}",
                        fontSize = 10.sp,
                        color = priorityColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = report.reportedAt.substringBefore(","),
                        fontSize = 10.sp,
                        color = Slate500
                    )
                }
            }
        }
    }
}

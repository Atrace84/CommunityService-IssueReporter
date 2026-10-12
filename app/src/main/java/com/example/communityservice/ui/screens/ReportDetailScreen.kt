package com.example.communityservice.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.communityservice.ui.viewmodel.IssueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    reportId: String,
    viewModel: IssueViewModel,
    onBack: () -> Unit
) {
    val reports by viewModel.reports.collectAsState()
    val report = reports.find { it.id == reportId }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Tracking", "Details", "Photos")


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(end = 48.dp)) {
                        Text(report?.id ?: "Report Details", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Emerald950)
                        Text(report?.category?.title ?: "Ward Ticket", fontSize = 11.sp, color = Emerald700)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Slate800)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showToast("Report link shared to clipboard!") }) {
                        Icon(Icons.Default.Share, "Share", tint = Emerald800)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        if (report == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Report not found.", color = Slate500, fontSize = 14.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate50)
                    .padding(paddingValues)
            ) {
                // Tab Header
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Emerald800,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Emerald800
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // Tab Contents
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    when (selectedTab) {
                        0 -> TrackingTabContent(report)
                        1 -> DetailsTabContent(report)
                        2 -> PhotosTabContent(report)
                    }
                }

                // Copyright Footer always present
                Text(
                    text = "Copyright © 2026 Community Service - Issue Reporter",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TrackingTabContent(report: IssueReport) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ticket Progression Timeline", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)

                report.timeline.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        if (step.isCompleted) Color(0xFF10B981) else Color(0xFFE2E8F0),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (step.isCompleted) Icons.Default.Check else Icons.Default.MoreHoriz,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (idx < report.timeline.size - 1) {
                                Spacer(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(30.dp)
                                        .background(if (step.isCompleted) Color(0xFF10B981) else Color(0xFFE2E8F0))
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = step.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (step.isCurrent) Emerald800 else Slate900
                                )
                                Text(
                                    text = step.timestamp,
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }
                            Text(
                                text = step.description,
                                fontSize = 11.sp,
                                color = Slate700
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsTabContent(report: IssueReport) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Issue Specifications", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)

                DetailItem(label = "Category", value = report.category.title)
                if (report.customCategory != null) {
                    DetailItem(label = "Custom Category", value = report.customCategory)
                }
                DetailItem(label = "Description", value = report.description)
                DetailItem(label = "Priority Level", value = report.priority.title)
                DetailItem(label = "Date Reported", value = report.reportedAt)
                DetailItem(label = "Reported By", value = report.reportedBy)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Municipal Operations", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)

                DetailItem(label = "Assigned Ward Team", value = report.assignedTo ?: "Unassigned")
                DetailItem(label = "Physical Address", value = report.locationAddress)
                DetailItem(label = "GPS Coordinates", value = report.gpsCoordinates)
                DetailItem(label = "Area Ward", value = report.areaWard)
                if (report.completionNotes != null) {
                    DetailItem(label = "Resolution Notes", value = report.completionNotes)
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, color = Slate900, modifier = Modifier.padding(top = 2.dp))
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Slate100, thickness = 1.dp)
    }
}

@Composable
fun PhotosTabContent(report: IssueReport) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Before Work (Reported)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Text("File: ${report.photoBeforeName}", fontSize = 11.sp, color = Slate500)
                Image(
                    painter = rememberIssuePainter(report.photoBefore),
                    contentDescription = "Before work",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Slate100, RoundedCornerShape(8.dp))
                )
            }
        }

        if (report.photoAfter != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("After Work (Resolved)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Text("File: ${report.photoAfterName}", fontSize = 11.sp, color = Slate500)
                    Image(
                        painter = rememberIssuePainter(report.photoAfter),
                        contentDescription = "After work",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Slate100, RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

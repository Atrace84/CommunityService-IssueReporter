package com.example.communityservice.data

enum class IssueCategory(val value: String, val title: String) {
    WATER("water", "Water"),
    POTHOLE("pothole", "Pothole & Road"),
    STREETLIGHT("streetlight", "Streetlight Failure"),
    SANITATION("sanitation", "Sanitation & Bins"),
    OTHER("other", "Other Issues")
}

enum class IssuePriority(val value: String, val title: String) {
    CRITICAL("critical", "Critical"),
    MAJOR("major", "Major Problem"),
    INCONVENIENCE("inconvenience", "Inconvenience"),
    IMPROVEMENT("improvement", "Improvement Request")
}

enum class IssueStatus(val value: String, val display: String) {
    REPORTED("reported", "Reported"),
    ASSIGNED("assigned", "Assigned"),
    IN_PROGRESS("in_progress", "In Progress"),
    RESOLVED("resolved", "Resolved")
}

data class TimelineStep(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean = false
)

data class IssueReport(
    val id: String,
    val title: String,
    val category: IssueCategory,
    val customCategory: String? = null,
    val description: String,
    val priority: IssuePriority,
    val locationAddress: String,
    val gpsCoordinates: String,
    val areaWard: String,
    val photoBefore: String,
    val photoBeforeName: String,
    val photoAfter: String? = null,
    val photoAfterName: String? = null,
    val status: IssueStatus,
    val reportedBy: String,
    val reporterId: String = "",
    val reportedAt: String,
    val assignedTo: String? = null,
    val completionNotes: String? = null,
    val timeline: List<TimelineStep> = emptyList()
)

data class ResidentUser(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val ward: String,
    val avatarUrl: String
)

data class NotificationItem(
    val id: String,
    val reportId: String? = null,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean,
    val type: String // "status_update", "community_alert", "assignment"
)

object SampleData {
    val SAMPLE_PHOTOS_LIGHT: String = ""
    val CURRENT_RESIDENT = ResidentUser(
        id = "RES-8821",
        name = "Sarah Jenkins",
        email = "sarah.jenkins@community.org",
        phone = "+27 (0)82 555 0192",
        address = "42 Main Road, Central Ward",
        ward = "Ward 4 (Central Suburbs)",
        avatarUrl = ""
    )

    val SAMPLE_PHOTOS_WATER_BEFORE = "https://images.unsplash.com/photo-1541888946425-d0fbb1861563?w=800&auto=format&fit=crop&q=80"
    val SAMPLE_PHOTOS_WATER_AFTER = "https://images.unsplash.com/photo-1584467735815-f778f274e296?w=800&auto=format&fit=crop&q=80"
    val SAMPLE_PHOTOS_STREETLIGHT_BEFORE = "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=800&auto=format&fit=crop&q=80"
    val SAMPLE_PHOTOS_STREETLIGHT_AFTER = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop&q=80"
    val SAMPLE_PHOTOS_BIN = "https://images.unsplash.com/photo-1605600659873-d808a13e4d2a?w=800&auto=format&fit=crop&q=80"
    val SAMPLE_PHOTOS_POTHOLE = "https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=800&auto=format&fit=crop&q=80"

    val INITIAL_REPORTS = listOf(
        IssueReport(
            id = "ISS-1042",
            title = "Water Leak on Main Rd",
            category = IssueCategory.WATER,
            description = "Major freshwater leak bursting from under the pavement outside 42 Main Road.",
            priority = IssuePriority.MAJOR,
            locationAddress = "42 Main Road, Central Suburbs",
            gpsCoordinates = "-33.9249° S, 18.4241° E",
            areaWard = "Ward 4 (Central Suburbs)",
            photoBefore = SAMPLE_PHOTOS_WATER_BEFORE,
            photoBeforeName = "water_leak_before.jpg",
            status = IssueStatus.ASSIGNED,
            reportedBy = "Sarah Jenkins",
            reportedAt = "21 Sep 2026, 09:14 AM",
            assignedTo = "Water Technical Crew B",
            timeline = listOf(
                TimelineStep("step-1", "Reported", "You reported this issue.", "21 Sep 2026, 09:14 AM", true),
                TimelineStep("step-2", "Assigned", "Assigned to Water Technical Crew B.", "21 Sep 2026, 11:30 AM", true, isCurrent = true),
                TimelineStep("step-3", "In Progress", "Crew is scheduled to arrive on site.", "Pending", false),
                TimelineStep("step-4", "Resolved", "Photo proof and sign-off.", "Pending", false)
            )
        ),
        IssueReport(
            id = "ISS-1039",
            title = "Pothole in Main St",
            category = IssueCategory.POTHOLE,
            description = "Extremely deep pothole damaging vehicles on the left lane near the traffic light.",
            priority = IssuePriority.CRITICAL,
            locationAddress = "109 Main Road, Central Suburbs",
            gpsCoordinates = "-33.9255° S, 18.4250° E",
            areaWard = "Ward 4 (Central Suburbs)",
            photoBefore = SAMPLE_PHOTOS_POTHOLE,
            photoBeforeName = "pothole_before.jpg",
            status = IssueStatus.REPORTED,
            reportedBy = "Sarah Jenkins",
            reportedAt = "20 Sep 2026, 04:30 PM",
            timeline = listOf(
                TimelineStep("step-1", "Reported", "You reported this issue.", "20 Sep 2026, 04:30 PM", true, isCurrent = true),
                TimelineStep("step-2", "Assigned", "Awaiting dispatch assignment.", "Pending", false),
                TimelineStep("step-3", "In Progress", "Scheduled road maintenance.", "Pending", false),
                TimelineStep("step-4", "Resolved", "Completed tar filling.", "Pending", false)
            )
        )
    )

    val INITIAL_ALERTS = listOf(
        NotificationItem(
            id = "notif-1",
            reportId = "ISS-1042",
            title = "Ticket Assigned: ISS-1042",
            message = "Your reported water leak has been assigned to municipal Technical Crew B. They will arrive shortly.",
            timestamp = "Just now",
            isRead = false,
            type = "status_update"
        ),
        NotificationItem(
            id = "notif-2",
            reportId = null,
            title = "Scheduled Water Outage",
            message = "Scheduled maintenance in Ward 4 on Wednesday from 08:00 to 14:00. Please store water.",
            timestamp = "2 hours ago",
            isRead = false,
            type = "community_alert"
        )
    )
}

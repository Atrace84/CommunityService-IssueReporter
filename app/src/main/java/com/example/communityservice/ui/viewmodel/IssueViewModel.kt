package com.example.communityservice.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communityservice.data.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ReportFormState(
    val category: IssueCategory? = null, // No category is chosen until the user picks one
    val customCategory: String = "",
    val description: String = "",
    val priority: IssuePriority? = null, // Empty start condition!
    val isPriorityManuallySet: Boolean = false,
    val address: String = "",
    val ward: String = "",
    val gpsCoordinates: String = "",
    val photoUrl: String = "",
    val photoName: String = ""
)

class IssueViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var reportsListener: ListenerRegistration? = null
    private var alertsListener: ListenerRegistration? = null

    // Auth State
    private val _currentUser = MutableStateFlow<ResidentUser?>(null)
    val currentUser: StateFlow<ResidentUser?> = _currentUser.asStateFlow()

    // Reports State: loaded live from Firestore (collection "reports"), only this resident's reports
    private val _reports = MutableStateFlow<List<IssueReport>>(emptyList())
    val reports: StateFlow<List<IssueReport>> = _reports.asStateFlow()

    // Alerts State: loaded live from Firestore (collection "notifications")
    private val _alerts = MutableStateFlow<List<NotificationItem>>(emptyList())
    val alerts: StateFlow<List<NotificationItem>> = _alerts.asStateFlow()

    // Active Creation Form State
    private val _formState = MutableStateFlow(ReportFormState())
    val formState: StateFlow<ReportFormState> = _formState.asStateFlow()

    // Toast/Message state
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // True while a report is being saved, so the Submit button can show a spinner
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    init {
        // Keep the resident signed in after the app is closed and opened again
        auth.currentUser?.let { firebaseUser ->
            viewModelScope.launch {
                val resident = loadResident(firebaseUser.uid)
                if (resident != null) {
                    _currentUser.value = resident
                    startListening(resident.id)
                } else {
                    auth.signOut()
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Accounts
    // ---------------------------------------------------------------------

    /** Creates the Firebase account and the resident's record in Firestore. Returns an error message, or null on success. */
    suspend fun register(
        name: String,
        email: String,
        phone: String,
        address: String,
        ward: String,
        password: String
    ): String? {
        return try {
            val uid = auth.createUserWithEmailAndPassword(email, password).await().user?.uid
                ?: return "Could not create the account. Please try again."
            try {
                // The password is NOT stored here. Firebase Authentication keeps it securely.
                db.collection("users").document(uid).set(
                    mapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "address" to address,
                        "ward" to ward,
                        "role" to "resident",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                // Don't leave a half-created account behind
                auth.currentUser?.delete()
                auth.signOut()
                return friendlyError(e)
            }
            _currentUser.value = ResidentUser(uid, name, email, phone, address, ward, "")
            startListening(uid)
            showToast("Welcome, $name!")
            null
        } catch (e: Exception) {
            friendlyError(e)
        }
    }

    /** Signs a resident in. Returns an error message, or null on success. */
    suspend fun signIn(email: String, password: String): String? {
        return try {
            val uid = auth.signInWithEmailAndPassword(email, password).await().user?.uid
                ?: return "Incorrect email or password."
            val resident = loadResident(uid)
            if (resident == null) {
                auth.signOut()
                return "This app is for residents. Staff and repair teams should use the staff portal."
            }
            _currentUser.value = resident
            startListening(uid)
            showToast("Welcome, ${resident.name}!")
            null
        } catch (e: Exception) {
            friendlyError(e)
        }
    }

    private suspend fun loadResident(uid: String): ResidentUser? = try {
        val snap = db.collection("users").document(uid).get().await()
        if (!snap.exists() || snap.getString("role") != "resident") {
            null
        } else {
            ResidentUser(
                id = uid,
                name = snap.getString("name").orEmpty(),
                email = snap.getString("email").orEmpty(),
                phone = snap.getString("phone").orEmpty(),
                address = snap.getString("address").orEmpty(),
                ward = snap.getString("ward").orEmpty(),
                avatarUrl = ""
            )
        }
    } catch (e: Exception) {
        null
    }

    fun logout() {
        stopListening()
        auth.signOut()
        _currentUser.value = null
        // Clear everything so the next person on this device never sees
        // the previous user's reports, alerts or half-filled form.
        _reports.value = emptyList()
        _alerts.value = emptyList()
        _formState.value = ReportFormState()
        showToast("Logged out successfully.")
    }

    private fun friendlyError(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> "An account with this email already exists. Please sign in."
        is FirebaseAuthWeakPasswordException -> "That password doesn't meet the security requirements."
        is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a few minutes and try again."
        is FirebaseNetworkException -> "No internet connection. Please check your network and try again."
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
    }

    // ---------------------------------------------------------------------
    // Live data from Firestore
    // ---------------------------------------------------------------------

    private fun startListening(uid: String) {
        stopListening()

        reportsListener = db.collection("reports").whereEqualTo("reporterId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                _reports.value = snap.documents
                    .sortedByDescending { it.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.seconds ?: 0L }
                    .map { it.toIssueReport() }
            }

        alertsListener = db.collection("notifications").whereEqualTo("userId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                _alerts.value = snap.documents
                    .sortedByDescending { it.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.seconds ?: 0L }
                    .map { doc ->
                        NotificationItem(
                            id = doc.id,
                            reportId = doc.getString("reportId"),
                            title = doc.getString("title").orEmpty(),
                            message = doc.getString("message").orEmpty(),
                            timestamp = doc.getString("timestamp").orEmpty(),
                            isRead = doc.getBoolean("isRead") ?: false,
                            type = doc.getString("type") ?: "status_update"
                        )
                    }
            }
    }

    private fun stopListening() {
        reportsListener?.remove(); reportsListener = null
        alertsListener?.remove(); alertsListener = null
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }

    // ---------------------------------------------------------------------
    // Messages
    // ---------------------------------------------------------------------

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // ---------------------------------------------------------------------
    // New Issue form
    // ---------------------------------------------------------------------

    // Accepts null so the screen can start with no category selected
    fun updateFormCategory(category: IssueCategory?) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun updateFormCustomCategory(custom: String) {
        _formState.value = _formState.value.copy(customCategory = custom)
    }

    private fun detectPriorityFromText(text: String): IssuePriority? {
        val lower = text.lowercase()

        // CRITICAL if description includes: "no water", "no electricity", "entire area", "flooding", "burst", "danger", "hazard", "no power"
        val criticalKeywords = listOf("no water", "no electricity", "entire area", "flooding", "burst", "danger", "hazard", "no power")
        if (criticalKeywords.any { lower.contains(it) }) return IssuePriority.CRITICAL

        // MAJOR PROBLEM if includes: "pothole", "blocked road", "broken", "leak", "pipe", "streetlight not working", "damaged"
        val majorKeywords = listOf("pothole", "blocked road", "broken", "leak", "pipe", "streetlight not working", "damaged")
        if (majorKeywords.any { lower.contains(it) }) return IssuePriority.MAJOR

        // INCONVENIENCE if includes: "litter", "rubbish", "dirty", "overgrown", "smell", "dumping", "noise"
        val inconvenienceKeywords = listOf("litter", "rubbish", "dirty", "overgrown", "smell", "dumping", "noise")
        if (inconvenienceKeywords.any { lower.contains(it) }) return IssuePriority.INCONVENIENCE

        // IMPROVEMENT REQUEST if includes: "need", "request", "please add", "suggest", "new bench", "new bin", "improve", "clean"
        val improvementKeywords = listOf("need", "request", "please add", "suggest", "new bench", "new bin", "improve", "clean")
        if (improvementKeywords.any { lower.contains(it) }) return IssuePriority.IMPROVEMENT

        return null
    }

    fun updateFormDescription(desc: String) {
        if (desc.length <= 250) {
            val isManual = _formState.value.isPriorityManuallySet
            var priorityState = _formState.value.priority
            if (!isManual) {
                val detected = detectPriorityFromText(desc)
                if (detected != null) {
                    priorityState = detected
                }
            }
            _formState.value = _formState.value.copy(
                description = desc,
                priority = priorityState
            )
        }
    }

    fun updateFormPriority(priority: IssuePriority, isManual: Boolean = true) {
        _formState.value = _formState.value.copy(
            priority = priority,
            isPriorityManuallySet = if (isManual) true else _formState.value.isPriorityManuallySet
        )
    }

    fun updateFormLocation(address: String, gps: String) {
        _formState.value = _formState.value.copy(address = address, gpsCoordinates = gps)
    }

    fun updateFormWard(ward: String) {
        _formState.value = _formState.value.copy(ward = ward)
    }

    fun updateFormPhoto(url: String, name: String) {
        _formState.value = _formState.value.copy(photoUrl = url, photoName = name)
    }

    fun resetForm() {
        _formState.value = ReportFormState()
    }

    // ---------------------------------------------------------------------
    // Submit a report: saved to Firestore, so the staff portal sees it straight away
    // ---------------------------------------------------------------------

    /** Returns the new report id, or null if it could not be saved. */
    suspend fun submitReport(context: Context): String? {
        val user = _currentUser.value ?: return null
        if (_isSubmitting.value) return null
        _isSubmitting.value = true

        return try {
            val state = _formState.value
            // The screen requires a category before submitting; OTHER is only a safety net
            val category = state.category ?: IssueCategory.OTHER
            val reportId = "ISS-" + (System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val ward = state.ward.ifEmpty { user.ward }
            val title = "${category.title} at ${state.address.substringBefore(",")}"

            // The photo travels inside the report, shrunk so it stays under Firestore's 1 MB document limit
            val photo = encodePhoto(context, state.photoUrl)

            // Field names and values match what the staff portal reads
            val report = hashMapOf<String, Any?>(
                "id" to reportId,
                "title" to title,
                "category" to category.name,
                "customCategory" to if (category == IssueCategory.OTHER) state.customCategory else null,
                "description" to state.description,
                "priority" to (state.priority ?: IssuePriority.MAJOR).name,
                "locationAddress" to state.address,
                "gpsCoordinates" to state.gpsCoordinates,
                "areaWard" to ward,
                "photoBefore" to photo,
                "photoBeforeName" to state.photoName,
                "photoAfter" to "",
                "status" to IssueStatus.REPORTED.name,
                "assignedTo" to null,
                "internalNotes" to "",
                "reporterId" to user.id,
                "reportedBy" to user.name,
                "reportedAt" to dateStr,
                "timeline" to listOf(
                    mapOf(
                        "id" to "step-1",
                        "title" to IssueStatus.REPORTED.display,
                        "description" to "Issue reported by resident.",
                        "timestamp" to dateStr,
                        "isCompleted" to true,
                        "isCurrent" to true
                    )
                ),
                "createdAt" to FieldValue.serverTimestamp()
            )

            val alert = hashMapOf<String, Any?>(
                "userId" to user.id,
                "reportId" to reportId,
                "title" to "Report Created successfully!",
                "message" to "Your issue report $title is registered in $ward.",
                "timestamp" to dateStr,
                "isRead" to false,
                "type" to "status_update",
                "createdAt" to FieldValue.serverTimestamp()
            )

            withTimeout(25_000L) {
                db.collection("reports").document(reportId).set(report).await()
                db.collection("notifications").add(alert).await()
            }

            showToast("Report $reportId submitted successfully!")
            resetForm()
            reportId
        } catch (e: TimeoutCancellationException) {
            showToast("Request timed out. Check your internet connection and try again.")
            null
        } catch (e: Exception) {
            showToast("Could not submit the report: ${e.localizedMessage ?: "unknown error"}")
            null
        } finally {
            _isSubmitting.value = false
        }
    }

    fun markAlertAsRead(alertId: String) {
        viewModelScope.launch {
            try {
                db.collection("notifications").document(alertId).update("isRead", true).await()
            } catch (_: Exception) {
                // The live listener will show the real state either way
            }
        }
    }

    // ---------------------------------------------------------------------
    // Photo: read from the phone, fix rotation, shrink, and turn into text
    // ---------------------------------------------------------------------

    private suspend fun encodePhoto(context: Context, uriString: String): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        val resolver = context.contentResolver
        val maxSide = 800

        // 1. Read the size first, so a huge photo is not loaded at full resolution
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IOException("Could not read the photo. Please choose it again.")

        // 2. Phones store "which way is up" separately, so apply it before shrinking
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        val scale = min(1f, maxSide / max(decoded.width, decoded.height).toFloat())
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(degrees)
        }
        val finalBitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        // 3. Compress, lowering the quality if the result is still too big
        var quality = 60
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 15
        } while (bytes.size > 500_000 && quality >= 15)

        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

// -------------------------------------------------------------------------
// Firestore document -> the app's IssueReport
// -------------------------------------------------------------------------

private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toIssueReport(): IssueReport {
    val status = enumOrDefault(getString("status"), IssueStatus.REPORTED)
    val steps = (get("timeline") as? List<Map<String, Any?>>).orEmpty()
    val photoAfter = getString("photoAfter").orEmpty()

    return IssueReport(
        id = id,
        title = getString("title") ?: "Untitled issue",
        category = enumOrDefault(getString("category"), IssueCategory.OTHER),
        customCategory = getString("customCategory"),
        description = getString("description").orEmpty(),
        priority = enumOrDefault(getString("priority"), IssuePriority.MAJOR),
        locationAddress = getString("locationAddress").orEmpty(),
        gpsCoordinates = getString("gpsCoordinates").orEmpty(),
        areaWard = getString("areaWard").orEmpty(),
        photoBefore = getString("photoBefore").orEmpty(),
        photoBeforeName = getString("photoBeforeName").orEmpty(),
        photoAfter = photoAfter.ifEmpty { null },
        photoAfterName = if (photoAfter.isNotEmpty()) "completion_photo.jpg" else null,
        status = status,
        reportedBy = getString("reportedBy").orEmpty(),
        reporterId = getString("reporterId").orEmpty(),
        reportedAt = getString("reportedAt").orEmpty(),
        assignedTo = getString("assignedTo"),
        completionNotes = if (status == IssueStatus.RESOLVED)
            steps.lastOrNull { it["title"] == IssueStatus.RESOLVED.display }?.get("description") as? String
        else null,
        timeline = buildTimeline(status, steps)
    )
}

/**
 * The portal only stores the steps that have actually happened. The app always shows all four stages,
 * so fill in the ones that are still pending.
 */
private fun buildTimeline(status: IssueStatus, steps: List<Map<String, Any?>>): List<TimelineStep> {
    val stages = listOf(
        IssueStatus.REPORTED to "You reported this issue.",
        IssueStatus.ASSIGNED to "Pending dispatch to local ward team.",
        IssueStatus.IN_PROGRESS to "Municipal crew to arrive on site.",
        IssueStatus.RESOLVED to "Photo proof & completion details."
    )
    return stages.mapIndexed { index, (stage, pendingText) ->
        val entry = steps.lastOrNull { it["title"] == stage.display }
        val reached = entry != null || stage.ordinal <= status.ordinal
        TimelineStep(
            id = "step-${index + 1}",
            title = stage.display,
            description = (entry?.get("description") as? String) ?: pendingText,
            timestamp = (entry?.get("timestamp") as? String) ?: if (reached) "—" else "Pending",
            isCompleted = reached,
            isCurrent = stage == status
        )
    }
}

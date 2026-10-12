package com.example.communityservice.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import coil.compose.rememberAsyncImagePainter
import com.example.communityservice.data.IssueCategory
import com.example.communityservice.data.IssuePriority
import com.example.communityservice.data.MUNICIPAL_DISTRICTS
import com.example.communityservice.ui.viewmodel.IssueViewModel
import com.example.communityservice.ui.viewmodel.ReportFormState
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private val Emerald600 = Color(0xFF059669)
private val Emerald50 = Color(0xFFECFDF5)
private val Amber600 = Color(0xFFD97706)
private val Amber500 = Color(0xFFF59E0B)
private val Amber400 = Color(0xFFFBBF24)
private val Slate600 = Color(0xFF475569)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)

// ---------------------------------------------------------------------------
// Validation: nothing may be left empty before moving on
// ---------------------------------------------------------------------------

/** Step 1: category, details, description and priority. Returns an error message or null. */
private fun validateDetails(form: ReportFormState): String? = when {
    form.category == null ->
        "Please select an issue category."
    form.category == IssueCategory.OTHER && form.customCategory.isBlank() ->
        "Please specify the issue category."
    form.description.isBlank() ->
        "Please enter an issue description."
    form.priority == null ->
        "Please select the issue severity."
    else -> null
}

/** Step 2: location and photo. Returns an error message or null. */
private fun validateLocationAndPhoto(form: ReportFormState): String? = when {
    form.address.isBlank() ->
        "Please enter the incident address."
    form.ward.isBlank() ->
        "Please select the municipal ward / district."
    form.photoUrl.isBlank() ->
        "Please add a photo of the incident. A photo is required."
    else -> null
}

private fun getDisplayName(context: Context, uri: Uri): String {
    val name: String? = try {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    } catch (e: Exception) {
        null
    }
    return name ?: "incident_photo.jpg"
}

// ---------------------------------------------------------------------------
// Real device location (no Google Play Services / API key needed)
// ---------------------------------------------------------------------------

/**
 * Asks the phone for its current position. Tries GPS first, then network location.
 * Returns null if location is switched off or no fix arrives in time.
 * The caller must already hold a location permission.
 */
@SuppressLint("MissingPermission")
private suspend fun getCurrentDeviceLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { manager.isProviderEnabled(it) }

    for (provider in providers) {
        val location = withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context)
                ) { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
            }
        }
        if (location != null) return location
    }
    return null
}

/** Turns coordinates into a readable street address, or null if none is available. */
private suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String? =
    withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            @Suppress("DEPRECATION")
            val results = Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)
            results?.firstOrNull()?.getAddressLine(0)
        } catch (e: Exception) {
            null
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueScreen(
    initialCategory: IssueCategory?, // null = the user must choose a category themselves
    viewModel: IssueViewModel,
    onSuccess: (String) -> Unit,
    onCancel: () -> Unit
) {
    var step by rememberSaveable { mutableStateOf(1) }
    val formState by viewModel.formState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var formInitialized by rememberSaveable { mutableStateOf(false) }

    // Needed to call suspend functions (like submitReport) from button clicks
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSubmitting by remember { mutableStateOf(false) }

    // Start every new report with a completely clean form: no category (unless a
    // quick tile chose one), no priority, no placeholder address, GPS or photo.
    LaunchedEffect(initialCategory) {
        if (!formInitialized) {
            viewModel.resetForm()
            viewModel.updateFormCategory(initialCategory)
            currentUser?.ward?.takeIf { it.isNotBlank() }?.let { viewModel.updateFormWard(it) }
            formInitialized = true
        }
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
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (step > 1) {
                            step--
                            errorMessage = null
                        } else {
                            onCancel()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Slate800)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Create Service Ticket",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Emerald950
                        )
                        Text(
                            text = "Step $step of 3 • ${if (step == 1) "Details" else if (step == 2) "Location & Photo" else "Review & Submit"}",
                            fontSize = 11.sp,
                            color = Emerald700,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Cancel", tint = Slate500)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Step Progress Bar
            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Emerald800,
                trackColor = Slate200
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

            Box(modifier = Modifier.weight(1f)) {
                when (step) {
                    1 -> Step1CategoryAndDetails(
                        viewModel = viewModel,
                        onNext = {
                            val problem = validateDetails(formState)
                            if (problem != null) {
                                errorMessage = problem
                            } else {
                                errorMessage = null
                                step = 2
                            }
                        }
                    )
                    2 -> Step2LocationAndPhoto(
                        viewModel = viewModel,
                        onBack = {
                            errorMessage = null
                            step = 1
                        },
                        onNext = {
                            val problem = validateLocationAndPhoto(formState)
                            if (problem != null) {
                                errorMessage = problem
                            } else {
                                errorMessage = null
                                step = 3
                            }
                        }
                    )
                    3 -> Step3ReviewAndSubmit(
                        viewModel = viewModel,
                        isSubmitting = isSubmitting,
                        onSubmit = {
                            // Final safety check: never submit an incomplete report
                            val detailsProblem = validateDetails(formState)
                            val locationProblem = validateLocationAndPhoto(formState)
                            when {
                                detailsProblem != null -> {
                                    errorMessage = detailsProblem
                                    step = 1
                                }
                                locationProblem != null -> {
                                    errorMessage = locationProblem
                                    step = 2
                                }
                                else -> {
                                    // submitReport is a suspend function, so it must run
                                    // inside a coroutine. Guard against double taps.
                                    if (!isSubmitting) {
                                        isSubmitting = true
                                        errorMessage = null
                                        scope.launch {
                                            val newReportId = try {
                                                viewModel.submitReport(context)
                                            } catch (e: Exception) {
                                                null
                                            }
                                            isSubmitting = false
                                            if (newReportId != null) {
                                                onSuccess(newReportId)
                                            } else {
                                                errorMessage =
                                                    "Couldn't submit your report. Check your connection and try again."
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onBack = {
                            errorMessage = null
                            step = 2
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Step1CategoryAndDetails(
    viewModel: IssueViewModel,
    onNext: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val scrollState = rememberScrollState()

    val categories = listOf(
        Triple(IssueCategory.WATER, "Water", Icons.Default.WaterDrop),
        Triple(IssueCategory.POTHOLE, "Potholes", Icons.Default.Warning),
        Triple(IssueCategory.STREETLIGHT, "Streetlights", Icons.Default.Lightbulb),
        Triple(IssueCategory.SANITATION, "Sanitation", Icons.Default.Delete),
        Triple(IssueCategory.OTHER, "Other Issue", Icons.Default.MoreHoriz)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Select Category *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in categories.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cat1 = categories[i]
                    CategoryButton(
                        modifier = Modifier.weight(1f),
                        category = cat1.first,
                        title = cat1.second,
                        icon = cat1.third,
                        isSelected = formState.category == cat1.first,
                        onClick = { viewModel.updateFormCategory(cat1.first) }
                    )

                    if (i + 1 < categories.size) {
                        val cat2 = categories[i + 1]
                        CategoryButton(
                            modifier = Modifier.weight(1f),
                            category = cat2.first,
                            title = cat2.second,
                            icon = cat2.third,
                            isSelected = formState.category == cat2.first,
                            onClick = { viewModel.updateFormCategory(cat2.first) }
                        )
                    }
                }
            }
        }

        if (formState.category == IssueCategory.OTHER) {
            OutlinedTextField(
                value = formState.customCategory,
                onValueChange = { viewModel.updateFormCustomCategory(it) },
                label = { Text("Specify Custom Issue Category *", fontSize = 12.sp) },
                placeholder = { Text("e.g. Overgrown tree branches", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Description Input with auto-detect keyword engine
        Text("Issue Description *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)

        OutlinedTextField(
            value = formState.description,
            onValueChange = { viewModel.updateFormDescription(it) },
            placeholder = { Text("Describe the fault, severity, and exact surroundings...", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        // Suggestion Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Suggestions:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
            SuggestionChip(label = "Burst water pipe") {
                viewModel.updateFormDescription("Burst water pipe flooding the street near corner intersection.")
            }
            SuggestionChip(label = "Deep pothole danger") {
                viewModel.updateFormDescription("Deep dangerous pothole damaging car tires on main road.")
            }
            SuggestionChip(label = "Streetlight out") {
                viewModel.updateFormDescription("Streetlight damaged and completely dark at night.")
            }
        }

        // Priority Selection
        Text("Issue Priority *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)

        val priorities = listOf(
            Triple(IssuePriority.CRITICAL, "Critical Danger", Color(0xFFDC2626)),
            Triple(IssuePriority.MAJOR, "Major Problem", Color(0xFFF43F5E)),
            Triple(IssuePriority.INCONVENIENCE, "Inconvenience", Color(0xFFD97706)),
            Triple(IssuePriority.IMPROVEMENT, "Improvement Request", Color(0xFF059669))
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            priorities.forEach { (p, label, color) ->
                val isSelected = formState.priority == p
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = { viewModel.updateFormPriority(p, isManual = true) }
                        ),
                    color = if (isSelected) color.copy(alpha = 0.08f) else Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else Slate200)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.updateFormPriority(p, isManual = true) },
                            colors = RadioButtonDefaults.colors(selectedColor = color)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, CircleShape)
                        )
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) color else Slate800
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Continue to Location & Photo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step2LocationAndPhoto(
    viewModel: IssueViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLocating by remember { mutableStateOf(false) }
    var locationNote by remember { mutableStateOf<String?>(null) }
    var photoNote by remember { mutableStateOf<String?>(null) }

    fun locateMe() {
        scope.launch {
            isLocating = true
            locationNote = null
            val location = getCurrentDeviceLocation(context)
            if (location == null) {
                locationNote = "Couldn't get your location. Make sure device location is turned on, or type the address instead."
            } else {
                val gps = String.format(Locale.US, "%.6f, %.6f", location.latitude, location.longitude)
                val street = reverseGeocode(context, location.latitude, location.longitude)
                if (street != null) {
                    viewModel.updateFormLocation(street, gps)
                } else {
                    viewModel.updateFormLocation(formState.address, gps)
                    locationNote = "Location found, but no street address was available. Please type the address."
                }
            }
            isLocating = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            locateMe()
        } else {
            locationNote = "Location permission was denied. Please type the address instead."
        }
    }

    // System photo picker: no storage permission needed
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoNote = null
            viewModel.updateFormPhoto(uri.toString(), getDisplayName(context, uri))
        }
    }

    // Live camera: the phone's own camera app takes the picture and saves it into a
    // file that our FileProvider shares. No CAMERA permission is needed for this.
    // rememberSaveable keeps the target file if the activity is recreated while the
    // camera app is open.
    var pendingPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingPhotoUri
        if (success && uri != null) {
            photoNote = null
            viewModel.updateFormPhoto(uri.toString(), "incident_photo.jpg")
        }
    }

    fun launchCamera() {
        try {
            val dir = File(context.filesDir, "incident_photos").apply { mkdirs() }
            val file = File(dir, "incident_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            pendingPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            photoNote = "Couldn't open the camera. You can choose a photo from your gallery instead."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Location Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Incident Location *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)

                OutlinedButton(
                    onClick = {
                        val hasPermission =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            locateMe()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !isLocating,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Emerald800
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finding your location...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald800)
                    } else {
                        Icon(Icons.Default.MyLocation, null, tint = Emerald800, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use My Current Location", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald800)
                    }
                }

                locationNote?.let {
                    Text(it, fontSize = 11.sp, color = Color(0xFFB91C1C))
                }

                OutlinedTextField(
                    value = formState.address,
                    onValueChange = { viewModel.updateFormLocation(it, formState.gpsCoordinates) },
                    label = { Text("Incident Address *", fontSize = 12.sp) },
                    placeholder = { Text("Street name and number, suburb", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Emerald800) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // South African Municipal District Dropdown
                var districtExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { districtExpanded = !districtExpanded }
                ) {
                    OutlinedTextField(
                        value = formState.ward,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Municipal Ward / District *", fontSize = 12.sp) },
                        placeholder = { Text("Select a district", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Map, null, tint = Emerald800) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
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
                                        fontWeight = if (district == formState.ward) FontWeight.Bold else FontWeight.Normal,
                                        color = if (district == formState.ward) Emerald800 else Slate900
                                    )
                                },
                                onClick = {
                                    viewModel.updateFormWard(district)
                                    districtExpanded = false
                                }
                            )
                        }
                    }
                }

                if (formState.gpsCoordinates.isNotBlank()) {
                    Text(
                        text = "GPS: ${formState.gpsCoordinates}",
                        fontSize = 10.sp,
                        color = Slate500
                    )
                }
            }
        }

        // Photo Evidence Card (required, real photo only)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Photo Evidence *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Text(
                    "Take a photo of the incident right now, or choose one from your gallery. A photo is required to continue.",
                    fontSize = 11.sp,
                    color = Slate500
                )

                val hasPhoto = formState.photoUrl.isNotBlank()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate100)
                        .border(1.dp, Slate200, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasPhoto) {
                        Image(
                            painter = rememberAsyncImagePainter(formState.photoUrl),
                            contentDescription = "Photo of the incident",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = formState.photoName,
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = Slate500,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "No photo added yet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                        }
                    }
                }

                photoNote?.let {
                    Text(it, fontSize = 11.sp, color = Color(0xFFB91C1C))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { launchCamera() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, tint = Emerald800, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasPhoto) "Retake" else "Take Photo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald800
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = Emerald800, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gallery",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald800
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Back", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
            }

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1.5f)
            ) {
                Text("Review & Submit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun Step3ReviewAndSubmit(
    viewModel: IssueViewModel,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Verify Report & Submit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
        Text("Review classification, description, and location before submitting to local municipal dispatch.", fontSize = 12.sp, color = Slate500)

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEW TICKET DRAFT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Emerald800,
                        letterSpacing = 0.5.sp
                    )
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = formState.priority?.title ?: "Major Problem",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Amber600,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = if (formState.category == IssueCategory.OTHER && formState.customCategory.isNotEmpty())
                                formState.customCategory else formState.category?.title.orEmpty(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Reporting Resident: ${currentUser?.name.orEmpty()}",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Divider(color = Slate100)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("LOCATION & WARD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                    Text(text = formState.address, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate800)
                    Text(text = "District: ${formState.ward}", fontSize = 11.sp, color = Emerald800, fontWeight = FontWeight.SemiBold)
                    if (formState.gpsCoordinates.isNotBlank()) {
                        Text(text = "GPS: ${formState.gpsCoordinates}", fontSize = 10.sp, color = Slate500)
                    }
                }

                Divider(color = Slate100)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DESCRIPTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                    Text(text = formState.description, fontSize = 12.sp, color = Slate800)
                }

                if (formState.photoUrl.isNotBlank()) {
                    Divider(color = Slate100)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("PHOTO EVIDENCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                        Image(
                            painter = rememberAsyncImagePainter(formState.photoUrl),
                            contentDescription = "Photo of the incident",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Back", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
            }

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1.5f)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submitting...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Submit Service Ticket", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(
    modifier: Modifier = Modifier,
    category: IssueCategory,
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (isSelected) Emerald50 else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Emerald800 else Slate200)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Emerald800 else Slate700,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Emerald950 else Slate800
            )
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = Slate100,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Slate700,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
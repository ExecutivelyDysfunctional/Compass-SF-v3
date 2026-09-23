package com.example.ui
import com.example.data.AppIconManager
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Note: Theme colors are dynamically provided via Theme.kt and LocalCompassTheme

// --- Share Resource Card Helpers ---

fun buildResourceShareText(res: Resource): String {
    val sb = StringBuilder()
    sb.appendLine("🧭 Compass SF Resource: ${res.name}")
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    if (res.category.isNotBlank()) sb.appendLine("📂 Category: ${res.category.replaceFirstChar { it.uppercase() }}")
    if (res.neighborhood.isNotBlank()) sb.appendLine("📍 Neighborhood: ${res.neighborhood}")
    if (res.address.isNotBlank()) sb.appendLine("🏢 Address: ${res.address}")
    if (res.phone.isNotBlank()) sb.appendLine("📞 Phone: ${res.phone}")
    if (res.hoursText.isNotBlank()) sb.appendLine("⏰ Hours: ${res.hoursText}")
    if (res.cost.isNotBlank()) sb.appendLine("💵 Cost: ${res.cost}")
    if (res.eligibility.isNotBlank()) sb.appendLine("👥 Eligibility: ${res.eligibility}")
    if (res.requirements.isNotEmpty()) sb.appendLine("📋 Requirements: ${res.requirements.joinToString(", ")}")
    if (res.bring.isNotEmpty()) sb.appendLine("🎒 Bring: ${res.bring.joinToString(", ")}")
    if (res.alsoOffers.isNotEmpty()) sb.appendLine("✨ Also Offers: ${res.alsoOffers.joinToString(", ")}")
    if (res.languages.isNotEmpty()) sb.appendLine("🗣️ Languages: ${res.languages.joinToString(", ")}")
    if (res.summary.isNotBlank()) sb.appendLine("\n📝 Summary:\n${res.summary}")
    if (res.aiTips.isNotBlank()) sb.appendLine("\n💡 Navigator Tip:\n${res.aiTips}")
    if (res.website.isNotBlank()) sb.appendLine("\n🌐 Website: ${res.website}")
    sb.appendLine("\nShared via Compass SF — Offline-First Street Navigator")
    return sb.toString()
}

fun buildRmpShareText(rmp: RmpLocation): String {
    val sb = StringBuilder()
    sb.appendLine("💳 SF CalFresh EBT Restaurant: ${rmp.name}")
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    sb.appendLine("🍔 Cuisine: ${rmp.cuisine.replaceFirstChar { it.uppercase() }}")
    if (rmp.neighborhood.isNotBlank()) sb.appendLine("📍 Neighborhood: ${rmp.neighborhood}")
    if (rmp.address.isNotBlank()) sb.appendLine("🏢 Address: ${rmp.address}")
    if (rmp.phone.isNotBlank()) sb.appendLine("📞 Phone: ${rmp.phone}")
    if (rmp.hoursText.isNotBlank()) sb.appendLine("⏰ Hours: ${rmp.hoursText}")
    if (rmp.notes.isNotBlank()) sb.appendLine("ℹ️ Notes: ${rmp.notes}")
    if (rmp.tips.isNotBlank()) sb.appendLine("💡 Tips: ${rmp.tips}")
    sb.appendLine("\nShared via Compass SF — SF CalFresh Restaurant Meals Program")
    return sb.toString()
}

fun shareResource(context: Context, res: Resource) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Compass SF: ${res.name}")
            putExtra(Intent.EXTRA_TEXT, buildResourceShareText(res))
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, "Share ${res.name}")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open share dialog: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
    }
}

fun copyResourceDetails(context: Context, res: Resource) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Compass SF Resource", buildResourceShareText(res))
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied details for ${res.name} to clipboard", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Could not copy: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
    }
}

fun shareRmpLocation(context: Context, rmp: RmpLocation) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "SF EBT Hot Meals: ${rmp.name}")
            putExtra(Intent.EXTRA_TEXT, buildRmpShareText(rmp))
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, "Share ${rmp.name}")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open share dialog: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
    }
}

fun copyRmpDetails(context: Context, rmp: RmpLocation) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SF EBT Hot Meals", buildRmpShareText(rmp))
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied details for ${rmp.name} to clipboard", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Could not copy: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
    }
}

// --- Helper Open/Closed Logic ---
fun isResourceOpen(open24: Boolean, hours: List<HourBlock>): Boolean {
    if (open24) return true
    if (hours.isEmpty()) return false
    val calendar = Calendar.getInstance()
    val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Convert to 0=Mon, ..., 6=Sun
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val currentTimeMinutes = hour * 60 + minute

    for (block in hours) {
        val mappedDay = when(block.day) {
            0 -> 6 // Sun
            else -> block.day - 1
        }
        if (mappedDay == dayOfWeek) {
            val openParts = block.open.split(":")
            val closeParts = block.close.split(":")
            if (openParts.size >= 2 && closeParts.size >= 2) {
                val openMinutes = (openParts[0].toIntOrNull() ?: 0) * 60 + (openParts[1].toIntOrNull() ?: 0)
                val closeMinutes = (closeParts[0].toIntOrNull() ?: 0) * 60 + (closeParts[1].toIntOrNull() ?: 0)
                if (currentTimeMinutes in openMinutes..closeMinutes) {
                    return true
                }
            }
        }
    }
    return false
}

fun formatTime(timeStr: String): String {
    val parts = timeStr.split(":")
    if (parts.size < 2) return timeStr
    val hr = parts[0].toIntOrNull() ?: return timeStr
    val min = parts[1].toIntOrNull() ?: return timeStr
    val suffix = if (hr >= 12) "pm" else "am"
    val displayHr = when {
        hr == 0 -> 12
        hr > 12 -> hr - 12
        else -> hr
    }
    val displayMin = if (min == 0) "" else ":%02d".format(min)
    return "$displayHr$displayMin$suffix"
}

// --- Location Selector & Dialog Components ---

@Composable
fun LocationAnchorDialog(
    viewModel: CompassViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val userLocation by viewModel.userLocation
    val isLocating by viewModel.isLocating
    val selectedAnchorId by viewModel.selectedNeighborhoodAnchorId

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.requestDeviceLocation(context)
            onDismiss()
        } else {
            viewModel.locationPermissionDenied.value = true
            Toast.makeText(
                context,
                "Location permission not granted. You can still choose a SF neighborhood anchor below!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ink900,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Beacon500)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Street Location & Proximity", color = Mist100, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Calculates walking distances locally on your device without transmitting coordinates anywhere.",
                    color = Mist400,
                    fontSize = 13.sp
                )

                // Current GPS Option
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (userLocation != null && !userLocation!!.isManualAnchor) Beacon500.copy(alpha = 0.15f) else Ink800
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (userLocation != null && !userLocation!!.isManualAnchor) Beacon500 else Ink700
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isLocating) Beacon500 else Ink700, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(color = OnAccentColor, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = if (userLocation != null && !userLocation!!.isManualAnchor) Beacon400 else Mist100,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use Current GPS Location", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 14.sp)
                            Text("Auto-detect closest street coordinate", color = Mist400, fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider(color = Ink800)

                Text(
                    "Or Choose SF Neighborhood Anchor:",
                    color = Beacon400,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                LocationHelper.NEIGHBORHOOD_ANCHORS.forEach { anchor ->
                    val isSelected = selectedAnchorId == anchor.id || (userLocation?.isManualAnchor == true && userLocation?.label == anchor.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Beacon500.copy(alpha = 0.2f) else Ink800)
                            .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.setNeighborhoodAnchor(anchor)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                viewModel.setNeighborhoodAnchor(anchor)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(anchor.name, fontWeight = FontWeight.SemiBold, color = Mist100, fontSize = 13.sp)
                            Text(anchor.subtitle, color = Mist400, fontSize = 11.sp)
                        }
                    }
                }

                if (userLocation != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.clearLocation()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                        border = BorderStroke(1.dp, Rose500.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Location / Disable Proximity", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor)
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
fun LocationBar(
    viewModel: CompassViewModel,
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userLocation by viewModel.userLocation
    val sortByDistance by viewModel.sortByDistance

    Card(
        colors = CardDefaults.cardColors(containerColor = Ink900),
        border = BorderStroke(1.dp, if (sortByDistance && userLocation != null) Beacon500.copy(alpha = 0.8f) else Ink700),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDialog)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (userLocation != null && !userLocation!!.isManualAnchor) Icons.Default.MyLocation else Icons.Default.LocationOn,
                    contentDescription = "Location Anchor",
                    tint = if (sortByDistance && userLocation != null) Beacon500 else Mist400,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    val label = when {
                        userLocation == null -> "Set Location Anchor (GPS / SF Neighborhood)"
                        userLocation!!.isManualAnchor -> "Near ${userLocation!!.label}"
                        else -> "Near Current GPS Location"
                    }
                    Text(
                        text = label,
                        color = if (sortByDistance && userLocation != null) Mist100 else Mist400,
                        fontSize = 12.sp,
                        fontWeight = if (sortByDistance && userLocation != null) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            userLocation == null -> "Tap to enable walking distances"
                            sortByDistance -> "Sorted by closest proximity • Tap to change"
                            else -> "Proximity sorting paused • Tap to configure"
                        },
                        color = if (sortByDistance && userLocation != null) Beacon400 else Mist400,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Sort Toggle Chip
            FilterChip(
                selected = sortByDistance && userLocation != null,
                onClick = {
                    if (userLocation == null) {
                        onOpenDialog()
                    } else {
                        viewModel.toggleSortByDistance()
                    }
                },
                label = { Text("Nearest", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        Icons.Default.NearMe,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (sortByDistance && userLocation != null) OnAccentColor else Mist400
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Beacon500,
                    selectedLabelColor = OnAccentColor,
                    containerColor = Ink800,
                    labelColor = Mist100
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (sortByDistance && userLocation != null) Beacon500 else Ink700,
                    selectedBorderColor = Beacon500,
                    enabled = true,
                    selected = sortByDistance && userLocation != null
                ),
                modifier = Modifier.defaultMinSize(minHeight = 36.dp)
            )
        }
    }
}

// --- Now Screen (Dashboard) ---
@Composable
fun NowScreen(
    viewModel: CompassViewModel,
    onNavigateToFind: (String) -> Unit,
    onNavigateToMap: () -> Unit = {},
    onNavigateToDetail: (Int) -> Unit
) {
    val resources by viewModel.allResources.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val userLocation by viewModel.userLocation
    val sortByDistance by viewModel.sortByDistance
    var showLocationDialog by remember { mutableStateOf(false) }
    
    val openNowList = remember(
        resources,
        userLocation,
        sortByDistance,
        viewModel.userProfile.value,
        viewModel.demographicPresets.value,
        viewModel.accessibilityMobilityMode.value,
        viewModel.dietaryPresets.value,
        viewModel.excludeNonLocationResources.value,
        viewModel.sourceShelterTechEnabled.value,
        viewModel.sourceDataSfEnabled.value,
        viewModel.source211Enabled.value,
        viewModel.sourceCommunityEnabled.value
    ) {
        val openResources = resources.filter { res ->
            !res.hidden && isResourceOpen(res.open24, res.hours) &&
            (!viewModel.excludeNonLocationResources.value || !res.isNonLocationOrHelpline()) &&
            PresetMatcher.matchesDemographic(res, viewModel.demographicPresets.value) &&
            PresetMatcher.matchesAccessibility(res, viewModel.accessibilityMobilityMode.value) &&
            PresetMatcher.matchesDietary(res, viewModel.dietaryPresets.value) &&
            viewModel.isSourceAllowed(res.source)
        }
        if (sortByDistance && userLocation != null) {
            openResources.sortedBy { res ->
                viewModel.getDistanceToResource(res) ?: Double.MAX_VALUE
            }.take(4)
        } else {
            ResourceRelevance.rankResources(openResources, viewModel.userProfile.value).take(4)
        }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$greeting, traveler",
                style = MaterialTheme.typography.headlineSmall,
                color = Beacon500,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Here is your street guide and open resources for today.",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist400
            )
        }

        // Location & Proximity Bar
        item {
            LocationBar(
                viewModel = viewModel,
                onOpenDialog = { showLocationDialog = true }
            )
        }
        
        item {
            ActivePresetsBanner(viewModel = viewModel)
        }

        item {
            RelevanceProfileBanner(viewModel = viewModel)
        }

        item {
            StreetAlertsBanner(
                viewModel = viewModel,
                onNavigateToFind = onNavigateToFind
            )
        }

        item {
            MorningBriefingCard(viewModel = viewModel)
        }

        // Quick Category Row & Map Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Search",
                    style = MaterialTheme.typography.titleMedium,
                    color = Mist100,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(
                    onClick = onNavigateToMap,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = Beacon500, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Explore Map", color = Beacon500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    "food" to "🍱 Food",
                    "shelter" to "🏠 Shelter",
                    "hygiene" to "🧼 Hygiene",
                    "connect" to "🤝 Connect",
                    "benefits" to "💳 EBT/Benefits",
                    "health" to "🩺 Medical"
                )
                items(categories) { (cat, label) ->
                    AssistChip(
                        onClick = { onNavigateToFind(cat) },
                        label = { Text(label, color = Mist100) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Ink900),
                        border = BorderStroke(1.dp, Ink700),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    )
                }
            }
        }

        // Checklist tasks snippet
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Day Checklist",
                            style = MaterialTheme.typography.titleMedium,
                            color = Beacon500,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Checklist icon",
                            tint = Mist400
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val pendingTasks = tasks.filter { !it.done }.take(3)
                    if (pendingTasks.isEmpty()) {
                        Text(
                            text = "No pending tasks. Feel free to rest or add plans in Day tab.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Mist400
                        )
                    } else {
                        pendingTasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable { viewModel.toggleTask(task) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "Unchecked",
                                    tint = Beacon500,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Mist100,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Open Now Spotlights
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (sortByDistance && userLocation != null) "Closest Open Resources in SF" else "Open Right Now in SF",
                    style = MaterialTheme.typography.titleMedium,
                    color = Mist100,
                    fontWeight = FontWeight.SemiBold
                )
                if (sortByDistance && userLocation != null) {
                    Text(
                        text = "Nearest first",
                        color = Beacon400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (openNowList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No resources currently analyzed as open.", color = Mist400)
                }
            }
        } else {
            items(openNowList) { res ->
                ResourceCard(
                    resource = res,
                    distanceMiles = viewModel.getDistanceToResource(res),
                    onCardClick = { onNavigateToDetail(res.id) },
                    onFavoriteClick = { viewModel.toggleResourceFavorite(res) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showLocationDialog) {
        LocationAnchorDialog(
            viewModel = viewModel,
            onDismiss = { showLocationDialog = false }
        )
    }
}

// --- Find Screen (Search & Filtering list) ---
@Composable
fun FindScreen(
    viewModel: CompassViewModel,
    initialCategory: String,
    onNavigateToMap: (String) -> Unit = {},
    onNavigateToDetail: (Int) -> Unit
) {
    val resources by viewModel.allResources.collectAsState()
    val userLocation by viewModel.userLocation
    val sortByDistance by viewModel.sortByDistance
    var showLocationDialog by remember { mutableStateOf(false) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(if (initialCategory.isBlank()) "all" else initialCategory) }
    var selectedNeighborhood by remember { mutableStateOf("all") }
    var selectedSource by remember { mutableStateOf("all") }
    
    var filterOpenNow by remember { mutableStateOf(false) }
    var filterFavorites by remember { mutableStateOf(false) }
    var filterHidden by remember { mutableStateOf(false) }
    var isMapView by remember { mutableStateOf(false) }

    val categories = listOf(
        "all" to "All",
        "food" to "Food",
        "shelter" to "Shelter",
        "hygiene" to "Hygiene",
        "health" to "Health",
        "mental" to "Crisis",
        "documents" to "ID/Docs",
        "benefits" to "Benefits",
        "legal" to "Legal",
        "connect" to "Connect"
    )

    val neighborhoods = listOf("all" to "All Neighborhoods") + 
            resources.map { it.neighborhood }.filter { it.isNotBlank() }.distinct().sorted().map { it to it }

    val sources = listOf(
        "all" to "All Sources",
        "ShelterTech" to "SF Service Guide",
        "DataSF" to "DataSF",
        "211" to "211 Bay Area"
    )

    val filteredList = remember(
        resources,
        searchQuery,
        selectedCategory,
        selectedNeighborhood,
        selectedSource,
        filterOpenNow,
        filterFavorites,
        filterHidden,
        userLocation,
        sortByDistance,
        viewModel.demographicPresets.value,
        viewModel.accessibilityMobilityMode.value,
        viewModel.dietaryPresets.value,
        viewModel.excludeNonLocationResources.value,
        viewModel.userProfile.value,
        viewModel.sourceShelterTechEnabled.value,
        viewModel.sourceDataSfEnabled.value,
        viewModel.source211Enabled.value,
        viewModel.sourceCommunityEnabled.value
    ) {
        val list = resources.filter { res ->
            val matchesSearch = res.name.contains(searchQuery, ignoreCase = true) || 
                    res.summary.contains(searchQuery, ignoreCase = true) ||
                    res.description.contains(searchQuery, ignoreCase = true) ||
                    res.source.contains(searchQuery, ignoreCase = true) ||
                    res.tags.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesCategory = selectedCategory == "all" || res.category == selectedCategory || res.alsoOffers.contains(selectedCategory)
            val matchesNeighborhood = selectedNeighborhood == "all" || res.neighborhood.equals(selectedNeighborhood, ignoreCase = true)
            val matchesSource = selectedSource == "all" || res.source.contains(selectedSource, ignoreCase = true)
            val matchesOpen = !filterOpenNow || isResourceOpen(res.open24, res.hours)
            val matchesFav = !filterFavorites || res.favorite
            val matchesHidden = if (filterHidden) res.hidden else !res.hidden
            val matchesNonLocation = !viewModel.excludeNonLocationResources.value || !res.isNonLocationOrHelpline()

            val matchesDemographic = PresetMatcher.matchesDemographic(res, viewModel.demographicPresets.value)
            val matchesAccessibility = PresetMatcher.matchesAccessibility(res, viewModel.accessibilityMobilityMode.value)
            val matchesDietary = PresetMatcher.matchesDietary(res, viewModel.dietaryPresets.value)
            val matchesAllowedSource = viewModel.isSourceAllowed(res.source)

            matchesSearch && matchesCategory && matchesNeighborhood && matchesSource && matchesOpen && matchesFav && matchesHidden && matchesNonLocation && matchesDemographic && matchesAccessibility && matchesDietary && matchesAllowedSource
        }
        if (sortByDistance && userLocation != null) {
            list.sortedBy { res ->
                viewModel.getDistanceToResource(res) ?: Double.MAX_VALUE
            }
        } else {
            ResourceRelevance.rankResources(list, viewModel.userProfile.value)
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length >= 2 && !viewModel.incognitoSearchMode.value) {
            viewModel.addRecentSearch(searchQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
    ) {
        // Search bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_input"),
            placeholder = { Text("Search meals, clinics, shelter, IDs, sources...", color = Mist400) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Mist400) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Mist400, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.toggleIncognitoSearchMode(!viewModel.incognitoSearchMode.value)
                        },
                        modifier = Modifier.testTag("toggle_incognito_search")
                    ) {
                        if (viewModel.incognitoSearchMode.value) {
                            Text("🕵️", fontSize = 16.sp)
                        } else {
                            Icon(Icons.Default.Security, contentDescription = "Incognito Search Mode", tint = Mist400, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Ink900,
                unfocusedContainerColor = Ink900,
                focusedTextColor = Mist100,
                unfocusedTextColor = Mist100,
                cursorColor = Beacon500,
                focusedIndicatorColor = Beacon500
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        // Ephemeral / Incognito Search Indicator
        if (viewModel.incognitoSearchMode.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .background(Ink900, RoundedCornerShape(6.dp))
                    .border(1.dp, Beacon500.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🕵️", fontSize = 13.sp)
                    Text("Incognito Search • Zero query logs recorded", fontSize = 11.sp, color = Beacon400, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Disable",
                    fontSize = 11.sp,
                    color = Mist300,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.toggleIncognitoSearchMode(false) }
                )
            }
        } else if (viewModel.recentSearches.value.isNotEmpty() && searchQuery.isBlank()) {
            // Recent Searches Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent:", fontSize = 11.sp, color = Mist400, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    items(viewModel.recentSearches.value) { query ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Ink850,
                            border = BorderStroke(1.dp, Ink700),
                            modifier = Modifier.clickable { searchQuery = query }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(query, fontSize = 11.sp, color = Mist200)
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove recent search",
                                    tint = Mist400,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { viewModel.removeRecentSearch(query) }
                                )
                            }
                        }
                    }
                }
                Text(
                    "Clear",
                    fontSize = 10.sp,
                    color = Mist400,
                    modifier = Modifier
                        .clickable { viewModel.clearRecentSearches() }
                        .padding(start = 6.dp)
                )
            }
        }

        // Active Curated Data Sources Filter Warning Banner
        if (viewModel.hasDisabledDataSources) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .background(Ink900, RoundedCornerShape(6.dp))
                    .border(1.dp, Amber500.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⚡", fontSize = 12.sp)
                    Text("Curated Sources Filter Active (Some directories hidden)", fontSize = 11.sp, color = Amber500)
                }
                Text(
                    "Reset All",
                    fontSize = 11.sp,
                    color = Beacon400,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.resetDataSourcesToDefaults() }
                )
            }
        }

        // Location & Proximity Bar
        LocationBar(
            viewModel = viewModel,
            onOpenDialog = { showLocationDialog = true },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        ActivePresetsBanner(
            viewModel = viewModel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        RelevanceProfileBanner(
            viewModel = viewModel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Categories chip row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            items(categories) { (id, label) ->
                val isSelected = selectedCategory == id
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = id },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Beacon500,
                        selectedLabelColor = Ink950,
                        containerColor = Ink900,
                        labelColor = Mist400
                    ),
                    border = BorderStroke(1.dp, if (isSelected) Beacon500 else Ink700),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                )
            }
        }

        // Dropdown spinners & Toggle Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                var nDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { nDropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Ink900),
                        border = BorderStroke(1.dp, Ink700),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text(neighborhoods.find { it.first == selectedNeighborhood }?.second ?: "Neighborhood", color = Mist100, fontSize = 12.sp)
                        Icon(Icons.Default.ArrowDropDown, "down", tint = Mist400)
                    }
                    DropdownMenu(
                        expanded = nDropdownExpanded,
                        onDismissRequest = { nDropdownExpanded = false },
                        modifier = Modifier.background(Ink900)
                    ) {
                        neighborhoods.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name, color = Mist100) },
                                onClick = {
                                    selectedNeighborhood = id
                                    nDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                var sDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { sDropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Ink900),
                        border = BorderStroke(1.dp, if (selectedSource != "all") Beacon500 else Ink700),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text(sources.find { it.first == selectedSource }?.second ?: "Source", color = if (selectedSource != "all") Beacon400 else Mist100, fontSize = 12.sp)
                        Icon(Icons.Default.ArrowDropDown, "down", tint = Mist400)
                    }
                    DropdownMenu(
                        expanded = sDropdownExpanded,
                        onDismissRequest = { sDropdownExpanded = false },
                        modifier = Modifier.background(Ink900)
                    ) {
                        sources.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name, color = Mist100) },
                                onClick = {
                                    selectedSource = id
                                    sDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Quick toggles (44px / 48dp thumb touch targets for Pixel 8 Pro)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = { filterOpenNow = !filterOpenNow },
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (filterOpenNow) Beacon500 else Ink900, RoundedCornerShape(8.dp)),
                ) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = "Open Now",
                        tint = if (filterOpenNow) Ink950 else Mist400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { filterFavorites = !filterFavorites },
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (filterFavorites) Beacon500 else Ink900, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        if (filterFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorites",
                        tint = if (filterFavorites) Ink950 else Mist400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { filterHidden = !filterHidden },
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (filterHidden) Rose500 else Ink900, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        if (filterHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Hidden Items",
                        tint = if (filterHidden) Mist100 else Mist400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleExcludeNonLocationResources() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (viewModel.excludeNonLocationResources.value) Beacon500 else Ink900, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        if (viewModel.excludeNonLocationResources.value) Icons.Default.Place else Icons.Default.Phone,
                        contentDescription = if (viewModel.excludeNonLocationResources.value) "Excluding Help Lines (Physical Places Only)" else "Include Help Lines & Non-Location Resources",
                        tint = if (viewModel.excludeNonLocationResources.value) Ink950 else Mist400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (sortByDistance && userLocation != null) "Showing ${filteredList.size} matches (sorted by proximity)" else "Showing ${filteredList.size} matches",
                fontSize = 12.sp,
                color = Mist400
            )

            // List / Map view switcher
            Row(
                modifier = Modifier
                    .background(Ink900, RoundedCornerShape(8.dp))
                    .border(1.dp, Ink700, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Button(
                    onClick = { isMapView = false },
                    colors = ButtonDefaults.buttonColors(containerColor = if (!isMapView) Beacon500 else Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = "List View", tint = if (!isMapView) Ink950 else Mist400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("List", color = if (!isMapView) Ink950 else Mist400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isMapView = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isMapView) Beacon500 else Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Map View", tint = if (isMapView) Ink950 else Mist400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Map", color = if (isMapView) Ink950 else Mist400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isMapView) {
            ComposeCanvasMap(
                resources = filteredList,
                viewModel = viewModel,
                onNavigateToDetail = onNavigateToDetail,
                onFavoriteClick = { res -> viewModel.toggleResourceFavorite(res) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // The list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No resources found matching filters.", color = Mist400)
                        }
                    }
                } else {
                    items(filteredList) { res ->
                        ResourceCard(
                            resource = res,
                            distanceMiles = viewModel.getDistanceToResource(res),
                            onCardClick = { onNavigateToDetail(res.id) },
                            onFavoriteClick = { viewModel.toggleResourceFavorite(res) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showLocationDialog) {
        LocationAnchorDialog(
            viewModel = viewModel,
            onDismiss = { showLocationDialog = false }
        )
    }
}

@Composable
fun ResourceMapView(
    resources: List<Resource>,
    onNavigateToDetail: (Int) -> Unit,
    onFavoriteClick: (Resource) -> Unit
) {
    val context = LocalContext.current
    var selectedResource by remember { mutableStateOf<Resource?>(null) }
    
    val cInk800 = Ink800
    // Fallback if standalone without ViewModel
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
    ) {
        // Render pins on responsive Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Simple subtle street grid
            for (i in 1..4) {
                drawLine(
                    color = cInk800,
                    start = Offset(0f, h * (i / 5f)),
                    end = Offset(w, h * (i / 5f)),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}

// --- Ask Screen (Gemini Q&A Client) ---
@Composable
fun AskScreen(
    viewModel: CompassViewModel,
    onNavigateToDetail: (Int) -> Unit
) {
    val resources by viewModel.allResources.collectAsState()
    val isAsking by viewModel.isAsking
    val askResult by viewModel.askResult
    val askError by viewModel.askError

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Ask Navigator",
                style = MaterialTheme.typography.titleLarge,
                color = Beacon500,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Describe your situation (e.g. 'I need some boots, clean socks, and a shower in the Mission right now'). We'll search and give picks.",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist400
            )

            Spacer(modifier = Modifier.height(10.dp))

            // AI Style Selector Chips Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "STYLE: ${viewModel.aiNavigatorStyle.value.title.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Beacon500
                    )
                    if (viewModel.aiOfflineOnly.value) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF854D0E).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color(0xFFFACC15), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("🟡 Offline Heuristics", color = Color(0xFFFDE047), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AiNavigatorStyle.entries.forEach { styleOption ->
                        val isSelected = viewModel.aiNavigatorStyle.value == styleOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Beacon500 else Ink900)
                                .border(
                                    1.dp,
                                    if (isSelected) Beacon500 else Ink700,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setAiNavigatorStyle(styleOption) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = styleOption.badge,
                                color = if (isSelected) Ink950 else Mist100,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = viewModel.askQuestion.value,
                onValueChange = { viewModel.askQuestion.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("ask_input"),
                placeholder = { Text("What are you looking for?", color = Mist400) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Beacon500,
                    unfocusedBorderColor = Ink700,
                    focusedTextColor = Mist100,
                    unfocusedTextColor = Mist100
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Neighborhood input filter
                OutlinedTextField(
                    value = viewModel.askNeighborhood.value,
                    onValueChange = { viewModel.askNeighborhood.value = it },
                    placeholder = { Text("Neighborhood (optional)", fontSize = 13.sp, color = Mist400) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Beacon500,
                        unfocusedBorderColor = Ink700,
                        focusedTextColor = Mist100,
                        unfocusedTextColor = Mist100
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                // Open only checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable { viewModel.askOpenOnly.value = !viewModel.askOpenOnly.value }
                ) {
                    Checkbox(
                        checked = viewModel.askOpenOnly.value,
                        onCheckedChange = { viewModel.askOpenOnly.value = it },
                        colors = CheckboxDefaults.colors(checkedColor = Beacon500)
                    )
                    Text("Open now", color = Mist100, fontSize = 13.sp)
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.askNavigator() },
                enabled = !isAsking && viewModel.askQuestion.value.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("ask_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Beacon500,
                    contentColor = OnAccentColor,
                    disabledContainerColor = Ink900,
                    disabledContentColor = Mist400
                )
            ) {
                if (isAsking) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnAccentColor, strokeWidth = 2.dp)
                } else {
                    Text("Consult Navigator AI", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (askError != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .border(1.dp, Rose500, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Text(
                        askError!!,
                        color = Mist100,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (askResult != null) {
            val response = askResult!!
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Ink900),
                    border = BorderStroke(1.dp, Ink700),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Navigator Guidance",
                            style = MaterialTheme.typography.titleMedium,
                            color = Beacon500,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = response.answer, color = Mist100, lineHeight = 20.sp)
                        
                        if (response.nextSteps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Recommended Steps:", color = Beacon400, fontWeight = FontWeight.Bold)
                            response.nextSteps.forEachIndexed { idx, step ->
                                Text("${idx + 1}. $step", color = Mist100, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            item {
                Text("Navigator Picks:", color = Beacon500, style = MaterialTheme.typography.titleMedium)
            }

            val matchingResources = response.picks.mapNotNull { pick ->
                val res = resources.find { it.id == pick.id }
                if (res != null) {
                    res to pick.why
                } else {
                    null
                }
            }

            if (matchingResources.isEmpty()) {
                item {
                    Text("No direct database matches recommended.", color = Mist400)
                }
            } else {
                items(matchingResources) { (res, why) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink900, RoundedCornerShape(8.dp))
                            .border(1.dp, Ink700, RoundedCornerShape(8.dp))
                            .clickable { onNavigateToDetail(res.id) }
                            .padding(12.dp)
                    ) {
                        Text(res.name, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 16.sp)
                        Text(why, color = Beacon400, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                        Text("📍 ${res.address} • ${res.neighborhood}", fontSize = 12.sp, color = Mist400)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- Ebt Screen (Restaurant Meals Program) ---
@Composable
fun EbtScreen(
    viewModel: CompassViewModel
) {
    val context = LocalContext.current
    val rmpLocations by viewModel.allRmpLocations.collectAsState()
    val userLocation by viewModel.userLocation
    val sortByDistance by viewModel.sortByDistance
    var showLocationDialog by remember { mutableStateOf(false) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCuisine by remember { mutableStateOf("all") }
    var selectedNeighborhood by remember { mutableStateOf("all") }
    var filterFavorites by remember { mutableStateOf(false) }
    var filterChains by remember { mutableStateOf(false) }
    var filterOpenNow by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCardCodingNotice by remember { mutableStateOf(true) }
    var isCardCodingCollapsed by remember { mutableStateOf(false) }

    val cuisines = listOf("all" to "All Cuisines") + 
            rmpLocations.map { it.cuisine }.filter { it.isNotBlank() }.distinct().sorted().map { it to it.replaceFirstChar { c -> c.uppercase() } }

    val neighborhoods = listOf("all" to "All Neighborhoods") +
            rmpLocations.map { it.neighborhood }.filter { it.isNotBlank() }.distinct().sorted().map { it to it }

    val filteredList = remember(rmpLocations, searchQuery, selectedCuisine, selectedNeighborhood, filterFavorites, filterChains, filterOpenNow, userLocation, sortByDistance, viewModel.demographicPresets.value, viewModel.accessibilityMobilityMode.value, viewModel.dietaryPresets.value) {
        val list = rmpLocations.filter { loc ->
            val matchesSearch = loc.name.contains(searchQuery, ignoreCase = true) || 
                    loc.address.contains(searchQuery, ignoreCase = true) ||
                    loc.neighborhood.contains(searchQuery, ignoreCase = true) ||
                    loc.cuisine.contains(searchQuery, ignoreCase = true) ||
                    loc.notes.contains(searchQuery, ignoreCase = true)
            val matchesCuisine = selectedCuisine == "all" || loc.cuisine.equals(selectedCuisine, ignoreCase = true)
            val matchesNeighborhood = selectedNeighborhood == "all" || loc.neighborhood.equals(selectedNeighborhood, ignoreCase = true)
            val matchesFav = !filterFavorites || loc.favorite
            val matchesChain = !filterChains || loc.chain
            val matchesOpen = !filterOpenNow || isResourceOpen(loc.open24, loc.hours)

            val matchesDemographic = PresetMatcher.matchesRmpDemographic(loc, viewModel.demographicPresets.value)
            val matchesAccessibility = PresetMatcher.matchesRmpAccessibility(loc, viewModel.accessibilityMobilityMode.value)
            val matchesDietary = PresetMatcher.matchesRmpDietary(loc, viewModel.dietaryPresets.value)

            matchesSearch && matchesCuisine && matchesNeighborhood && matchesFav && matchesChain && matchesOpen && matchesDemographic && matchesAccessibility && matchesDietary
        }
        if (sortByDistance && userLocation != null) {
            list.sortedBy { loc ->
                viewModel.getDistanceToRmp(loc) ?: Double.MAX_VALUE
            }
        } else {
            list
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "EBT Restaurant Meals",
                        style = MaterialTheme.typography.titleLarge,
                        color = Beacon500,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "CalFresh cardholders can get prepared hot meals at participating SF outlets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist400
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink900),
                    border = BorderStroke(1.dp, Ink700),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Outlet", tint = Beacon500, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Spot", color = Mist100, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location & Proximity Bar
            LocationBar(
                viewModel = viewModel,
                onOpenDialog = { showLocationDialog = true }
            )

            ActivePresetsBanner(
                viewModel = viewModel,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // RMP Hotline & Info Banner (Dismissible & Collapsible)
            if (showCardCodingNotice) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Ink900),
                    border = BorderStroke(1.dp, Ink700),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isCardCodingCollapsed = !isCardCodingCollapsed }
                            ) {
                                Text(
                                    "💳 Card coding required",
                                    color = Beacon400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isCardCodingCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = if (isCardCodingCollapsed) "Expand notice" else "Collapse notice",
                                    tint = Mist400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:8553555757"))
                                        try {
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open dialer: ${e.localizedMessage ?: "No phone app"}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Beacon500),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = OnAccentColor, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Helpline", color = OnAccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { showCardCodingNotice = false },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss notice",
                                        tint = Mist400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (!isCardCodingCollapsed) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "CalFresh cards must be RMP coded for hot meals (homeless, seniors 60+, disabled).",
                                color = Mist400,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Search
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search outlets, Subway, pizza, tacos, dumplings...", color = Mist400) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Mist400) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Mist400, modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Ink900,
                unfocusedContainerColor = Ink900,
                focusedTextColor = Mist100,
                unfocusedTextColor = Mist100,
                cursorColor = Beacon500,
                focusedIndicatorColor = Beacon500
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        // Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cuisine Dropdown
            var cDropdownExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { cDropdownExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedCuisine != "all") Beacon500 else Ink900),
                    border = BorderStroke(1.dp, Ink700),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                ) {
                    Text(
                        cuisines.find { it.first == selectedCuisine }?.second ?: "Cuisine",
                        color = if (selectedCuisine != "all") OnAccentColor else Mist100,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        "down",
                        tint = if (selectedCuisine != "all") OnAccentColor else Mist400,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = cDropdownExpanded,
                    onDismissRequest = { cDropdownExpanded = false },
                    modifier = Modifier.background(Ink900)
                ) {
                    cuisines.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name, color = Mist100) },
                            onClick = {
                                selectedCuisine = id
                                cDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Neighborhood Dropdown
            var nDropdownExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1.2f)) {
                Button(
                    onClick = { nDropdownExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedNeighborhood != "all") Beacon500 else Ink900),
                    border = BorderStroke(1.dp, Ink700),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                ) {
                    Text(
                        neighborhoods.find { it.first == selectedNeighborhood }?.second ?: "Area",
                        color = if (selectedNeighborhood != "all") OnAccentColor else Mist100,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        "down",
                        tint = if (selectedNeighborhood != "all") OnAccentColor else Mist400,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = nDropdownExpanded,
                    onDismissRequest = { nDropdownExpanded = false },
                    modifier = Modifier.background(Ink900)
                ) {
                    neighborhoods.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name, color = Mist100) },
                            onClick = {
                                selectedNeighborhood = id
                                nDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Open Now toggle
            IconButton(
                onClick = { filterOpenNow = !filterOpenNow },
                modifier = Modifier
                    .size(44.dp)
                    .background(if (filterOpenNow) Emerald500 else Ink900, RoundedCornerShape(8.dp))
                    .border(1.dp, Ink700, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Open Now",
                    tint = if (filterOpenNow) OnAccentColor else Mist400,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Chains toggle
            IconButton(
                onClick = { filterChains = !filterChains },
                modifier = Modifier
                    .size(44.dp)
                    .background(if (filterChains) Beacon500 else Ink900, RoundedCornerShape(8.dp))
                    .border(1.dp, Ink700, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = "Chains only",
                    tint = if (filterChains) OnAccentColor else Mist400,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Favorites toggle
            IconButton(
                onClick = { filterFavorites = !filterFavorites },
                modifier = Modifier
                    .size(44.dp)
                    .background(if (filterFavorites) Beacon500 else Ink900, RoundedCornerShape(8.dp))
                    .border(1.dp, Ink700, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    if (filterFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorites",
                    tint = if (filterFavorites) OnAccentColor else Mist400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Count indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (sortByDistance && userLocation != null) "${filteredList.size} participating EBT outlets (nearest first)" else "${filteredList.size} participating EBT outlets",
                color = Mist400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (selectedCuisine != "all" || selectedNeighborhood != "all" || filterFavorites || filterChains || filterOpenNow || searchQuery.isNotBlank()) {
                Text(
                    text = "Reset filters",
                    color = Beacon400,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .clickable {
                            searchQuery = ""
                            selectedCuisine = "all"
                            selectedNeighborhood = "all"
                            filterFavorites = false
                            filterChains = false
                            filterOpenNow = false
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                )
            }
        }

        // List of RMP Cards
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No outlets found matching filters.", color = Mist400, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedCuisine = "all"
                                    selectedNeighborhood = "all"
                                    filterFavorites = false
                                    filterChains = false
                                    filterOpenNow = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Ink900),
                                border = BorderStroke(1.dp, Ink700)
                            ) {
                                Text("Clear all filters", color = Mist100, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { loc ->
                    RmpCard(
                        location = loc,
                        distanceMiles = viewModel.getDistanceToRmp(loc),
                        onFavoriteClick = { viewModel.toggleRmpFavorite(loc) },
                        onNotesSaved = { note -> viewModel.saveRmpPersonalNotes(loc.id, note) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showLocationDialog) {
        LocationAnchorDialog(
            viewModel = viewModel,
            onDismiss = { showLocationDialog = false }
        )
    }

    // Add Outlet Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var neighborhood by remember { mutableStateOf("") }
        var cuisine by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var hoursText by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var tips by remember { mutableStateOf("") }
        var isChain by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add EBT RMP Outlet", color = Mist100, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Add a San Francisco restaurant that accepts EBT cards for hot meals.", color = Mist400, fontSize = 12.sp)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Restaurant Name *", color = Mist400) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address", color = Mist400) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = neighborhood,
                            onValueChange = { neighborhood = it },
                            label = { Text("Neighborhood", color = Mist400) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                        )
                        OutlinedTextField(
                            value = cuisine,
                            onValueChange = { cuisine = it },
                            label = { Text("Cuisine", color = Mist400) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone", color = Mist400) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                        )
                        OutlinedTextField(
                            value = hoursText,
                            onValueChange = { hoursText = it },
                            label = { Text("Hours", color = Mist400) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                        )
                    }
                    OutlinedTextField(
                        value = tips,
                        onValueChange = { tips = it },
                        label = { Text("Tips for EBT users", color = Mist400) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isChain,
                            onCheckedChange = { isChain = it },
                            colors = CheckboxDefaults.colors(checkedColor = Beacon500, checkmarkColor = OnAccentColor)
                        )
                        Text("Is a chain brand (e.g. Subway, Taco Bell)", color = Mist100, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addRmpLocation(
                                name = name,
                                address = address,
                                neighborhood = neighborhood,
                                cuisine = cuisine,
                                chain = isChain,
                                phone = phone,
                                hoursText = hoursText,
                                notes = notes,
                                tips = tips
                            )
                            showAddDialog = false
                            Toast.makeText(context, "Added $name to RMP directory", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor)
                ) {
                    Text("Save Outlet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Mist400)
                }
            },
            containerColor = Ink900
        )
    }
}

// --- Add Screen (Intake / Parser & Create Guide form) ---
@Composable
fun AddScreen(
    viewModel: CompassViewModel,
    onNavigateToDetail: (Int) -> Unit
) {
    val context = LocalContext.current
    val isParsing by viewModel.isParsing
    val isProcessingImage by viewModel.isProcessingImage
    val draft = viewModel.draftResource.value
    val parseError by viewModel.parseError
    val isSaving by viewModel.isSavingResource
    val selectedImageUri by viewModel.selectedImageUri
    val selectedImageBase64 by viewModel.selectedImageBase64

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoSelected(context, uri)
        }
    }

    var viewingImageUri by remember { mutableStateOf<Uri?>(null) }

    if (viewingImageUri != null) {
        FullScreenImageViewer(
            imageUri = viewingImageUri!!,
            onDismiss = { viewingImageUri = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
    ) {
        if (draft == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Verify & Add Resource",
                        style = MaterialTheme.typography.titleLarge,
                        color = Beacon500,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Ingest street flyers, clinic schedules, or food pantry brochures using your camera or photo library, or paste unstructured text. The Navigator AI extracts structured entries for your verification.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Mist400
                    )
                }

                // --- Visual Flyer Photo Scanner (Compact) ---
                item {
                    if (selectedImageUri == null) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_flyer_photo_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Ink900,
                                contentColor = Mist100
                            ),
                            border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Beacon500, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Scan Flyer or Brochure Photo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    } else {
                        // Image attached state (compact)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Ink900),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "Selected flyer photo",
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Ink700)
                                            .clickable { viewingImageUri = selectedImageUri },
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Flyer Photo Attached", color = Mist100, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text(
                                            if (isProcessingImage) "Optimizing image..." else "Ready for AI extraction",
                                            color = if (isProcessingImage) Beacon500 else Mist400,
                                            fontSize = 11.sp
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.clearSelectedPhoto() },
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Remove", color = Rose500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Button(
                                    onClick = { viewModel.parseFlyerPhoto() },
                                    enabled = !isParsing && !isProcessingImage && !selectedImageBase64.isNullOrBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("extract_flyer_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(10.dp)
                                ) {
                                    if (isParsing) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = OnAccentColor, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Extracting with Gemini AI...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Extract from Flyer Photo via AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Raw Text / Notes Ingestion Card ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Or Paste Text / Flyer Notes",
                                style = MaterialTheme.typography.titleSmall,
                                color = Mist200,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (viewModel.addRawText.value.isNotBlank()) {
                                Text(
                                    "Clear",
                                    color = Mist400,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { viewModel.addRawText.value = "" }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = viewModel.addRawText.value,
                            onValueChange = { viewModel.addRawText.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .testTag("add_input"),
                            placeholder = {
                                Text(
                                    if (selectedImageUri != null)
                                        "Optional extra notes or context to accompany your photo..."
                                    else
                                        "Paste flyer text, SMS notice, website snippet, or clinic hours...",
                                    color = Mist400,
                                    fontSize = 13.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Beacon500,
                                unfocusedBorderColor = Ink700,
                                focusedTextColor = Mist100,
                                unfocusedTextColor = Mist100,
                                focusedContainerColor = Ink900,
                                unfocusedContainerColor = Ink900
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // --- Action Buttons ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedImageUri != null && !selectedImageBase64.isNullOrBlank()) {
                                    viewModel.parseFlyerPhoto()
                                } else {
                                    viewModel.parseDraft()
                                }
                            },
                            enabled = !isParsing && (viewModel.addRawText.value.isNotBlank() || (!selectedImageBase64.isNullOrBlank())),
                            modifier = Modifier.weight(1f).testTag("parse_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isParsing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnAccentColor, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    if (selectedImageUri != null) "Parse Photo & Text" else "Parse Text via AI",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.createManualBlankDraft() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Ink900, contentColor = Mist100),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Ink700)
                        ) {
                            Text("Manual Blank")
                        }
                    }
                }

                // --- Quick Sample Flyers for Testing ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Try a Sample Flyer Notice:", color = Mist400, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                AssistChip(
                                    onClick = {
                                        viewModel.addRawText.value = """
                                            St. Anthony Medical Clinic - Open Walk-in Hours
                                            Address: 150 Golden Gate Ave, San Francisco, CA 94102
                                            Tenderloin neighborhood. Phone: (415) 241-8320
                                            Free primary medical care, pediatrics, and asthma care for uninsured SF residents.
                                            Hours: Monday-Friday 8:00am - 12:00pm, 1:00pm - 4:30pm. Closed weekends.
                                            Cost: 100% Free. No insurance or ID strictly required for emergency intake.
                                            Languages: English, Spanish, Cantonese.
                                            Tips: Line forms at 7:30am on Golden Gate Ave for same-day triage slips.
                                        """.trimIndent()
                                    },
                                    label = { Text("🏥 Clinic Schedule Flyer", color = Mist200, fontSize = 12.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Ink900),
                                    border = BorderStroke(1.dp, Ink700)
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        viewModel.addRawText.value = """
                                            Mission Community Food Pantry - Weekly Grocery Distribution
                                            Location: 2929 19th St, San Francisco, CA 94110 (Mission District)
                                            Fresh produce, pantry staples, and hot soup kits.
                                            Distribution Days: Tuesdays & Thursdays from 10:00am to 2:00pm, Saturdays 9:00am - 1:00pm.
                                            Eligibility: All low-income families & individuals welcome. Bring your own reusable bags.
                                            Cost: Free. Contact: (415) 555-0199.
                                            Languages: Spanish and English speaking volunteers.
                                            Tips: Enter through courtyard gate on 19th St.
                                        """.trimIndent()
                                    },
                                    label = { Text("🍎 Food Pantry Flyer", color = Mist200, fontSize = 12.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Ink900),
                                    border = BorderStroke(1.dp, Ink700)
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        viewModel.addRawText.value = """
                                            Lava Mae SF Mobile Showers & Hygiene Trailer
                                            Tenderloin stop: 330 Ellis St, San Francisco, CA
                                            Free 15-minute hot showers, fresh towels, hygiene kits, dental kits, and clean socks.
                                            Schedule: Wednesdays and Fridays 9:00am to 1:00pm.
                                            Requirements: Walk-in sign up starting at 8:45am. First come, first served.
                                            Cost: Free. Phone: (415) 359-2454.
                                        """.trimIndent()
                                    },
                                    label = { Text("🚿 Mobile Hygiene Schedule", color = Mist200, fontSize = 12.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Ink900),
                                    border = BorderStroke(1.dp, Ink700)
                                )
                            }
                        }
                    }
                }

                if (parseError != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .border(1.dp, Rose500, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 16.sp)
                            Text(parseError!!, color = Mist100, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        } else {
            // Edit review draft screen
            var name by remember { mutableStateOf(draft.name) }
            var category by remember { mutableStateOf(draft.category) }
            var summary by remember { mutableStateOf(draft.summary) }
            var description by remember { mutableStateOf(draft.description) }
            var address by remember { mutableStateOf(draft.address) }
            var neighborhood by remember { mutableStateOf(draft.neighborhood) }
            var phone by remember { mutableStateOf(draft.phone) }
            var cost by remember { mutableStateOf(draft.cost) }
            var tips by remember { mutableStateOf(draft.aiTips) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Review Draft Entry",
                            style = MaterialTheme.typography.titleMedium,
                            color = Beacon500,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { viewModel.resetDraft() },
                            colors = ButtonDefaults.buttonColors(containerColor = Ink900, contentColor = Rose500)
                        ) {
                            Text("Reset")
                        }
                    }
                }

                if (selectedImageUri != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Ink900),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Source flyer photo",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewingImageUri = selectedImageUri },
                                    contentScale = ContentScale.Crop
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Beacon500, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Extracted from Flyer Photo", color = Mist100, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Text("Fields pre-filled via Gemini vision model. Edit any corrections below.", color = Mist400, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Resource Name", color = Mist400) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                }

                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (food, shelter, hygiene, connect, etc.)", color = Mist400) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                }

                item {
                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = { Text("One-sentence summary", color = Mist400) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Full description", color = Mist400) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Street Address", color = Mist400) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                }

                item {
                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = { Text("Neighborhood", color = Mist400) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number", color = Mist400) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                }

                item {
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = { Text("Cost (Free, Low Cost, etc.)", color = Mist400) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                }

                item {
                    OutlinedTextField(
                        value = tips,
                        onValueChange = { tips = it },
                        label = { Text("Navigator Tips", color = Mist400) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                }

                item {
                    OutlinedTextField(
                        value = viewModel.personalNotes.value,
                        onValueChange = { viewModel.personalNotes.value = it },
                        label = { Text("Personal Notes (Private to you)", color = Mist400) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                }

                item {
                    Button(
                        onClick = {
                            viewModel.draftResource.value = draft.copy(
                                name = name,
                                category = category,
                                summary = summary,
                                description = description,
                                address = address,
                                neighborhood = neighborhood,
                                phone = phone,
                                cost = cost,
                                aiTips = tips
                            )
                            viewModel.saveDraftToDatabase { newId ->
                                onNavigateToDetail(newId)
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = OnAccentColor)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnAccentColor, strokeWidth = 2.dp)
                        } else {
                            Text("Verify & Save to Guide", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

// --- Day Screen (Checklist and Route planner) ---
@Composable
fun DayScreen(
    viewModel: CompassViewModel,
    onNavigateToFind: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Route Checklist",
                        style = MaterialTheme.typography.titleLarge,
                        color = Beacon500,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Manage your appointments and drop-in errands.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Mist400
                    )
                }
                IconButton(
                    onClick = { showAddTaskDialog = true },
                    modifier = Modifier.background(Beacon500, RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add task", tint = Ink950)
                }
            }
        }

        val completedTasks = tasks.filter { it.done }
        val pendingTasks = tasks.filter { !it.done }

        if (tasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Your checklist is empty.", color = Mist400)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAddTaskDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Ink900, contentColor = Beacon500)
                        ) {
                            Text("Add your first task")
                        }
                    }
                }
            }
        } else {
            if (pendingTasks.isNotEmpty()) {
                item {
                    Text("In Progress", color = Beacon400, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                items(pendingTasks) { task ->
                    TaskRow(task = task, viewModel = viewModel)
                }
            }

            if (completedTasks.isNotEmpty()) {
                item {
                    Text("Completed", color = Mist400, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                items(completedTasks) { task ->
                    TaskRow(task = task, viewModel = viewModel)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var kind by remember { mutableStateOf("errand") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            containerColor = Ink900,
            title = { Text("New Task Element", color = Beacon500) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title", color = Mist400) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Helper notes", color = Mist400) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Kind of Task", color = Mist400, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("benefit", "errand", "appointment").forEach { type ->
                            val isSelected = kind == type
                            Button(
                                onClick = { kind = type },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Beacon500 else Ink800),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(type.replaceFirstChar { it.uppercase() }, color = if (isSelected) OnAccentColor else Mist100, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addTask(title, kind, notes)
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor)
                ) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel", color = Mist400)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRow(task: Task, viewModel: CompassViewModel) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    viewModel.deleteTask(task)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    viewModel.toggleTask(task)
                    false // Return false so it springs back
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Rose500
                    SwipeToDismissBoxValue.StartToEnd -> Emerald500
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                }, label = "swipeColor"
            )
            val icon = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.DeleteOutline
                SwipeToDismissBoxValue.StartToEnd -> if (task.done) Icons.Default.Undo else Icons.Default.Check
                else -> Icons.Default.DeleteOutline
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(color, RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (direction != SwipeToDismissBoxValue.Settled) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
            }
        },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Ink900, RoundedCornerShape(8.dp))
                    .border(1.dp, Ink700, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleTask(task) }) {
                    Icon(
                        imageVector = if (task.done) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Toggle task done",
                        tint = if (task.done) Emerald500 else Beacon500
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        color = if (task.done) Mist400 else Mist100,
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (task.notes.isNotBlank()) {
                        Text(
                            text = task.notes,
                            color = Mist400,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    // Kind label
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Ink800, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(task.kind.replaceFirstChar { it.uppercase() }, color = Beacon400, fontSize = 10.sp)
                    }
                }
            }
        }
    )
}

// --- Info Screen ---
@Composable
fun InfoScreen() {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Info & Help Hotlines",
                style = MaterialTheme.typography.titleLarge,
                color = Beacon500,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Useful contacts and phone lines in San Francisco for shelter, healthcare, and peer support.",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist400
            )
        }

        val hotlines = listOf(
            Hotline("311 General City Info", "311", "Call for shelter queue, street cleaning, general help"),
            Hotline("Mobile Crisis Support", "415-970-4000", "Immediate psychiatric crisis intervention line"),
            Hotline("Sutter Health Nurse Line", "877-384-5178", "24/7 medical advice line"),
            Hotline("Homeless Youth Alliance", "415-318-6384", "Help, needle exchange & drop-in for youths"),
            Hotline("Mental Health Warm Line", "855-845-7415", "Non-emergency peer counselors")
        )

        items(hotlines) { hl ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Ink900, RoundedCornerShape(8.dp))
                    .border(1.dp, Ink700, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(hl.name, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Text(hl.desc, color = Mist400, fontSize = 12.sp)
                    Text(hl.phone, color = Beacon400, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${hl.phone}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open dialer: ${e.localizedMessage ?: "No phone app"}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Ink800, RoundedCornerShape(22.dp))
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Dial", tint = Beacon500)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Integrated Online Data Repositories",
                style = MaterialTheme.typography.titleLarge,
                color = Beacon500,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Compass SF imports and syncs verified resource listings from the primary online directory systems in San Francisco:",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist400
            )
        }

        val onlineSources = listOf(
            OnlineDirectorySource(
                name = "SF Service Guide (ShelterTech)",
                org = "ShelterTech Non-Profit Open Platform",
                desc = "Civic technology platform mapping street-level services, youth resources, and verified walk-in hours across SF.",
                url = "https://sfserviceguide.org",
                tag = "SF Service Guide"
            ),
            OnlineDirectorySource(
                name = "DataSF (data.sf.gov)",
                org = "City & County of San Francisco Open Data",
                desc = "Official municipal datasets for SFDPH public health clinics, emergency shelters, and city services.",
                url = "https://data.sf.gov",
                tag = "DataSF Portal"
            ),
            OnlineDirectorySource(
                name = "211 Bay Area (Eden I&R)",
                org = "Eden Information & Referral Inc.",
                desc = "Comprehensive 9-county regional directory for CalFresh/food distribution, legal assistance, and social safety net programs.",
                url = "https://www.211bayarea.org",
                tag = "2-1-1 Bay Area"
            )
        )

        items(onlineSources) { src ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(src.name, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(src.tag, color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(src.org, color = Beacon400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(src.desc, color = Mist400, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(src.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Ink800),
                            border = BorderStroke(1.dp, Ink700),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Visit Portal ↗", color = Beacon400, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About Compass SF", fontWeight = FontWeight.Bold, color = Beacon500)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Compass SF is designed for street-level navigation in San Francisco. It coordinates verified free meal centers, drop-in hygiene hubs, emergency shelter assess points, and EBT-hot-meal restaurants. It works 100% offline with a built-in pre-loaded SQLite database, syncing updates seamlessly on-demand.",
                        color = Mist400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

data class Hotline(val name: String, val phone: String, val desc: String)
data class OnlineDirectorySource(val name: String, val org: String, val desc: String, val url: String, val tag: String)

// --- Settings Hub & Sub-Pages ---
enum class SettingsSubPage(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val emoji: String,
    val route: String
) {
    HUB("hub", "Settings Hub", "Select a category to customize options", Icons.Default.Settings, "⚙️", "settings"),
    APPEARANCE("appearance", "Appearance & Visuals", "Themes, accent colors, text scale, map style & launcher icons", Icons.Default.Palette, "🎨", "settings/appearance"),
    WORKSPACE("workspace", "Navigation & Workspace", "Startup screen, bottom bar tabs, anchors & workspace reset", Icons.Default.Navigation, "🧭", "settings/workspace"),
    PERSONALIZATION("personalization", "Personalization & Profile", "Demographic filters, needs, dietary rules & mobility mode", Icons.Default.Person, "👤", "settings/personalization"),
    SEARCH("search", "Search & Resource Filters", "Open-only defaults, physical address filters & incognito search", Icons.Default.Search, "🔍", "settings/search"),
    MAP("map", "Map & Cartography", "Layer toggles, clustering density, reduced motion & anchors", Icons.Default.Map, "🗺️", "settings/map"),
    AI("ai", "AI Navigator", "Guide persona tone, online/offline mode, API key & test", Icons.Default.AutoAwesome, "🤖", "settings/ai"),
    DATA("data", "Data, Privacy & Storage", "Civic feeds, photo cache, JSON backup & factory re-seed", Icons.Default.Storage, "💾", "settings/data"),
    ABOUT("about", "About & Help", "App version, data sources, crisis hotlines & offline guide", Icons.Default.Info, "ℹ️", "settings/about")
}

@Composable
fun SettingsScreen(
    viewModel: CompassViewModel,
    onNavigateToSubpage: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val resources by viewModel.allResources.collectAsState()
    val rmpLocations by viewModel.allRmpLocations.collectAsState()
    val visits by viewModel.allVisits.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()

    var activeSubPage by remember { mutableStateOf(SettingsSubPage.HUB) }

    // Intercept back button on local sub-pages
    if (onNavigateToSubpage == null && activeSubPage != SettingsSubPage.HUB) {
        BackHandler {
            activeSubPage = SettingsSubPage.HUB
        }
    }

    var showPasteJsonDialog by remember { mutableStateOf(false) }
    var pastedJsonText by remember { mutableStateOf("") }
    var showPhotoCacheInspectionDialog by remember { mutableStateOf(false) }
    var showReseedConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshPhotoCacheAudit(context)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = viewModel.getExportJson()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.setExportSuccess("Backup saved successfully!")
                    Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                    if (!text.isNullOrBlank()) {
                        viewModel.loadImportJson(text)
                    } else {
                        Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read file: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (onNavigateToSubpage == null && activeSubPage != SettingsSubPage.HUB) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { activeSubPage = SettingsSubPage.HUB },
                        border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon500),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("settings_back_to_hub")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings Hub",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Settings Hub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(activeSubPage.emoji, fontSize = 14.sp)
                            Text(activeSubPage.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Mist100)
                        }
                        Text(activeSubPage.subtitle, fontSize = 11.sp, color = Mist400, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else if (onNavigateToSubpage == null || activeSubPage == SettingsSubPage.HUB) {
                // Top Welcome Banner for Settings Hub
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        "Settings & Preferences",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Beacon500,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Categorized preferences with live preview & instant controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist400
                    )
                }
            }

            if (onNavigateToSubpage != null) {
                SettingsHubContent(
                    viewModel = viewModel,
                    onSelectCategory = { page ->
                        onNavigateToSubpage(page.route)
                    }
                )
            } else {
                when (activeSubPage) {
                    SettingsSubPage.HUB -> {
                        SettingsHubContent(
                            viewModel = viewModel,
                            onSelectCategory = { activeSubPage = it }
                        )
                    }
                    SettingsSubPage.APPEARANCE -> AppearanceSettingsPage(viewModel = viewModel)
                    SettingsSubPage.WORKSPACE -> NavigationSettingsPage(viewModel = viewModel)
                    SettingsSubPage.PERSONALIZATION -> ProfileSettingsPage(viewModel = viewModel)
                    SettingsSubPage.SEARCH -> SearchSettingsPage(viewModel = viewModel)
                    SettingsSubPage.MAP -> MapSettingsPage(viewModel = viewModel)
                    SettingsSubPage.AI -> AiSettingsPage(viewModel = viewModel)
                    SettingsSubPage.DATA -> StorageSettingsPage(
                        viewModel = viewModel,
                        resources = resources,
                        rmpLocations = rmpLocations,
                        visits = visits,
                        tasks = tasks,
                        onExportClick = { exportLauncher.launch("compass_sf_backup_${System.currentTimeMillis()}.json") },
                        onImportClick = { importLauncher.launch(arrayOf("application/json")) },
                        onOpenPasteJson = { showPasteJsonDialog = true },
                        onOpenPhotoAudit = { showPhotoCacheInspectionDialog = true },
                        onOpenReseedConfirm = { showReseedConfirmDialog = true }
                    )
                    SettingsSubPage.ABOUT -> AboutHelpSettingsPage(viewModel = viewModel)
                }
            }
        }
    }

    // Dialogs
    if (showPasteJsonDialog) {
        AlertDialog(
            onDismissRequest = { showPasteJsonDialog = false },
            containerColor = Ink900,
            title = { Text("Paste JSON Backup Data", color = Beacon500, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste a valid JSON backup exported from Compass SF:", color = Mist300, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pastedJsonText,
                        onValueChange = { pastedJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        placeholder = { Text("Paste JSON string here...", color = Mist400, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pastedJsonText.isNotBlank()) {
                            viewModel.loadImportJson(pastedJsonText)
                            showPasteJsonDialog = false
                            pastedJsonText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor)
                ) { Text("Import JSON Data") }
            },
            dismissButton = {
                TextButton(onClick = { showPasteJsonDialog = false }) { Text("Cancel", color = Mist400) }
            }
        )
    }

    if (showPhotoCacheInspectionDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoCacheInspectionDialog = false },
            containerColor = Ink900,
            title = { Text("Offline Photo Cache Audit", color = Mist100, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val photos = viewModel.cachedPhotosList.value
                    Text("Cached Flyer Photos: ${photos.size}", color = Beacon400, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (photos.isEmpty()) {
                        Text("No flyer photos stored in cache.", color = Mist300, fontSize = 12.sp)
                    } else {
                        photos.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth().background(Ink800, RoundedCornerShape(6.dp)).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.fileName, color = Mist100, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Size: ${p.sizeBytes / 1024} KB", color = Mist400, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPhotoCacheInspectionDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor)) { Text("Close") }
            }
        )
    }

    if (showReseedConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showReseedConfirmDialog = false },
            containerColor = Ink900,
            title = { Text("Confirm Factory Re-seed", color = Rose500, fontWeight = FontWeight.Bold) },
            text = { Text("This will restore the original baseline SF resources and EBT restaurants from seed data. Your personal notes, custom resources, and task checklists will NOT be lost.", color = Mist300, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.triggerFactoryReseed {
                            showReseedConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Color.White)
                ) { Text("Confirm & Re-seed") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReseedConfirmDialog = false }, border = BorderStroke(1.dp, Ink700)) { Text("Cancel", fontSize = 13.sp) }
            }
        )
    }
}

// --- Settings Hub View ---
@Composable
fun SettingsHubContent(
    viewModel: CompassViewModel,
    onSelectCategory: (SettingsSubPage) -> Unit
) {
    val activeMode = viewModel.currentThemeMode.value
    val activeAccent = viewModel.currentAccentColor.value
    val activeFontScale = viewModel.currentFontScale.value
    val demographicPresets = viewModel.demographicPresets.value
    val mobilityMode = viewModel.accessibilityMobilityMode.value
    val dietaryPresets = viewModel.dietaryPresets.value
    val startupRoute = viewModel.startupScreenRoute.value
    val defaultAnchor = viewModel.defaultNeighborhoodAnchorId.value
    val hasDisabledSources = viewModel.hasDisabledDataSources
    val aiStyle = viewModel.aiNavigatorStyle.value
    val aiOffline = viewModel.aiOfflineOnly.value
    val incognito = viewModel.incognitoSearchMode.value
    val recents = viewModel.recentSearches.value
    val cacheBytes = viewModel.totalPhotoCacheBytes.value

    val subPages = listOf(
        Triple(
            SettingsSubPage.APPEARANCE,
            "${activeMode.displayName} • ${activeAccent.displayName} • ${viewModel.currentAppIconKey.value.uppercase()}",
            "Live preview card, color themes, font scaling & launcher icon switcher"
        ),
        Triple(
            SettingsSubPage.WORKSPACE,
            "Start: ${startupRoute.replaceFirstChar { it.uppercase() }} • Anchor: ${defaultAnchor.replaceFirstChar { it.uppercase() }}",
            "Startup screen, bottom navigation bar active items & neighborhood anchor"
        ),
        Triple(
            SettingsSubPage.PERSONALIZATION,
            "${demographicPresets.size} Presets • ${if (mobilityMode) "Mobility On" else "Standard"} • ${dietaryPresets.size} Dietary",
            "Primary need priorities, demographic filters, dietary rules & mobility mode"
        ),
        Triple(
            SettingsSubPage.SEARCH,
            "Incognito: ${if (incognito) "ENABLED" else "Off"} • Physical Address Filters",
            "Open-only search defaults, non-location helpline filters & query history"
        ),
        Triple(
            SettingsSubPage.MAP,
            "Transit, Neighborhoods & Landmarks Layers",
            "Vector map overlays, marker clustering density & reduced motion"
        ),
        Triple(
            SettingsSubPage.AI,
            "${aiStyle.title} • ${if (aiOffline) "Offline Gemini" else "Online Gemini"}",
            "Guide persona tone, online/offline Gemini mode, API key & server test"
        ),
        Triple(
            SettingsSubPage.DATA,
            "Photo Cache: ${(cacheBytes / 1024 / 1024)}MB • Feed Toggles & JSON Backup",
            "Civic feed toggles, offline photo cache, factory re-seed & JSON export/import"
        ),
        Triple(
            SettingsSubPage.ABOUT,
            "Compass SF v3.0.0 • 100% Local Privacy",
            "App version info, open data sources, local storage guarantee & emergency hotlines"
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(subPages) { (page, summaryText, description) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectCategory(page) }
                    .testTag("settings_category_${page.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Beacon500.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = page.title,
                            tint = Beacon500,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(page.emoji, fontSize = 14.sp)
                            Text(
                                page.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Mist100
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(6.dp))
                                .border(1.dp, Ink700, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                summaryText,
                                color = Beacon400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            description,
                            color = Mist400,
                            fontSize = 11.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open ${page.title}",
                        tint = Mist400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


// --- Appearance & Visuals Sub-Page (with Live Preview anchored at top) ---
@Composable
fun AppearanceSettingsPage(
    viewModel: CompassViewModel
) {
    val activeMode = viewModel.currentThemeMode.value
    val activeAccent = viewModel.currentAccentColor.value
    val activeFontScale = viewModel.currentFontScale.value
    val highContrast = viewModel.highContrastEnabled.value
    val compactListing = viewModel.compactListingEnabled.value
    val activeAppIcon = viewModel.currentAppIconKey.value

    val iconChoices = listOf(
        "classic" to "Compass Rose",
        "beacon" to "Beacon Pulse",
        "bridge" to "Golden Gate",
        "lantern" to "Lantern Light",
        "wayfinder" to "Wayfinder"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP ANCHORED LIVE PREVIEW CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(
                    if (highContrast) 2.dp else 1.dp,
                    if (highContrast) Beacon500 else Ink700
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("appearance_live_preview_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("✨ LIVE PREVIEW", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Beacon500, letterSpacing = 1.sp)
                        }

                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(12.dp))
                                .border(1.dp, Ink700, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "${activeMode.displayName} • ${activeAccent.displayName} • ${activeFontScale.displayName}",
                                color = Mist100,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Ink800),
                        border = BorderStroke(1.dp, if (highContrast) Beacon500 else Ink700),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "GLIDE — Daily Free Meals",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Mist100
                                )
                                Text("❤️", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "330 Ellis St • Tenderloin • Breakfast 8am, Lunch 12pm",
                                color = Mist400,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Beacon500, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Food & Dining", color = OnAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Emerald500, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Open Now", color = OnAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor)
                        ) {
                            Text("Primary Action", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Ink700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist200)
                        ) {
                            Text("Secondary Action", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Atmosphere / Theme Modes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Atmosphere & Theme Mode", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Text("Select UI theme tone", color = Mist400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = activeMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Beacon500.copy(alpha = 0.18f) else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectThemeMode(mode) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectThemeMode(mode) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                                )
                                Column {
                                    Text(mode.displayName, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                                    Text(mode.subtitle, color = Mist400, fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Highlight Accent Colors
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Highlight Accent Colors", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Text("Choose highlight color for buttons, badges, & indicators", color = Mist400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    AppAccentColor.entries.forEach { accent ->
                        val isSelected = activeAccent == accent
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Beacon500.copy(alpha = 0.18f) else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectAccentColor(accent) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectAccentColor(accent) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                                )
                                Text(accent.displayName, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(accent.primary, CircleShape)
                                    .border(1.dp, Mist100.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Text Scale
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Text Scale & Readability", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Text("Adjust font size for street legibility", color = Mist400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppFontScale.entries.forEach { scale ->
                            val isSelected = activeFontScale == scale
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Beacon500 else Ink800)
                                    .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectFontScale(scale) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    scale.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) OnAccentColor else Mist100
                                )
                            }
                        }
                    }
                }
            }
        }

        // Display Density & High Contrast
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Display Density & Contrast", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("High Contrast Mode", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Enforce strong borders & high-visibility text", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = highContrast,
                            onCheckedChange = { viewModel.toggleHighContrast(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Ink800)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Compact Directory Cards", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Show more listings per screen height", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = compactListing,
                            onCheckedChange = { viewModel.toggleCompactListing(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }
                }
            }
        }

        // App Launcher Icon Branding
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Custom App Launcher Branding", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Text("Select your preferred icon aesthetic", color = Mist400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    iconChoices.forEach { (key, name) ->
                        val isSelected = activeAppIcon == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Beacon500.copy(alpha = 0.18f) else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectAppIcon(key) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectAppIcon(key) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                                )
                                Text(name, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Reset
        item {
            OutlinedButton(
                onClick = { viewModel.resetThemingToDefaults() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon500),
                border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Appearance Defaults", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- Navigator Profile & Presets Sub-Page ---
@Composable
fun ProfileSettingsPage(
    viewModel: CompassViewModel
) {
    val demographicPresets = viewModel.demographicPresets.value
    val mobilityMode = viewModel.accessibilityMobilityMode.value
    val dietaryPresets = viewModel.dietaryPresets.value
    val excludeNonLocation = viewModel.excludeNonLocationResources.value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Demographic Presets", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Text("Highlight services catering to specific populations", color = Mist400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    DemographicPreset.entries.forEach { preset ->
                        val isSelected = demographicPresets.contains(preset)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleDemographicPreset(preset) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(preset.icon, fontSize = 16.sp)
                                Text(preset.title, color = Mist100, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleDemographicPreset(preset) },
                                colors = CheckboxDefaults.colors(checkedColor = Beacon500)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Accessibility & Mobility", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wheelchair & Mobility Access Only", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Only surface locations with verified ramp or elevator access", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = mobilityMode,
                            onCheckedChange = { viewModel.toggleAccessibilityMobilityMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dietary Requirements", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    DietaryPreset.entries.forEach { preset ->
                        val isSelected = dietaryPresets.contains(preset)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleDietaryPreset(preset) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(preset.icon, fontSize = 16.sp)
                                Text(preset.title, color = Mist100, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleDietaryPreset(preset) },
                                colors = CheckboxDefaults.colors(checkedColor = Beacon500)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Exclude Non-Location Helplines", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Hide phone-only or online resources when browsing physical spots", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = excludeNonLocation,
                            onCheckedChange = { viewModel.toggleExcludeNonLocationResources(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { viewModel.clearAllPresets() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                border = BorderStroke(1.dp, Rose500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear All Presets & Preferences", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- Navigation & Data Sources Sub-Page ---
@Composable
fun NavigationSettingsPage(
    viewModel: CompassViewModel
) {
    val defaultTab = viewModel.startupScreenRoute.value
    val resources = viewModel.allResources.collectAsState(initial = emptyList()).value
    val rmpLocations = viewModel.allRmpLocations.collectAsState(initial = emptyList()).value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🧭 Workspace Navigation", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Startup Screen", color = Mist300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink800, RoundedCornerShape(8.dp))
                            .border(1.dp, Ink700, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text("Default startup route: $defaultTab", color = Mist100, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Civic Data Sources", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    DataSourceToggleCard(
                        title = "SF OpenData Directory",
                        description = "Verified community shelters, meal sites, and clinics",
                        count = resources.size,
                        badge = "Primary Feed",
                        checked = true,
                        onCheckedChange = {}
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DataSourceToggleCard(
                        title = "EBT Restaurant Meals Program (RMP)",
                        description = "CalFresh hot food vendors in San Francisco",
                        count = rmpLocations.size,
                        badge = "CalFresh RMP",
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { viewModel.resetWorkspaceNavigationToDefaults() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon500),
                border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Navigation Defaults", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- AI Navigator Sub-Page ---
@Composable
fun AiSettingsPage(
    viewModel: CompassViewModel
) {
    val aiStyle = viewModel.aiNavigatorStyle.value
    val aiOffline = viewModel.aiOfflineOnly.value
    val customKey = viewModel.aiCustomApiKey.value
    val customEndpoint = viewModel.aiCustomEndpoint.value
    val connectionStatus = viewModel.aiConnectionStatus.value
    val isTesting = viewModel.isTestingAiConnection.value

    var keyInput by remember(customKey) { mutableStateOf(customKey) }
    var endpointInput by remember(customEndpoint) { mutableStateOf(customEndpoint) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🤖", fontSize = 20.sp)
                        Text("AI Navigator & Gemini API Integration", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Compass SF uses online Gemini AI as its primary experience to deliver intelligent recommendations, natural language queries, and street flyer parsing. If offline or without an API key, local keyword matching acts as a graceful fallback.",
                        color = Mist300,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Response Style Persona
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Guide Persona & Response Style", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 14.sp)
                    Text("Customize how the AI assistant frames recommendations and guidance", color = Mist400, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    AiNavigatorStyle.entries.forEach { style ->
                        val isSelected = aiStyle == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Beacon500.copy(alpha = 0.18f) else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setAiNavigatorStyle(style) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(style.title, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                                Text(style.description, color = Mist400, fontSize = 11.sp)
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setAiNavigatorStyle(style) },
                                colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Online / Offline Control
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Connectivity & Offline Mode Control", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Force Offline-Only Mode", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Never contact Gemini API; force local keyword search fallback", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = aiOffline,
                            onCheckedChange = { viewModel.setAiOfflineOnly(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }
                }
            }
        }

        // Custom API Key & Endpoint
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Custom API Key & Server Endpoint", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 14.sp)
                    Text("Optional custom Gemini API credentials or server proxy", color = Mist400, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            viewModel.setAiCustomApiKey(it)
                        },
                        label = { Text("Custom Gemini API Key", fontSize = 12.sp) },
                        placeholder = { Text("Enter AI Studio or Gemini API key", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Beacon500,
                            unfocusedBorderColor = Ink700,
                            focusedTextColor = Mist100,
                            unfocusedTextColor = Mist100
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = endpointInput,
                        onValueChange = {
                            endpointInput = it
                            viewModel.setAiCustomEndpoint(it)
                        },
                        label = { Text("Custom API Base Endpoint", fontSize = 12.sp) },
                        placeholder = { Text("https://generativelanguage.googleapis.com", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Beacon500,
                            unfocusedBorderColor = Ink700,
                            focusedTextColor = Mist100,
                            unfocusedTextColor = Mist100
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.testAiConnection() },
                        enabled = !isTesting,
                        colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnAccentColor, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Connection...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Gemini API Connection", fontSize = 12.sp)
                        }
                    }

                    if (!connectionStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Ink800, RoundedCornerShape(6.dp))
                                .border(1.dp, Ink700, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(connectionStatus, color = Mist100, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Reset
        item {
            OutlinedButton(
                onClick = {
                    viewModel.resetAiSettings()
                    keyInput = ""
                    endpointInput = ""
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon500),
                border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset AI Settings to Defaults", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- Search Sub-Page ---
@Composable
fun SearchSettingsPage(
    viewModel: CompassViewModel
) {
    val excludeNonLoc = viewModel.excludeNonLocationResources.value
    val openOnly = viewModel.askOpenOnly.value
    val incognito = viewModel.incognitoSearchMode.value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔍 Directory Search & Filtering Preferences", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Exclude Non-Location Helplines", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Hide phone-only / website-only resources that lack physical SF street addresses", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = excludeNonLoc,
                            onCheckedChange = { viewModel.toggleExcludeNonLocationResources(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Open-Now Filter", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Automatically filter search results to currently open services", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = openOnly,
                            onCheckedChange = { viewModel.askOpenOnly.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🕵️ Ephemeral Search Privacy", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Incognito Search Mode", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Do not save recent search queries to local search log", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = incognito,
                            onCheckedChange = { viewModel.toggleIncognitoSearchMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.clearRecentSearches() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist200),
                        border = BorderStroke(1.dp, Ink700),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Recent Search Queries History", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- Map Sub-Page ---
@Composable
fun MapSettingsPage(
    viewModel: CompassViewModel
) {
    val transit = viewModel.mapLayerTransit.value
    val neighborhoods = viewModel.mapLayerNeighborhoods.value
    val landmarks = viewModel.mapLayerLandmarks.value
    val clustering = viewModel.mapClusteringMode.value
    val reducedMotion = viewModel.mapReducedMotion.value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🗺️ Map Visual Layers", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Transit Lines & Stops", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Show Muni bus, BART, and light rail corridors", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = transit,
                            onCheckedChange = { viewModel.toggleMapLayerTransit(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Neighborhood Boundaries", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Highlight Tenderloin, Mission, SoMa & Civic Center boundaries", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = neighborhoods,
                            onCheckedChange = { viewModel.toggleMapLayerNeighborhoods(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("City Landmarks", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Display key reference points like City Hall, BART stations & parks", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = landmarks,
                            onCheckedChange = { viewModel.toggleMapLayerLandmarks(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Density & Motion Settings", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Pin Clustering Mode", fontSize = 12.sp, color = Mist300, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    MapClusteringMode.entries.forEach { mode ->
                        val isSelected = clustering == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Beacon500.copy(alpha = 0.18f) else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectMapClusteringMode(mode) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mode.displayName, color = Mist100, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectMapClusteringMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reduced Motion", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Disable map pan & zoom animations for reduced visual stress", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = reducedMotion,
                            onCheckedChange = { viewModel.toggleMapReducedMotion(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }
                }
            }
        }
    }
}

// --- About & Help Sub-Page ---
@Composable
fun AboutHelpSettingsPage(
    viewModel: CompassViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Explore, contentDescription = null, tint = Beacon500, modifier = Modifier.size(32.dp))
                        Column {
                            Text("Compass SF", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 18.sp)
                            Text("v3.0.0 • Local Personal Civic Navigator", color = Beacon400, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Designed specifically for San Francisco residents, unhoused community members, social workers, and street navigators. Provides fast, offline-ready access to daily essential services with intelligent AI search.",
                        color = Mist300,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Open Civic Data Sources", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• ShelterTech SF Service Guide — Community-verified shelters, meals & hygiene", color = Mist300, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• DataSF Open Data Portal — City & County of San Francisco public facilities", color = Mist300, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• CalFresh RMP — Verified EBT hot food restaurant vendor directory", color = Mist300, fontSize = 12.sp)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🛡️ Privacy & Storage Architecture", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "100% Private & Local-Only. Your notes, checklist tasks, search history, and relevance profile never leave your phone. There are no remote user accounts or profile tracking.",
                        color = Mist300,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🚨 Emergency & Street Hotlines", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    HotlineRow("988 Suicide & Crisis Lifeline", "988", "24/7 free & confidential emotional support")
                    Spacer(modifier = Modifier.height(8.dp))
                    HotlineRow("SF City Services (311)", "311", "San Francisco non-emergency city services")
                    Spacer(modifier = Modifier.height(8.dp))
                    HotlineRow("211 Bay Area Help", "211", "Community health and human services referral")
                }
            }
        }
    }
}

@Composable
fun HotlineRow(title: String, phone: String, desc: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink800, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Mist100, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Mist400, fontSize = 10.5.sp)
        }
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("Call $phone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- Storage & Backup Sub-Page ---
@Composable
fun StorageSettingsPage(
    viewModel: CompassViewModel,
    resources: List<Resource> = emptyList(),
    rmpLocations: List<RmpLocation> = emptyList(),
    visits: List<Visit> = emptyList(),
    tasks: List<Task> = emptyList(),
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onOpenPasteJson: () -> Unit = {},
    onOpenPhotoAudit: () -> Unit = {},
    onOpenReseedConfirm: () -> Unit = {}
) {
    val hiddenResources = remember(resources) { resources.filter { it.hidden } }
    val hiddenRmp = remember(rmpLocations) { rmpLocations.filter { it.hidden } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Granular JSON Backup & Export", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val exportSel = viewModel.exportSelection.value
                    BackupCheckboxRow("Starred Favorites", exportSel.includeFavorites) {
                        viewModel.setExportEntitySelected("favorites", it)
                    }
                    BackupCheckboxRow("Personal Notes", exportSel.includeNotes) {
                        viewModel.setExportEntitySelected("notes", it)
                    }
                    BackupCheckboxRow("Visit Logs & Field Reviews", exportSel.includeVisits) {
                        viewModel.setExportEntitySelected("visits", it)
                    }
                    BackupCheckboxRow("Day Checklist Tasks", exportSel.includeTasks) {
                        viewModel.setExportEntitySelected("tasks", it)
                    }
                    BackupCheckboxRow("Custom Saved Places", exportSel.includeCustomPlaces) {
                        viewModel.setExportEntitySelected("custom", it)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onExportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup File (.json)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onImportClick,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Ink700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist100)
                        ) {
                            Text("Open JSON File", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenPasteJson,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Ink700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist100)
                        ) {
                            Text("Paste Raw Text", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔄 Baseline Database Re-seed", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Restores all SF civic directory entries and EBT hot meal spots to initial baseline state.",
                        color = Mist400,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onOpenReseedConfirm,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon500),
                        border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Re-seed Civic Database", fontSize = 12.sp)
                    }
                }
            }
        }

        if (hiddenResources.isNotEmpty() || hiddenRmp.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Ink900),
                    border = BorderStroke(1.dp, Ink700),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hidden Items (${hiddenResources.size + hiddenRmp.size})", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        hiddenResources.forEach { res ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(res.name, color = Mist100, fontSize = 12.sp)
                                TextButton(onClick = { viewModel.toggleResourceHidden(res) }) {
                                    Text("Unhide", color = Beacon500, fontSize = 11.sp)
                                }
                            }
                        }

                        hiddenRmp.forEach { rmp ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${rmp.name} (EBT)", color = Mist100, fontSize = 12.sp)
                                TextButton(onClick = { viewModel.toggleRmpHidden(rmp) }) {
                                    Text("Unhide", color = Beacon500, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Cleaned Old Monolithic Body ---

// --- Chunk 5 Settings Helpers ---
@Composable
fun BackupCheckboxRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (enabled) (if (checked) Mist100 else Mist300) else Mist400.copy(alpha = 0.5f),
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
        )
        Checkbox(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = Beacon500,
                checkmarkColor = Ink950,
                uncheckedColor = Mist400
            ),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun DataSourceToggleCard(
    title: String,
    description: String,
    count: Int,
    badge: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (checked) Ink950 else Ink900, RoundedCornerShape(8.dp))
            .border(1.dp, if (checked) Ink800 else Ink700.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (checked) Mist100 else Mist400
                )
                Box(
                    modifier = Modifier
                        .background(if (checked) Beacon500.copy(alpha = 0.15f) else Ink800, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        badge,
                        color = if (checked) Beacon400 else Mist400,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 11.sp, color = Mist400)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$count entries available in directory", fontSize = 10.sp, color = if (checked) Emerald500 else Mist400)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Beacon500,
                checkedTrackColor = Beacon500.copy(alpha = 0.3f),
                uncheckedThumbColor = Mist400,
                uncheckedTrackColor = Ink800
            )
        )
    }
}

// --- Detail Screen ---
@Composable
fun DetailScreen(
    resourceId: Int,
    viewModel: CompassViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val resources by viewModel.allResources.collectAsState()
    val res = remember(resources, resourceId) { resources.find { it.id == resourceId } }
    
    if (res == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Beacon500)
        }
        return
    }

    val visits by viewModel.getVisitsFlow(resourceId).collectAsState(initial = emptyList())
    var showVisitDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "back", tint = Mist100)
                }
                Row {
                    IconButton(onClick = { showReminderDialog = true }) {
                        Icon(Icons.Default.NotificationsActive, "Set alert reminder", tint = Beacon400)
                    }
                    IconButton(onClick = { shareResource(context, res) }) {
                        Icon(Icons.Default.Share, "share", tint = Beacon400)
                    }
                    IconButton(onClick = { viewModel.toggleResourceFavorite(res) }) {
                        Icon(
                            if (res.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "fav",
                            tint = if (res.favorite) Rose500 else Mist400
                        )
                    }
                    IconButton(onClick = { viewModel.toggleResourceHidden(res) }) {
                        Icon(Icons.Default.VisibilityOff, "hide", tint = Mist400)
                    }
                }
            }
        }

        item {
            Text(res.name, style = MaterialTheme.typography.headlineSmall, color = Mist100, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .background(Beacon500, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(res.category.replaceFirstChar { it.uppercase() }, color = OnAccentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                
                val isOpen = isResourceOpen(res.open24, res.hours)
                Box(
                    modifier = Modifier
                        .background(if (isOpen) Emerald500 else Ink900, RoundedCornerShape(4.dp))
                        .border(1.dp, if (isOpen) Color.Transparent else Ink700, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(if (isOpen) "Open Now" else "Closed", color = if (isOpen) OnAccentColor else Mist400, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .background(if (res.confidence == "verified") Emerald500 else Ink900, RoundedCornerShape(4.dp))
                        .border(1.dp, if (res.confidence == "verified") Color.Transparent else Ink700, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(if (res.confidence == "verified") "Verified" else "Reported", color = if (res.confidence == "verified") OnAccentColor else Beacon500, fontSize = 11.sp)
                }
            }
        }

        if (res.summary.isNotBlank()) {
            item {
                Text(res.summary, color = Mist100, fontSize = 14.sp, style = MaterialTheme.typography.bodyLarge)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📍 Address: ${res.address}", color = Mist100)
                    if (res.neighborhood.isNotBlank()) {
                        Text("🏢 Neighborhood: ${res.neighborhood}", color = Mist100)
                    }
                    if (res.hoursText.isNotBlank()) {
                        Text("⏰ Hours: ${res.hoursText}", color = Mist100)
                    }
                    if (res.source.isNotBlank()) {
                        Text("📋 Online Source: ${res.source}", color = Color(0xFF38BDF8), fontSize = 12.sp)
                    }

                    // Direct 48dp action buttons for Directions, Phone, and Website
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (res.address.isNotBlank()) {
                            Button(
                                onClick = {
                                    val geoUri = Uri.parse("geo:0,0?q=${Uri.encode("${res.name}, ${res.address}, San Francisco, CA")}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No map application found: ${e.localizedMessage ?: "Cannot open map"}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = Ink950),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = "Directions", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Directions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (res.phone.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${res.phone}"))
                                    try {
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open phone dialer: ${e.localizedMessage ?: "No dialer installed"}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                border = BorderStroke(1.dp, Ink700),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist100),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", tint = Beacon400, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (res.website.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(res.website))
                                        context.startActivity(webIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open web link: ${e.localizedMessage ?: "No browser app"}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                border = BorderStroke(1.dp, Ink700),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist100),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = "Website", tint = Beacon400, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Website", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Share Resource Card & Copy Info Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { shareResource(context, res) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("share_resource_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Ink800, contentColor = Mist100),
                            border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Resource", tint = Beacon400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Card", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { copyResourceDetails(context, res) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("copy_resource_button"),
                            border = BorderStroke(1.dp, Ink700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist100),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Info", tint = Beacon400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Info", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (res.aiTips.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Ink900),
                    border = BorderStroke(1.dp, Beacon500),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💡 Street Navigator Smart Tip", fontWeight = FontWeight.Bold, color = Beacon500)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(res.aiTips, color = Mist100, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Text("Requirements & Rules", fontWeight = FontWeight.Bold, color = Beacon500)
            if (res.requirements.isEmpty()) {
                Text("No specific intake requirements reported. Usually open to walk-ins.", color = Mist400)
            } else {
                res.requirements.forEach { req ->
                    Text("• $req", color = Mist100)
                }
            }
        }

        item {
            Text("Cost: ${res.cost}", fontWeight = FontWeight.Bold, color = Mist100)
        }

        // Private personal notes block
        item {
            var inputNotes by remember(res.personalNotes) { mutableStateOf(res.personalNotes) }
            Column {
                Text("My Personal Notes (private)", fontWeight = FontWeight.Bold, color = Beacon500)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = inputNotes,
                    onValueChange = {
                        inputNotes = it
                        viewModel.saveResourcePersonalNotes(res.id, it)
                    },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    placeholder = { Text("Add code, access steps, case worker name...", color = Mist400) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100)
                )
            }
        }

        // Action Buttons: Verify & Add visit log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.verifyResource(res); Toast.makeText(context, "Marked as Verified!", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = OnAccentColor)
                ) {
                    Text("Verify Details", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showVisitDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink900, contentColor = Beacon500),
                    border = BorderStroke(1.dp, Ink700)
                ) {
                    Text("Log Visit Activity")
                }
            }
        }

        // Visits history
        item {
            Text("Visit History logs (${visits.size})", fontWeight = FontWeight.Bold, color = Mist100)
        }

        if (visits.isEmpty()) {
            item {
                Text("No visits logged yet. Tap 'Log Visit Activity' to record your queue wait time.", color = Mist400, fontSize = 13.sp)
            }
        } else {
            items(visits) { visit ->
                val dateStr = remember(visit.visitedAt) {
                    SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date(visit.visitedAt))
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Ink900, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when(visit.outcome) {
                                "got_help" -> "✅ Got Help"
                                "partial" -> "⚠️ Partial support"
                                "turned_away" -> "❌ Turned Away"
                                "closed" -> "🔒 Closed upon arrival"
                                else -> "🔍 Drop-in looking"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Mist100,
                            fontSize = 13.sp
                        )
                        Text(dateStr, color = Mist400, fontSize = 11.sp)
                    }
                    if (visit.notes.isNotBlank()) {
                        Text(visit.notes, color = Mist100, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showVisitDialog) {
        var outcome by remember { mutableStateOf("got_help") }
        var notes by remember { mutableStateOf("") }
        var waitString by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showVisitDialog = false },
            containerColor = Ink900,
            title = { Text("Log visit at ${res.name}", color = Beacon500) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Outcome:", color = Mist400)
                    val outcomes = listOf(
                        "got_help" to "Got Help",
                        "partial" to "Partial Help",
                        "turned_away" to "Turned Away",
                        "closed" to "Closed",
                        "just_looking" to "Just Looking"
                    )
                    outcomes.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { outcome = key }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = outcome == key, onClick = { outcome = key }, colors = RadioButtonDefaults.colors(selectedColor = Beacon500))
                            Text(label, color = Mist100, modifier = Modifier.padding(start = 6.dp))
                        }
                    }

                    OutlinedTextField(
                        value = waitString,
                        onValueChange = { waitString = it },
                        label = { Text("Wait time in minutes (optional)", color = Mist400) },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Visit details/notes", color = Mist400) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Beacon500, unfocusedBorderColor = Ink700, focusedTextColor = Mist100, unfocusedTextColor = Mist100),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addVisitLog(
                            resourceId = res.id,
                            outcome = outcome,
                            notes = notes,
                            rating = null,
                            waitMin = waitString.toIntOrNull()
                        )
                        showVisitDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor)
                ) {
                    Text("Save log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVisitDialog = false }) {
                    Text("Cancel", color = Mist400)
                }
            }
        )
    }

    if (showReminderDialog) {
        AddStreetReminderDialog(
            viewModel = viewModel,
            onDismiss = { showReminderDialog = false },
            prefillResource = res
        )
    }
}

// --- Common UI Components ---

@Composable
fun ResourceCard(
    resource: Resource,
    distanceMiles: Double? = null,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalCompassTheme.current
    val cardPadding = if (theme.compactView) 10.dp else 14.dp
    val borderStroke = BorderStroke(
        if (theme.highContrast) 2.dp else 1.dp,
        if (theme.highContrast) Beacon500.copy(alpha = 0.7f) else Ink700
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Ink900),
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = resource.name,
                    fontWeight = FontWeight.Bold,
                    color = Mist100,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { shareResource(context, resource) },
                        modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Mist400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = if (resource.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (resource.favorite) Rose500 else Mist400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!theme.compactView || resource.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = resource.summary,
                    color = Mist400,
                    fontSize = 13.sp,
                    maxLines = if (theme.compactView) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(if (theme.compactView) 6.dp else 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Ink800, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(resource.category.replaceFirstChar { it.uppercase() }, color = Beacon400, fontSize = 10.sp)
                    }
                    if (resource.isNonLocationOrHelpline()) {
                        Box(
                            modifier = Modifier
                                .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Beacon500.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("📞 Phone/Helpline", color = Beacon400, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (distanceMiles != null) {
                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(4.dp))
                                .border(0.5.dp, Beacon500.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🚶 ${LocationHelper.formatDistance(distanceMiles)}",
                                color = Beacon400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (resource.neighborhood.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(resource.neighborhood, color = Mist400, fontSize = 10.sp)
                        }
                    }
                    if (resource.source.isNotBlank()) {
                        val shortSource = when {
                            resource.source.contains("ShelterTech", ignoreCase = true) -> "ShelterTech"
                            resource.source.contains("DataSF", ignoreCase = true) -> "DataSF"
                            resource.source.contains("211", ignoreCase = true) -> "211 Bay Area"
                            else -> resource.source
                        }
                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(shortSource, color = Color(0xFF38BDF8), fontSize = 10.sp)
                        }
                    }
                }
                
                val isOpen = isResourceOpen(resource.open24, resource.hours)
                Text(
                    text = if (isOpen) "• Open now" else "• Closed",
                    color = if (isOpen) Emerald500 else Rose500,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun RmpCard(
    location: RmpLocation,
    distanceMiles: Double? = null,
    onFavoriteClick: () -> Unit,
    onNotesSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val theme = LocalCompassTheme.current
    var isExpanded by remember { mutableStateOf(false) }
    var notes by remember(location.personalNotes) { mutableStateOf(location.personalNotes) }
    val borderStroke = BorderStroke(
        if (theme.highContrast) 2.dp else 1.dp,
        if (theme.highContrast) Beacon500.copy(alpha = 0.7f) else Ink700
    )

    val isOpen = remember(location.open24, location.hours) {
        if (location.hours.isEmpty() && !location.open24) null
        else isResourceOpen(location.open24, location.hours)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Ink900),
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(location.name, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 16.sp)
                    val locSub = buildString {
                        if (location.address.isNotBlank()) append("📍 ${location.address}")
                        if (location.neighborhood.isNotBlank()) append(" • ${location.neighborhood}")
                        if (location.zip.isNotBlank()) append(" (${location.zip})")
                    }
                    Text(locSub, color = Mist400, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { shareRmpLocation(context, location) },
                        modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Mist400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = if (location.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (location.favorite) Rose500 else Mist400,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Beacon500, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            location.cuisine.replaceFirstChar { it.uppercase() },
                            color = OnAccentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    if (distanceMiles != null) {
                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(4.dp))
                                .border(0.5.dp, Beacon500.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🚶 ${LocationHelper.formatDistance(distanceMiles)}",
                                color = Beacon400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (location.chain) {
                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Chain", color = Mist400, fontSize = 10.sp)
                        }
                    }
                    if (location.confidence == "sfhsa") {
                        Box(
                            modifier = Modifier
                                .background(Emerald500, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SF HSA List", color = OnAccentColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    } else if (location.confidence == "reported") {
                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Community Reported", color = Color(0xFFFBBF24), fontSize = 10.sp)
                        }
                    }
                }

                if (isOpen != null) {
                    Text(
                        text = if (isOpen) "• Open now" else "• Closed",
                        color = if (isOpen) Emerald500 else Rose500,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (location.hoursText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "Hours",
                        tint = Mist400,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(location.hoursText, color = Mist400, fontSize = 11.sp)
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Quick Action Buttons: Directions and Call
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (location.address.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val geoUri = Uri.parse("geo:0,0?q=${Uri.encode("${location.name}, ${location.address}, San Francisco, CA")}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No map application found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon400),
                                border = BorderStroke(1.dp, Ink700),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = "Directions", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Directions", fontSize = 12.sp)
                            }
                        }

                        if (location.phone.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${location.phone}"))
                                    try {
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open dialer", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald500),
                                border = BorderStroke(1.dp, Ink700),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(location.phone, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (location.notes.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text("ℹ️", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rules: ${location.notes}", color = Mist100, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (location.tips.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text("💡", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tip: ${location.tips}", color = Beacon400, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Payment helper box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink800, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            "💳 Payment: Swipe your Golden State Advantage card, select EBT Food (or Cash), and enter your 4-digit PIN. Tell the cashier you are paying with Restaurant Meals EBT.",
                            color = Mist400,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Private personal notes for RMP
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            onNotesSaved(it)
                        },
                        label = { Text("My private notes for this place", color = Mist400) },
                        modifier = Modifier.fillMaxWidth().height(70.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Beacon500,
                            unfocusedBorderColor = Ink700,
                            focusedTextColor = Mist100,
                            unfocusedTextColor = Mist100
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenImageViewer(imageUri: Uri, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale > 1f) offset + pan else Offset.Zero
                    }
                }
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Full screen image view",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
fun ActivePresetsBanner(
    viewModel: CompassViewModel,
    modifier: Modifier = Modifier
) {
    if (!viewModel.hasActivePresets) return

    val activeDetails = remember(viewModel.demographicPresets.value, viewModel.accessibilityMobilityMode.value, viewModel.dietaryPresets.value, viewModel.excludeNonLocationResources.value) {
        val items = mutableListOf<String>()
        if (viewModel.excludeNonLocationResources.value) items.add("Physical locations only")
        if (viewModel.accessibilityMobilityMode.value) items.add("Wheelchair accessible")
        if (viewModel.demographicPresets.value.isNotEmpty()) items.add("${viewModel.demographicPresets.value.size} demographic filters")
        if (viewModel.dietaryPresets.value.isNotEmpty()) items.add("${viewModel.dietaryPresets.value.size} dietary filters")
        if (items.isEmpty()) "Results are currently filtered by your profile." else items.joinToString(" • ")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Beacon500.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = null,
                tint = Beacon500,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    "Settings Filters Active",
                    color = Beacon500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    activeDetails,
                    color = Mist200,
                    fontSize = 11.sp
                )
            }
        }
        TextButton(
            onClick = { viewModel.clearAllPresets() },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.defaultMinSize(minHeight = 30.dp)
        ) {
            Text("Clear", color = Beacon400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RelevanceProfileBanner(
    viewModel: CompassViewModel,
    modifier: Modifier = Modifier
) {
    val profile = viewModel.userProfile.value
    if (profile.isEmpty) return

    val summaryText = remember(profile) {
        val parts = mutableListOf<String>()
        if (profile.primaryNeeds.isNotEmpty()) {
            val names = profile.primaryNeeds.mapNotNull { id ->
                ProfileConstants.PRIMARY_NEEDS.find { it.id == id }?.title?.split(" ")?.firstOrNull()
            }
            parts.add(names.joinToString(", "))
        }
        if (profile.preferredNeighborhood.isNotBlank()) {
            parts.add(profile.preferredNeighborhood)
        }
        if (profile.demographics.isNotEmpty()) {
            parts.add("${profile.demographics.size} demographic")
        }
        if (profile.dietary.isNotEmpty()) {
            parts.add("${profile.dietary.size} dietary")
        }
        if (profile.accessibilityMobility) {
            parts.add("Step-free")
        }
        if (profile.preferredLanguages.isNotEmpty()) {
            parts.add(profile.preferredLanguages.joinToString(", "))
        }
        if (parts.isEmpty()) "${profile.activePreferenceCount} active boosts" else parts.joinToString(" • ")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Beacon500.copy(alpha = 0.12f))
            .border(1.dp, Beacon500.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Stars,
                contentDescription = null,
                tint = Beacon500,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    "Relevance Profile Active",
                    color = Beacon500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Boosting: $summaryText",
                    color = Mist200,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        TextButton(
            onClick = { viewModel.resetUserProfile() },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.defaultMinSize(minHeight = 28.dp)
        ) {
            Text("Reset", color = Beacon400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}


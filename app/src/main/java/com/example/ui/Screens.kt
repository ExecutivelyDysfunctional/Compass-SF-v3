package com.example.ui
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState


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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    
    val openNowList = remember(resources, userLocation, sortByDistance, viewModel.demographicPresets.value, viewModel.accessibilityMobilityMode.value, viewModel.dietaryPresets.value) {
        val openResources = resources.filter { res ->
            !res.hidden && isResourceOpen(res.open24, res.hours) &&
            PresetMatcher.matchesDemographic(res, viewModel.demographicPresets.value) &&
            PresetMatcher.matchesAccessibility(res, viewModel.accessibilityMobilityMode.value) &&
            PresetMatcher.matchesDietary(res, viewModel.dietaryPresets.value)
        }
        if (sortByDistance && userLocation != null) {
            openResources.sortedBy { res ->
                viewModel.getDistanceToResource(res) ?: Double.MAX_VALUE
            }.take(4)
        } else {
            openResources.take(4)
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

    val filteredList = remember(resources, searchQuery, selectedCategory, selectedNeighborhood, selectedSource, filterOpenNow, filterFavorites, filterHidden, userLocation, sortByDistance, viewModel.demographicPresets.value, viewModel.accessibilityMobilityMode.value, viewModel.dietaryPresets.value) {
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

            val matchesDemographic = PresetMatcher.matchesDemographic(res, viewModel.demographicPresets.value)
            val matchesAccessibility = PresetMatcher.matchesAccessibility(res, viewModel.accessibilityMobilityMode.value)
            val matchesDietary = PresetMatcher.matchesDietary(res, viewModel.dietaryPresets.value)

            matchesSearch && matchesCategory && matchesNeighborhood && matchesSource && matchesOpen && matchesFav && matchesHidden && matchesDemographic && matchesAccessibility && matchesDietary
        }
        if (sortByDistance && userLocation != null) {
            list.sortedBy { res ->
                viewModel.getDistanceToResource(res) ?: Double.MAX_VALUE
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

// --- Settings Screen ---
@Composable
fun SettingsScreen(
    viewModel: CompassViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val resources by viewModel.allResources.collectAsState()
    val rmpLocations by viewModel.allRmpLocations.collectAsState()
    val visits by viewModel.allVisits.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()

    val totalFavs = remember(resources, rmpLocations) {
        resources.count { it.favorite } + rmpLocations.count { it.favorite }
    }
    val totalNotes = remember(resources, rmpLocations) {
        resources.count { it.personalNotes.isNotBlank() } + rmpLocations.count { it.personalNotes.isNotBlank() }
    }
    val totalVisits = remember(visits) { visits.size }
    val totalTasks = remember(tasks) { tasks.size }

    var showPasteJsonDialog by remember { mutableStateOf(false) }
    var pastedJsonText by remember { mutableStateOf("") }

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
                    viewModel.setExportSuccess("Backup saved successfully! JSON backup file created.")
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

    val hiddenResources = remember(resources) { resources.filter { it.hidden } }
    val hiddenRmp = remember(rmpLocations) { rmpLocations.filter { it.hidden } }

    val activeMode = viewModel.currentThemeMode.value
    val activeAccent = viewModel.currentAccentColor.value
    val activeFontScale = viewModel.currentFontScale.value
    val highContrast = viewModel.highContrastEnabled.value
    val compactListing = viewModel.compactListingEnabled.value

    val iconChoices = listOf(
        "classic" to "Compass Rose",
        "beacon" to "Beacon Pulse",
        "bridge" to "Golden Gate",
        "lantern" to "Lantern Light",
        "wayfinder" to "Wayfinder"
    )

    val listState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }

    // Exact index targets in the LazyColumn
    val targetWorkspaceIndex = 3
    val targetFiltersIndex = 4
    val targetThemeIndex = 5
    val targetAccentIndex = 6
    val targetTextIndex = 7
    val targetDisplayIndex = 8
    val targetMapIndex = 9
    val targetIconIndex = 10
    val targetBackupIndex = 11
    val targetHiddenIndex = 12
    val targetStatsIndex = if (hiddenResources.isNotEmpty() || hiddenRmp.isNotEmpty()) {
        12 + 1 + hiddenResources.size + hiddenRmp.size
    } else {
        12
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header
            item {
                Column {
                    Text(
                        "Settings & Customization",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Beacon500,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Personalize theme atmosphere, navigation shortcuts, font scaling, and neighborhood anchors",
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist400
                    )
                }
            }

            // Quick Settings & Customizations Menu Index Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Ink900),
                    border = BorderStroke(
                        if (highContrast) 2.dp else 1.dp,
                        if (highContrast) Beacon500 else Ink700
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_index_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(Beacon500.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = Beacon500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "QUICK SETTINGS INDEX",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = Beacon500,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        "Tap any section to jump directly to it",
                                        fontSize = 11.sp,
                                        color = Mist400
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(Ink800, RoundedCornerShape(12.dp))
                                    .border(1.dp, Ink700, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                val totalSections = if (hiddenResources.isNotEmpty() || hiddenRmp.isNotEmpty()) 11 else 10
                                Text(
                                    "$totalSections Sections",
                                    color = Mist200,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val indexEntries = remember(hiddenResources.size, hiddenRmp.size, targetStatsIndex) {
                            val list = mutableListOf(
                                Triple("🧭 Navigation", "Startup & Bottom Bar", targetWorkspaceIndex),
                                Triple("⚙️ Filters", "Demographic & Dietary", targetFiltersIndex),
                                Triple("🎨 Themes", "Atmosphere & Tones", targetThemeIndex),
                                Triple("🌈 Accents", "Highlight Colors", targetAccentIndex),
                                Triple("🔤 Text Size", "Scaling & Readability", targetTextIndex),
                                Triple("🔆 Display", "Contrast & Density", targetDisplayIndex),
                                Triple("🗺️ Map Layers", "Transit, Badges & Radar", targetMapIndex),
                                Triple("📱 App Icon", "Launcher Style", targetIconIndex),
                                Triple("💾 Backup", "Export & Import JSON", targetBackupIndex)
                            )
                            if (hiddenResources.isNotEmpty() || hiddenRmp.isNotEmpty()) {
                                list.add(Triple("👁️ Hidden", "Unhide (${hiddenResources.size + hiddenRmp.size})", targetHiddenIndex))
                            }
                            list.add(Triple("📊 Statistics", "Storage & DB Stats", targetStatsIndex))
                            list
                        }

                        // 2-Column Responsive Navigation Grid
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (i in indexEntries.indices step 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val item1 = indexEntries[i]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Ink800)
                                            .border(1.dp, Ink700, RoundedCornerShape(8.dp))
                                            .clickable {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(item1.third)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    item1.first,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Mist100,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    item1.second,
                                                    fontSize = 9.5.sp,
                                                    color = Mist400,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                contentDescription = "Jump to ${item1.first}",
                                                tint = Beacon500,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    if (i + 1 < indexEntries.size) {
                                        val item2 = indexEntries[i + 1]
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Ink800)
                                                .border(1.dp, Ink700, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem(item2.third)
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        item2.first,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Mist100,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        item2.second,
                                                        fontSize = 9.5.sp,
                                                        color = Mist400,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Icon(
                                                    Icons.Default.ArrowDownward,
                                                    contentDescription = "Jump to ${item2.first}",
                                                    tint = Beacon500,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Live Interactive Theme Preview Card
            item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(
                    if (highContrast) 2.dp else 1.dp,
                    if (highContrast) Beacon500 else Ink700
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "LIVE PREVIEW",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Beacon500,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "${activeMode.displayName} • ${activeAccent.displayName}",
                                color = Mist100,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mock Resource Card inside Live Preview
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Ink800),
                        border = BorderStroke(1.dp, if (highContrast) Beacon500.copy(alpha = 0.8f) else Ink700),
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
                                Box(
                                    modifier = Modifier
                                        .background(Ink900, RoundedCornerShape(4.dp))
                                        .border(1.dp, Ink700, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Verified", color = Beacon500, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sample Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { /* Demo */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor)
                        ) {
                            Text("Sample Action", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { /* Demo */ },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Ink700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist100)
                        ) {
                            Text("Secondary", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section 1: Navigation & Workspace Configuration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_section_navigation")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SECTION 1", color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                "Navigation & Workspace",
                                fontWeight = FontWeight.Bold,
                                color = Beacon500,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(1) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                        ) {
                            Text("Index ↑", color = Mist400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        "Configure default startup destination, customize bottom navigation shortcuts, and set default neighborhood anchor",
                        fontSize = 12.sp,
                        color = Mist400
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subsection A: Startup Screen Destination
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            tint = Beacon500,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Default Startup Screen",
                            fontWeight = FontWeight.Bold,
                            color = Mist100,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        "Select which screen automatically opens when starting Compass SF",
                        fontSize = 11.sp,
                        color = Mist400
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val startupOptions = listOf(
                        Triple("now", "Now — Today's Navigator", "Daily meal times, urgent shelters & open rest stops"),
                        Triple("find", "Find — Directory Search", "Directory categories, tags & keyword search"),
                        Triple("map", "Map — Fullscreen City Map", "Interactive map with pins, GPS tracker & transit routes"),
                        Triple("day", "Day — My Day Checklist", "Personal schedule, visits & daily tasks"),
                        Triple("ask", "Ask — AI Street Navigator", "AI guide for emergency & street resources"),
                        Triple("ebt", "EBT — Restaurant Meals", "CalFresh EBT restaurant hot meals across SF")
                    )

                    val activeStartup = viewModel.startupScreenRoute.value
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        startupOptions.forEach { (route, title, desc) ->
                            val isSelected = activeStartup == route
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Ink800 else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) Beacon500.copy(alpha = 0.6f) else Ink800,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setStartupScreen(route) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val icon = when (route) {
                                        "now" -> Icons.Default.Explore
                                        "find" -> Icons.Default.Search
                                        "map" -> Icons.Default.Map
                                        "day" -> Icons.Default.Checklist
                                        "ask" -> Icons.Default.AutoAwesome
                                        "ebt" -> Icons.Default.CreditCard
                                        else -> Icons.Default.Home
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (isSelected) Beacon500.copy(alpha = 0.2f) else Ink800,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Beacon400 else Mist400,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Beacon400 else Mist100,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            desc,
                                            fontSize = 10.5.sp,
                                            color = Mist400,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setStartupScreen(route) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Ink800)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Subsection B: Bottom Navigation Bar Customization
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
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = Beacon500,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Bottom Navigation Bar",
                                fontWeight = FontWeight.Bold,
                                color = Mist100,
                                fontSize = 14.sp
                            )
                        }
                        val navCount = viewModel.bottomNavItems.value.size
                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(10.dp))
                                .border(1.dp, Ink700, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "$navCount / 6 active",
                                color = Beacon400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        "Toggle shortcuts to display on the persistent bottom navigation bar (min 2, max 6). Use the arrows to reorder tabs.",
                        fontSize = 11.sp,
                        color = Mist400
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Bottom Bar Order Strip
                    Text(
                        "CURRENT BAR ORDER (TAP ARROWS TO REORDER):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Mist400,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val activeNavs = viewModel.bottomNavItems.value
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink950, RoundedCornerShape(8.dp))
                            .border(1.dp, Ink800, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeNavs.forEachIndexed { index, route ->
                            val (title, icon) = when (route) {
                                "now" -> "Now (Navigator)" to Icons.Default.Explore
                                "find" -> "Find (Directory)" to Icons.Default.Search
                                "map" -> "Map (City Map)" to Icons.Default.Map
                                "ask" -> "Ask (AI Guide)" to Icons.Default.AutoAwesome
                                "day" -> "Day (Checklist)" to Icons.Default.Checklist
                                "ebt" -> "EBT (Meals)" to Icons.Default.CreditCard
                                "info" -> "Info (City Hotlines)" to Icons.Default.Info
                                "add" -> "Add (Resource)" to Icons.Default.Add
                                else -> route.replaceFirstChar { it.uppercase() } to Icons.Default.Explore
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Ink800, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Beacon500.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Beacon400
                                        )
                                    }
                                    Icon(icon, contentDescription = null, tint = Beacon500, modifier = Modifier.size(16.dp))
                                    Text(title, color = Mist100, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewModel.moveBottomNavItem(route, -1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowBack,
                                            contentDescription = "Move Left/Up",
                                            tint = if (index > 0) Mist100 else Ink700,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.moveBottomNavItem(route, 1) },
                                        enabled = index < activeNavs.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowForward,
                                            contentDescription = "Move Right/Down",
                                            tint = if (index < activeNavs.size - 1) Mist100 else Ink700,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Available Tabs Toggles
                    Text(
                        "AVAILABLE TAB SHORTCUTS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Mist400,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val allAvailableShortcuts = listOf(
                        "now" to ("Now" to Icons.Default.Explore),
                        "find" to ("Find" to Icons.Default.Search),
                        "map" to ("Map" to Icons.Default.Map),
                        "ask" to ("Ask AI" to Icons.Default.AutoAwesome),
                        "day" to ("Day" to Icons.Default.Checklist),
                        "ebt" to ("EBT" to Icons.Default.CreditCard),
                        "info" to ("Info" to Icons.Default.Info),
                        "add" to ("Add" to Icons.Default.Add)
                    )

                    // Render as a 2-column grid of toggle chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in allAvailableShortcuts.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val item1 = allAvailableShortcuts[i]
                                val isActive1 = activeNavs.contains(item1.first)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isActive1) Beacon500.copy(alpha = 0.15f) else Ink800)
                                        .border(
                                            1.dp,
                                            if (isActive1) Beacon500.copy(alpha = 0.6f) else Ink700,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.toggleBottomNavItem(item1.first) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                ) {
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
                                                item1.second.second,
                                                contentDescription = null,
                                                tint = if (isActive1) Beacon400 else Mist400,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                item1.second.first,
                                                color = if (isActive1) Mist100 else Mist400,
                                                fontSize = 12.sp,
                                                fontWeight = if (isActive1) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                        Checkbox(
                                            checked = isActive1,
                                            onCheckedChange = { viewModel.toggleBottomNavItem(item1.first) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Beacon500,
                                                checkmarkColor = OnAccentColor
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (i + 1 < allAvailableShortcuts.size) {
                                    val item2 = allAvailableShortcuts[i + 1]
                                    val isActive2 = activeNavs.contains(item2.first)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isActive2) Beacon500.copy(alpha = 0.15f) else Ink800)
                                            .border(
                                                1.dp,
                                                if (isActive2) Beacon500.copy(alpha = 0.6f) else Ink700,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.toggleBottomNavItem(item2.first) }
                                            .padding(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
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
                                                    item2.second.second,
                                                    contentDescription = null,
                                                    tint = if (isActive2) Beacon400 else Mist400,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    item2.second.first,
                                                    color = if (isActive2) Mist100 else Mist400,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isActive2) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                            Checkbox(
                                                checked = isActive2,
                                                onCheckedChange = { viewModel.toggleBottomNavItem(item2.first) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Beacon500,
                                                    checkmarkColor = OnAccentColor
                                                ),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Ink800)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Subsection C: Persistent Default Neighborhood Anchor
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = Beacon500,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Default Neighborhood Anchor",
                            fontWeight = FontWeight.Bold,
                            color = Mist100,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        "Set the home anchor used for distance calculations, walking estimates, and nearby service sorting when GPS is unavailable",
                        fontSize = 11.sp,
                        color = Mist400
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val activeAnchorId = viewModel.defaultNeighborhoodAnchorId.value
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LocationHelper.NEIGHBORHOOD_ANCHORS.forEach { anchor ->
                            val isSelected = activeAnchorId == anchor.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Ink800 else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) Beacon500.copy(alpha = 0.6f) else Ink800,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.setDefaultNeighborhoodAnchor(anchor.id)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(
                                                if (isSelected) Beacon500.copy(alpha = 0.2f) else Ink800,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (isSelected) Beacon400 else Mist400,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            anchor.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Beacon400 else Mist100,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            anchor.subtitle,
                                            fontSize = 10.5.sp,
                                            color = Mist400,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setDefaultNeighborhoodAnchor(anchor.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reset Navigation Defaults Button
                    OutlinedButton(
                        onClick = {
                            viewModel.resetWorkspaceNavigationToDefaults()
                            Toast.makeText(context, "Navigation & workspace reset to defaults", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Ink700),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon400)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Navigation & Workspace to Defaults", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 2: Search, Accessibility & Demographic Presets
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_section_filters")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SECTION 2", color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                "Filters & Presets",
                                fontWeight = FontWeight.Bold,
                                color = Beacon500,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(1) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                        ) {
                            Text("Index ↑", color = Mist400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        "Set global demographic, accessibility, and dietary preferences for the directory and map.",
                        fontSize = 12.sp,
                        color = Mist400
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Physical Accessibility
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Accessibility & Mobility", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 14.sp)
                            Text("Filter for wheelchair-accessible entrances and step-free navigation.", fontSize = 11.sp, color = Mist400)
                        }
                        Switch(
                            checked = viewModel.accessibilityMobilityMode.value,
                            onCheckedChange = { viewModel.toggleAccessibilityMobilityMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccentColor,
                                checkedTrackColor = Beacon500
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Ink800)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Demographics
                    Text("Demographic Focus", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 14.sp)
                    Text("Select target population groups to highlight relevant services.", fontSize = 11.sp, color = Mist400)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val activeDemos = viewModel.demographicPresets.value
                    val demoPresets = DemographicPreset.entries
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in demoPresets.indices step 2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                for (j in 0..1) {
                                    if (i + j < demoPresets.size) {
                                        val preset = demoPresets[i + j]
                                        val isSelected = activeDemos.contains(preset)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.toggleDemographicPreset(preset) },
                                            label = { Text("${preset.icon} ${preset.title}", fontSize = 11.sp, color = if (isSelected) Ink950 else Mist200) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Beacon500,
                                                containerColor = Ink800
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                borderColor = if (isSelected) Beacon500 else Ink700,
                                                enabled = true, selected = isSelected
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Ink800)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Dietary
                    Text("Dietary Preferences", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 14.sp)
                    Text("Filter EBT locations and free dining services.", fontSize = 11.sp, color = Mist400)
                    Spacer(modifier = Modifier.height(10.dp))

                    val activeDietary = viewModel.dietaryPresets.value
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        DietaryPreset.entries.forEach { preset ->
                            val isSelected = activeDietary.contains(preset)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.toggleDietaryPreset(preset) },
                                label = { Text("${preset.icon} ${preset.title}", fontSize = 11.sp, color = if (isSelected) Ink950 else Mist200) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Beacon500,
                                    containerColor = Ink800
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) Beacon500 else Ink700,
                                    enabled = true, selected = isSelected
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Atmosphere & Theme Modes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SECTION 3", color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                "Theme Atmosphere",
                                fontWeight = FontWeight.Bold,
                                color = Beacon500,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(1) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                        ) {
                            Text("Index ↑", color = Mist400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        "Select background tone and street lighting mode",
                        fontSize = 12.sp,
                        color = Mist400
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = activeMode == mode
                        val previewColors = resolveCompassColors(mode, activeAccent, highContrast, compactListing)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Ink800 else Color.Transparent)
                                .clickable { viewModel.selectThemeMode(mode) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Color swatches preview
                                Row(
                                    modifier = Modifier
                                        .background(previewColors.background, RoundedCornerShape(6.dp))
                                        .border(1.dp, previewColors.border, RoundedCornerShape(6.dp))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(previewColors.surface, CircleShape)
                                            .border(0.5.dp, previewColors.border, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(previewColors.accent, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(previewColors.textPrimary, CircleShape)
                                    )
                                }

                                Column {
                                    Text(
                                        mode.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Beacon400 else Mist100,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        mode.subtitle,
                                        fontSize = 11.sp,
                                        color = Mist400
                                    )
                                }
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectThemeMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Accent Highlight Color
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SECTION 4", color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                "Compass Accent Color",
                                fontWeight = FontWeight.Bold,
                                color = Beacon500,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(1) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                        ) {
                            Text("Index ↑", color = Mist400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        "Primary color for action buttons, active navigation, and priority badges",
                        fontSize = 12.sp,
                        color = Mist400
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppAccentColor.entries.forEach { accent ->
                            val isSelected = activeAccent == accent
                            val subtitle = when (accent) {
                                AppAccentColor.BEACON -> "Warm golden beacon illumination"
                                AppAccentColor.GOLDEN_GATE -> "Iconic bridge international orange"
                                AppAccentColor.PACIFIC_EMERALD -> "Vibrant coastal marine green"
                                AppAccentColor.OCEAN_CYAN -> "Cool Pacific blue & high clarity"
                                AppAccentColor.MISSION_PURPLE -> "Rich Mission district twilight violet"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Ink800 else Color.Transparent)
                                    .clickable { viewModel.selectAccentColor(accent) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(accent.primary, CircleShape)
                                            .border(
                                                if (isSelected) 2.dp else 1.dp,
                                                if (isSelected) Mist100 else Color.Transparent,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Text(
                                                "✓",
                                                color = if (accent == AppAccentColor.BEACON) Color.Black else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            accent.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) accent.primary else Mist100,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            subtitle,
                                            fontSize = 11.sp,
                                            color = Mist400
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectAccentColor(accent) },
                                    colors = RadioButtonDefaults.colors(selectedColor = accent.primary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Text Scaling & Readability
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SECTION 5", color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                "Text Scaling & Readability",
                                fontWeight = FontWeight.Bold,
                                color = Beacon500,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(1) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                        ) {
                            Text("Index ↑", color = Mist400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        "Enlarge text for walking outdoors, direct sunlight, or easier scanning",
                        fontSize = 12.sp,
                        color = Mist400
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFontScale.entries.forEach { scale ->
                            val isSelected = activeFontScale == scale
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Beacon500 else Ink800)
                                    .border(
                                        1.dp,
                                        if (isSelected) Beacon400 else Ink700,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectFontScale(scale) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        scale.displayName,
                                        color = if (isSelected) OnAccentColor else Mist100,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${(scale.scaleFactor * 100).toInt()}%",
                                        color = if (isSelected) OnAccentColor.copy(alpha = 0.8f) else Mist400,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink800, RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "Preview: St. Anthony's Dining Room • Open 10:00 AM – 1:30 PM",
                            color = Mist100,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Section 5: Street Display & Contrast Configurations
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SECTION 6", color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                "Street Display Configuration",
                                fontWeight = FontWeight.Bold,
                                color = Beacon500,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(1) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                        ) {
                            Text("Index ↑", color = Mist400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        "Fine-tune accessibility and layout density",
                        fontSize = 12.sp,
                        color = Mist400
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // High Contrast Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                "High-Contrast Outlines",
                                fontWeight = FontWeight.Bold,
                                color = Mist100,
                                fontSize = 14.sp
                            )
                            Text(
                                "Sharpen card edges and borders for bright daylight reflection",
                                fontSize = 11.sp,
                                color = Mist400
                            )
                        }
                        Switch(
                            checked = highContrast,
                            onCheckedChange = { viewModel.toggleHighContrast(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccentColor,
                                checkedTrackColor = Beacon500
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Ink800)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Compact Listing Density Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                "Compact Card Density",
                                fontWeight = FontWeight.Bold,
                                color = Mist100,
                                fontSize = 14.sp
                            )
                            Text(
                                "Condense cards to see more services per screen without scrolling",
                                fontSize = 11.sp,
                                color = Mist400
                            )
                        }
                        Switch(
                            checked = compactListing,
                            onCheckedChange = { viewModel.toggleCompactListing(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccentColor,
                                checkedTrackColor = Beacon500
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reset Appearance Defaults Button
                    OutlinedButton(
                        onClick = {
                            viewModel.resetThemingToDefaults()
                            Toast.makeText(context, "Theme reset to defaults", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Ink700),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon400)
                    ) {
                        Text("Reset Appearance to Defaults", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 7: Map & Cartography Controls (Chunk 2)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_section_map")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SECTION 7", color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                "Map & Cartography",
                                fontWeight = FontWeight.Bold,
                                color = Beacon500,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(1) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                        ) {
                            Text("Index ↑", color = Mist400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        "Configure map vector layers, marker clustering distance, and battery saver radar mode",
                        fontSize = 12.sp,
                        color = Mist400
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subsection A: Map Vector Layers
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = null,
                            tint = Beacon500,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Map Vector Layers",
                            fontWeight = FontWeight.Bold,
                            color = Mist100,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        "Toggle cartographic elements and transit overlays rendered on the canvas",
                        fontSize = 11.sp,
                        color = Mist400
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val mapTransit by viewModel.mapLayerTransit
                    val mapNeighborhoods by viewModel.mapLayerNeighborhoods
                    val mapLandmarks by viewModel.mapLayerLandmarks

                    // 1. Transit Lines & Stops Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (mapTransit) Beacon500.copy(alpha = 0.08f) else Ink800)
                            .border(1.dp, if (mapTransit) Beacon500.copy(alpha = 0.4f) else Ink700, RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleMapLayerTransit(!mapTransit) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFF0099D8).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DirectionsTransit,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Transit Lines & Subway",
                                    fontWeight = FontWeight.Bold,
                                    color = Mist100,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "BART tunnels, Muni Metro light rail routes, and station nodes",
                                    fontSize = 10.5.sp,
                                    color = Mist400
                                )
                            }
                        }
                        Switch(
                            checked = mapTransit,
                            onCheckedChange = { viewModel.toggleMapLayerTransit(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccentColor,
                                checkedTrackColor = Beacon500
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. Neighborhood Boundary Outlines Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (mapNeighborhoods) Beacon500.copy(alpha = 0.08f) else Ink800)
                            .border(1.dp, if (mapNeighborhoods) Beacon500.copy(alpha = 0.4f) else Ink700, RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleMapLayerNeighborhoods(!mapNeighborhoods) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Neighborhood Boundaries",
                                    fontWeight = FontWeight.Bold,
                                    color = Mist100,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Outlined zones & labels for Tenderloin, SoMa, Mission & more",
                                    fontSize = 10.5.sp,
                                    color = Mist400
                                )
                            }
                        }
                        Switch(
                            checked = mapNeighborhoods,
                            onCheckedChange = { viewModel.toggleMapLayerNeighborhoods(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccentColor,
                                checkedTrackColor = Beacon500
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Street Names & Landmark Badges Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (mapLandmarks) Beacon500.copy(alpha = 0.08f) else Ink800)
                            .border(1.dp, if (mapLandmarks) Beacon500.copy(alpha = 0.4f) else Ink700, RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleMapLayerLandmarks(!mapLandmarks) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationCity,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Street Names & Landmarks",
                                    fontWeight = FontWeight.Bold,
                                    color = Mist100,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "SF landmark badges (City Hall, Ferry Bldg) and street corridors",
                                    fontSize = 10.5.sp,
                                    color = Mist400
                                )
                            }
                        }
                        Switch(
                            checked = mapLandmarks,
                            onCheckedChange = { viewModel.toggleMapLayerLandmarks(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccentColor,
                                checkedTrackColor = Beacon500
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Ink800)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Subsection B: Marker Clustering Density
                    val activeClustering by viewModel.mapClusteringMode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Grain,
                            contentDescription = null,
                            tint = Beacon500,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Marker Clustering Density",
                            fontWeight = FontWeight.Bold,
                            color = Mist100,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        "Controls how closely grouped map pins merge into numbered cluster bubbles",
                        fontSize = 11.sp,
                        color = Mist400
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MapClusteringMode.entries.forEach { mode ->
                            val isSelected = activeClustering == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Beacon500 else Ink800)
                                    .border(
                                        1.dp,
                                        if (isSelected) Beacon400 else Ink700,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectMapClusteringMode(mode) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        mode.displayName,
                                        color = if (isSelected) OnAccentColor else Mist100,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        mode.subtitle,
                                        color = if (isSelected) OnAccentColor.copy(alpha = 0.85f) else Mist400,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Ink800)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Subsection C: Performance & Battery Saver Mode
                    val mapReducedMotion by viewModel.mapReducedMotion
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(if (mapReducedMotion) Emerald500.copy(alpha = 0.2f) else Ink800, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.BatterySaver,
                                    contentDescription = null,
                                    tint = if (mapReducedMotion) Emerald500 else Mist400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Battery Saver / Reduced Motion",
                                    fontWeight = FontWeight.Bold,
                                    color = Mist100,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Disables continuous GPS radar pulse waves and simplifies rendering to save battery outdoors",
                                    fontSize = 11.sp,
                                    color = Mist400
                                )
                            }
                        }
                        Switch(
                            checked = mapReducedMotion,
                            onCheckedChange = { viewModel.toggleMapReducedMotion(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccentColor,
                                checkedTrackColor = Beacon500
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reset Map Defaults Button
                    OutlinedButton(
                        onClick = {
                            viewModel.resetMapSettingsToDefaults()
                            Toast.makeText(context, "Map & cartography settings reset to defaults", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Ink700),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon400)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Map Settings to Defaults", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 7: Launcher Identity Style
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SECTION 8", color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Text("Launcher Identity Style", fontWeight = FontWeight.Bold, color = Beacon500, fontSize = 16.sp)
                        }
                        TextButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(1) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                        ) {
                            Text("Index ↑", color = Mist400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text("Switch launcher shortcut theme & branding badge", fontSize = 12.sp, color = Mist400)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    iconChoices.forEach { (key, label) ->
                        val isSelected = viewModel.currentAppIconKey.value == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectAppIcon(key) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Beacon400 else Mist100,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectAppIcon(key) },
                                colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                            )
                        }
                    }
                }
            }
        }

        // Section 8: Data Portability & Backup (Export & Import)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                    Text("SECTION 9", color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                                Text(
                                    "Data Portability & Backup",
                                    fontWeight = FontWeight.Bold,
                                    color = Beacon500,
                                    fontSize = 16.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Ink800, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Offline JSON", color = Emerald500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Download or restore your saved favorites, private notes, visit history, and checklist tasks so personal data is never lost.",
                                fontSize = 12.sp,
                                color = Mist400
                            )
                        }
                        TextButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(1) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                        ) {
                            Text("Index ↑", color = Mist400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Current Personal Data Inventory Summary Pills
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink950, RoundedCornerShape(8.dp))
                            .border(1.dp, Ink800, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "YOUR PERSONAL RECORDS READY TO BACK UP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Mist400,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Favorites Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Ink900, RoundedCornerShape(6.dp))
                                    .border(1.dp, Ink700, RoundedCornerShape(6.dp))
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$totalFavs", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Beacon400)
                                    Text("Favorites", fontSize = 10.sp, color = Mist400)
                                }
                            }
                            // Notes Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Ink900, RoundedCornerShape(6.dp))
                                    .border(1.dp, Ink700, RoundedCornerShape(6.dp))
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$totalNotes", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Beacon400)
                                    Text("Notes", fontSize = 10.sp, color = Mist400)
                                }
                            }
                            // Visits Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Ink900, RoundedCornerShape(6.dp))
                                    .border(1.dp, Ink700, RoundedCornerShape(6.dp))
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$totalVisits", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Beacon400)
                                    Text("Visits", fontSize = 10.sp, color = Mist400)
                                }
                            }
                            // Tasks Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Ink900, RoundedCornerShape(6.dp))
                                    .border(1.dp, Ink700, RoundedCornerShape(6.dp))
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$totalTasks", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Beacon400)
                                    Text("Tasks", fontSize = 10.sp, color = Mist400)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row 1: Export Data (Download) & Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                exportLauncher.launch("compass_sf_backup_$timeStamp.json")
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp)
                                .testTag("export_data_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = Ink950),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = "Export Data", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export (Download JSON)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val json = viewModel.getExportJson()
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Compass SF Backup JSON")
                                            putExtra(Intent.EXTRA_TEXT, json)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Backup JSON"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .heightIn(min = 48.dp)
                                .testTag("share_backup_button"),
                            border = BorderStroke(1.dp, Ink700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist100),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share Backup", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons Row 2: Import Data (Upload) & Paste JSON
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp)
                                .testTag("import_data_button"),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Ink800, contentColor = Mist100),
                            border = BorderStroke(1.dp, Ink700),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = "Import Data", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import (Upload JSON)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                pastedJsonText = ""
                                showPasteJsonDialog = true
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .heightIn(min = 48.dp)
                                .testTag("paste_json_button"),
                            border = BorderStroke(1.dp, Ink700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist200),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = "Paste JSON", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste Text", fontSize = 12.sp)
                        }
                    }

                    // Export Success Banner
                    viewModel.exportSuccessMessage.value?.let { successMsg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Emerald500.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, Emerald500.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("✅", fontSize = 16.sp)
                                Text(successMsg, color = Mist100, fontSize = 12.sp)
                            }
                            IconButton(
                                onClick = { viewModel.dismissExportSuccess() },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Mist400, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Import Success Banner
                    viewModel.lastImportResult.value?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Emerald500.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, Emerald500.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🎉", fontSize = 16.sp)
                                    Text("Backup Restored Successfully!", fontWeight = FontWeight.Bold, color = Emerald500, fontSize = 13.sp)
                                }
                                IconButton(
                                    onClick = { viewModel.dismissLastImportResult() },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Mist400, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Favorites updated: ${result.favoritesUpdated}", fontSize = 12.sp, color = Mist200)
                            Text("• Private notes merged: ${result.notesUpdated}", fontSize = 12.sp, color = Mist200)
                            Text("• Visit history logs added: ${result.visitsRestored}", fontSize = 12.sp, color = Mist200)
                            Text("• Checklist tasks added: ${result.tasksRestored}", fontSize = 12.sp, color = Mist200)
                            if (result.customPlacesRestored > 0) {
                                Text("• Custom community places added: ${result.customPlacesRestored}", fontSize = 12.sp, color = Mist200)
                            }
                        }
                    }

                    // Import Error Banner
                    viewModel.importErrorMessage.value?.let { errMsg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⚠️", fontSize = 16.sp)
                                Text(errMsg, color = Mist100, fontSize = 12.sp)
                            }
                            IconButton(
                                onClick = { viewModel.dismissImportDialog() },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Mist400, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section 9: Hidden Resources Manager
        if (hiddenResources.isNotEmpty() || hiddenRmp.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SECTION 9", color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Text("Hidden Resources Manager", color = Beacon500, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { coroutineScope.launch { listState.animateScrollToItem(1) } },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                    ) {
                        Text("Index ↑", color = Mist400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            items(hiddenResources) { res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Ink900, RoundedCornerShape(8.dp))
                        .border(1.dp, Ink700, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(res.name, color = Mist100, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Button(
                        onClick = { viewModel.toggleResourceHidden(res) },
                        colors = ButtonDefaults.buttonColors(containerColor = Ink800)
                    ) {
                        Text("Unhide", color = Beacon500, fontSize = 12.sp)
                    }
                }
            }

            items(hiddenRmp) { loc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Ink900, RoundedCornerShape(8.dp))
                        .border(1.dp, Ink700, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(loc.name, color = Mist100, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Button(
                        onClick = { viewModel.toggleRmpHidden(loc) },
                        colors = ButtonDefaults.buttonColors(containerColor = Ink800)
                    ) {
                        Text("Unhide", color = Beacon500, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 10: App Statistics & Data Storage Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                val secNum = if (hiddenResources.isNotEmpty() || hiddenRmp.isNotEmpty()) "SECTION 11" else "SECTION 10"
                                Text(secNum, color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Text("Offline Guide Statistics & Storage", fontWeight = FontWeight.Bold, color = Beacon500, fontSize = 14.sp)
                        }
                        TextButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(1) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                        ) {
                            Text("Index ↑", color = Mist400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Total Directory Resources: ${resources.size}", color = Mist100, fontSize = 12.sp)
                    Text("• SF Service Guide (ShelterTech): ${resources.filter { it.source.contains("ShelterTech", ignoreCase = true) }.size}", color = Mist100, fontSize = 12.sp)
                    Text("• DataSF Municipal Services (data.sf.gov): ${resources.filter { it.source.contains("DataSF", ignoreCase = true) }.size}", color = Mist100, fontSize = 12.sp)
                    Text("• 211 Bay Area Human Services (Eden I&R): ${resources.filter { it.source.contains("211", ignoreCase = true) }.size}", color = Mist100, fontSize = 12.sp)
                    Text("• San Francisco CalFresh EBT Restaurants: ${rmpLocations.size}", color = Mist100, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("All data is stored locally in Room database for offline survival access.", color = Mist400, fontSize = 11.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    AnimatedVisibility(
        visible = showScrollToTop,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 90.dp)
    ) {
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            containerColor = Beacon500,
            contentColor = Ink950,
            modifier = Modifier
                .size(48.dp)
                .testTag("scroll_to_top_button")
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Scroll to top index",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

    // --- Import Confirmation Dialog ---
    val importPreview = viewModel.importPreviewState.value
    if (importPreview != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportDialog() },
            containerColor = Ink900,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📥", fontSize = 20.sp)
                    Text("Restore Backup Data", fontWeight = FontWeight.Bold, color = Beacon500, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Found records from backup created on ${importPreview.exportedDate}:",
                        fontSize = 13.sp,
                        color = Mist200
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink800, RoundedCornerShape(8.dp))
                            .border(1.dp, Ink700, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Saved Favorites:", fontSize = 12.sp, color = Mist400)
                            Text("${importPreview.favoriteCount} items", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Beacon400)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Private User Notes:", fontSize = 12.sp, color = Mist400)
                            Text("${importPreview.notesCount} notes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Beacon400)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Visit History Logs:", fontSize = 12.sp, color = Mist400)
                            Text("${importPreview.visitCount} visits", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Beacon400)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Checklist Tasks:", fontSize = 12.sp, color = Mist400)
                            Text("${importPreview.taskCount} tasks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Beacon400)
                        }
                        if (importPreview.customPlacesCount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Custom Places:", fontSize = 12.sp, color = Mist400)
                                Text("${importPreview.customPlacesCount} places", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Beacon400)
                            }
                        }
                    }

                    Text(
                        "Safe Merge: Restoring will merge your personal bookmarks, notes, visit records, and tasks without removing existing directory entries.",
                        fontSize = 11.sp,
                        color = Mist400
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmRestore { result ->
                            Toast.makeText(context, "Restored ${result.favoritesUpdated} favorites, ${result.notesUpdated} notes, ${result.visitsRestored} visits, ${result.tasksRestored} tasks", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = Ink950),
                    modifier = Modifier.testTag("confirm_restore_button"),
                    enabled = !viewModel.isImporting.value
                ) {
                    if (viewModel.isImporting.value) {
                        CircularProgressIndicator(color = Ink950, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restoring...", fontSize = 13.sp)
                    } else {
                        Text("Restore & Merge", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissImportDialog() },
                    border = BorderStroke(1.dp, Ink700),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist200),
                    modifier = Modifier.testTag("cancel_restore_button")
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }
            }
        )
    }

    // --- Paste JSON Text Dialog ---
    if (showPasteJsonDialog) {
        AlertDialog(
            onDismissRequest = { showPasteJsonDialog = false },
            containerColor = Ink900,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = null, tint = Beacon500)
                    Text("Paste JSON Backup", fontWeight = FontWeight.Bold, color = Beacon500, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste the raw backup JSON text below (convenient if copied from an email, clipboard, or note):",
                        fontSize = 12.sp,
                        color = Mist400
                    )
                    OutlinedTextField(
                        value = pastedJsonText,
                        onValueChange = { pastedJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("{\"metadata\": {...}, ...}", color = Mist400, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Ink800,
                            unfocusedContainerColor = Ink800,
                            focusedBorderColor = Beacon500,
                            unfocusedBorderColor = Ink700,
                            focusedTextColor = Mist100,
                            unfocusedTextColor = Mist100
                        ),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pastedJsonText.isNotBlank()) {
                            showPasteJsonDialog = false
                            viewModel.loadImportJson(pastedJsonText)
                        } else {
                            Toast.makeText(context, "Please paste JSON text first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = Ink950),
                    enabled = pastedJsonText.isNotBlank()
                ) {
                    Text("Inspect & Restore", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPasteJsonDialog = false },
                    border = BorderStroke(1.dp, Ink700),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist200)
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }
            }
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
}

// --- Common UI Components ---

@Composable
fun ResourceCard(
    resource: Resource,
    distanceMiles: Double? = null,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
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
                    "Settings Presets Active",
                    color = Beacon500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Results are currently filtered by your profile.",
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

package com.example.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
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

// --- Now Screen (Dashboard) ---
@Composable
fun NowScreen(
    viewModel: CompassViewModel,
    onNavigateToFind: (String) -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    val resources by viewModel.allResources.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    
    val openNowList = remember(resources) {
        resources.filter { !it.hidden && isResourceOpen(it.open24, it.hours) }.take(4)
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

        // Quick Category Row
        item {
            Text(
                text = "Quick Search",
                style = MaterialTheme.typography.titleMedium,
                color = Mist100,
                fontWeight = FontWeight.SemiBold
            )
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
                        border = BorderStroke(1.dp, Ink700)
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
                                    .clickable { viewModel.toggleTask(task) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "Unchecked",
                                    tint = Beacon500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
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
            Text(
                text = "Open Right Now in SF",
                style = MaterialTheme.typography.titleMedium,
                color = Mist100,
                fontWeight = FontWeight.SemiBold
            )
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

// --- Find Screen (Search & Filtering list) ---
@Composable
fun FindScreen(
    viewModel: CompassViewModel,
    initialCategory: String,
    onNavigateToDetail: (Int) -> Unit
) {
    val resources by viewModel.allResources.collectAsState()
    
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

    val filteredList = remember(resources, searchQuery, selectedCategory, selectedNeighborhood, selectedSource, filterOpenNow, filterFavorites, filterHidden) {
        resources.filter { res ->
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

            matchesSearch && matchesCategory && matchesNeighborhood && matchesSource && matchesOpen && matchesFav && matchesHidden
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
                .padding(16.dp)
                .testTag("search_input"),
            placeholder = { Text("Search meals, clinics, shelter, IDs, sources...", color = Mist400) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Mist400) },
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

        // Categories chip row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
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
                    border = BorderStroke(1.dp, if (isSelected) Beacon500 else Ink700)
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
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
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
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
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

            // Quick toggles
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = { filterOpenNow = !filterOpenNow },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (filterOpenNow) Beacon500 else Ink900, RoundedCornerShape(6.dp)),
                ) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = "Open Now",
                        tint = if (filterOpenNow) Ink950 else Mist400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { filterFavorites = !filterFavorites },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (filterFavorites) Beacon500 else Ink900, RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        if (filterFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorites",
                        tint = if (filterFavorites) Ink950 else Mist400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { filterHidden = !filterHidden },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (filterHidden) Rose500 else Ink900, RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        if (filterHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Hidden Items",
                        tint = if (filterHidden) Mist100 else Mist400,
                        modifier = Modifier.size(18.dp)
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
                text = "Showing ${filteredList.size} matches",
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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = "List View", tint = if (!isMapView) Ink950 else Mist400, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("List", color = if (!isMapView) Ink950 else Mist400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isMapView = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isMapView) Beacon500 else Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Map View", tint = if (isMapView) Ink950 else Mist400, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Map", color = if (isMapView) Ink950 else Mist400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isMapView) {
            ResourceMapView(
                resources = filteredList,
                onNavigateToDetail = onNavigateToDetail,
                onFavoriteClick = { res -> viewModel.toggleResourceFavorite(res) }
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
}

@Composable
fun ResourceMapView(
    resources: List<Resource>,
    onNavigateToDetail: (Int) -> Unit,
    onFavoriteClick: (Resource) -> Unit
) {
    var selectedResource by remember { mutableStateOf<Resource?>(null) }
    
    fun getNeighborhoodCoords(neighborhood: String): Pair<Float, Float> {
        return when (neighborhood.lowercase()) {
            "tenderloin" -> 0.45f to 0.42f
            "soma" -> 0.60f to 0.55f
            "mission" -> 0.50f to 0.70f
            "financial district", "financial" -> 0.70f to 0.35f
            "bayview" -> 0.80f to 0.85f
            "haight" -> 0.25f to 0.50f
            "marina" -> 0.45f to 0.15f
            "civic center" -> 0.40f to 0.45f
            "castro" -> 0.35f to 0.65f
            else -> {
                val hash = Math.abs(neighborhood.hashCode()) % 100
                Pair(0.2f + (hash % 60) / 100f, 0.2f + ((hash / 10) % 60) / 100f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
    ) {
        val streetColor = Ink800
        val marketColor = Ink700

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Draw SF Bay water on east
            drawRect(
                color = Color(0xFF0F2027),
                topLeft = androidx.compose.ui.geometry.Offset(canvasWidth * 0.75f, 0f),
                size = androidx.compose.ui.geometry.Size(canvasWidth * 0.25f, canvasHeight)
            )

            // Grid lines
            for (i in 1..5) {
                val y = canvasHeight * (i / 6f)
                drawLine(
                    color = streetColor,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(canvasWidth * 0.75f, y),
                    strokeWidth = 2f
                )
            }
            for (j in 1..5) {
                val x = canvasWidth * (j / 6f) * 0.75f
                drawLine(
                    color = streetColor,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, canvasHeight),
                    strokeWidth = 2f
                )
            }

            // Market Street
            drawLine(
                color = marketColor,
                start = androidx.compose.ui.geometry.Offset(canvasWidth * 0.75f, canvasHeight * 0.3f),
                end = androidx.compose.ui.geometry.Offset(canvasWidth * 0.2f, canvasHeight * 0.6f),
                strokeWidth = 4f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Ink900.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Ink700)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Beacon500, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SF Area • ${resources.size} Active Pins", color = Mist100, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text("Tap any pin to inspect", color = Mist400, fontSize = 10.sp)
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()

            resources.forEach { res ->
                val (nx, ny) = getNeighborhoodCoords(res.neighborhood)
                val pinX = (nx * width * 0.75f).coerceIn(24f, width - 24f)
                val pinY = (ny * height).coerceIn(40f, height - 100f)

                val isSelected = selectedResource?.id == res.id
                val pinColor = when (res.category) {
                    "food" -> Beacon500
                    "shelter" -> Color(0xFF38BDF8)
                    "hygiene" -> Color(0xFF34D399)
                    "health" -> Color(0xFFF43F5E)
                    "mental" -> Color(0xFFFB923C)
                    else -> Color(0xFFA78BFA)
                }

                Box(
                    modifier = Modifier
                        .offset { androidx.compose.ui.unit.IntOffset((pinX - 16).toInt(), (pinY - 16).toInt()) }
                        .size(32.dp)
                        .clickable { selectedResource = res },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Mist100 else pinColor,
                        shadowElevation = if (isSelected) 8.dp else 4.dp,
                        border = BorderStroke(2.dp, if (isSelected) Beacon500 else Ink950)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (res.category) {
                                    "food" -> "🍲"
                                    "shelter" -> "🏠"
                                    "hygiene" -> "🚿"
                                    "health" -> "🩺"
                                    "mental" -> "💬"
                                    else -> "📍"
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        selectedResource?.let { res ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Ink900),
                    border = BorderStroke(1.dp, Beacon500),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Beacon500.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = res.category.uppercase(),
                                            color = Beacon400,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = res.neighborhood,
                                        color = Mist400,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = res.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Mist100,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = res.address,
                                    color = Mist400,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(
                                onClick = { selectedResource = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close preview", tint = Mist400)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedResource = null },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist100),
                                border = BorderStroke(1.dp, Ink700),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Dismiss", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { onNavigateToDetail(res.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Beacon500),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("View Details", color = OnAccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
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
                    modifier = Modifier.clickable { viewModel.askOpenOnly.value = !viewModel.askOpenOnly.value }
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
                Card(
                    colors = CardDefaults.cardColors(containerColor = Ink900),
                    border = BorderStroke(1.dp, Rose500)
                ) {
                    Text(askError!!, color = Rose500, modifier = Modifier.padding(12.dp))
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

    val filteredList = remember(rmpLocations, searchQuery, selectedCuisine, selectedNeighborhood, filterFavorites, filterChains, filterOpenNow) {
        rmpLocations.filter { loc ->
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

            matchesSearch && matchesCuisine && matchesNeighborhood && matchesFav && matchesChain && matchesOpen
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
                                            Toast.makeText(context, "Cannot open dialer", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Beacon500),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = OnAccentColor, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Helpline", color = OnAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { showCardCodingNotice = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss notice",
                                        tint = Mist400,
                                        modifier = Modifier.size(16.dp)
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
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
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
                    .size(36.dp)
                    .background(if (filterOpenNow) Emerald500 else Ink900, RoundedCornerShape(6.dp))
                    .border(1.dp, Ink700, RoundedCornerShape(6.dp))
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Open Now",
                    tint = if (filterOpenNow) OnAccentColor else Mist400,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Chains toggle
            IconButton(
                onClick = { filterChains = !filterChains },
                modifier = Modifier
                    .size(36.dp)
                    .background(if (filterChains) Beacon500 else Ink900, RoundedCornerShape(6.dp))
                    .border(1.dp, Ink700, RoundedCornerShape(6.dp))
            ) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = "Chains only",
                    tint = if (filterChains) OnAccentColor else Mist400,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Favorites toggle
            IconButton(
                onClick = { filterFavorites = !filterFavorites },
                modifier = Modifier
                    .size(36.dp)
                    .background(if (filterFavorites) Beacon500 else Ink900, RoundedCornerShape(6.dp))
                    .border(1.dp, Ink700, RoundedCornerShape(6.dp))
            ) {
                Icon(
                    if (filterFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorites",
                    tint = if (filterFavorites) OnAccentColor else Mist400,
                    modifier = Modifier.size(18.dp)
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
                text = "${filteredList.size} participating EBT outlets",
                color = Mist400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (selectedCuisine != "all" || selectedNeighborhood != "all" || filterFavorites || filterChains || filterOpenNow || searchQuery.isNotBlank()) {
                Text(
                    text = "Reset filters",
                    color = Beacon400,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        searchQuery = ""
                        selectedCuisine = "all"
                        selectedNeighborhood = "all"
                        filterFavorites = false
                        filterChains = false
                        filterOpenNow = false
                    }
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
    val isParsing by viewModel.isParsing
    val draft = viewModel.draftResource.value
    val parseError by viewModel.parseError
    val isSaving by viewModel.isSavingResource

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
                        "Paste a street flyer, text message, website listing, or raw notes. The Navigator AI will parse it into a structured resource entry for your review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Mist400
                    )
                }

                item {
                    OutlinedTextField(
                        value = viewModel.addRawText.value,
                        onValueChange = { viewModel.addRawText.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("add_input"),
                        placeholder = { Text("Paste flyer, message or note here...", color = Mist400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Beacon500,
                            unfocusedBorderColor = Ink700,
                            focusedTextColor = Mist100,
                            unfocusedTextColor = Mist100
                        )
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.parseDraft() },
                            enabled = !isParsing && viewModel.addRawText.value.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("parse_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor)
                        ) {
                            if (isParsing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnAccentColor, strokeWidth = 2.dp)
                            } else {
                                Text("Parse via AI Guide", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { viewModel.createManualBlankDraft() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Ink900, contentColor = Mist100)
                        ) {
                            Text("Manual Blank")
                        }
                    }
                }

                if (parseError != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Ink900),
                            border = BorderStroke(1.dp, Rose500)
                        ) {
                            Text(parseError!!, color = Rose500, modifier = Modifier.padding(12.dp))
                        }
                    }
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

@Composable
fun TaskRow(task: Task, viewModel: CompassViewModel) {
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
        IconButton(onClick = { viewModel.deleteTask(task) }) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Rose500)
        }
    }
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
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${hl.phone}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.background(Ink800, RoundedCornerShape(20.dp))
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
    val resources by viewModel.allResources.collectAsState()
    val rmpLocations by viewModel.allRmpLocations.collectAsState()
    
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950)
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
                    "Personalize theme atmosphere, contrast, font scaling, and navigation styles",
                    style = MaterialTheme.typography.bodySmall,
                    color = Mist400
                )
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

        // Section 1: Atmosphere & Theme Modes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Theme Atmosphere",
                        fontWeight = FontWeight.Bold,
                        color = Beacon500,
                        fontSize = 16.sp
                    )
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

        // Section 2: Accent Highlight Color
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Compass Accent Color",
                        fontWeight = FontWeight.Bold,
                        color = Beacon500,
                        fontSize = 16.sp
                    )
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

        // Section 3: Text Scaling & Readability
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Text Scaling & Readability",
                        fontWeight = FontWeight.Bold,
                        color = Beacon500,
                        fontSize = 16.sp
                    )
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

        // Section 4: Street Display & Contrast Configurations
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Street Display Configuration",
                        fontWeight = FontWeight.Bold,
                        color = Beacon500,
                        fontSize = 16.sp
                    )
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

        // Section 5: Launcher Identity Style
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Launcher Identity Style", fontWeight = FontWeight.Bold, color = Beacon500, fontSize = 16.sp)
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

        // Section 6: Hidden Resources Manager
        if (hiddenResources.isNotEmpty() || hiddenRmp.isNotEmpty()) {
            item {
                Text("Hidden Resources Manager", color = Beacon500, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

        // Section 7: App Statistics & Data Storage Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(if (highContrast) 2.dp else 1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Offline Guide Statistics & Storage", fontWeight = FontWeight.Bold, color = Beacon500, fontSize = 14.sp)
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📍 Address: ${res.address}", color = Mist100)
                    if (res.neighborhood.isNotBlank()) {
                        Text("🏢 Neighborhood: ${res.neighborhood}", color = Mist100)
                    }
                    if (res.phone.isNotBlank()) {
                        Text("📞 Phone: ${res.phone}", color = Beacon400, modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${res.phone}"))
                            context.startActivity(intent)
                        })
                    }
                    if (res.hoursText.isNotBlank()) {
                        Text("⏰ Hours: ${res.hoursText}", color = Mist100)
                    }
                    if (res.website.isNotBlank()) {
                        Text("🌐 Website: ${res.website}", color = Beacon400, modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.website))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    if (res.source.isNotBlank()) {
                        Text("📋 Online Source: ${res.source}", color = Color(0xFF38BDF8), fontSize = 12.sp)
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
                IconButton(onClick = onFavoriteClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (resource.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (resource.favorite) Rose500 else Mist400,
                        modifier = Modifier.size(18.dp)
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
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Ink800, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(resource.category.replaceFirstChar { it.uppercase() }, color = Beacon400, fontSize = 10.sp)
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
                IconButton(onClick = onFavoriteClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (location.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (location.favorite) Rose500 else Mist400,
                        modifier = Modifier.size(20.dp)
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

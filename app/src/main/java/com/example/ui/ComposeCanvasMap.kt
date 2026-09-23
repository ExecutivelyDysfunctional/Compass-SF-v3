package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlin.math.hypot

/**
 * Geographic bounding box coordinates for San Francisco Peninsula.
 */
object SfMapBounds {
    const val MIN_LAT = 37.7050
    const val MAX_LAT = 37.8150
    const val MIN_LNG = -122.5200
    const val MAX_LNG = -122.3700

    const val SF_CENTER_LAT = 37.7749
    const val SF_CENTER_LNG = -122.4194

    const val DOWNTOWN_LAT = 37.7840
    const val DOWNTOWN_LNG = -122.4140
}

/**
 * Represents a mapped pin item (either single resource or grouped cluster).
 */
sealed class MapPinItem {
    data class Single(
        val resource: Resource,
        val latitude: Double,
        val longitude: Double,
        val isOpen: Boolean
    ) : MapPinItem()

    data class Cluster(
        val items: List<Single>,
        val latitude: Double,
        val longitude: Double
    ) : MapPinItem()
}

/**
 * Fullscreen Dedicated Map Screen.
 * Provides high-performance 60/120fps native vector map, live GPS radar,
 * search bar overlay, category filter chips, clustering, and quick action cards.
 */
@Composable
fun MapScreen(
    viewModel: CompassViewModel,
    initialCategory: String = "all",
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToFind: (String) -> Unit
) {
    val allResources by viewModel.allResources.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf(if (initialCategory.isBlank()) "all" else initialCategory) }
    var filterOpenOnly by remember { mutableStateOf(false) }

    val filteredResources = remember(
        allResources,
        searchQuery,
        selectedCategoryFilter,
        filterOpenOnly,
        viewModel.demographicPresets.value,
        viewModel.accessibilityMobilityMode.value,
        viewModel.dietaryPresets.value,
        viewModel.sourceShelterTechEnabled.value,
        viewModel.sourceDataSfEnabled.value,
        viewModel.source211Enabled.value,
        viewModel.sourceCommunityEnabled.value
    ) {
        allResources.filter { res ->
            if (res.hidden) return@filter false

            val matchesSearch = searchQuery.isBlank() ||
                    res.name.contains(searchQuery, ignoreCase = true) ||
                    res.neighborhood.contains(searchQuery, ignoreCase = true) ||
                    res.address.contains(searchQuery, ignoreCase = true) ||
                    res.category.contains(searchQuery, ignoreCase = true) ||
                    res.summary.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategoryFilter == "all" ||
                    res.category.equals(selectedCategoryFilter, ignoreCase = true) ||
                    res.alsoOffers.any { it.equals(selectedCategoryFilter, ignoreCase = true) }

            val matchesOpen = !filterOpenOnly || isResourceOpen(res.open24, res.hours)

            val matchesDemographic = PresetMatcher.matchesDemographic(res, viewModel.demographicPresets.value)
            val matchesAccessibility = PresetMatcher.matchesAccessibility(res, viewModel.accessibilityMobilityMode.value)
            val matchesDietary = PresetMatcher.matchesDietary(res, viewModel.dietaryPresets.value)
            val matchesSource = viewModel.isSourceAllowed(res.source)

            matchesSearch && matchesCategory && matchesOpen && matchesDemographic && matchesAccessibility && matchesDietary && matchesSource
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink950)) {
        // Native Vector Canvas Map Engine
        ComposeCanvasMap(
            resources = filteredResources,
            viewModel = viewModel,
            onNavigateToDetail = onNavigateToDetail,
            onFavoriteClick = { res -> viewModel.toggleResourceFavorite(res) },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedCategoryFilter = selectedCategoryFilter,
            onCategoryFilterChange = { selectedCategoryFilter = it },
            filterOpenOnly = filterOpenOnly,
            onFilterOpenOnlyToggle = { filterOpenOnly = !filterOpenOnly },
            onNavigateBack = onNavigateBack,
            onNavigateToFind = { onNavigateToFind(selectedCategoryFilter) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * High-performance, 100% offline Native Jetpack Compose Canvas Vector Map Engine.
 * Provides silky-smooth 60/120fps pan, pinch-to-zoom, SF cartographic roads,
 * category badges, dynamic clustering, live GPS radar, and bottom preview sheet.
 */
@Composable
fun ComposeCanvasMap(
    resources: List<Resource>,
    viewModel: CompassViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onFavoriteClick: (Resource) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    selectedCategoryFilter: String = "all",
    onCategoryFilterChange: ((String) -> Unit)? = null,
    filterOpenOnly: Boolean = false,
    onFilterOpenOnlyToggle: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToFind: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userLocation by viewModel.userLocation
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current

    // Map & Cartography Settings from ViewModel (Chunk 2)
    val mapLayerTransit by viewModel.mapLayerTransit
    val mapLayerNeighborhoods by viewModel.mapLayerNeighborhoods
    val mapLayerLandmarks by viewModel.mapLayerLandmarks
    val mapClusteringMode by viewModel.mapClusteringMode
    val mapReducedMotion by viewModel.mapReducedMotion

    // Resolve static colors for Canvas drawscope
    val canvasInk950 = Ink950
    val canvasInk900 = Ink900
    val canvasInk800 = Ink800
    val canvasInk700 = Ink700
    val canvasBeacon500 = Beacon500
    val canvasMist400 = Mist400
    val canvasEmerald500 = Emerald500
    val canvasSky500 = Color(0xFF38BDF8)

    // Viewport transform state (Zoom & Pan)
    var zoom by remember { mutableFloatStateOf(1.35f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Internal state if not controlled externally
    var internalCategoryFilter by remember { mutableStateOf(selectedCategoryFilter) }
    var internalOpenOnly by remember { mutableStateOf(filterOpenOnly) }
    var showRoadLabels by remember { mutableStateOf(true) }

    val activeCategoryFilter = if (onCategoryFilterChange != null) selectedCategoryFilter else internalCategoryFilter
    val activeOpenOnly = if (onFilterOpenOnlyToggle != null) filterOpenOnly else internalOpenOnly

    // Selected item for preview sheet
    var selectedResource by remember { mutableStateOf<Resource?>(null) }

    // Pulsing radar animation (Disabled when reduced motion / battery saver is active)
    val infiniteTransition = rememberInfiniteTransition(label = "mapRadar")
    val animatedPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val animatedPulseRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    val pulseAlpha = if (mapReducedMotion) 0.3f else animatedPulseAlpha
    val pulseRadiusScale = if (mapReducedMotion) 1.2f else animatedPulseRadiusScale

    // Filter resources based on top chips if not already filtered
    val mapFilteredResources = remember(resources, activeCategoryFilter, activeOpenOnly) {
        resources.filter { res ->
            val matchesCategory = activeCategoryFilter == "all" ||
                    res.category.equals(activeCategoryFilter, ignoreCase = true) ||
                    res.alsoOffers.any { it.equals(activeCategoryFilter, ignoreCase = true) }
            val matchesOpen = !activeOpenOnly || isResourceOpen(res.open24, res.hours)
            matchesCategory && matchesOpen
        }
    }

    // Convert resources to geo-located single items
    val locatedItems = remember(mapFilteredResources) {
        mapFilteredResources.mapNotNull { res ->
            val coords = LocationHelper.getCoordinates(res)
            if (coords != null) {
                MapPinItem.Single(
                    resource = res,
                    latitude = coords.first,
                    longitude = coords.second,
                    isOpen = isResourceOpen(res.open24, res.hours)
                )
            } else {
                null
            }
        }
    }

    // Category filter chips definition
    val filterCategories = listOf(
        "all" to "All Resources",
        "food" to "🍲 Food",
        "shelter" to "🏠 Shelter",
        "hygiene" to "🚿 Hygiene",
        "health" to "🩺 Medical",
        "mental" to "💬 Crisis",
        "documents" to "🪪 ID/Docs",
        "benefits" to "💳 Benefits",
        "legal" to "⚖️ Legal"
    )

    // Dynamic clustering radius threshold in pixels based on user setting (Tight: 24dp, Balanced: 38dp, Spread: 56dp)
    val clusterRadiusPx = remember(density, mapClusteringMode) {
        with(density) { mapClusteringMode.radiusDp.dp.toPx() }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(canvasInk950)
            .testTag("compose_canvas_map_container")
    ) {
        val canvasWidthPx = constraints.maxWidth.toFloat()
        val canvasHeightPx = constraints.maxHeight.toFloat()

        // Helper to convert lat/lng to Screen Pixel Offset
        fun geoToScreen(lat: Double, lng: Double): Offset {
            val normX = (lng - SfMapBounds.MIN_LNG) / (SfMapBounds.MAX_LNG - SfMapBounds.MIN_LNG)
            val normY = (SfMapBounds.MAX_LAT - lat) / (SfMapBounds.MAX_LAT - SfMapBounds.MIN_LAT)

            val baseW = canvasWidthPx
            val baseH = canvasHeightPx

            val mapOriginX = canvasWidthPx / 2f + panX
            val mapOriginY = canvasHeightPx / 2f + panY

            val x = mapOriginX + (normX.toFloat() - 0.5f) * baseW * zoom
            val y = mapOriginY + (normY.toFloat() - 0.5f) * baseH * zoom
            return Offset(x, y)
        }

        // Helper to convert screen coordinates back to Geo
        fun screenToGeo(screenX: Float, screenY: Float): Pair<Double, Double> {
            val mapOriginX = canvasWidthPx / 2f + panX
            val mapOriginY = canvasHeightPx / 2f + panY

            val normX = ((screenX - mapOriginX) / (canvasWidthPx * zoom)) + 0.5f
            val normY = ((screenY - mapOriginY) / (canvasHeightPx * zoom)) + 0.5f

            val lng = SfMapBounds.MIN_LNG + normX * (SfMapBounds.MAX_LNG - SfMapBounds.MIN_LNG)
            val lat = SfMapBounds.MAX_LAT - normY * (SfMapBounds.MAX_LAT - SfMapBounds.MIN_LAT)
            return Pair(lat, lng)
        }

        // Recenter function
        fun centerOnLocation(targetLat: Double, targetLng: Double, targetZoom: Float = 2.0f) {
            zoom = targetZoom
            val normX = (targetLng - SfMapBounds.MIN_LNG) / (SfMapBounds.MAX_LNG - SfMapBounds.MIN_LNG)
            val normY = (SfMapBounds.MAX_LAT - targetLat) / (SfMapBounds.MAX_LAT - SfMapBounds.MIN_LAT)
            panX = -((normX.toFloat() - 0.5f) * canvasWidthPx * targetZoom)
            panY = -((normY.toFloat() - 0.5f) * canvasHeightPx * targetZoom)
        }

        // Smart marker clustering calculation based on screen distance
        val clusteredPins = remember(locatedItems, zoom, panX, panY, canvasWidthPx, canvasHeightPx, clusterRadiusPx) {
            val result = mutableListOf<MapPinItem>()
            val visited = BooleanArray(locatedItems.size) { false }

            for (i in locatedItems.indices) {
                if (visited[i]) continue
                val p1 = locatedItems[i]
                val pos1 = geoToScreen(p1.latitude, p1.longitude)

                val clusterGroup = mutableListOf(p1)
                visited[i] = true

                // Only cluster when zoomed out (< 2.8x)
                if (zoom < 2.8f) {
                    for (j in (i + 1) until locatedItems.size) {
                        if (visited[j]) continue
                        val p2 = locatedItems[j]
                        val pos2 = geoToScreen(p2.latitude, p2.longitude)
                        val dist = hypot(pos1.x - pos2.x, pos1.y - pos2.y)
                        if (dist < clusterRadiusPx) {
                            clusterGroup.add(p2)
                            visited[j] = true
                        }
                    }
                }

                if (clusterGroup.size == 1) {
                    result.add(clusterGroup[0])
                } else {
                    val avgLat = clusterGroup.map { it.latitude }.average()
                    val avgLng = clusterGroup.map { it.longitude }.average()
                    result.add(MapPinItem.Cluster(clusterGroup, avgLat, avgLng))
                }
            }
            result
        }

        // --- HARDWARE-ACCELERATED VECTOR CANVAS ---
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        val oldZoom = zoom
                        val newZoom = (oldZoom * gestureZoom).coerceIn(0.85f, 5.0f)
                        zoom = newZoom

                        // Pan with bounds resistance
                        val maxPan = canvasWidthPx * newZoom * 0.7f
                        panX = (panX + pan.x).coerceIn(-maxPan, maxPan)
                        panY = (panY + pan.y).coerceIn(-maxPan, maxPan)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val (tapLat, tapLng) = screenToGeo(offset.x, offset.y)
                            val targetZoom = (zoom * 1.5f).coerceAtMost(4.5f)
                            centerOnLocation(tapLat, tapLng, targetZoom)
                        },
                        onTap = {
                            focusManager.clearFocus()
                            // Tap background dismisses bottom card
                            selectedResource = null
                        }
                    )
                }
        ) {
            // 1. Draw SF Bay & Ocean Water Canvas Background
            val waterColor = Color(0xFF07121E)
            val landColor = Color(0xFF10192A)
            val parkColor = Color(0xFF0F2C20)
            val highwayColor = Color(0xFF2C3E5E)
            val arterialColor = Color(0xFF1F2D44)
            val streetColor = Color(0xFF162032)

            // Fill water canvas
            drawRect(color = waterColor)

            // 2. Draw San Francisco Peninsula Land Mass Polygon
            val landPath = Path().apply {
                val ptFtPoint = geoToScreen(37.8100, -122.4770)
                val ptMarina = geoToScreen(37.8050, -122.4350)
                val ptFishermans = geoToScreen(37.8080, -122.4150)
                val ptEmbarcadero = geoToScreen(37.7950, -122.3920)
                val ptBayBridge = geoToScreen(37.7900, -122.3880)
                val ptMissionBay = geoToScreen(37.7700, -122.3850)
                val ptPotreroPoint = geoToScreen(37.7550, -122.3820)
                val ptHuntersPoint = geoToScreen(37.7300, -122.3720)
                val ptCandlestick = geoToScreen(37.7120, -122.3820)
                val ptBayshoreCounty = geoToScreen(37.7050, -122.4000)
                val ptDalyCitySouth = geoToScreen(37.7050, -122.4600)
                val ptOceanBeachSouth = geoToScreen(37.7080, -122.5050)
                val ptOceanBeachMid = geoToScreen(37.7550, -122.5080)
                val ptOceanBeachNorth = geoToScreen(37.7760, -122.5120)
                val ptCliffHouse = geoToScreen(37.7785, -122.5140)
                val ptLandsEnd = geoToScreen(37.7880, -122.5050)
                val ptBakerBeach = geoToScreen(37.7960, -122.4830)

                moveTo(ptFtPoint.x, ptFtPoint.y)
                lineTo(ptMarina.x, ptMarina.y)
                lineTo(ptFishermans.x, ptFishermans.y)
                lineTo(ptEmbarcadero.x, ptEmbarcadero.y)
                lineTo(ptBayBridge.x, ptBayBridge.y)
                lineTo(ptMissionBay.x, ptMissionBay.y)
                lineTo(ptPotreroPoint.x, ptPotreroPoint.y)
                lineTo(ptHuntersPoint.x, ptHuntersPoint.y)
                lineTo(ptCandlestick.x, ptCandlestick.y)
                lineTo(ptBayshoreCounty.x, ptBayshoreCounty.y)
                lineTo(ptDalyCitySouth.x, ptDalyCitySouth.y)
                lineTo(ptOceanBeachSouth.x, ptOceanBeachSouth.y)
                lineTo(ptOceanBeachMid.x, ptOceanBeachMid.y)
                lineTo(ptOceanBeachNorth.x, ptOceanBeachNorth.y)
                lineTo(ptCliffHouse.x, ptCliffHouse.y)
                lineTo(ptLandsEnd.x, ptLandsEnd.y)
                lineTo(ptBakerBeach.x, ptBakerBeach.y)
                close()
            }
            drawPath(path = landPath, color = landColor)
            drawPath(
                path = landPath,
                color = Color(0xFF1E3A5F),
                style = Stroke(width = 2.5f * zoom.coerceIn(1f, 2f), join = StrokeJoin.Round)
            )

            // 3. Draw Major SF Parks & Green Spaces
            val ggParkPath = Path().apply {
                val nw = geoToScreen(37.7730, -122.5100)
                val ne = geoToScreen(37.7710, -122.4530)
                val se = geoToScreen(37.7650, -122.4530)
                val sw = geoToScreen(37.7660, -122.5100)
                moveTo(nw.x, nw.y)
                lineTo(ne.x, ne.y)
                lineTo(se.x, se.y)
                lineTo(sw.x, sw.y)
                close()
            }
            drawPath(path = ggParkPath, color = parkColor)
            drawPath(path = ggParkPath, color = Color(0xFF1E5E43), style = Stroke(width = 1f))

            // Presidio
            val presidioPath = Path().apply {
                val p1 = geoToScreen(37.8020, -122.4780)
                val p2 = geoToScreen(37.8010, -122.4480)
                val p3 = geoToScreen(37.7890, -122.4520)
                val p4 = geoToScreen(37.7890, -122.4720)
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                lineTo(p4.x, p4.y)
                close()
            }
            drawPath(path = presidioPath, color = parkColor)

            // Mission Dolores Park & McLaren Park
            val dolores = geoToScreen(37.7597, -122.4270)
            drawCircle(color = parkColor, radius = 14f * zoom, center = dolores)

            val mclaren = geoToScreen(37.7180, -122.4180)
            drawCircle(color = parkColor, radius = 24f * zoom, center = mclaren)

            // 4. Secondary Street Grid (Tenderloin, SoMa, Mission)
            val gridStepLat = 0.006
            val gridStepLng = 0.008
            var gLat = 37.730
            while (gLat <= 37.800) {
                val start = geoToScreen(gLat, -122.440)
                val end = geoToScreen(gLat, -122.395)
                drawLine(
                    color = streetColor,
                    start = start,
                    end = end,
                    strokeWidth = 1f * zoom.coerceIn(0.8f, 1.6f)
                )
                gLat += gridStepLat
            }

            var gLng = -122.450
            while (gLng <= -122.390) {
                val start = geoToScreen(37.740, gLng)
                val end = geoToScreen(37.800, gLng)
                drawLine(
                    color = streetColor,
                    start = start,
                    end = end,
                    strokeWidth = 1f * zoom.coerceIn(0.8f, 1.6f)
                )
                gLng += gridStepLng
            }

            // 4b. Neighborhood Boundary Outlines Layer (Chunk 2 Toggle)
            if (mapLayerNeighborhoods) {
                val boundaryDash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))

                // Tenderloin
                val tlPath = Path().apply {
                    val p1 = geoToScreen(37.7875, -122.4195)
                    val p2 = geoToScreen(37.7875, -122.4085)
                    val p3 = geoToScreen(37.7815, -122.4095)
                    val p4 = geoToScreen(37.7788, -122.4160)
                    val p5 = geoToScreen(37.7815, -122.4195)
                    moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); lineTo(p5.x, p5.y); close()
                }
                drawPath(tlPath, Color(0xFFF59E0B).copy(alpha = 0.07f))
                drawPath(tlPath, Color(0xFFF59E0B).copy(alpha = 0.65f), style = Stroke(width = 1.8f * zoom.coerceIn(1f, 1.8f), pathEffect = boundaryDash))

                // Civic Center
                val ccPath = Path().apply {
                    val p1 = geoToScreen(37.7820, -122.4230)
                    val p2 = geoToScreen(37.7820, -122.4150)
                    val p3 = geoToScreen(37.7760, -122.4165)
                    val p4 = geoToScreen(37.7740, -122.4225)
                    moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
                }
                drawPath(ccPath, Color(0xFF38BDF8).copy(alpha = 0.07f))
                drawPath(ccPath, Color(0xFF38BDF8).copy(alpha = 0.65f), style = Stroke(width = 1.8f * zoom.coerceIn(1f, 1.8f), pathEffect = boundaryDash))

                // SoMa (South of Market)
                val somaPath = Path().apply {
                    val p1 = geoToScreen(37.7915, -122.3965)
                    val p2 = geoToScreen(37.7845, -122.3875)
                    val p3 = geoToScreen(37.7695, -122.3995)
                    val p4 = geoToScreen(37.7685, -122.4155)
                    val p5 = geoToScreen(37.7780, -122.4140)
                    moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); lineTo(p5.x, p5.y); close()
                }
                drawPath(somaPath, Color(0xFF8B5CF6).copy(alpha = 0.05f))
                drawPath(somaPath, Color(0xFF8B5CF6).copy(alpha = 0.55f), style = Stroke(width = 1.8f * zoom.coerceIn(1f, 1.8f), pathEffect = boundaryDash))

                // Mission District
                val missPath = Path().apply {
                    val p1 = geoToScreen(37.7685, -122.4265)
                    val p2 = geoToScreen(37.7685, -122.4075)
                    val p3 = geoToScreen(37.7475, -122.4060)
                    val p4 = geoToScreen(37.7475, -122.4245)
                    moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
                }
                drawPath(missPath, Color(0xFF10B981).copy(alpha = 0.06f))
                drawPath(missPath, Color(0xFF10B981).copy(alpha = 0.55f), style = Stroke(width = 1.8f * zoom.coerceIn(1f, 1.8f), pathEffect = boundaryDash))

                // Chinatown
                val chinaPath = Path().apply {
                    val p1 = geoToScreen(37.7985, -122.4095)
                    val p2 = geoToScreen(37.7985, -122.4040)
                    val p3 = geoToScreen(37.7895, -122.4040)
                    val p4 = geoToScreen(37.7895, -122.4095)
                    moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
                }
                drawPath(chinaPath, Color(0xFFF43F5E).copy(alpha = 0.07f))
                drawPath(chinaPath, Color(0xFFF43F5E).copy(alpha = 0.55f), style = Stroke(width = 1.8f * zoom.coerceIn(1f, 1.8f), pathEffect = boundaryDash))

                // Western Addition / Fillmore
                val fillmorePath = Path().apply {
                    val p1 = geoToScreen(37.7885, -122.4395)
                    val p2 = geoToScreen(37.7885, -122.4240)
                    val p3 = geoToScreen(37.7735, -122.4240)
                    val p4 = geoToScreen(37.7735, -122.4395)
                    moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
                }
                drawPath(fillmorePath, Color(0xFF06B6D4).copy(alpha = 0.05f))
                drawPath(fillmorePath, Color(0xFF06B6D4).copy(alpha = 0.5f), style = Stroke(width = 1.6f * zoom.coerceIn(1f, 1.8f), pathEffect = boundaryDash))

                // Castro / Eureka Valley
                val castroPath = Path().apply {
                    val p1 = geoToScreen(37.7685, -122.4410)
                    val p2 = geoToScreen(37.7685, -122.4290)
                    val p3 = geoToScreen(37.7545, -122.4290)
                    val p4 = geoToScreen(37.7545, -122.4410)
                    moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
                }
                drawPath(castroPath, Color(0xFFEC4899).copy(alpha = 0.05f))
                drawPath(castroPath, Color(0xFFEC4899).copy(alpha = 0.5f), style = Stroke(width = 1.6f * zoom.coerceIn(1f, 1.8f), pathEffect = boundaryDash))
            }

            // 5. Draw Major SF Arterial Corridors & Freeways
            // Market Street (Embarcadero -> Castro)
            val marketStart = geoToScreen(37.7940, -122.3950)
            val marketEnd = geoToScreen(37.7625, -122.4350)
            drawLine(
                color = Color(0xFF475569),
                start = marketStart,
                end = marketEnd,
                strokeWidth = 5f * zoom.coerceIn(1f, 2.5f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = canvasBeacon500.copy(alpha = 0.4f),
                start = marketStart,
                end = marketEnd,
                strokeWidth = 2f * zoom.coerceIn(1f, 2f),
                cap = StrokeCap.Round
            )

            // Van Ness Avenue / US-101 (North - South)
            val vnStart = geoToScreen(37.8040, -122.4240)
            val vnMid = geoToScreen(37.7700, -122.4200)
            val vnEnd = geoToScreen(37.7300, -122.4080)
            drawLine(color = highwayColor, start = vnStart, end = vnMid, strokeWidth = 3.5f * zoom.coerceIn(1f, 2f))
            drawLine(color = highwayColor, start = vnMid, end = vnEnd, strokeWidth = 3.5f * zoom.coerceIn(1f, 2f))

            // Geary Boulevard (East - West)
            val gearyStart = geoToScreen(37.7870, -122.4080)
            val gearyEnd = geoToScreen(37.7800, -122.5080)
            drawLine(color = arterialColor, start = gearyStart, end = gearyEnd, strokeWidth = 3f * zoom.coerceIn(1f, 2f))

            // Mission Street Corridor
            val missStart = geoToScreen(37.7810, -122.4110)
            val missEnd = geoToScreen(37.7120, -122.4430)
            drawLine(color = arterialColor, start = missStart, end = missEnd, strokeWidth = 3f * zoom.coerceIn(1f, 2f))

            // 3rd Street Corridor
            val thirdStart = geoToScreen(37.7870, -122.3990)
            val thirdEnd = geoToScreen(37.7120, -122.3920)
            drawLine(color = arterialColor, start = thirdStart, end = thirdEnd, strokeWidth = 3f * zoom.coerceIn(1f, 2f))

            // I-80 Bay Bridge
            val bayBridgeWest = geoToScreen(37.7850, -122.3980)
            val bayBridgeEast = geoToScreen(37.8000, -122.3650)
            drawLine(
                color = Color(0xFF64748B),
                start = bayBridgeWest,
                end = bayBridgeEast,
                strokeWidth = 4f * zoom.coerceIn(1f, 2.5f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
            )

            // 5b. Draw Transit Lines & Subway Corridors (Chunk 2 Toggle)
            if (mapLayerTransit) {
                // BART & Muni Metro Main Subway Spine (Market St to Balboa Park)
                val bartStops = listOf(
                    geoToScreen(37.7929, -122.3970), // Embarcadero
                    geoToScreen(37.7894, -122.4011), // Montgomery St
                    geoToScreen(37.7844, -122.4079), // Powell St
                    geoToScreen(37.7797, -122.4141), // Civic Center
                    geoToScreen(37.7650, -122.4196), // 16th St Mission
                    geoToScreen(37.7522, -122.4184), // 24th St Mission
                    geoToScreen(37.7331, -122.4338), // Glen Park
                    geoToScreen(37.7216, -122.4475)  // Balboa Park
                )

                // BART Line Glow & Solid Tracks
                for (k in 0 until bartStops.size - 1) {
                    drawLine(
                        color = Color(0xFF0099D8).copy(alpha = 0.45f),
                        start = bartStops[k],
                        end = bartStops[k + 1],
                        strokeWidth = 6f * zoom.coerceIn(1f, 1.8f),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF0099D8),
                        start = bartStops[k],
                        end = bartStops[k + 1],
                        strokeWidth = 3.2f * zoom.coerceIn(1f, 1.8f),
                        cap = StrokeCap.Round
                    )
                }

                // Central Subway / T-Third Line (Chinatown -> SoMa -> Mission Bay -> Bayview)
                val tThirdStops = listOf(
                    geoToScreen(37.7946, -122.4075), // Chinatown Rose Pak
                    geoToScreen(37.7876, -122.4070), // Union Square
                    geoToScreen(37.7766, -122.3949), // 4th & King (Caltrain)
                    geoToScreen(37.7680, -122.3880), // Mission Bay / Chase Center
                    geoToScreen(37.7300, -122.3900)  // Bayview / 3rd St
                )
                for (k in 0 until tThirdStops.size - 1) {
                    drawLine(
                        color = Color(0xFFE11D48).copy(alpha = 0.85f),
                        start = tThirdStops[k],
                        end = tThirdStops[k + 1],
                        strokeWidth = 2.8f * zoom.coerceIn(1f, 1.8f),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 4f))
                    )
                }

                // N-Judah Metro Light Rail (Market -> Duboce -> Sunset -> Ocean Beach)
                val nJudahStops = listOf(
                    geoToScreen(37.7929, -122.3970), // Embarcadero
                    geoToScreen(37.7695, -122.4330), // Church & Duboce
                    geoToScreen(37.7638, -122.4665), // 9th & Irving (Sunset)
                    geoToScreen(37.7605, -122.5090)  // Ocean Beach
                )
                for (k in 0 until nJudahStops.size - 1) {
                    drawLine(
                        color = Color(0xFF059669).copy(alpha = 0.85f),
                        start = nJudahStops[k],
                        end = nJudahStops[k + 1],
                        strokeWidth = 2.8f * zoom.coerceIn(1f, 1.8f),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f))
                    )
                }

                // Draw Subway & Rail Station Nodes
                val allTransitNodes = bartStops + tThirdStops + listOf(geoToScreen(37.7695, -122.4330), geoToScreen(37.7638, -122.4665))
                allTransitNodes.forEach { stationPos ->
                    drawCircle(color = canvasInk950, radius = 5.5f * zoom.coerceIn(1f, 1.6f), center = stationPos)
                    drawCircle(color = Color(0xFF0099D8), radius = 4.2f * zoom.coerceIn(1f, 1.6f), center = stationPos)
                    drawCircle(color = Color.White, radius = 2f * zoom.coerceIn(1f, 1.6f), center = stationPos)
                }
            }

            // 6. User Location Pulsing Beacon & Compass Ray
            if (userLocation != null) {
                val userPt = geoToScreen(userLocation!!.latitude, userLocation!!.longitude)

                // Pulsing radar ring
                drawCircle(
                    color = canvasSky500.copy(alpha = pulseAlpha),
                    radius = 24f * pulseRadiusScale,
                    center = userPt
                )
                // Solid GPS dot
                drawCircle(color = Color(0xFF0284C7), radius = 10f, center = userPt)
                drawCircle(color = Color.White, radius = 5f, center = userPt)
                drawCircle(color = canvasSky500, radius = 11f, center = userPt, style = Stroke(width = 2f))

                // Directional vector ray to selected resource
                if (selectedResource != null) {
                    val targetCoords = LocationHelper.getCoordinates(selectedResource!!)
                    if (targetCoords != null) {
                        val targetPt = geoToScreen(targetCoords.first, targetCoords.second)
                        drawLine(
                            color = canvasBeacon500,
                            start = userPt,
                            end = targetPt,
                            strokeWidth = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )
                    }
                }
            }
        }

        // --- NEIGHBORHOOD LABELS OVERLAY (When Zoom >= 1.15x and Neighborhoods Layer Active) ---
        if (showRoadLabels && mapLayerNeighborhoods && zoom >= 1.15f) {
            val neighborhoods = listOf(
                "Tenderloin" to Pair(37.7840, -122.4140),
                "Civic Center" to Pair(37.7795, -122.4175),
                "SoMa" to Pair(37.7780, -122.4050),
                "Mission" to Pair(37.7600, -122.4190),
                "Western Addition" to Pair(37.7830, -122.4320),
                "Downtown" to Pair(37.7880, -122.4075),
                "Castro" to Pair(37.7620, -122.4350),
                "Bayview" to Pair(37.7340, -122.3900),
                "Haight" to Pair(37.7690, -122.4460),
                "Chinatown" to Pair(37.7940, -122.4070),
                "Sunset" to Pair(37.7550, -122.4750),
                "Richmond" to Pair(37.7780, -122.4750)
            )

            neighborhoods.forEach { (name, coords) ->
                val pt = geoToScreen(coords.first, coords.second)
                if (pt.x in -60f..(canvasWidthPx + 60f) && pt.y in -40f..(canvasHeightPx + 40f)) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(pt.x.toInt() - 40, pt.y.toInt() - 10) }
                            .background(canvasInk950.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, canvasInk700, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = name,
                            color = canvasMist400.copy(alpha = 0.9f),
                            fontSize = (9.5f * zoom.coerceIn(1f, 1.35f)).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // --- LANDMARKS & MAJOR STREET NAMES OVERLAY (Chunk 2 Toggle) ---
        if (showRoadLabels && mapLayerLandmarks && zoom >= 1.25f) {
            val landmarks = listOf(
                "🏛️ City Hall" to Pair(37.7793, -122.4192),
                "⛴️ Ferry Bldg" to Pair(37.7955, -122.3937),
                "🏢 Transit Ctr" to Pair(37.7897, -122.3972),
                "🗼 Coit Tower" to Pair(37.8024, -122.4058),
                "🦀 Fisherman Wharf" to Pair(37.8080, -122.4177),
                "📚 Main Library" to Pair(37.7788, -122.4162),
                "⚾ Oracle Park" to Pair(37.7786, -122.3893),
                "🏀 Chase Center" to Pair(37.7680, -122.3875),
                "🏛️ Palace Fine Arts" to Pair(37.8020, -122.4485),
                "📡 Sutro Tower" to Pair(37.7552, -122.4528),
                "🌉 Golden Gate" to Pair(37.8077, -122.4750),
                "⛪ Mission Dolores" to Pair(37.7640, -122.4267)
            )

            landmarks.forEach { (label, coords) ->
                val pt = geoToScreen(coords.first, coords.second)
                if (pt.x in -60f..(canvasWidthPx + 60f) && pt.y in -40f..(canvasHeightPx + 40f)) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(pt.x.toInt() - 36, pt.y.toInt() - 18) }
                            .background(canvasInk900.copy(alpha = 0.88f), RoundedCornerShape(6.dp))
                            .border(1.dp, canvasBeacon500.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = label,
                            color = canvasMist400,
                            fontSize = (8.5f * zoom.coerceIn(1f, 1.25f)).sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Street Corridor Badges
            val streetCorridors = listOf(
                "Market St" to Pair(37.7800, -122.4120),
                "Van Ness Ave" to Pair(37.7880, -122.4225),
                "Mission St" to Pair(37.7580, -122.4190),
                "Geary Blvd" to Pair(37.7830, -122.4700),
                "The Embarcadero" to Pair(37.8000, -122.3980),
                "3rd St" to Pair(37.7500, -122.3880)
            )

            streetCorridors.forEach { (name, coords) ->
                val pt = geoToScreen(coords.first, coords.second)
                if (pt.x in -60f..(canvasWidthPx + 60f) && pt.y in -40f..(canvasHeightPx + 40f)) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(pt.x.toInt() - 30, pt.y.toInt() - 8) }
                            .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = name,
                            color = canvasMist400.copy(alpha = 0.8f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }

        // --- TRANSIT STATION BADGES (When Zoom >= 1.45x and Transit Active) ---
        if (showRoadLabels && mapLayerTransit && zoom >= 1.45f) {
            val transitStations = listOf(
                "🚇 Embarcadero" to Pair(37.7929, -122.3970),
                "🚇 Montgomery" to Pair(37.7894, -122.4011),
                "🚇 Powell St" to Pair(37.7844, -122.4079),
                "🚇 Civic Center" to Pair(37.7797, -122.4141),
                "🚇 16th Mission" to Pair(37.7650, -122.4196),
                "🚇 24th Mission" to Pair(37.7522, -122.4184),
                "🚇 Glen Park" to Pair(37.7331, -122.4338),
                "🚇 Balboa Park" to Pair(37.7216, -122.4475),
                "🚇 Chinatown" to Pair(37.7946, -122.4075),
                "🚆 4th & King" to Pair(37.7766, -122.3949)
            )

            transitStations.forEach { (label, coords) ->
                val pt = geoToScreen(coords.first, coords.second)
                if (pt.x in -60f..(canvasWidthPx + 60f) && pt.y in -40f..(canvasHeightPx + 40f)) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(pt.x.toInt() + 8, pt.y.toInt() - 8) }
                            .background(Color(0xFF003B5C).copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color(0xFF0099D8), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color(0xFFE0F2FE),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- INTERACTIVE PINS LAYER ---
        clusteredPins.forEach { item ->
            when (item) {
                is MapPinItem.Single -> {
                    val pos = geoToScreen(item.latitude, item.longitude)
                    val isSelected = selectedResource?.id == item.resource.id
                    val pinColor = resolveCategoryColor(item.resource.category)
                    val pinGlyph = resolveCategoryGlyph(item.resource.category)

                    Box(
                        modifier = Modifier
                            .offset { IntOffset((pos.x - 22).toInt(), (pos.y - 22).toInt()) }
                            .size(44.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                selectedResource = item.resource
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Halo glow if selected
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Beacon500.copy(alpha = 0.35f), CircleShape)
                                    .border(2.dp, Beacon500, CircleShape)
                            )
                        }

                        // Pin Badge
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Mist100 else pinColor,
                            shadowElevation = if (isSelected) 10.dp else 4.dp,
                            border = BorderStroke(1.5.dp, if (isSelected) Beacon500 else Ink950),
                            modifier = Modifier.size(if (isSelected) 34.dp else 28.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pinGlyph,
                                    fontSize = if (isSelected) 15.sp else 12.sp
                                )
                            }
                        }

                        // Open Now green status dot badge
                        if (item.isOpen) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                                    .border(1.dp, Ink950, CircleShape)
                            )
                        }
                    }
                }

                is MapPinItem.Cluster -> {
                    val pos = geoToScreen(item.latitude, item.longitude)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((pos.x - 20).toInt(), (pos.y - 20).toInt()) }
                            .size(40.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // Zoom into cluster
                                centerOnLocation(item.latitude, item.longitude, (zoom * 1.6f).coerceAtMost(4.5f))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Beacon500,
                            shadowElevation = 6.dp,
                            border = BorderStroke(2.dp, Ink950),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${item.items.size}",
                                    color = Ink950,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- TOP FLOATING CONTROLS: SEARCH BAR & CATEGORY CHIPS ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            // Optional Search Input Bar on Dedicated Map Page
            if (onSearchQueryChange != null) {
                Surface(
                    color = Ink900.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Ink700),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Mist100)
                            }
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Beacon500, modifier = Modifier.padding(start = 6.dp).size(20.dp))
                        }

                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search map resources...", color = Mist400, fontSize = 13.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Beacon500,
                                focusedTextColor = Mist100,
                                unfocusedTextColor = Mist100
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier.weight(1f)
                        )

                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Mist400, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (onNavigateToFind != null) {
                            IconButton(
                                onClick = onNavigateToFind,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.List, contentDescription = "View as List", tint = Beacon500, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Open Now Quick Toggle Chip
                item {
                    FilterChip(
                        selected = activeOpenOnly,
                        onClick = {
                            if (onFilterOpenOnlyToggle != null) {
                                onFilterOpenOnlyToggle()
                            } else {
                                internalOpenOnly = !internalOpenOnly
                            }
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (activeOpenOnly) Ink950 else canvasEmerald500, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald500,
                            selectedLabelColor = Ink950,
                            containerColor = Ink900.copy(alpha = 0.92f),
                            labelColor = Mist100
                        ),
                        border = BorderStroke(1.dp, if (activeOpenOnly) Emerald500 else Ink700),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    )
                }

                items(filterCategories) { (id, label) ->
                    val isSelected = activeCategoryFilter == id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (onCategoryFilterChange != null) {
                                onCategoryFilterChange(id)
                            } else {
                                internalCategoryFilter = id
                            }
                        },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Beacon500,
                            selectedLabelColor = Ink950,
                            containerColor = Ink900.copy(alpha = 0.92f),
                            labelColor = Mist400
                        ),
                        border = BorderStroke(1.dp, if (isSelected) Beacon500 else Ink700),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    )
                }
            }
            ActivePresetsBanner(
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // --- FLOATING ACTION CONTROLS (Right Side) ---
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = if (onSearchQueryChange != null) 100.dp else 58.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoom In (+)
            FloatingMapButton(
                icon = Icons.Default.Add,
                contentDescription = "Zoom In",
                onClick = {
                    val target = (zoom * 1.3f).coerceAtMost(5.0f)
                    zoom = target
                }
            )

            // Zoom Out (-)
            FloatingMapButton(
                icon = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                onClick = {
                    val target = (zoom / 1.3f).coerceAtLeast(0.85f)
                    zoom = target
                }
            )

            // Recenter on User / Downtown
            FloatingMapButton(
                icon = if (userLocation != null) Icons.Default.MyLocation else Icons.Default.Explore,
                contentDescription = "Recenter",
                active = userLocation != null,
                onClick = {
                    if (userLocation != null) {
                        centerOnLocation(userLocation!!.latitude, userLocation!!.longitude, 2.4f)
                        Toast.makeText(context, "Centered on ${userLocation!!.label}", Toast.LENGTH_SHORT).show()
                    } else {
                        centerOnLocation(SfMapBounds.DOWNTOWN_LAT, SfMapBounds.DOWNTOWN_LNG, 1.8f)
                        Toast.makeText(context, "Centered on SF Civic Center", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Toggle Labels
            FloatingMapButton(
                icon = if (showRoadLabels) Icons.Default.Layers else Icons.Outlined.Layers,
                contentDescription = "Toggle Map Labels",
                active = showRoadLabels,
                onClick = { showRoadLabels = !showRoadLabels }
            )
        }

        // --- MAP STATS BADGE (Top Left) ---
        Surface(
            color = Ink900.copy(alpha = 0.88f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Ink700),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = if (onSearchQueryChange != null) 100.dp else 58.dp, start = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Beacon500, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "${locatedItems.size} Pins",
                    color = Mist100,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- BOTTOM FLOATING RESOURCE PREVIEW SHEET ---
        AnimatedVisibility(
            visible = selectedResource != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            selectedResource?.let { res ->
                val distanceMiles = viewModel.getDistanceToResource(res)
                val isOpen = isResourceOpen(res.open24, res.hours)
                val catColor = resolveCategoryColor(res.category)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Ink900.copy(alpha = 0.98f)),
                    border = BorderStroke(1.5.dp, Beacon500),
                    elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(16.dp))
                        .testTag("map_resource_preview_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Header Row: Category, Open Badge, Distance, Close 'X'
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Pill
                                Surface(
                                    color = catColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${resolveCategoryGlyph(res.category)} ${res.category.uppercase()}",
                                        color = catColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                // Open / Closed Status Pill
                                Surface(
                                    color = if (isOpen) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFF7F1D1D).copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isOpen) "OPEN NOW" else "CLOSED",
                                        color = if (isOpen) Color(0xFF34D399) else Color(0xFFF87171),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                // Distance Pill if available
                                if (distanceMiles != null) {
                                    Surface(
                                        color = Ink800,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = LocationHelper.formatShortDistance(distanceMiles),
                                            color = Mist100,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { selectedResource = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close preview", tint = Mist400, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Title & Address
                        Text(
                            text = res.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Mist100,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${res.address} • ${res.neighborhood}",
                            color = Mist400,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (distanceMiles != null) {
                            Text(
                                text = LocationHelper.formatWalkTime(distanceMiles),
                                color = Beacon400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        if (res.summary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = res.summary,
                                color = Mist200,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons: Call, Directions, Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Phone Call Button
                            if (res.phone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${res.phone.replace(Regex("[^0-9]"), "")}"))
                                        try {
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open dialer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist100),
                                    border = BorderStroke(1.dp, Ink700),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .defaultMinSize(minHeight = 44.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = Beacon500, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", fontSize = 12.sp, maxLines = 1)
                                }
                            }

                            // Turn-by-Turn Directions Button (Free system intent handoff)
                            Button(
                                onClick = {
                                    val coords = LocationHelper.getCoordinates(res)
                                    val query = if (coords != null) "${coords.first},${coords.second}(${Uri.encode(res.name)})" else Uri.encode("${res.name} ${res.address} San Francisco CA")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$query"))
                                        try {
                                            context.startActivity(webMapIntent)
                                        } catch (err: Exception) {
                                            Toast.makeText(context, "No navigation app available", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Ink800),
                                border = BorderStroke(1.dp, Ink700),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .defaultMinSize(minHeight = 44.dp)
                            ) {
                                Icon(Icons.Default.DirectionsWalk, contentDescription = "Directions", tint = canvasSky500, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Directions", color = Mist100, fontSize = 12.sp, maxLines = 1)
                            }

                            // Full Details Button
                            Button(
                                onClick = { onNavigateToDetail(res.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Beacon500),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .defaultMinSize(minHeight = 44.dp)
                            ) {
                                Text("Details", color = OnAccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = "View Details", tint = OnAccentColor, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingMapButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = if (active) Beacon500 else Ink900.copy(alpha = 0.92f),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, if (active) Beacon400 else Ink700),
        modifier = modifier
            .size(44.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (active) Ink950 else Mist100,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun resolveCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food" -> Color(0xFFF59E0B)       // Beacon Amber
        "shelter" -> Color(0xFF38BDF8)    // Sky Cyan
        "hygiene" -> Color(0xFF10B981)    // Emerald Green
        "health" -> Color(0xFFF43F5E)     // Rose Red
        "mental" -> Color(0xFFFB923C)     // Orange
        "documents" -> Color(0xFF8B5CF6)  // Violet
        "benefits" -> Color(0xFFA78BFA)   // Light Purple
        "legal" -> Color(0xFF818CF8)      // Indigo
        "connect" -> Color(0xFF06B6D4)    // Cyan
        "ebt" -> Color(0xFFEC4899)        // Pink
        else -> Color(0xFF94A3B8)         // Slate
    }
}

fun resolveCategoryGlyph(category: String): String {
    return when (category.lowercase()) {
        "food" -> "🍲"
        "shelter" -> "🏠"
        "hygiene" -> "🚿"
        "health" -> "🩺"
        "mental" -> "💬"
        "documents" -> "🪪"
        "benefits" -> "💳"
        "legal" -> "⚖️"
        "connect" -> "📱"
        "ebt" -> "🍽️"
        else -> "📍"
    }
}

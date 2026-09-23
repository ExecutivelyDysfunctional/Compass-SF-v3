package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

sealed class Screen(
    val route: String,
    val title: String,
    val icon: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector = selectedIcon
) {
    object Now : Screen("now", "Now", "🧭", Icons.Filled.Explore, Icons.Outlined.Explore)
    object Find : Screen("find", "Find", "🔎", Icons.Filled.Search, Icons.Outlined.Search)
    object Map : Screen("map", "Map", "🗺️", Icons.Filled.Map, Icons.Outlined.Map)
    object Ask : Screen("ask", "Ask AI", "💬", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
    object Day : Screen("day", "Day", "🗓️", Icons.Filled.Checklist, Icons.Outlined.Checklist)
    object Ebt : Screen("ebt", "EBT", "💳", Icons.Filled.CreditCard, Icons.Outlined.CreditCard)
    object Add : Screen("add", "Add", "➕", Icons.Filled.Add, Icons.Outlined.Add)
    object Info : Screen("info", "Info", "ℹ️", Icons.Filled.Info, Icons.Outlined.Info)
    object Settings : Screen("settings", "Settings", "⚙️", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class CompassViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CompassRepository
    
    val allResources: StateFlow<List<Resource>>
    val allTasks: StateFlow<List<Task>>
    val allVisits: StateFlow<List<Visit>>
    val allPlans: StateFlow<List<Plan>>
    val allRmpLocations: StateFlow<List<RmpLocation>>
    val allCaptures: StateFlow<List<Capture>>

    // --- Add Screen UI State ---
    val addRawText = mutableStateOf("")
    val isParsing = mutableStateOf(false)
    val parseError = mutableStateOf<String?>(null)
    val draftResource = mutableStateOf<ResourceDraft?>(null)
    val isSavingResource = mutableStateOf(false)
    val personalNotes = mutableStateOf("")
    val selectedImageUri = mutableStateOf<Uri?>(null)
    val selectedImageBase64 = mutableStateOf<String?>(null)
    val selectedImageName = mutableStateOf<String?>(null)
    val isProcessingImage = mutableStateOf(false)

    // --- Ask Screen UI State ---
    val askQuestion = mutableStateOf("")
    val askNeighborhood = mutableStateOf("")
    val askOpenOnly = mutableStateOf(false)
    val isAsking = mutableStateOf(false)
    val askError = mutableStateOf<String?>(null)
    val askResult = mutableStateOf<AskResponse?>(null)

    // --- Settings & Theming UI State ---
    val currentAppIconKey = mutableStateOf("classic")
    val currentThemeMode = mutableStateOf(AppThemeMode.MIDNIGHT)
    val currentAccentColor = mutableStateOf(AppAccentColor.BEACON)
    val currentFontScale = mutableStateOf(AppFontScale.STANDARD)
    val highContrastEnabled = mutableStateOf(false)
    val compactListingEnabled = mutableStateOf(false)

    // --- Navigation & Workspace Config UI State (Chunk 1) ---
    val startupScreenRoute = mutableStateOf("now")
    val bottomNavItems = mutableStateOf(listOf("now", "find", "ask", "day", "ebt"))
    val defaultNeighborhoodAnchorId = mutableStateOf("tenderloin")

    // --- Map & Cartography Controls UI State (Chunk 2) ---
    val mapLayerTransit = mutableStateOf(true)
    val mapLayerNeighborhoods = mutableStateOf(true)
    val mapLayerLandmarks = mutableStateOf(true)
    val mapClusteringMode = mutableStateOf(MapClusteringMode.BALANCED)
    val mapReducedMotion = mutableStateOf(false)

    // --- Search, Accessibility & Demographic Presets UI State (Chunk 3) ---
    val demographicPresets = mutableStateOf<Set<DemographicPreset>>(emptySet())
    val accessibilityMobilityMode = mutableStateOf(false)
    val dietaryPresets = mutableStateOf<Set<DietaryPreset>>(emptySet())
    val excludeNonLocationResources = mutableStateOf(false)

    // --- AI Navigator Customization (Chunk 4) ---
    val aiNavigatorStyle = mutableStateOf(AiNavigatorStyle.GUIDE)
    val aiOfflineOnly = mutableStateOf(false)
    val aiCustomApiKey = mutableStateOf("")
    val aiCustomEndpoint = mutableStateOf("")
    val aiConnectionStatus = mutableStateOf<String?>(null)
    val isTestingAiConnection = mutableStateOf(false)

    val hasActivePresets: Boolean
        get() = demographicPresets.value.isNotEmpty() || accessibilityMobilityMode.value || dietaryPresets.value.isNotEmpty() || excludeNonLocationResources.value

    // --- Location & Distance Sorting UI State ---
    val userLocation = mutableStateOf<UserLocation?>(null)
    val isLocating = mutableStateOf(false)
    val locationStatusMessage = mutableStateOf<String?>(null)
    val sortByDistance = mutableStateOf(false)
    val selectedNeighborhoodAnchorId = mutableStateOf<String?>(null)
    val locationPermissionDenied = mutableStateOf(false)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CompassRepository(database.resourceDao())

        allResources = repository.allResourcesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allTasks = repository.allTasksFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allVisits = repository.allVisitsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allPlans = repository.allPlansFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allRmpLocations = repository.allRmpLocationsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allCaptures = repository.allCapturesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Check seeding and saved preferences on start
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
            val savedIcon = repository.getSetting("app_icon") ?: "classic"
            currentAppIconKey.value = savedIcon

            repository.getSetting("theme_mode")?.let { id ->
                currentThemeMode.value = AppThemeMode.fromId(id)
            }
            repository.getSetting("accent_color")?.let { id ->
                currentAccentColor.value = AppAccentColor.fromId(id)
            }
            repository.getSetting("font_scale")?.let { id ->
                currentFontScale.value = AppFontScale.fromId(id)
            }
            repository.getSetting("high_contrast")?.let {
                highContrastEnabled.value = it.toBooleanStrictOrNull() ?: false
            }
            repository.getSetting("compact_listing")?.let {
                compactListingEnabled.value = it.toBooleanStrictOrNull() ?: false
            }

            // Load Workspace & Navigation Preferences
            val savedStartup = repository.getSetting("startup_screen") ?: "now"
            startupScreenRoute.value = savedStartup

            val savedNavItems = repository.getSetting("bottom_nav_items")
            if (!savedNavItems.isNullOrBlank()) {
                val parsed = savedNavItems.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (parsed.size in 2..6) {
                    bottomNavItems.value = parsed
                }
            }

            val savedDefaultAnchor = repository.getSetting("default_neighborhood_anchor") ?: "tenderloin"
            defaultNeighborhoodAnchorId.value = savedDefaultAnchor

            // Load Map & Cartography Preferences (Chunk 2)
            repository.getSetting("map_layer_transit")?.let {
                mapLayerTransit.value = it.toBooleanStrictOrNull() ?: true
            }
            repository.getSetting("map_layer_neighborhoods")?.let {
                mapLayerNeighborhoods.value = it.toBooleanStrictOrNull() ?: true
            }
            repository.getSetting("map_layer_landmarks")?.let {
                mapLayerLandmarks.value = it.toBooleanStrictOrNull() ?: true
            }
            repository.getSetting("map_clustering_mode")?.let {
                mapClusteringMode.value = MapClusteringMode.fromId(it)
            }
            repository.getSetting("map_reduced_motion")?.let {
                mapReducedMotion.value = it.toBooleanStrictOrNull() ?: false
            }

            // Load Search, Accessibility & Demographic Presets (Chunk 3)
            repository.getSetting("demographic_presets")?.let { raw ->
                if (raw.isNotBlank()) {
                    val parsed = raw.split(",").mapNotNull { DemographicPreset.fromId(it.trim()) }.toSet()
                    demographicPresets.value = parsed
                }
            }
            repository.getSetting("accessibility_mobility_mode")?.let {
                accessibilityMobilityMode.value = it.toBooleanStrictOrNull() ?: false
            }
            repository.getSetting("dietary_presets")?.let { raw ->
                if (raw.isNotBlank()) {
                    val parsed = raw.split(",").mapNotNull { DietaryPreset.fromId(it.trim()) }.toSet()
                    dietaryPresets.value = parsed
                }
            }
            repository.getSetting("exclude_non_location")?.let {
                excludeNonLocationResources.value = it.toBooleanStrictOrNull() ?: false
            }

            // Load AI Navigator Customization Preferences (Chunk 4)
            repository.getSetting("ai_navigator_style")?.let { id ->
                aiNavigatorStyle.value = AiNavigatorStyle.fromId(id)
            }
            repository.getSetting("ai_offline_only")?.let {
                aiOfflineOnly.value = it.toBooleanStrictOrNull() ?: false
            }
            repository.getSetting("ai_custom_api_key")?.let {
                aiCustomApiKey.value = it
            }
            repository.getSetting("ai_custom_endpoint")?.let {
                aiCustomEndpoint.value = it
            }

            val locType = repository.getSetting("location_type") ?: "none"
            val savedAnchorId = repository.getSetting("selected_anchor_id")
            val targetAnchorId = if (!savedAnchorId.isNullOrBlank()) savedAnchorId else savedDefaultAnchor
            if (locType == "anchor" && targetAnchorId.isNotBlank()) {
                val anchor = LocationHelper.NEIGHBORHOOD_ANCHORS.find { it.id == targetAnchorId }
                if (anchor != null) {
                    userLocation.value = UserLocation(
                        latitude = anchor.latitude,
                        longitude = anchor.longitude,
                        label = anchor.name,
                        isManualAnchor = true
                    )
                    selectedNeighborhoodAnchorId.value = anchor.id
                    sortByDistance.value = true
                }
            }
        }
    }

    fun requestDeviceLocation(context: android.content.Context) {
        isLocating.value = true
        locationStatusMessage.value = "Locating via GPS..."
        locationPermissionDenied.value = false
        LocationHelper.requestFreshLocation(context) { location ->
            isLocating.value = false
            if (location != null) {
                userLocation.value = UserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    label = "Current Location",
                    isManualAnchor = false,
                    accuracyMeters = if (location.hasAccuracy()) location.accuracy else null
                )
                selectedNeighborhoodAnchorId.value = null
                locationStatusMessage.value = "Using GPS location"
                sortByDistance.value = true
                viewModelScope.launch {
                    repository.saveSetting("location_type", "gps")
                }
            } else {
                locationStatusMessage.value = "Could not get current GPS fix. Select a neighborhood anchor."
            }
        }
    }

    fun setNeighborhoodAnchor(anchor: NeighborhoodAnchor) {
        userLocation.value = UserLocation(
            latitude = anchor.latitude,
            longitude = anchor.longitude,
            label = anchor.name,
            isManualAnchor = true
        )
        selectedNeighborhoodAnchorId.value = anchor.id
        locationStatusMessage.value = "Anchored at ${anchor.name}"
        sortByDistance.value = true
        viewModelScope.launch {
            repository.saveSetting("location_type", "anchor")
            repository.saveSetting("selected_anchor_id", anchor.id)
        }
    }

    fun clearLocation() {
        userLocation.value = null
        selectedNeighborhoodAnchorId.value = null
        locationStatusMessage.value = null
        sortByDistance.value = false
        viewModelScope.launch {
            repository.saveSetting("location_type", "none")
            repository.saveSetting("selected_anchor_id", "")
        }
    }

    fun toggleSortByDistance(explicit: Boolean? = null) {
        val next = explicit ?: !sortByDistance.value
        sortByDistance.value = next
        if (next && userLocation.value == null) {
            val defaultAnchor = LocationHelper.NEIGHBORHOOD_ANCHORS.first()
            setNeighborhoodAnchor(defaultAnchor)
        }
    }

    fun getDistanceToResource(resource: Resource): Double? {
        val loc = userLocation.value ?: return null
        val coords = LocationHelper.getCoordinates(resource) ?: return null
        return LocationHelper.calculateDistanceMiles(loc.latitude, loc.longitude, coords.first, coords.second)
    }

    fun getDistanceToRmp(rmp: RmpLocation): Double? {
        val loc = userLocation.value ?: return null
        val coords = LocationHelper.getCoordinates(rmp) ?: return null
        return LocationHelper.calculateDistanceMiles(loc.latitude, loc.longitude, coords.first, coords.second)
    }

    // --- Actions ---

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(done = !task.done))
        }
    }

    fun addTask(title: String, kind: String, notes: String = "", resourceId: Int? = null) {
        viewModelScope.launch {
            repository.insertTask(Task(title = title, kind = kind, notes = notes, resourceId = resourceId))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleResourceFavorite(resource: Resource) {
        viewModelScope.launch {
            repository.updateResource(resource.copy(favorite = !resource.favorite))
        }
    }

    fun toggleResourceHidden(resource: Resource) {
        viewModelScope.launch {
            repository.updateResource(resource.copy(hidden = !resource.hidden))
        }
    }

    fun toggleRmpFavorite(location: RmpLocation) {
        viewModelScope.launch {
            repository.updateRmpLocation(location.copy(favorite = !location.favorite))
        }
    }

    fun toggleRmpHidden(location: RmpLocation) {
        viewModelScope.launch {
            repository.updateRmpLocation(location.copy(hidden = !location.hidden))
        }
    }

    fun saveResourcePersonalNotes(resourceId: Int, notes: String) {
        viewModelScope.launch {
            val res = repository.getResourceById(resourceId)
            if (res != null) {
                repository.updateResource(res.copy(personalNotes = notes))
            }
        }
    }

    fun saveRmpPersonalNotes(rmpId: Int, notes: String) {
        viewModelScope.launch {
            val all = repository.getAllRmpLocations()
            val loc = all.find { it.id == rmpId }
            if (loc != null) {
                repository.updateRmpLocation(loc.copy(personalNotes = notes))
            }
        }
    }

    fun addRmpLocation(
        name: String,
        address: String,
        neighborhood: String,
        cuisine: String,
        chain: Boolean,
        phone: String,
        hoursText: String,
        notes: String,
        tips: String
    ) {
        viewModelScope.launch {
            repository.insertRmpLocation(
                RmpLocation(
                    name = name.trim(),
                    address = address.trim(),
                    neighborhood = neighborhood.trim(),
                    cuisine = if (cuisine.isBlank()) "other" else cuisine.lowercase().trim(),
                    chain = chain,
                    phone = phone.trim(),
                    hoursText = hoursText.trim(),
                    notes = notes.trim(),
                    tips = tips.trim(),
                    confidence = "reported",
                    createdVia = "manual"
                )
            )
        }
    }

    fun verifyResource(resource: Resource) {
        viewModelScope.launch {
            repository.updateResource(resource.copy(
                confidence = "verified",
                lastVerifiedAt = System.currentTimeMillis()
            ))
        }
    }

    fun addVisitLog(resourceId: Int, outcome: String, notes: String, rating: Int?, waitMin: Int?) {
        viewModelScope.launch {
            repository.insertVisit(
                Visit(
                    resourceId = resourceId,
                    outcome = outcome,
                    notes = notes,
                    rating = rating,
                    waitMinutes = waitMin
                )
            )
            // also mark the resource as verified if not already
            val res = repository.getResourceById(resourceId)
            if (res != null && res.confidence != "verified") {
                repository.updateResource(res.copy(confidence = "verified", lastVerifiedAt = System.currentTimeMillis()))
            }
        }
    }

    // --- AI Integration Methods ---

    fun onPhotoSelected(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isProcessingImage.value = true
            try {
                selectedImageUri.value = uri
                selectedImageName.value = "Street Flyer Photo"
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val maxDim = 1200
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val scaledBitmap = if (width > maxDim || height > maxDim) {
                        val ratio = width.toFloat() / height.toFloat()
                        val newW = if (width > height) maxDim else (maxDim * ratio).toInt()
                        val newH = if (height > width) maxDim else (maxDim / ratio).toInt()
                        Bitmap.createScaledBitmap(originalBitmap, newW.coerceAtLeast(1), newH.coerceAtLeast(1), true)
                    } else {
                        originalBitmap
                    }

                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val bytes = outputStream.toByteArray()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    selectedImageBase64.value = base64
                }
            } catch (e: Exception) {
                parseError.value = "Failed to load flyer photo: ${e.message}"
            } finally {
                isProcessingImage.value = false
            }
        }
    }

    fun clearSelectedPhoto() {
        selectedImageUri.value = null
        selectedImageBase64.value = null
        selectedImageName.value = null
    }

    fun parseFlyerPhoto() {
        val base64 = selectedImageBase64.value
        if (base64.isNullOrBlank()) {
            parseError.value = "Please select or take a flyer photo first."
            return
        }
        isParsing.value = true
        parseError.value = null
        viewModelScope.launch {
            try {
                val result = AiService.parseFlyerImage(
                    base64Image = base64,
                    mimeType = "image/jpeg",
                    notes = addRawText.value,
                    offlineOnly = aiOfflineOnly.value,
                    customApiKey = aiCustomApiKey.value,
                    customEndpoint = aiCustomEndpoint.value
                )
                if (result != null) {
                    draftResource.value = result
                } else {
                    parseError.value = "Unable to read structure from flyer photo. You can still fill out the manual entry below."
                }
            } catch (e: Exception) {
                parseError.value = e.message ?: "Failed to contact parser service"
            } finally {
                isParsing.value = false
            }
        }
    }

    fun parseDraft() {
        if (addRawText.value.isBlank()) return
        isParsing.value = true
        parseError.value = null
        viewModelScope.launch {
            try {
                val result = AiService.parseMessyText(
                    rawText = addRawText.value,
                    offlineOnly = aiOfflineOnly.value,
                    customApiKey = aiCustomApiKey.value,
                    customEndpoint = aiCustomEndpoint.value
                )
                if (result != null) {
                    draftResource.value = result
                } else {
                    parseError.value = "Unable to read structure from that text. Try manual entry."
                }
            } catch (e: Exception) {
                parseError.value = e.message ?: "Failed to contact parser service"
            } finally {
                isParsing.value = false
            }
        }
    }

    fun resetDraft() {
        draftResource.value = null
        addRawText.value = ""
        parseError.value = null
        personalNotes.value = ""
        clearSelectedPhoto()
    }

    fun createManualBlankDraft() {
        draftResource.value = ResourceDraft(
            name = "",
            category = "food",
            description = addRawText.value,
            cost = "Free"
        )
    }

    fun saveDraftToDatabase(onSuccess: (Int) -> Unit) {
        val draft = draftResource.value ?: return
        if (draft.name.isBlank()) {
            parseError.value = "Please give it a name first"
            return
        }
        isSavingResource.value = true
        viewModelScope.launch {
            try {
                val newRes = Resource(
                    name = draft.name,
                    category = draft.category,
                    alsoOffers = draft.alsoOffers,
                    summary = draft.summary,
                    description = draft.description,
                    address = draft.address,
                    neighborhood = draft.neighborhood,
                    phone = draft.phone,
                    website = draft.website,
                    hoursText = draft.hoursText,
                    hours = draft.hours,
                    open24 = draft.open24,
                    requirements = draft.requirements,
                    bring = draft.bring,
                    eligibility = draft.eligibility,
                    cost = draft.cost,
                    languages = draft.languages,
                    tags = draft.tags,
                    aiTips = draft.aiTips,
                    personalNotes = personalNotes.value,
                    createdVia = "ai"
                )
                val id = repository.insertResource(newRes)
                onSuccess(id.toInt())
                resetDraft()
            } catch (e: Exception) {
                parseError.value = "Failed to save: ${e.message}"
            } finally {
                isSavingResource.value = false
            }
        }
    }

    fun askNavigator() {
        if (askQuestion.value.isBlank()) return
        isAsking.value = true
        askError.value = null
        viewModelScope.launch {
            try {
                val list = repository.getAllResources()
                val response = AiService.askAi(
                    question = askQuestion.value,
                    resources = list.filter { !it.hidden },
                    neighborhoodFilter = askNeighborhood.value,
                    openNowFilter = askOpenOnly.value,
                    style = aiNavigatorStyle.value,
                    offlineOnly = aiOfflineOnly.value,
                    customApiKey = aiCustomApiKey.value,
                    customEndpoint = aiCustomEndpoint.value
                )
                askResult.value = response
            } catch (e: Exception) {
                askError.value = e.message ?: "Failed to generate answer"
            } finally {
                isAsking.value = false
            }
        }
    }

    // --- AI Navigator Customization Operations (Chunk 4) ---

    fun setAiNavigatorStyle(style: AiNavigatorStyle) {
        aiNavigatorStyle.value = style
        viewModelScope.launch {
            repository.saveSetting("ai_navigator_style", style.id)
        }
    }

    fun setAiOfflineOnly(enabled: Boolean) {
        aiOfflineOnly.value = enabled
        viewModelScope.launch {
            repository.saveSetting("ai_offline_only", enabled.toString())
        }
    }

    fun setAiCustomApiKey(key: String) {
        aiCustomApiKey.value = key
        viewModelScope.launch {
            repository.saveSetting("ai_custom_api_key", key)
        }
    }

    fun setAiCustomEndpoint(endpoint: String) {
        aiCustomEndpoint.value = endpoint
        viewModelScope.launch {
            repository.saveSetting("ai_custom_endpoint", endpoint)
        }
    }

    fun testAiConnection() {
        isTestingAiConnection.value = true
        aiConnectionStatus.value = "Testing Gemini connection..."
        viewModelScope.launch {
            val (success, message) = AiService.testConnection(
                customApiKey = aiCustomApiKey.value,
                customEndpoint = aiCustomEndpoint.value
            )
            aiConnectionStatus.value = if (success) "✅ $message" else "❌ $message"
            isTestingAiConnection.value = false
        }
    }

    fun resetAiSettings() {
        aiNavigatorStyle.value = AiNavigatorStyle.GUIDE
        aiOfflineOnly.value = false
        aiCustomApiKey.value = ""
        aiCustomEndpoint.value = ""
        aiConnectionStatus.value = null
        viewModelScope.launch {
            repository.saveSetting("ai_navigator_style", AiNavigatorStyle.GUIDE.id)
            repository.saveSetting("ai_offline_only", "false")
            repository.saveSetting("ai_custom_api_key", "")
            repository.saveSetting("ai_custom_endpoint", "")
        }
    }

    fun selectAppIcon(iconKey: String) {
        currentAppIconKey.value = iconKey
        viewModelScope.launch {
            repository.saveSetting("app_icon", iconKey)
        }
    }

    fun selectThemeMode(mode: AppThemeMode) {
        currentThemeMode.value = mode
        viewModelScope.launch {
            repository.saveSetting("theme_mode", mode.id)
        }
    }

    fun selectAccentColor(accent: AppAccentColor) {
        currentAccentColor.value = accent
        viewModelScope.launch {
            repository.saveSetting("accent_color", accent.id)
        }
    }

    fun selectFontScale(scale: AppFontScale) {
        currentFontScale.value = scale
        viewModelScope.launch {
            repository.saveSetting("font_scale", scale.id)
        }
    }

    fun toggleHighContrast(enabled: Boolean) {
        highContrastEnabled.value = enabled
        viewModelScope.launch {
            repository.saveSetting("high_contrast", enabled.toString())
        }
    }

    fun toggleCompactListing(enabled: Boolean) {
        compactListingEnabled.value = enabled
        viewModelScope.launch {
            repository.saveSetting("compact_listing", enabled.toString())
        }
    }

    fun resetThemingToDefaults() {
        currentThemeMode.value = AppThemeMode.MIDNIGHT
        currentAccentColor.value = AppAccentColor.BEACON
        currentFontScale.value = AppFontScale.STANDARD
        highContrastEnabled.value = false
        compactListingEnabled.value = false
        viewModelScope.launch {
            repository.saveSetting("theme_mode", AppThemeMode.MIDNIGHT.id)
            repository.saveSetting("accent_color", AppAccentColor.BEACON.id)
            repository.saveSetting("font_scale", AppFontScale.STANDARD.id)
            repository.saveSetting("high_contrast", "false")
            repository.saveSetting("compact_listing", "false")
        }
    }

    // --- Navigation & Workspace Configuration Actions (Chunk 1) ---

    fun setStartupScreen(route: String) {
        startupScreenRoute.value = route
        viewModelScope.launch {
            repository.saveSetting("startup_screen", route)
        }
    }

    fun toggleBottomNavItem(route: String) {
        val current = bottomNavItems.value.toMutableList()
        if (current.contains(route)) {
            // Must keep at least 2 navigation items
            if (current.size > 2) {
                current.remove(route)
            }
        } else {
            // Cap at 6 maximum to preserve touch targets
            if (current.size < 6) {
                current.add(route)
            }
        }
        bottomNavItems.value = current
        viewModelScope.launch {
            repository.saveSetting("bottom_nav_items", current.joinToString(","))
        }
    }

    fun moveBottomNavItem(route: String, direction: Int) {
        val current = bottomNavItems.value.toMutableList()
        val index = current.indexOf(route)
        if (index == -1) return
        val targetIndex = index + direction
        if (targetIndex in current.indices) {
            current.removeAt(index)
            current.add(targetIndex, route)
            bottomNavItems.value = current
            viewModelScope.launch {
                repository.saveSetting("bottom_nav_items", current.joinToString(","))
            }
        }
    }

    fun setDefaultNeighborhoodAnchor(anchorId: String) {
        defaultNeighborhoodAnchorId.value = anchorId
        viewModelScope.launch {
            repository.saveSetting("default_neighborhood_anchor", anchorId)
        }
        // If current location is an anchor, update to the new anchor
        val anchor = LocationHelper.NEIGHBORHOOD_ANCHORS.find { it.id == anchorId }
        if (anchor != null && userLocation.value?.isManualAnchor == true) {
            setNeighborhoodAnchor(anchor)
        }
    }

    fun resetWorkspaceNavigationToDefaults() {
        startupScreenRoute.value = "now"
        bottomNavItems.value = listOf("now", "find", "ask", "day", "ebt")
        defaultNeighborhoodAnchorId.value = "tenderloin"
        viewModelScope.launch {
            repository.saveSetting("startup_screen", "now")
            repository.saveSetting("bottom_nav_items", "now,find,ask,day,ebt")
            repository.saveSetting("default_neighborhood_anchor", "tenderloin")
        }
    }

    // --- Map & Cartography Configuration Actions (Chunk 2) ---

    fun toggleMapLayerTransit(enabled: Boolean) {
        mapLayerTransit.value = enabled
        viewModelScope.launch {
            repository.saveSetting("map_layer_transit", enabled.toString())
        }
    }

    fun toggleMapLayerNeighborhoods(enabled: Boolean) {
        mapLayerNeighborhoods.value = enabled
        viewModelScope.launch {
            repository.saveSetting("map_layer_neighborhoods", enabled.toString())
        }
    }

    fun toggleMapLayerLandmarks(enabled: Boolean) {
        mapLayerLandmarks.value = enabled
        viewModelScope.launch {
            repository.saveSetting("map_layer_landmarks", enabled.toString())
        }
    }

    fun selectMapClusteringMode(mode: MapClusteringMode) {
        mapClusteringMode.value = mode
        viewModelScope.launch {
            repository.saveSetting("map_clustering_mode", mode.id)
        }
    }

    fun toggleMapReducedMotion(enabled: Boolean) {
        mapReducedMotion.value = enabled
        viewModelScope.launch {
            repository.saveSetting("map_reduced_motion", enabled.toString())
        }
    }

    fun resetMapSettingsToDefaults() {
        mapLayerTransit.value = true
        mapLayerNeighborhoods.value = true
        mapLayerLandmarks.value = true
        mapClusteringMode.value = MapClusteringMode.BALANCED
        mapReducedMotion.value = false
        viewModelScope.launch {
            repository.saveSetting("map_layer_transit", "true")
            repository.saveSetting("map_layer_neighborhoods", "true")
            repository.saveSetting("map_layer_landmarks", "true")
            repository.saveSetting("map_clustering_mode", MapClusteringMode.BALANCED.id)
            repository.saveSetting("map_reduced_motion", "false")
        }
    }

    // --- Search, Accessibility & Demographic Presets Actions (Chunk 3) ---

    fun toggleDemographicPreset(preset: DemographicPreset) {
        val current = demographicPresets.value.toMutableSet()
        if (current.contains(preset)) {
            current.remove(preset)
        } else {
            current.add(preset)
        }
        demographicPresets.value = current
        viewModelScope.launch {
            repository.saveSetting("demographic_presets", current.joinToString(",") { it.id })
        }
    }

    fun setDemographicPresets(presets: Set<DemographicPreset>) {
        demographicPresets.value = presets
        viewModelScope.launch {
            repository.saveSetting("demographic_presets", presets.joinToString(",") { it.id })
        }
    }

    fun toggleAccessibilityMobilityMode(enabled: Boolean) {
        accessibilityMobilityMode.value = enabled
        viewModelScope.launch {
            repository.saveSetting("accessibility_mobility_mode", enabled.toString())
        }
    }

    fun toggleDietaryPreset(preset: DietaryPreset) {
        val current = dietaryPresets.value.toMutableSet()
        if (current.contains(preset)) {
            current.remove(preset)
        } else {
            current.add(preset)
        }
        dietaryPresets.value = current
        viewModelScope.launch {
            repository.saveSetting("dietary_presets", current.joinToString(",") { it.id })
        }
    }

    fun setDietaryPresets(presets: Set<DietaryPreset>) {
        dietaryPresets.value = presets
        viewModelScope.launch {
            repository.saveSetting("dietary_presets", presets.joinToString(",") { it.id })
        }
    }

    fun toggleExcludeNonLocationResources(enabled: Boolean? = null) {
        val next = enabled ?: !excludeNonLocationResources.value
        excludeNonLocationResources.value = next
        viewModelScope.launch {
            repository.saveSetting("exclude_non_location", next.toString())
        }
    }

    fun clearAllPresets() {
        demographicPresets.value = emptySet()
        accessibilityMobilityMode.value = false
        dietaryPresets.value = emptySet()
        excludeNonLocationResources.value = false
        viewModelScope.launch {
            repository.saveSetting("demographic_presets", "")
            repository.saveSetting("accessibility_mobility_mode", "false")
            repository.saveSetting("dietary_presets", "")
            repository.saveSetting("exclude_non_location", "false")
        }
    }

    fun getVisitsFlow(resourceId: Int): Flow<List<Visit>> {
        return repository.getVisitsForResource(resourceId)
    }

    // --- Data Portability: Backup & Restore UI State & Methods ---
    val importPreviewState = mutableStateOf<ImportPreview?>(null)
    val importErrorMessage = mutableStateOf<String?>(null)
    val isImporting = mutableStateOf(false)
    val lastImportResult = mutableStateOf<ImportResult?>(null)
    val exportSuccessMessage = mutableStateOf<String?>(null)
    val isExporting = mutableStateOf(false)

    suspend fun getExportJson(): String {
        isExporting.value = true
        return try {
            val backup = repository.createBackupData()
            repository.serializeBackup(backup)
        } finally {
            isExporting.value = false
        }
    }

    fun setExportSuccess(msg: String) {
        exportSuccessMessage.value = msg
    }

    fun dismissExportSuccess() {
        exportSuccessMessage.value = null
    }

    fun loadImportJson(jsonText: String) {
        importErrorMessage.value = null
        try {
            val preview = repository.parseBackupJson(jsonText)
            importPreviewState.value = preview
        } catch (e: Exception) {
            importErrorMessage.value = "Failed to parse backup JSON: ${e.localizedMessage ?: "Invalid or incompatible format"}"
            importPreviewState.value = null
        }
    }

    fun confirmRestore(onSuccess: (ImportResult) -> Unit) {
        val preview = importPreviewState.value ?: return
        isImporting.value = true
        viewModelScope.launch {
            try {
                val result = repository.restoreBackup(preview.rawBackup)
                lastImportResult.value = result
                importPreviewState.value = null
                onSuccess(result)
            } catch (e: Exception) {
                importErrorMessage.value = "Restore failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isImporting.value = false
            }
        }
    }

    fun dismissImportDialog() {
        importPreviewState.value = null
        importErrorMessage.value = null
    }

    fun dismissLastImportResult() {
        lastImportResult.value = null
    }
}

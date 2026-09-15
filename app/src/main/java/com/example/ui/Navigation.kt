package com.example.ui

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val title: String,
    val icon: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector = selectedIcon
) {
    object Now : Screen("now", "Now", "🧭", Icons.Filled.Explore, Icons.Outlined.Explore)
    object Find : Screen("find", "Find", "🔎", Icons.Filled.Search, Icons.Outlined.Search)
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

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CompassRepository(database.resourceDao())

        allResources = repository.allResourcesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allTasks = repository.allTasksFlow
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
        }
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

    fun parseDraft() {
        if (addRawText.value.isBlank()) return
        isParsing.value = true
        parseError.value = null
        viewModelScope.launch {
            try {
                val result = AiService.parseMessyText(addRawText.value)
                if (result != null) {
                    draftResource.value = result
                } else {
                    parseError.value = "Unable to read structure from that text. Try manually entry."
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
                    openNowFilter = askOpenOnly.value
                )
                askResult.value = response
            } catch (e: Exception) {
                askError.value = e.message ?: "Failed to generate answer"
            } finally {
                isAsking.value = false
            }
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

    fun getVisitsFlow(resourceId: Int): Flow<List<Visit>> {
        return repository.getVisitsForResource(resourceId)
    }
}

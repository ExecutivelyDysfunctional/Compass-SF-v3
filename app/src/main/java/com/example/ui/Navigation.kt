package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: String) {
    object Now : Screen("now", "Now", "🧭")
    object Find : Screen("find", "Find", "🔎")
    object Ask : Screen("ask", "Ask", "💬")
    object Ebt : Screen("ebt", "EBT", "💳")
    object Add : Screen("add", "Add", "➕")
    object Day : Screen("day", "Day", "🗓️")
    object Info : Screen("info", "Info", "ℹ️")
    object Settings : Screen("settings", "Settings", "⚙️")
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

    // --- Settings UI State ---
    val currentAppIconKey = mutableStateOf("classic")

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

        // Check seeding and icon choice on start
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
            val savedIcon = repository.getSetting("app_icon") ?: "classic"
            currentAppIconKey.value = savedIcon
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

    fun getVisitsFlow(resourceId: Int): Flow<List<Visit>> {
        return repository.getVisitsForResource(resourceId)
    }
}

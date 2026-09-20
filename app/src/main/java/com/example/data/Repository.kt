package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class CompassRepository(private val dao: ResourceDao) {

    val allResourcesFlow: Flow<List<Resource>> = dao.getAllResourcesFlow()
    val allTasksFlow: Flow<List<Task>> = dao.getAllTasksFlow()
    val allVisitsFlow: Flow<List<Visit>> = dao.getAllVisitsFlow()
    val allPlansFlow: Flow<List<Plan>> = dao.getAllPlansFlow()
    val allRmpLocationsFlow: Flow<List<RmpLocation>> = dao.getAllRmpLocationsFlow()
    val allCapturesFlow: Flow<List<Capture>> = dao.getAllCapturesFlow()

    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        val existingResources = dao.getAllResources()
        val existingMap = existingResources.associateBy { it.name.trim().lowercase() }
        for (res in SeedData.SEED_RESOURCES) {
            val coords = LocationHelper.getCoordinates(res)
            val resWithCoords = if (res.lat == null && coords != null) {
                res.copy(lat = coords.first, lng = coords.second)
            } else res
            val existing = existingMap[res.name.trim().lowercase()]
            if (existing == null) {
                dao.insertResource(resWithCoords)
            } else if ((existing.lat == null || existing.lng == null) && coords != null) {
                dao.updateResource(
                    existing.copy(
                        lat = coords.first,
                        lng = coords.second,
                        source = if (existing.source.isBlank()) res.source else existing.source,
                        website = if (existing.website.isBlank()) res.website else existing.website
                    )
                )
            } else if (existing.source.isBlank() && res.source.isNotBlank()) {
                dao.updateResource(
                    existing.copy(
                        source = res.source,
                        website = if (existing.website.isBlank()) res.website else existing.website
                    )
                )
            }
        }
        if (dao.getAllTasks().isEmpty()) {
            for (task in SeedData.SEED_TASKS) {
                dao.insertTask(task)
            }
        }
        val existingRmp = dao.getAllRmpLocations()
        val existingRmpMap = existingRmp.associateBy { it.name.trim().lowercase() }
        for (rmp in SeedData.SEED_RMP_LOCATIONS) {
            val coords = LocationHelper.getCoordinates(rmp)
            val rmpWithCoords = if (rmp.lat == null && coords != null) {
                rmp.copy(lat = coords.first, lng = coords.second)
            } else rmp
            val existing = existingRmpMap[rmp.name.trim().lowercase()]
            if (existing == null) {
                dao.insertRmpLocation(rmpWithCoords)
            } else if ((existing.lat == null || existing.lng == null) && coords != null) {
                dao.updateRmpLocation(
                    existing.copy(
                        lat = coords.first,
                        lng = coords.second,
                        hours = if (existing.hours.isEmpty()) rmp.hours else existing.hours,
                        hoursText = if (existing.hoursText.isBlank()) rmp.hoursText else existing.hoursText,
                        phone = if (existing.phone.isBlank()) rmp.phone else existing.phone,
                        tips = if (existing.tips.isBlank()) rmp.tips else existing.tips
                    )
                )
            } else if (existing.hours.isEmpty() && rmp.hours.isNotEmpty()) {
                dao.updateRmpLocation(
                    existing.copy(
                        hours = rmp.hours,
                        hoursText = if (existing.hoursText.isBlank()) rmp.hoursText else existing.hoursText,
                        phone = if (existing.phone.isBlank()) rmp.phone else existing.phone,
                        tips = if (existing.tips.isBlank()) rmp.tips else existing.tips
                    )
                )
            }
        }
    }

    suspend fun getAllResources(): List<Resource> = dao.getAllResources()
    suspend fun getResourceById(id: Int): Resource? = dao.getResourceById(id)
    suspend fun insertResource(resource: Resource): Long = dao.insertResource(resource)
    suspend fun updateResource(resource: Resource) = dao.updateResource(resource)
    suspend fun deleteResource(resource: Resource) = dao.deleteResource(resource)

    fun getVisitsForResource(resourceId: Int): Flow<List<Visit>> = dao.getVisitsForResourceFlow(resourceId)
    suspend fun getVisitsForResourceSync(resourceId: Int): List<Visit> = dao.getVisitsForResource(resourceId)
    suspend fun insertVisit(visit: Visit): Long = dao.insertVisit(visit)

    suspend fun getAllTasks(): List<Task> = dao.getAllTasks()
    suspend fun insertTask(task: Task): Long = dao.insertTask(task)
    suspend fun updateTask(task: Task) = dao.updateTask(task)
    suspend fun deleteTask(task: Task) = dao.deleteTask(task)

    suspend fun getAllPlans(): List<Plan> = dao.getAllPlans()
    suspend fun insertPlan(plan: Plan): Long = dao.insertPlan(plan)
    suspend fun deletePlan(plan: Plan) = dao.deletePlan(plan)

    suspend fun getAllRmpLocations(): List<RmpLocation> = dao.getAllRmpLocations()
    suspend fun insertRmpLocation(location: RmpLocation): Long = dao.insertRmpLocation(location)
    suspend fun updateRmpLocation(location: RmpLocation) = dao.updateRmpLocation(location)

    suspend fun getSetting(key: String): String? = dao.getSetting(key)?.value
    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(AppSetting(key, value))
    }

    // --- AI Navigator Preferences (Chunk 4) ---

    suspend fun getAiPreferences(): AiPreferences = withContext(Dispatchers.IO) {
        val styleId = getSetting(SETTING_AI_RESPONSE_STYLE)
        val modeId = getSetting(SETTING_AI_CONNECTION_MODE)
        val tips = getSetting(SETTING_AI_INCLUDE_TIPS)
        val eligibility = getSetting(SETTING_AI_INCLUDE_ELIGIBILITY)

        AiPreferences(
            responseStyle = AiResponseStyle.fromId(styleId),
            connectionMode = AiConnectionMode.fromId(modeId),
            includeStreetTips = tips?.toBooleanStrictOrNull() ?: true,
            includeEligibilityDetails = eligibility?.toBooleanStrictOrNull() ?: true
        )
    }

    suspend fun saveAiPreferences(prefs: AiPreferences) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_AI_RESPONSE_STYLE, prefs.responseStyle.id)
        saveSetting(SETTING_AI_CONNECTION_MODE, prefs.connectionMode.id)
        saveSetting(SETTING_AI_INCLUDE_TIPS, prefs.includeStreetTips.toString())
        saveSetting(SETTING_AI_INCLUDE_ELIGIBILITY, prefs.includeEligibilityDetails.toString())
    }

    suspend fun setAiResponseStyle(style: AiResponseStyle) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_AI_RESPONSE_STYLE, style.id)
    }

    suspend fun setAiConnectionMode(mode: AiConnectionMode) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_AI_CONNECTION_MODE, mode.id)
    }

    suspend fun setAiIncludeStreetTips(enabled: Boolean) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_AI_INCLUDE_TIPS, enabled.toString())
    }

    suspend fun setAiIncludeEligibilityDetails(enabled: Boolean) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_AI_INCLUDE_ELIGIBILITY, enabled.toString())
    }

    suspend fun resetAiPreferences() = withContext(Dispatchers.IO) {
        saveAiPreferences(AiPreferences.DEFAULT)
    }

    // --- Schedule Reminders & Alerts Preferences (Chunk 6) ---

    suspend fun getReminderPreferences(): ReminderPreferences = withContext(Dispatchers.IO) {
        val enabledStr = getSetting(SETTING_REMINDERS_ENABLED)
        val leadTimeStr = getSetting(SETTING_REMINDERS_LEAD_TIME)
        val mealsStr = getSetting(SETTING_REMINDERS_MEALS)
        val clinicsStr = getSetting(SETTING_REMINDERS_CLINICS)
        val briefingStr = getSetting(SETTING_REMINDERS_BRIEFING)
        val soundStr = getSetting(SETTING_REMINDERS_SOUND)

        ReminderPreferences(
            enabled = enabledStr?.toBooleanStrictOrNull() ?: false,
            leadTime = ReminderLeadTime.fromMinutes(leadTimeStr?.toIntOrNull()),
            notifyMealsClosing = mealsStr?.toBooleanStrictOrNull() ?: true,
            notifyClinicsClosing = clinicsStr?.toBooleanStrictOrNull() ?: true,
            notifyDailyBriefing = briefingStr?.toBooleanStrictOrNull() ?: true,
            soundAndVibrate = soundStr?.toBooleanStrictOrNull() ?: true
        )
    }

    suspend fun saveReminderPreferences(prefs: ReminderPreferences) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_REMINDERS_ENABLED, prefs.enabled.toString())
        saveSetting(SETTING_REMINDERS_LEAD_TIME, prefs.leadTime.minutes.toString())
        saveSetting(SETTING_REMINDERS_MEALS, prefs.notifyMealsClosing.toString())
        saveSetting(SETTING_REMINDERS_CLINICS, prefs.notifyClinicsClosing.toString())
        saveSetting(SETTING_REMINDERS_BRIEFING, prefs.notifyDailyBriefing.toString())
        saveSetting(SETTING_REMINDERS_SOUND, prefs.soundAndVibrate.toString())
    }

    suspend fun setRemindersEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_REMINDERS_ENABLED, enabled.toString())
    }

    suspend fun setReminderLeadTime(leadTime: ReminderLeadTime) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_REMINDERS_LEAD_TIME, leadTime.minutes.toString())
    }

    suspend fun toggleRemindersMeals(enabled: Boolean) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_REMINDERS_MEALS, enabled.toString())
    }

    suspend fun toggleRemindersClinics(enabled: Boolean) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_REMINDERS_CLINICS, enabled.toString())
    }

    suspend fun toggleRemindersDailyBriefing(enabled: Boolean) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_REMINDERS_BRIEFING, enabled.toString())
    }

    suspend fun toggleRemindersSound(enabled: Boolean) = withContext(Dispatchers.IO) {
        saveSetting(SETTING_REMINDERS_SOUND, enabled.toString())
    }

    suspend fun resetReminderPreferences() = withContext(Dispatchers.IO) {
        saveReminderPreferences(ReminderPreferences.DEFAULT)
    }

    companion object {
        const val SETTING_AI_RESPONSE_STYLE = "ai_response_style"
        const val SETTING_AI_CONNECTION_MODE = "ai_connection_mode"
        const val SETTING_AI_INCLUDE_TIPS = "ai_include_tips"
        const val SETTING_AI_INCLUDE_ELIGIBILITY = "ai_include_eligibility"

        const val SETTING_REMINDERS_ENABLED = "reminders_enabled"
        const val SETTING_REMINDERS_LEAD_TIME = "reminders_lead_time"
        const val SETTING_REMINDERS_MEALS = "reminders_notify_meals"
        const val SETTING_REMINDERS_CLINICS = "reminders_notify_clinics"
        const val SETTING_REMINDERS_BRIEFING = "reminders_notify_briefing"
        const val SETTING_REMINDERS_SOUND = "reminders_sound_vibrate"
    }

    suspend fun insertCapture(capture: Capture): Long = dao.insertCapture(capture)
    suspend fun updateCapture(capture: Capture) = dao.updateCapture(capture)

    suspend fun getAllVisits(): List<Visit> = dao.getAllVisits()

    suspend fun createBackupData(selectedGroups: Set<BackupGroup> = BackupGroup.entries.toSet()): CompassBackup = withContext(Dispatchers.IO) {
        val allRes = dao.getAllResources()
        val allRmp = dao.getAllRmpLocations()
        val allVisits = dao.getAllVisits()
        val allTasks = dao.getAllTasks()
        val allCaptures = dao.getAllCaptures()
        val allSettings = dao.getAllSettings()

        val resMap = allRes.associateBy { it.id }

        val includeFavsNotes = selectedGroups.contains(BackupGroup.FAVORITES_NOTES)
        val includeResUserData = selectedGroups.contains(BackupGroup.RESOURCE_USER_DATA) || includeFavsNotes
        val includeRmpUserData = selectedGroups.contains(BackupGroup.RMP_LOCATIONS) || includeFavsNotes
        val includeVisits = selectedGroups.contains(BackupGroup.VISITS)
        val includeTasks = selectedGroups.contains(BackupGroup.TASKS)
        val includeCustomPlaces = selectedGroups.contains(BackupGroup.CUSTOM_PLACES)
        val includeCaptures = selectedGroups.contains(BackupGroup.CAPTURES)
        val includeSettings = selectedGroups.contains(BackupGroup.SETTINGS)

        // Filter user favorites and items with private notes
        val userResources = if (includeResUserData) {
            allRes.filter { it.favorite || it.personalNotes.isNotBlank() }
                .map { res ->
                    ResourceUserDataBackup(
                        resourceId = res.id,
                        name = res.name,
                        address = res.address,
                        favorite = res.favorite,
                        personalNotes = res.personalNotes,
                        createdVia = res.createdVia,
                        source = res.source
                    )
                }
        } else emptyList()

        val userRmp = if (includeRmpUserData) {
            allRmp.filter { it.favorite || it.personalNotes.isNotBlank() || it.timesUsed > 0 }
                .map { rmp ->
                    RmpUserDataBackup(
                        rmpId = rmp.id,
                        name = rmp.name,
                        address = rmp.address,
                        favorite = rmp.favorite,
                        personalNotes = rmp.personalNotes,
                        timesUsed = rmp.timesUsed,
                        lastUsedAt = rmp.lastUsedAt,
                        createdVia = rmp.createdVia,
                        source = rmp.source
                    )
                }
        } else emptyList()

        // Visits with resource metadata for portable reference
        val visitBackups = if (includeVisits) {
            allVisits.map { v ->
                val linkedRes = resMap[v.resourceId]
                VisitBackup(
                    id = v.id,
                    resourceId = v.resourceId,
                    resourceName = linkedRes?.name ?: "",
                    resourceAddress = linkedRes?.address ?: "",
                    visitedAt = v.visitedAt,
                    outcome = v.outcome,
                    waitMinutes = v.waitMinutes,
                    rating = v.rating,
                    notes = v.notes
                )
            }
        } else emptyList()

        // Tasks with resource name if linked
        val taskBackups = if (includeTasks) {
            allTasks.map { t ->
                val linkedRes = t.resourceId?.let { resMap[it] }
                TaskBackup(
                    id = t.id,
                    title = t.title,
                    notes = t.notes,
                    kind = t.kind,
                    dueAt = t.dueAt,
                    resourceId = t.resourceId,
                    resourceName = linkedRes?.name,
                    done = t.done,
                    priority = t.priority,
                    createdAt = t.createdAt
                )
            }
        } else emptyList()

        // Custom places added manually or via AI
        val customRes = if (includeCustomPlaces) allRes.filter { it.createdVia != "seed" } else emptyList()
        val customRmp = if (includeCustomPlaces) allRmp.filter { it.createdVia != "seed" } else emptyList()

        // Captures
        val captureBackups = if (includeCaptures) allCaptures else emptyList()

        // Settings
        val settingsBackup = if (includeSettings) allSettings.associate { it.key to it.value } else emptyMap()

        // Provenance calculation
        var seedCount = 0
        var userCount = 0
        var importedCount = 0
        var aiCount = 0

        fun tallyProvenance(createdVia: String) {
            when (createdVia.lowercase()) {
                "seed" -> seedCount++
                "imported" -> importedCount++
                "ai" -> aiCount++
                else -> userCount++
            }
        }

        allRes.forEach { tallyProvenance(it.createdVia) }
        allRmp.forEach { tallyProvenance(it.createdVia) }

        val totalFavs = userResources.count { it.favorite } + userRmp.count { it.favorite }
        val totalNotes = userResources.count { it.personalNotes.isNotBlank() } + userRmp.count { it.personalNotes.isNotBlank() }
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US)
        val now = System.currentTimeMillis()

        CompassBackup(
            metadata = BackupMetadata(
                app = "Compass SF",
                version = 1,
                exportedAt = now,
                exportedDateFormatted = sdf.format(Date(now)),
                totalFavorites = totalFavs,
                totalNotes = totalNotes,
                totalVisits = visitBackups.size,
                totalTasks = taskBackups.size,
                totalCustomPlaces = customRes.size + customRmp.size,
                totalCaptures = captureBackups.size,
                totalSettings = settingsBackup.size,
                includedGroups = selectedGroups.map { it.id },
                provenanceSummary = mapOf(
                    "seed" to seedCount,
                    "user" to userCount,
                    "imported" to importedCount,
                    "ai" to aiCount
                )
            ),
            resources = userResources,
            rmpLocations = userRmp,
            visits = visitBackups,
            tasks = taskBackups,
            customResources = customRes,
            customRmpLocations = customRmp,
            captures = captureBackups,
            settings = settingsBackup
        )
    }

    private val backupJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    fun serializeBackup(backup: CompassBackup): String {
        return backupJson.encodeToString(backup)
    }

    fun parseBackupJson(jsonString: String): ImportPreview {
        val trimmed = jsonString.trim()
        val backup = backupJson.decodeFromString<CompassBackup>(trimmed)
        val favCount = if (backup.metadata.totalFavorites > 0) backup.metadata.totalFavorites
            else (backup.resources.count { it.favorite } + backup.rmpLocations.count { it.favorite })
        val notesCount = if (backup.metadata.totalNotes > 0) backup.metadata.totalNotes
            else (backup.resources.count { it.personalNotes.isNotBlank() } + backup.rmpLocations.count { it.personalNotes.isNotBlank() })
        val dateStr = if (backup.metadata.exportedDateFormatted.isNotBlank()) backup.metadata.exportedDateFormatted
            else SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(backup.metadata.exportedAt))

        val parsedGroups = if (backup.metadata.includedGroups.isNotEmpty()) {
            backup.metadata.includedGroups.mapNotNull { BackupGroup.fromId(it) }
        } else {
            // Infer from payload content for backward compatibility with legacy backups
            val inferred = mutableListOf<BackupGroup>()
            if (backup.resources.isNotEmpty() || backup.rmpLocations.isNotEmpty()) {
                inferred.add(BackupGroup.FAVORITES_NOTES)
                inferred.add(BackupGroup.RESOURCE_USER_DATA)
                inferred.add(BackupGroup.RMP_LOCATIONS)
            }
            if (backup.visits.isNotEmpty()) inferred.add(BackupGroup.VISITS)
            if (backup.tasks.isNotEmpty()) inferred.add(BackupGroup.TASKS)
            if (backup.customResources.isNotEmpty() || backup.customRmpLocations.isNotEmpty()) inferred.add(BackupGroup.CUSTOM_PLACES)
            if (backup.captures.isNotEmpty()) inferred.add(BackupGroup.CAPTURES)
            if (backup.settings.isNotEmpty()) inferred.add(BackupGroup.SETTINGS)
            if (inferred.isEmpty()) BackupGroup.entries else inferred
        }

        return ImportPreview(
            app = backup.metadata.app.ifBlank { "Compass SF" },
            exportedDate = dateStr,
            favoriteCount = favCount,
            notesCount = notesCount,
            visitCount = backup.visits.size,
            taskCount = backup.tasks.size,
            customPlacesCount = backup.customResources.size + backup.customRmpLocations.size,
            captureCount = backup.captures.size,
            settingsCount = backup.settings.size,
            includedGroups = parsedGroups,
            rawBackup = backup
        )
    }

    suspend fun restoreBackup(
        backup: CompassBackup,
        groupsToRestore: Set<BackupGroup> = BackupGroup.entries.toSet()
    ): ImportResult = withContext(Dispatchers.IO) {
        var favsUpdated = 0
        var notesUpdated = 0
        var visitsRestored = 0
        var tasksRestored = 0
        var customPlacesRestored = 0
        var capturesRestored = 0
        var settingsRestored = 0

        var currentResources = dao.getAllResources()
        var currentRmp = dao.getAllRmpLocations()

        val restoreCustomPlaces = groupsToRestore.contains(BackupGroup.CUSTOM_PLACES)
        val restoreFavsNotes = groupsToRestore.contains(BackupGroup.FAVORITES_NOTES)
        val restoreResUserData = groupsToRestore.contains(BackupGroup.RESOURCE_USER_DATA) || restoreFavsNotes
        val restoreRmpUserData = groupsToRestore.contains(BackupGroup.RMP_LOCATIONS) || restoreFavsNotes
        val restoreVisits = groupsToRestore.contains(BackupGroup.VISITS)
        val restoreTasks = groupsToRestore.contains(BackupGroup.TASKS)
        val restoreCaptures = groupsToRestore.contains(BackupGroup.CAPTURES)
        val restoreSettings = groupsToRestore.contains(BackupGroup.SETTINGS)

        // 1. Restore Custom Resources
        if (restoreCustomPlaces) {
            for (custom in backup.customResources) {
                if (custom.name.isBlank()) continue
                val exists = currentResources.any { it.name.trim().equals(custom.name.trim(), ignoreCase = true) }
                if (!exists) {
                    val provenance = if (custom.createdVia.isBlank() || custom.createdVia == "seed") "imported" else custom.createdVia
                    dao.insertResource(custom.copy(id = 0, createdVia = provenance))
                    customPlacesRestored++
                }
            }
            if (backup.customResources.isNotEmpty()) {
                currentResources = dao.getAllResources()
            }
        }

        // 2. Restore Resource Favorites & Notes
        if (restoreResUserData) {
            for (resBackup in backup.resources) {
                if (resBackup.name.isBlank() && resBackup.resourceId == null) continue
                val existing = currentResources.find {
                    (resBackup.name.isNotBlank() && it.name.trim().equals(resBackup.name.trim(), ignoreCase = true)) ||
                    (resBackup.resourceId != null && it.id == resBackup.resourceId)
                }
                if (existing != null) {
                    var changed = false
                    var newFav = existing.favorite
                    var newNotes = existing.personalNotes

                    if (resBackup.favorite && !existing.favorite) {
                        newFav = true
                        favsUpdated++
                        changed = true
                    }
                    if (resBackup.personalNotes.isNotBlank()) {
                        if (existing.personalNotes.isBlank()) {
                            newNotes = resBackup.personalNotes
                            notesUpdated++
                            changed = true
                        } else if (!existing.personalNotes.contains(resBackup.personalNotes.trim())) {
                            newNotes = "${existing.personalNotes}\n---\n${resBackup.personalNotes.trim()}"
                            notesUpdated++
                            changed = true
                        }
                    }
                    if (changed) {
                        dao.updateResource(existing.copy(favorite = newFav, personalNotes = newNotes))
                    }
                }
            }
        }

        // 3. Restore Custom RMP Locations
        if (restoreCustomPlaces || groupsToRestore.contains(BackupGroup.RMP_LOCATIONS)) {
            for (custom in backup.customRmpLocations) {
                if (custom.name.isBlank()) continue
                val exists = currentRmp.any { it.name.trim().equals(custom.name.trim(), ignoreCase = true) }
                if (!exists) {
                    val provenance = if (custom.createdVia.isBlank() || custom.createdVia == "seed") "imported" else custom.createdVia
                    dao.insertRmpLocation(custom.copy(id = 0, createdVia = provenance))
                    customPlacesRestored++
                }
            }
            if (backup.customRmpLocations.isNotEmpty()) {
                currentRmp = dao.getAllRmpLocations()
            }
        }

        // 4. Restore RMP Favorites & Notes
        if (restoreRmpUserData) {
            for (rmpBackup in backup.rmpLocations) {
                if (rmpBackup.name.isBlank() && rmpBackup.rmpId == null) continue
                val existing = currentRmp.find {
                    (rmpBackup.name.isNotBlank() && it.name.trim().equals(rmpBackup.name.trim(), ignoreCase = true)) ||
                    (rmpBackup.rmpId != null && it.id == rmpBackup.rmpId)
                }
                if (existing != null) {
                    var changed = false
                    var newFav = existing.favorite
                    var newNotes = existing.personalNotes
                    var newTimes = existing.timesUsed

                    if (rmpBackup.favorite && !existing.favorite) {
                        newFav = true
                        favsUpdated++
                        changed = true
                    }
                    if (rmpBackup.personalNotes.isNotBlank()) {
                        if (existing.personalNotes.isBlank()) {
                            newNotes = rmpBackup.personalNotes
                            notesUpdated++
                            changed = true
                        } else if (!existing.personalNotes.contains(rmpBackup.personalNotes.trim())) {
                            newNotes = "${existing.personalNotes}\n---\n${rmpBackup.personalNotes.trim()}"
                            notesUpdated++
                            changed = true
                        }
                    }
                    if (rmpBackup.timesUsed > existing.timesUsed) {
                        newTimes = rmpBackup.timesUsed
                        changed = true
                    }
                    if (changed) {
                        dao.updateRmpLocation(
                            existing.copy(
                                favorite = newFav,
                                personalNotes = newNotes,
                                timesUsed = newTimes,
                                lastUsedAt = rmpBackup.lastUsedAt ?: existing.lastUsedAt
                            )
                        )
                    }
                }
            }
        }

        // 5. Restore Visits
        if (restoreVisits) {
            val currentVisits = dao.getAllVisits()
            val latestResources = dao.getAllResources()
            for (v in backup.visits) {
                if (v.resourceId <= 0 && v.resourceName.isBlank()) continue
                val targetResId = if (v.resourceName.isNotBlank()) {
                    latestResources.find { it.name.trim().equals(v.resourceName.trim(), ignoreCase = true) }?.id ?: v.resourceId
                } else {
                    v.resourceId
                }

                val isDuplicate = currentVisits.any { existing ->
                    existing.resourceId == targetResId &&
                    existing.outcome == v.outcome &&
                    (abs(existing.visitedAt - v.visitedAt) < 60000 || (v.notes.isNotBlank() && existing.notes == v.notes))
                }

                if (!isDuplicate) {
                    dao.insertVisit(
                        Visit(
                            id = 0,
                            resourceId = targetResId,
                            visitedAt = v.visitedAt,
                            outcome = v.outcome,
                            waitMinutes = v.waitMinutes,
                            rating = v.rating,
                            notes = v.notes
                        )
                    )
                    visitsRestored++
                }
            }
        }

        // 6. Restore Tasks
        if (restoreTasks) {
            val currentTasks = dao.getAllTasks()
            val latestResources = dao.getAllResources()
            for (t in backup.tasks) {
                if (t.title.isBlank()) continue
                val isDuplicate = currentTasks.any { existing ->
                    existing.title.trim().equals(t.title.trim(), ignoreCase = true) &&
                    existing.kind == t.kind
                }
                if (!isDuplicate) {
                    val matchedResId = if (!t.resourceName.isNullOrBlank()) {
                        latestResources.find { it.name.trim().equals(t.resourceName.trim(), ignoreCase = true) }?.id ?: t.resourceId
                    } else {
                        t.resourceId
                    }
                    dao.insertTask(
                        Task(
                            id = 0,
                            title = t.title,
                            notes = t.notes,
                            kind = t.kind,
                            dueAt = t.dueAt,
                            resourceId = matchedResId,
                            done = t.done,
                            priority = t.priority,
                            createdAt = t.createdAt
                        )
                    )
                    tasksRestored++
                }
            }
        }

        // 7. Restore Captures
        if (restoreCaptures) {
            val currentCaptures = dao.getAllCaptures()
            for (c in backup.captures) {
                if (c.rawText.isBlank()) continue
                val isDuplicate = currentCaptures.any { existing ->
                    existing.rawText.trim() == c.rawText.trim()
                }
                if (!isDuplicate) {
                    dao.insertCapture(c.copy(id = 0))
                    capturesRestored++
                }
            }
        }

        // 8. Restore Settings
        if (restoreSettings) {
            for ((key, valStr) in backup.settings) {
                if (key.isBlank()) continue
                saveSetting(key, valStr)
                settingsRestored++
            }
        }

        ImportResult(
            favoritesUpdated = favsUpdated,
            notesUpdated = notesUpdated,
            visitsRestored = visitsRestored,
            tasksRestored = tasksRestored,
            customPlacesRestored = customPlacesRestored,
            capturesRestored = capturesRestored,
            settingsRestored = settingsRestored
        )
    }

    // --- Privacy, Search History & Photo Cache Methods (Chunk 5) ---

    suspend fun getIncognitoSearchMode(): Boolean = withContext(Dispatchers.IO) {
        getSetting("incognito_search_mode")?.toBooleanStrictOrNull() ?: false
    }

    suspend fun saveIncognitoSearchMode(enabled: Boolean) = withContext(Dispatchers.IO) {
        saveSetting("incognito_search_mode", enabled.toString())
    }

    suspend fun getRecentSearches(): List<String> = withContext(Dispatchers.IO) {
        val raw = getSetting("recent_searches") ?: ""
        if (raw.isBlank()) emptyList()
        else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    suspend fun addRecentSearch(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext
        if (getIncognitoSearchMode()) return@withContext // Do not store in incognito mode

        val current = getRecentSearches().toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val capped = current.take(10)
        saveSetting("recent_searches", capped.joinToString(","))
    }

    suspend fun clearRecentSearches() = withContext(Dispatchers.IO) {
        saveSetting("recent_searches", "")
    }

    suspend fun getPhotoCacheStats(): PhotoCacheStats = withContext(Dispatchers.IO) {
        val count = dao.getCaptureCount()
        PhotoCacheStats(
            captureCount = count,
            estimatedSizeKb = count * 28L,
            hasPendingPhoto = count > 0
        )
    }

    suspend fun clearPhotoCache(): Int = withContext(Dispatchers.IO) {
        dao.deleteAllCaptures()
    }

    suspend fun getProvenanceStats(): ProvenanceStats = withContext(Dispatchers.IO) {
        val allRes = dao.getAllResources()
        val allRmp = dao.getAllRmpLocations()

        var seed = 0
        var user = 0
        var imported = 0
        var ai = 0

        fun tally(via: String) {
            when (via.lowercase()) {
                "seed" -> seed++
                "imported" -> imported++
                "ai" -> ai++
                else -> user++
            }
        }

        allRes.forEach { tally(it.createdVia) }
        allRmp.forEach { tally(it.createdVia) }

        ProvenanceStats(
            totalRecords = allRes.size + allRmp.size,
            seedCount = seed,
            userCreatedCount = user,
            importedCount = imported,
            aiAssistedCount = ai
        )
    }
}

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

    suspend fun insertCapture(capture: Capture): Long = dao.insertCapture(capture)
    suspend fun updateCapture(capture: Capture) = dao.updateCapture(capture)

    suspend fun getAllVisits(): List<Visit> = dao.getAllVisits()

    suspend fun createBackupData(): CompassBackup = withContext(Dispatchers.IO) {
        val allRes = dao.getAllResources()
        val allRmp = dao.getAllRmpLocations()
        val allVisits = dao.getAllVisits()
        val allTasks = dao.getAllTasks()

        val resMap = allRes.associateBy { it.id }

        // Filter user favorites and items with private notes
        val userResources = allRes.filter { it.favorite || it.personalNotes.isNotBlank() }
            .map { res ->
                ResourceUserDataBackup(
                    resourceId = res.id,
                    name = res.name,
                    address = res.address,
                    favorite = res.favorite,
                    personalNotes = res.personalNotes
                )
            }

        val userRmp = allRmp.filter { it.favorite || it.personalNotes.isNotBlank() || it.timesUsed > 0 }
            .map { rmp ->
                RmpUserDataBackup(
                    rmpId = rmp.id,
                    name = rmp.name,
                    address = rmp.address,
                    favorite = rmp.favorite,
                    personalNotes = rmp.personalNotes,
                    timesUsed = rmp.timesUsed,
                    lastUsedAt = rmp.lastUsedAt
                )
            }

        // Visits with resource metadata for portable reference
        val visitBackups = allVisits.map { v ->
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

        // Tasks with resource name if linked
        val taskBackups = allTasks.map { t ->
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

        // Custom places added manually or via AI
        val customRes = allRes.filter { it.createdVia != "seed" }
        val customRmp = allRmp.filter { it.createdVia != "seed" }

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
                totalCustomPlaces = customRes.size + customRmp.size
            ),
            resources = userResources,
            rmpLocations = userRmp,
            visits = visitBackups,
            tasks = taskBackups,
            customResources = customRes,
            customRmpLocations = customRmp
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
        val backup = backupJson.decodeFromString<CompassBackup>(jsonString)
        val favCount = if (backup.metadata.totalFavorites > 0) backup.metadata.totalFavorites
            else (backup.resources.count { it.favorite } + backup.rmpLocations.count { it.favorite })
        val notesCount = if (backup.metadata.totalNotes > 0) backup.metadata.totalNotes
            else (backup.resources.count { it.personalNotes.isNotBlank() } + backup.rmpLocations.count { it.personalNotes.isNotBlank() })
        val dateStr = if (backup.metadata.exportedDateFormatted.isNotBlank()) backup.metadata.exportedDateFormatted
            else SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(backup.metadata.exportedAt))

        return ImportPreview(
            app = backup.metadata.app.ifBlank { "Compass SF" },
            exportedDate = dateStr,
            favoriteCount = favCount,
            notesCount = notesCount,
            visitCount = backup.visits.size,
            taskCount = backup.tasks.size,
            customPlacesCount = backup.customResources.size + backup.customRmpLocations.size,
            rawBackup = backup
        )
    }

    suspend fun restoreBackup(backup: CompassBackup): ImportResult = withContext(Dispatchers.IO) {
        var favsUpdated = 0
        var notesUpdated = 0
        var visitsRestored = 0
        var tasksRestored = 0
        var customPlacesRestored = 0

        var currentResources = dao.getAllResources()
        var currentRmp = dao.getAllRmpLocations()

        // 1. Restore Custom Resources
        for (custom in backup.customResources) {
            val exists = currentResources.any { it.name.trim().equals(custom.name.trim(), ignoreCase = true) }
            if (!exists) {
                dao.insertResource(custom.copy(id = 0))
                customPlacesRestored++
            }
        }
        if (backup.customResources.isNotEmpty()) {
            currentResources = dao.getAllResources()
        }

        // 2. Restore Resource Favorites & Notes
        for (resBackup in backup.resources) {
            val existing = currentResources.find {
                it.name.trim().equals(resBackup.name.trim(), ignoreCase = true) ||
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

        // 3. Restore Custom RMP Locations
        for (custom in backup.customRmpLocations) {
            val exists = currentRmp.any { it.name.trim().equals(custom.name.trim(), ignoreCase = true) }
            if (!exists) {
                dao.insertRmpLocation(custom.copy(id = 0))
                customPlacesRestored++
            }
        }
        if (backup.customRmpLocations.isNotEmpty()) {
            currentRmp = dao.getAllRmpLocations()
        }

        // 4. Restore RMP Favorites & Notes
        for (rmpBackup in backup.rmpLocations) {
            val existing = currentRmp.find {
                it.name.trim().equals(rmpBackup.name.trim(), ignoreCase = true) ||
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

        // 5. Restore Visits
        val currentVisits = dao.getAllVisits()
        val latestResources = dao.getAllResources()
        for (v in backup.visits) {
            val targetResId = if (v.resourceName.isNotBlank()) {
                latestResources.find { it.name.trim().equals(v.resourceName.trim(), ignoreCase = true) }?.id ?: v.resourceId
            } else {
                v.resourceId
            }

            val isDuplicate = currentVisits.any { existing ->
                existing.resourceId == targetResId &&
                existing.outcome == v.outcome &&
                (abs(existing.visitedAt - v.visitedAt) < 60000 || existing.notes == v.notes)
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

        // 6. Restore Tasks
        val currentTasks = dao.getAllTasks()
        for (t in backup.tasks) {
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

        ImportResult(
            favoritesUpdated = favsUpdated,
            notesUpdated = notesUpdated,
            visitsRestored = visitsRestored,
            tasksRestored = tasksRestored,
            customPlacesRestored = customPlacesRestored
        )
    }
}

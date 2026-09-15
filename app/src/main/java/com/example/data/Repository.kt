package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CompassRepository(private val dao: ResourceDao) {

    val allResourcesFlow: Flow<List<Resource>> = dao.getAllResourcesFlow()
    val allTasksFlow: Flow<List<Task>> = dao.getAllTasksFlow()
    val allPlansFlow: Flow<List<Plan>> = dao.getAllPlansFlow()
    val allRmpLocationsFlow: Flow<List<RmpLocation>> = dao.getAllRmpLocationsFlow()
    val allCapturesFlow: Flow<List<Capture>> = dao.getAllCapturesFlow()

    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        val existingResources = dao.getAllResources()
        val existingMap = existingResources.associateBy { it.name.trim().lowercase() }
        for (res in SeedData.SEED_RESOURCES) {
            val existing = existingMap[res.name.trim().lowercase()]
            if (existing == null) {
                dao.insertResource(res)
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
            val existing = existingRmpMap[rmp.name.trim().lowercase()]
            if (existing == null) {
                dao.insertRmpLocation(rmp)
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
}

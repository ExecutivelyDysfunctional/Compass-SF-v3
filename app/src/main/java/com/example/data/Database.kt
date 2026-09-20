package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {
    // Resources
    @Query("SELECT * FROM resources ORDER BY id DESC")
    fun getAllResourcesFlow(): Flow<List<Resource>>

    @Query("SELECT * FROM resources")
    suspend fun getAllResources(): List<Resource>

    @Query("SELECT * FROM resources WHERE id = :id")
    suspend fun getResourceById(id: Int): Resource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: Resource): Long

    @Update
    suspend fun updateResource(resource: Resource): Int

    @Delete
    suspend fun deleteResource(resource: Resource): Int

    @Query("SELECT COUNT(*) FROM resources")
    suspend fun getResourceCount(): Int

    // Visits
    @Query("SELECT * FROM visits ORDER BY visitedAt DESC")
    fun getAllVisitsFlow(): Flow<List<Visit>>

    @Query("SELECT * FROM visits ORDER BY visitedAt DESC")
    suspend fun getAllVisits(): List<Visit>

    @Query("SELECT * FROM visits WHERE resourceId = :resourceId ORDER BY visitedAt DESC")
    fun getVisitsForResourceFlow(resourceId: Int): Flow<List<Visit>>

    @Query("SELECT * FROM visits WHERE resourceId = :resourceId ORDER BY visitedAt DESC")
    suspend fun getVisitsForResource(resourceId: Int): List<Visit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: Visit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<Visit>): List<Long>

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY done ASC, priority ASC, id DESC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task): Int

    @Delete
    suspend fun deleteTask(task: Task): Int

    // Plans
    @Query("SELECT * FROM plans ORDER BY id DESC")
    fun getAllPlansFlow(): Flow<List<Plan>>

    @Query("SELECT * FROM plans")
    suspend fun getAllPlans(): List<Plan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: Plan): Long

    @Delete
    suspend fun deletePlan(plan: Plan): Int

    // RmpLocations
    @Query("SELECT * FROM rmp_locations ORDER BY id DESC")
    fun getAllRmpLocationsFlow(): Flow<List<RmpLocation>>

    @Query("SELECT * FROM rmp_locations")
    suspend fun getAllRmpLocations(): List<RmpLocation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRmpLocations(locations: List<RmpLocation>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRmpLocation(location: RmpLocation): Long

    @Update
    suspend fun updateRmpLocation(location: RmpLocation): Int

    @Query("SELECT COUNT(*) FROM rmp_locations")
    suspend fun getRmpLocationCount(): Int

    // AppSettings
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting): Long

    // Captures
    @Query("SELECT * FROM captures ORDER BY id DESC")
    fun getAllCapturesFlow(): Flow<List<Capture>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: Capture): Long

    @Update
    suspend fun updateCapture(capture: Capture): Int
}

@Database(
    entities = [
        Resource::class,
        Visit::class,
        Task::class,
        Plan::class,
        RmpLocation::class,
        AppSetting::class,
        Capture::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resourceDao(): ResourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "compass_sf_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

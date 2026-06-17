package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Int): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("SELECT * FROM components WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun getComponentsForProject(projectId: Int): Flow<List<ComponentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponent(component: ComponentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<ComponentEntity>)

    @Delete
    suspend fun deleteComponent(component: ComponentEntity)

    @Query("DELETE FROM components WHERE projectId = :projectId")
    suspend fun deleteComponentsOfProject(projectId: Int)

    @Query("DELETE FROM components WHERE id = :componentId")
    suspend fun deleteComponentById(componentId: String)
}

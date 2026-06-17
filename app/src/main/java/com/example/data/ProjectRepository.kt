package com.example.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getProjectById(id: Int): Flow<ProjectEntity?> {
        return projectDao.getProjectById(id)
    }

    fun getComponentsForProject(projectId: Int): Flow<List<ComponentEntity>> {
        return projectDao.getComponentsForProject(projectId)
    }

    suspend fun insertProject(project: ProjectEntity): Long {
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectDao.deleteComponentsOfProject(project.id)
        projectDao.deleteProject(project)
    }

    suspend fun insertComponent(component: ComponentEntity) {
        projectDao.insertComponent(component)
    }

    suspend fun insertComponents(components: List<ComponentEntity>) {
        projectDao.insertComponents(components)
    }

    suspend fun deleteComponent(component: ComponentEntity) {
        projectDao.deleteComponent(component)
    }

    suspend fun deleteComponentById(id: String) {
        projectDao.deleteComponentById(id)
    }
}

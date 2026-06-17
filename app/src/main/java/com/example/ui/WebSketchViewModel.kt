package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    PROJECT_LIST,
    EDITOR_DASHBOARD
}

enum class EditorTab {
    COMPONENTS,
    RAW_CODE,
    PREVIEW,
    AI_ASSISTANT
}

class WebSketchViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = ProjectRepository(db.projectDao())

    // UI screen logic
    val currentScreen = MutableStateFlow(AppScreen.PROJECT_LIST)
    val currentTab = MutableStateFlow(EditorTab.COMPONENTS)

    // Projects list
    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active project state
    val selectedProject = MutableStateFlow<ProjectEntity?>(null)
    
    // Components of the active project
    val selectedProjectComponents = selectedProject.flatMapLatest { project ->
        if (project != null) {
            repository.getComponentsForProject(project.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active selected component for editing details
    val editingComponent = MutableStateFlow<ComponentEntity?>(null)

    // AI Assist States
    val aiPrompt = MutableStateFlow("")
    val aiResponseAr = MutableStateFlow("")
    val aiHtmlSnippet = MutableStateFlow("")
    val isAiLoading = MutableStateFlow(false)

    init {
        // Automatically pre-populate default project if empty
        viewModelScope.launch {
            projects.collectLatest { list ->
                if (list.isEmpty()) {
                    createDefaultTemplateProject()
                }
            }
        }
    }

    private suspend fun createDefaultTemplateProject() {
        val projectId = repository.insertProject(
            ProjectEntity(
                name = "💡 تطبيق الويب التعريفي الذكي",
                description = "نموذج موقع تعريفي تفاعلي متكامل تم تصميمه بالسحب والإفلات لترويج الأعمال بنجاح.",
                customCss = """
                    body {
                        background: radial-gradient(circle at top left, #1e293b 0%, #0f172a 100%);
                        color: #f1f5f9;
                    }
                    .glass-button {
                        background: rgba(59, 130, 246, 0.2);
                        backdrop-filter: blur(12px);
                        border: 1px solid rgba(59, 130, 246, 0.4);
                        color: #60a5fa;
                        border-radius: 8px;
                    }
                """.trimIndent(),
                customJs = """
                    console.log("مرحباً بك في ويب سكتش برو! موقعك جاهز للعمل.");
                """.trimIndent()
            )
        ).toInt()

        // Default components
        val comps = listOf(
            ComponentEntity(
                id = "component_hero_title",
                projectId = projectId,
                type = "card_hero",
                text = "مرحباً بك في عالم الويب الحديث 🚀",
                orderIndex = 0
            ),
            ComponentEntity(
                id = "component_heading_services",
                projectId = projectId,
                type = "heading",
                text = "✨ خدماتنا المتميزة والمبتكرة",
                orderIndex = 1,
                textColorStyle = "primary",
                alignment = "center"
            ),
            ComponentEntity(
                id = "component_pricing_table",
                projectId = projectId,
                type = "card_pricing",
                text = "خطط التسعير والباقات المتاحة حالياً",
                orderIndex = 2
            ),
            ComponentEntity(
                id = "component_contact_heading",
                projectId = projectId,
                type = "heading",
                text = "📬 ابق على تواصل معنا الآن دقيقة واحدة",
                orderIndex = 3,
                textColorStyle = "accent",
                alignment = "center"
            ),
            ComponentEntity(
                id = "contact_form_container",
                projectId = projectId,
                type = "container",
                text = "صندوق المراسلات التفاعلي",
                orderIndex = 4,
                bgStyle = "glass",
                paddingStyle = "large",
                roundedStyle = "large"
            ),
            ComponentEntity(
                id = "input_field_email",
                projectId = projectId,
                parentId = "contact_form_container",
                type = "input",
                text = "البريد الإلكتروني",
                orderIndex = 0,
                roundedStyle = "small",
                placeholder = "أدخل بريدك الإلكتروني هنا..."
            ),
            ComponentEntity(
                id = "btn_submit_newsletter",
                projectId = projectId,
                parentId = "contact_form_container",
                type = "button",
                text = "اشترك في النشرة البريدية",
                orderIndex = 1,
                bgStyle = "primary",
                roundedStyle = "medium"
            )
        )
        repository.insertComponents(comps)
    }

    // Projects actions
    fun createProject(name: String, desc: String) {
        viewModelScope.launch {
            val proj = ProjectEntity(name = name, description = desc)
            repository.insertProject(proj)
        }
    }

    fun updateProjectDetails(project: ProjectEntity) {
        viewModelScope.launch {
            repository.updateProject(project)
            selectedProject.value = project
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (selectedProject.value?.id == project.id) {
                selectedProject.value = null
                currentScreen.value = AppScreen.PROJECT_LIST
            }
        }
    }

    fun openProject(project: ProjectEntity) {
        selectedProject.value = project
        currentScreen.value = AppScreen.EDITOR_DASHBOARD
        currentTab.value = EditorTab.COMPONENTS
    }

    fun closeProject() {
        selectedProject.value = null
        currentScreen.value = AppScreen.PROJECT_LIST
    }

    // Component operations
    fun addComponent(
        type: String, 
        text: String = "عنصر جديد", 
        parentId: String? = null,
        defaultValue: ComponentEntity? = null
    ) {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            val list = selectedProjectComponents.value
            val maxIndex = list.filter { it.parentId == parentId }.maxOfOrNull { it.orderIndex } ?: -1
            
            val comp = defaultValue?.copy(
                id = UUID.randomUUID().toString(),
                projectId = project.id,
                parentId = parentId,
                orderIndex = maxIndex + 1
            ) ?: ComponentEntity(
                id = UUID.randomUUID().toString(),
                projectId = project.id,
                parentId = parentId,
                type = type,
                text = text,
                orderIndex = maxIndex + 1,
                bgStyle = if (type == "container") "white" else "transparent",
                paddingStyle = if (type == "container") "medium" else "none"
            )
            repository.insertComponent(comp)
        }
    }

    fun updateComponent(component: ComponentEntity) {
        viewModelScope.launch {
            repository.insertComponent(component)
            if (editingComponent.value?.id == component.id) {
                editingComponent.value = component
            }
        }
    }

    fun deleteComponent(component: ComponentEntity) {
        viewModelScope.launch {
            val allComps = selectedProjectComponents.value
            val children = allComps.filter { it.parentId == component.id }
            for (child in children) {
                repository.deleteComponent(child)
            }
            repository.deleteComponent(component)
            if (editingComponent.value?.id == component.id) {
                editingComponent.value = null
            }
        }
    }

    fun moveComponentUp(component: ComponentEntity) {
        viewModelScope.launch {
            val list = selectedProjectComponents.value
                .filter { it.parentId == component.parentId }
                .sortedBy { it.orderIndex }
            val index = list.indexOfFirst { it.id == component.id }
            if (index > 0) {
                val prevComp = list[index - 1]
                repository.insertComponent(prevComp.copy(orderIndex = component.orderIndex))
                repository.insertComponent(component.copy(orderIndex = prevComp.orderIndex))
            }
        }
    }

    fun moveComponentDown(component: ComponentEntity) {
        viewModelScope.launch {
            val list = selectedProjectComponents.value
                .filter { it.parentId == component.parentId }
                .sortedBy { it.orderIndex }
            val index = list.indexOfFirst { it.id == component.id }
            if (index < list.size - 1 && index != -1) {
                val nextComp = list[index + 1]
                repository.insertComponent(nextComp.copy(orderIndex = component.orderIndex))
                repository.insertComponent(component.copy(orderIndex = nextComp.orderIndex))
            }
        }
    }

    // AI Assistant calls
    fun askAiAssistant(prompt: String) {
        if (prompt.isBlank() || isAiLoading.value) return
        isAiLoading.value = true
        aiPrompt.value = prompt
        viewModelScope.launch {
            try {
                val responseString = GeminiService.generateWebSnippet(prompt)
                val responseObj = org.json.JSONObject(responseString)
                aiResponseAr.value = responseObj.optString("responseAr", "تم التوليد بنجاح.")
                aiHtmlSnippet.value = responseObj.optString("htmlSnippet", "")
            } catch (e: Exception) {
                aiResponseAr.value = "حدث عطل في معالجة استجابة الذكاء الاصطناعي: ${e.localizedMessage}"
                aiHtmlSnippet.value = ""
            } finally {
                isAiLoading.value = false
            }
        }
    }

    fun importAiSnippetToProject() {
        val snippet = aiHtmlSnippet.value
        if (snippet.isBlank()) return
        
        viewModelScope.launch {
            val project = selectedProject.value ?: return@launch
            val list = selectedProjectComponents.value
            val maxIndex = list.maxOfOrNull { it.orderIndex } ?: -1
            
            // Insert parsed HTML as raw HTML card body
            val containerId = "ai_container_${UUID.randomUUID().toString().take(6)}"
            val parentContainer = ComponentEntity(
                id = containerId,
                projectId = project.id,
                type = "container",
                text = "💻 قسم مولد بالذكاء الاصطناعي",
                orderIndex = maxIndex + 1,
                bgStyle = "glass",
                paddingStyle = "medium",
                roundedStyle = "medium"
            )
            repository.insertComponent(parentContainer)
            
            val htmlBlock = ComponentEntity(
                id = UUID.randomUUID().toString(),
                projectId = project.id,
                parentId = containerId,
                type = "paragraph",
                text = snippet,
                orderIndex = 0
            )
            repository.insertComponent(htmlBlock)
            
            // Success reset
            aiHtmlSnippet.value = ""
            aiResponseAr.value = "تم إدراج القسم المولد بالذكاء الاصطناعي بنجاح في هيكل السحب والإفلات!"
        }
    }
}

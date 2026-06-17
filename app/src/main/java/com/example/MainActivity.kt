package com.example

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ComponentEntity
import com.example.data.ProjectEntity
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.WebCompiler
import kotlinx.coroutines.launch

// Color scheme for Cosmic IDE Vibe
val DeepSlateBg = Color(0xFF0F172A)
val EditorCardBg = Color(0xFF1E293B)
val LightBorderColor = Color(0xFF334155)
val CyberCyan = Color(0xFF06B6D4)
val NeonPurple = Color(0xFF8B5CF6)
val GlassWhite = Color(0x19FFFFFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepSlateBg
                ) {
                    WebSketchApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSketchApp() {
    val viewModel: WebSketchViewModel = viewModel()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val components by viewModel.selectedProjectComponents.collectAsState()
    
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = DeepSlateBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            when (currentScreen) {
                AppScreen.PROJECT_LIST -> {
                    ProjectListScreen(
                        projects = projects,
                        onCreateProject = { showCreateDialog = true },
                        onOpenProject = { viewModel.openProject(it) },
                        onDeleteProject = { viewModel.deleteProject(it) }
                    )
                }
                AppScreen.EDITOR_DASHBOARD -> {
                    selectedProject?.let { project ->
                        EditorDashboardScreen(
                            project = project,
                            components = components,
                            viewModel = viewModel,
                            onBackToProjects = { viewModel.closeProject() }
                        )
                    }
                }
            }
            
            // New Project Dialog
            if (showCreateDialog) {
                CreateProjectDialog(
                    onDismiss = { showCreateDialog = false },
                    onConfirm = { name, desc ->
                        viewModel.createProject(name, desc)
                        showCreateDialog = false
                        Toast.makeText(context, "تم إنشاء المشروع بنجاح", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// ==================== PROJECTS DIALOG & ELEMENTS ====================

@Composable
fun CreateProjectDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EditorCardBg,
        title = {
            Text(
                "🚀 مشروع ويب جديد",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المشروع") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = LightBorderColor,
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("وصف المشروع") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = LightBorderColor,
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, desc) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("تأكيد وإنشاء", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}

// ==================== SCREEN 1: PROJECT LIST ====================

@Composable
fun ProjectListScreen(
    projects: List<ProjectEntity>,
    onCreateProject: () -> Unit,
    onOpenProject: (ProjectEntity) -> Unit,
    onDeleteProject: (ProjectEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(CyberCyan, NeonPurple)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ويب سكتش برو WebSketch",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Code icon",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "برمجة وبناء صفحات الويب التفاعلية بالسحب والإفلات، الكود وتحرير الاستايل ومساعد Gemini الذكي.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مشاريعك البرمجية",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = onCreateProject,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = CyberCyan.copy(alpha = 0.15f),
                    contentColor = CyberCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add project", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مشروع جديد", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(color = CyberCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "جاري تحضير أول موقع جاهز تفاعلي لك...",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(
                        project = project,
                        onOpen = { onOpenProject(project) },
                        onDelete = { onDeleteProject(project) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: ProjectEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = EditorCardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LightBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = project.description.ifBlank { "لا يوجد وصف حالي لهذا المشروع الويبي." },
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = "Project icon",
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تصميم تفاعلي كامل",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "اضغط لتعديل الكود والمكونات",
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}


// ==================== SCREEN 2: EDITOR DASHBOARD ====================

@Composable
fun EditorDashboardScreen(
    project: ProjectEntity,
    components: List<ComponentEntity>,
    viewModel: WebSketchViewModel,
    onBackToProjects: () -> Unit
) {
    val tab by viewModel.currentTab.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showExportDialog by remember { mutableStateOf(false) }
    var showAddWidgetSheet by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // TOP CONTROL APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EditorCardBg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToProjects) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = project.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                    Text("بيئة عمل سكتش الويب", color = Color.Gray, fontSize = 11.sp)
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Compile / Hot-link live preview Tab
                IconButton(
                    onClick = { viewModel.currentTab.value = EditorTab.PREVIEW },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = CyberCyan.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run Preview", tint = CyberCyan)
                }
                
                // Export HTML code
                IconButton(
                    onClick = { showExportDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = NeonPurple.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export Code", tint = NeonPurple)
                }
            }
        }
        
        // Dynamic Workspace Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(DeepSlateBg)
        ) {
            when (tab) {
                EditorTab.COMPONENTS -> {
                    ComponentTreeWorkspace(
                        components = components,
                        viewModel = viewModel,
                        onOpenAddWidget = { showAddWidgetSheet = true }
                    )
                }
                EditorTab.RAW_CODE -> {
                    CustomCodeWorkspace(
                        project = project,
                        onSave = { css, js ->
                            viewModel.updateProjectDetails(project.copy(customCss = css, customJs = js))
                            Toast.makeText(context, "تم حفظ أكواد CSS/JS المخصصة نجاحاً", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                EditorTab.PREVIEW -> {
                    val html = WebCompiler.compileProjectToHtml(project, components)
                    LivePreviewWorkspace(htmlContent = html)
                }
                EditorTab.AI_ASSISTANT -> {
                    AiAssistantWorkspace(viewModel = viewModel)
                }
            }
        }
        
        // Navigation Bar for IDE Tabs
        NavigationBar(
            containerColor = EditorCardBg,
            tonalElevation = 8.dp,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            NavigationBarItem(
                selected = tab == EditorTab.COMPONENTS,
                onClick = { viewModel.currentTab.value = EditorTab.COMPONENTS },
                label = { Text("المكونات", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Layers, contentDescription = "Tree Components") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberCyan,
                    selectedTextColor = CyberCyan,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = CyberCyan.copy(alpha = 0.12f)
                )
            )
            NavigationBarItem(
                selected = tab == EditorTab.RAW_CODE,
                onClick = { viewModel.currentTab.value = EditorTab.RAW_CODE },
                label = { Text("أكواد مخصصة", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Code, contentDescription = "Edit manual Code") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberCyan,
                    selectedTextColor = CyberCyan,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = CyberCyan.copy(alpha = 0.12f)
                )
            )
            NavigationBarItem(
                selected = tab == EditorTab.PREVIEW,
                onClick = { viewModel.currentTab.value = EditorTab.PREVIEW },
                label = { Text("معاينة حية", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Computer, contentDescription = "Live browser") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberCyan,
                    selectedTextColor = CyberCyan,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = CyberCyan.copy(alpha = 0.12f)
                )
            )
            NavigationBarItem(
                selected = tab == EditorTab.AI_ASSISTANT,
                onClick = { viewModel.currentTab.value = EditorTab.AI_ASSISTANT },
                label = { Text("مساعد الذكاء", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Face, contentDescription = "Gemini helper") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonPurple,
                    selectedTextColor = NeonPurple,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = NeonPurple.copy(alpha = 0.12f)
                )
            )
        }
    }
    
    // Quick Add Drag-and-drop Component Bottom Sheet/Dialog
    if (showAddWidgetSheet) {
        AddWidgetDialog(
            components = components,
            onDismiss = { showAddWidgetSheet = false },
            onAdd = { type, title, containerId ->
                viewModel.addComponent(type, title, containerId)
                showAddWidgetSheet = false
                Toast.makeText(context, "$title أضيف للتصميم الويبي", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // Code Exporter Dialog
    if (showExportDialog) {
        val htmlContent = WebCompiler.compileProjectToHtml(project, components)
        ExportDialog(
            htmlContent = htmlContent,
            project = project,
            onDismiss = { showExportDialog = false }
        )
    }
}

// ==================== WORKSPACE 1: COMPONENT TREE (DRAG & DROP SYSTEM) ====================

@Composable
fun ComponentTreeWorkspace(
    components: List<ComponentEntity>,
    viewModel: WebSketchViewModel,
    onOpenAddWidget: () -> Unit
) {
    val editingComponent by viewModel.editingComponent.collectAsState()
    val listState = rememberLazyListState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Workspace Instruction Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "هيكل شجرة الصفحة (سحب وترتيب)",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${components.size} مكونات نشطة",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (components.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = "empty design",
                            modifier = Modifier.size(56.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("الملف فارغ. انقر الزر بالأسفل لإدراج المكونات الأولى.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Filter roots and nested layers
                    val parentIds = components.map { it.id }.toSet()
                    val roots = components.filter { it.parentId == null || !parentIds.contains(it.parentId) }
                        .sortedBy { it.orderIndex }
                    
                    for (root in roots) {
                        item(key = root.id) {
                            ComponentTreeItemCard(
                                component = root,
                                depth = 0,
                                viewModel = viewModel,
                                components = components,
                                isEditing = editingComponent?.id == root.id,
                                onEditProperty = { viewModel.editingComponent.value = root }
                            )
                        }
                        
                        // Recursive rendering of child cards with visual padding indenting to look like code tree
                        val children = components.filter { it.parentId == root.id }.sortedBy { it.orderIndex }
                        items(children, key = { it.id }) { child ->
                            ComponentTreeItemCard(
                                component = child,
                                depth = 1,
                                viewModel = viewModel,
                                components = components,
                                isEditing = editingComponent?.id == child.id,
                                onEditProperty = { viewModel.editingComponent.value = child }
                            )
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
        
        // Floating action Add Component triggers
        FloatingActionButton(
            onClick = onOpenAddWidget,
            containerColor = CyberCyan,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_widget_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(32.dp))
        }
        
        // Render details Style inspector if editing component is selected
        editingComponent?.let { component ->
            StylePropertyInspectorDialog(
                component = component,
                components = components,
                onDismiss = { viewModel.editingComponent.value = null },
                onSave = { updated ->
                    viewModel.updateComponent(updated)
                    viewModel.editingComponent.value = null
                }
            )
        }
    }
}

@Composable
fun ComponentTreeItemCard(
    component: ComponentEntity,
    depth: Int,
    viewModel: WebSketchViewModel,
    components: List<ComponentEntity>,
    isEditing: Boolean,
    onEditProperty: () -> Unit
) {
    val paddingIndent = (depth * 22).dp
    
    val badgeColor = when (component.type) {
        "container" -> neonPurpleGradient()
        "card_hero" -> cyberGradient()
        "card_pricing" -> cyberGradient()
        else -> defaultBadgeGradient()
    }
    
    val typeLabelAr = when (component.type) {
        "container" -> "حاوية Div"
        "heading" -> "عنوان H"
        "paragraph" -> "فقرة نصية Text"
        "button" -> "زر تفاعلي Button"
        "link" -> "رابط تشعبي 🔗"
        "image" -> "صورة جرافكس Image"
        "input" -> "حقل إدخال Input"
        "divider" -> "فاصل خطي Divider"
        "card_hero" -> "قسم بروموشن Hero"
        "card_pricing" -> "قسم تسعير Pricing"
        else -> "مكون مخصص"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingIndent)
            .clip(RoundedCornerShape(12.dp))
            .background(EditorCardBg)
            .border(
                width = if (isEditing) 1.5.dp else 1.dp,
                color = if (isEditing) CyberCyan else LightBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple visual line to represent Sketchware's connection guides
        if (depth > 0) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .background(CyberCyan.copy(alpha = 0.5f))
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // tag/badge color gradient
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = typeLabelAr,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (component.parentId != null) {
                    Text(
                        text = "داخلي",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Editable text preview
            Text(
                text = if (component.type == "input") "مؤشر المحتوى: ${component.placeholder.ifBlank { component.text }}" else component.text,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Interactive buttons: Move Up, Move Down, Edit Property, Delete Card
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = { viewModel.moveComponentUp(component) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color.LightGray, modifier = Modifier.size(14.dp))
            }
            IconButton(
                onClick = { viewModel.moveComponentDown(component) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color.LightGray, modifier = Modifier.size(14.dp))
            }
            IconButton(
                onClick = onEditProperty,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Edit Property", tint = CyberCyan, modifier = Modifier.size(15.dp))
            }
            IconButton(
                onClick = { viewModel.deleteComponent(component) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete component", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

// Gradients for tag badges
fun neonPurpleGradient() = Brush.linearGradient(colors = listOf(NeonPurple, Color(0xFFC084FC)))
fun cyberGradient() = Brush.linearGradient(colors = listOf(CyberCyan, Color(0xFF22D3EE)))
fun defaultBadgeGradient() = Brush.linearGradient(colors = listOf(Color(0xFF475569), Color(0xFF64748B)))

// ==================== WORKSPACE 2: CUSTOM CSS / JS EDITOR WORKSPACE ====================

@Composable
fun CustomCodeWorkspace(
    project: ProjectEntity,
    onSave: (String, String) -> Unit
) {
    var cssCode by remember { mutableStateOf(project.customCss) }
    var jsCode by remember { mutableStateOf(project.customJs) }
    var activeEditorIsCss by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = { activeEditorIsCss = true },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (activeEditorIsCss) CyberCyan.copy(alpha = 0.2f) else GlassWhite,
                    contentColor = if (activeEditorIsCss) CyberCyan else Color.LightGray
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Code, contentDescription = "CSS", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ستايل مخصص Style CSS")
            }
            
            FilledTonalButton(
                onClick = { activeEditorIsCss = false },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (!activeEditorIsCss) CyberCyan.copy(alpha = 0.2f) else GlassWhite,
                    contentColor = if (!activeEditorIsCss) CyberCyan else Color.LightGray
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Javascript, contentDescription = "JS", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تفوق تفاعلي Javascript")
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // Presets shortcuts for 1-click injections to avoid typing complex web codes on mobile keypad
        Text(
            text = "قوالب حقن برمجية سريعة بلمت واحدة بنقرة:",
            color = Color.LightGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeEditorIsCss) {
                // CSS presets
                AssistChip(
                    onClick = {
                        cssCode += "\n.glowing-card {\n    box-shadow: 0 0 20px rgba(6, 182, 212, 0.45);\n    border: 1px solid #06b6d4;\n}\n"
                    },
                    label = { Text("بطاقة متوهجة Glowing", color = Color.White, fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = EditorCardBg)
                )
                AssistChip(
                    onClick = {
                        cssCode += "\n@keyframes bouncePulse {\n    0%, 100% { transform: scale(1); }\n    50% { transform: scale(1.04); }\n}\n.animate-pulse-custom { animation: bouncePulse 2s infinite; }\n"
                    },
                    label = { Text("أنيميشن نبض Pulse", color = Color.White, fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = EditorCardBg)
                )
            } else {
                // JS presets
                AssistChip(
                    onClick = {
                        jsCode += "\n// تأكيد نقر الأزرار\ndocument.querySelectorAll('button').forEach(btn => {\n    btn.addEventListener('click', () -> {\n        alert('أهلاً بك! تم النقر على زر تفاعلي بنجاح.');\n    });\n});\n"
                    },
                    label = { Text("أكشن نقر الأزرار click", color = Color.White, fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = EditorCardBg)
                )
                AssistChip(
                    onClick = {
                        jsCode += "\n// تغيير عشوائي للون الخلفية\ndocument.body.addEventListener('dblclick', () => {\n    const colors = ['#0f172a','#1e1b4b','#111827'];\n    document.body.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];\n});\n"
                    },
                    label = { Text("تغير الخلفية DbClick", color = Color.White, fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = EditorCardBg)
                )
            }
        }
        
        // Monospace Text Editor field
        OutlinedTextField(
            value = if (activeEditorIsCss) cssCode else jsCode,
            onValueChange = {
                if (activeEditorIsCss) {
                    cssCode = it
                } else {
                    jsCode = it
                }
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                fontSize = 12.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = EditorCardBg,
                unfocusedContainerColor = EditorCardBg,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = LightBorderColor
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(14.dp))
        
        Button(
            onClick = { onSave(cssCode, jsCode) },
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = "Save Code", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("حفظ الأكواد محلياً بالكامل", fontWeight = FontWeight.Bold)
        }
    }
}

// ==================== WORKSPACE 3: LIVE PREVIEW BROWSER ====================

@Composable
fun LivePreviewWorkspace(htmlContent: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .height(28.dp),
            colors = CardDefaults.cardColors(containerColor = EditorCardBg),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Yellow))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Green))
                }
                Text("https://websketch.local/preview.html", color = Color.Gray, fontSize = 9.sp)
                Icon(Icons.Default.Refresh, contentDescription = "reload", tint = Color.Gray, modifier = Modifier.size(12.dp))
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
        ) {
            WebPreview(htmlContent = htmlContent, modifier = Modifier.fillMaxSize())
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPreview(htmlContent: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                
                // Set RTL dir explicitly on WebView settings if possible or handled in HTML tags
                loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}

// ==================== WORKSPACE 4: GEMINI AI WEB ASSISTANT ====================

@Composable
fun AiAssistantWorkspace(viewModel: WebSketchViewModel) {
    val prompt by viewModel.aiPrompt.collectAsState()
    val replyAr by viewModel.aiResponseAr.collectAsState()
    val snippet by viewModel.aiHtmlSnippet.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()
    
    var localPromptInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "🧠 مساعد الويب الذكي بالذكاء الاصطناعي",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            "اكتب له فكرتك لتوليد قسم كامل فريد بخواص ستايل مذهل، ثم قم بإدراجه مباشرة لبيئة العمل لشجرة الصفحة.",
            color = Color.Gray,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // clickable quick suggestion prompt clips
        Text("أفكار اقتراح للتوليد بلمسة واحدة:", color = Color.LightGray, fontSize = 11.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssistChip(
                onClick = { localPromptInput = "صمم نموذج تسجيل دخول زجاجي أنيق مع زر تسجيل ملون" },
                label = { Text("نموذج زجاجي 🔑", color = Color.White, fontSize = 10.sp) },
                colors = AssistChipDefaults.assistChipColors(containerColor = EditorCardBg)
            )
            AssistChip(
                onClick = { localPromptInput = "قم بتصميم قسم مميزات الشركة مع ثلاثة أيقونات وبطاقات ثلاثية الأبعاد" },
                label = { Text("قسم الخدمات 📈", color = Color.White, fontSize = 10.sp) },
                colors = AssistChipDefaults.assistChipColors(containerColor = EditorCardBg)
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Chat results container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(EditorCardBg)
                .border(1.dp, LightBorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NeonPurple)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("جاري استخلاص كود الويب المذهل بالذكاء الاصطناعي...", color = Color.LightGray, fontSize = 13.sp)
                }
            } else if (replyAr.isBlank() && snippet.isBlank()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Face, contentDescription = "ai face", modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("اكتب ما ترغب في تصميمه بالويب بالأسفل لتبهرك قوة الذكاء!", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            } else {
                // Interactive Result Display
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "👤 سؤالك: $prompt",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            Text(
                                text = "🤖 مساعد الويب: $replyAr",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        if (snippet.isNotBlank()) {
                            item {
                                Text(
                                    text = "📐 الكود البرمجي HTML المولد:",
                                    color = CyberCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DeepSlateBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = snippet,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            
                            item {
                                Text(
                                    text = "🔴 معاينة حية للمكون المولد:",
                                    color = Color.Green,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // In-app Mini Live preview frame of the component generated!
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                ) {
                                    WebPreview(
                                        htmlContent = """
                                            <!DOCTYPE html>
                                            <html lang="ar" dir="rtl">
                                            <head>
                                                <meta charset="UTF-8">
                                                <script src="https://cdn.tailwindcss.com"></script>
                                            </head>
                                            <body class="p-4 bg-slate-900 justify-center flex items-center min-h-screen">
                                                $snippet
                                            </body>
                                            </html>
                                        """.trimIndent(),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                    
                    if (snippet.isNotBlank()) {
                        Button(
                            onClick = {
                                viewModel.importAiSnippetToProject()
                                Toast.makeText(context, "تم إدراج المكون بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "import to tree", tint = DeepSlateBg)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استيراد كقسم لشجرة الصفحة", color = DeepSlateBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Input text prompt
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = localPromptInput,
                onValueChange = { localPromptInput = it },
                placeholder = { Text("مثال: بطاقة تسعير زجاجية...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = CyberCyan,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = LightBorderColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            
            FloatingActionButton(
                onClick = {
                    if (localPromptInput.isNotBlank()) {
                        viewModel.askAiAssistant(localPromptInput)
                        localPromptInput = ""
                    }
                },
                containerColor = NeonPurple,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send prompt", modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ==================== DIALOG 2: PROPERTY STYLE INSPECTOR DIALOG ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylePropertyInspectorDialog(
    component: ComponentEntity,
    components: List<ComponentEntity>,
    onDismiss: () -> Unit,
    onSave: (ComponentEntity) -> Unit
) {
    var textState by remember { mutableStateOf(component.text) }
    var bgSelected by remember { mutableStateOf(component.bgStyle) }
    var textColorSelected by remember { mutableStateOf(component.textColorStyle) }
    var paddingSelected by remember { mutableStateOf(component.paddingStyle) }
    var marginSelected by remember { mutableStateOf(component.marginStyle) }
    var roundedSelected by remember { mutableStateOf(component.roundedStyle) }
    var widthSelected by remember { mutableStateOf(component.widthStyle) }
    var alignSelected by remember { mutableStateOf(component.alignment) }
    var parentIdSelected by remember { mutableStateOf(component.parentId) }
    
    // Type specific additions
    var hrefState by remember { mutableStateOf(component.href) }
    var imageUrlState by remember { mutableStateOf(component.imageUrl) }
    var placeholderState by remember { mutableStateOf(component.placeholder) }
    var isFlexRowState by remember { mutableStateOf(component.isFlexRow) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EditorCardBg,
        title = {
            Text(
                "🎨 خصائص الستايل والترميز",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Component ID & Tag preview
                item {
                    Text(
                        "معرف العنصر الويبي: ID: `${component.id.take(8)}`",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                // Editable component string content
                if (component.type != "divider") {
                    item {
                        OutlinedTextField(
                            value = textState,
                            onValueChange = { textState = it },
                            label = { Text("النص أو التسمية للعنصر") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = LightBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                
                // Nesting level controller (parent dropdown)
                item {
                    Text("الحاوية الأب (الاحتواء الداخلي للهيكل):", color = Color.LightGray, fontSize = 12.sp)
                    val containerOptions = components.filter { it.type == "container" && it.id != component.id }
                    
                    ScrollableRadioGroup(
                        options = listOf("بدون حاوية (قيمة فارغة)") + containerOptions.map { "${it.text} (${it.id.take(4)})" },
                        selectedIndex = if (parentIdSelected == null) 0 else {
                            containerOptions.indexOfFirst { it.id == parentIdSelected } + 1
                        },
                        onSelected = { index ->
                            parentIdSelected = if (index == 0) null else containerOptions[index - 1].id
                        }
                    )
                }
                
                // Background properties
                item {
                    Text("خلفية العنصر (Background):", color = Color.LightGray, fontSize = 12.sp)
                    val bgOptions = listOf("transparent", "white", "dark", "primary", "secondary", "accent", "glass")
                    ScrollableRadioGroup(
                        options = bgOptions,
                        selectedIndex = bgOptions.indexOf(bgSelected).coerceAtLeast(0),
                        onSelected = { bgSelected = bgOptions[it] }
                    )
                }
                
                // Text color properties
                item {
                    Text("لون الخط (Text Color):", color = Color.LightGray, fontSize = 12.sp)
                    val textColors = listOf("dark", "light", "primary", "accent")
                    ScrollableRadioGroup(
                        options = textColors,
                        selectedIndex = textColors.indexOf(textColorSelected).coerceAtLeast(0),
                        onSelected = { textColorSelected = textColors[it] }
                    )
                }
                
                // Width style properties
                item {
                    Text("عرض العنصر (Width):", color = Color.LightGray, fontSize = 12.sp)
                    val widthOptions = listOf("auto", "half", "full")
                    ScrollableRadioGroup(
                        options = widthOptions,
                        selectedIndex = widthOptions.indexOf(widthSelected).coerceAtLeast(0),
                        onSelected = { widthSelected = widthOptions[it] }
                    )
                }

                // Alignment properties
                item {
                    Text("محاذاة المحتوى (Alignment):", color = Color.LightGray, fontSize = 12.sp)
                    val alignList = listOf("right", "center", "left")
                    ScrollableRadioGroup(
                        options = alignList,
                        selectedIndex = alignList.indexOf(alignSelected).coerceAtLeast(0),
                        onSelected = { alignSelected = alignList[it] }
                    )
                }

                // Padding properties
                item {
                    Text("محاذاة الهامش الداخلي (Padding):", color = Color.LightGray, fontSize = 12.sp)
                    val paddings = listOf("none", "small", "medium", "large")
                    ScrollableRadioGroup(
                        options = paddings,
                        selectedIndex = paddings.indexOf(paddingSelected).coerceAtLeast(0),
                        onSelected = { paddingSelected = paddings[it] }
                    )
                }

                // Margin properties
                item {
                    Text("الهامش الخارجي بين المكونات (Margin):", color = Color.LightGray, fontSize = 12.sp)
                    val margins = listOf("none", "small", "medium", "large")
                    ScrollableRadioGroup(
                        options = margins,
                        selectedIndex = margins.indexOf(marginSelected).coerceAtLeast(0),
                        onSelected = { marginSelected = margins[it] }
                    )
                }

                // Rounded properties
                item {
                    Text("انحناء حواف الصندوق لزوايا الأطراف:", color = Color.LightGray, fontSize = 12.sp)
                    val roundedList = listOf("none", "small", "medium", "large", "full")
                    ScrollableRadioGroup(
                        options = roundedList,
                        selectedIndex = roundedList.indexOf(roundedSelected).coerceAtLeast(0),
                        onSelected = { roundedSelected = roundedList[it] }
                    )
                }
                
                // If type == container, allow flex settings
                if (component.type == "container") {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isFlexRowState,
                                onCheckedChange = { isFlexRowState = it },
                                colors = CheckboxDefaults.colors(checkedColor = CyberCyan)
                            )
                            Text("تخطيط أفقي بجانب بعض Flex Row", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
                
                // Type specific attributes editors
                if (component.type == "link") {
                    item {
                        OutlinedTextField(
                            value = hrefState,
                            onValueChange = { hrefState = it },
                            label = { Text("وجهة الرابط href") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = LightBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                
                if (component.type == "image") {
                    item {
                        OutlinedTextField(
                            value = imageUrlState,
                            onValueChange = { imageUrlState = it },
                            label = { Text("رابط عنوان الصورة src") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = LightBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                
                if (component.type == "input") {
                    item {
                        OutlinedTextField(
                            value = placeholderState,
                            onValueChange = { placeholderState = it },
                            label = { Text("نص الإرشاد داخل الحقل placeholder") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = LightBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        component.copy(
                            text = textState,
                            bgStyle = bgSelected,
                            textColorStyle = textColorSelected,
                            paddingStyle = paddingSelected,
                            marginStyle = marginSelected,
                            roundedStyle = roundedSelected,
                            widthStyle = widthSelected,
                            alignment = alignSelected,
                            parentId = parentIdSelected,
                            href = hrefState,
                            imageUrl = imageUrlState,
                            placeholder = placeholderState,
                            isFlexRow = isFlexRowState
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text("تطبيق التعديلات", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}

@Composable
fun ScrollableRadioGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options.size) { index ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else EditorCardBg)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) CyberCyan else LightBorderColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelected(index) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = options[index],
                    color = if (isSelected) CyberCyan else Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==================== DIALOG 3: ADD WIDGET DIALOG ====================

@Composable
fun AddWidgetDialog(
    components: List<ComponentEntity>,
    onDismiss: () -> Unit,
    onAdd: (type: String, name: String, parentId: String?) -> Unit
) {
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EditorCardBg,
        title = {
            Text(
                "➕ إدراج مكون ويب جديد",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dropdown to select parent layout (Container)
                val containers = components.filter { it.type == "container" }
                if (containers.isNotEmpty()) {
                    Column {
                        Text(
                            "موقع الحشرو (هل داخل حاوية معينة):",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        ScrollableRadioGroup(
                            options = listOf("الرئيسي (بودي)") + containers.map { "${it.text} (${it.id.take(4)})" },
                            selectedIndex = if (selectedParentId == null) 0 else {
                                containers.indexOfFirst { it.id == selectedParentId } + 1
                            },
                            onSelected = { index ->
                                selectedParentId = if (index == 0) null else containers[index - 1].id
                            }
                        )
                    }
                }
                
                Text(
                    "اختر المكون لإضافته فورياً لشجرتك:",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                
                // Scrolling grid of component options
                val widgets = listOf(
                    Triple("heading", "عنوان H1", Icons.Default.Title),
                    Triple("paragraph", "فقرة نصية P", Icons.Default.Notes),
                    Triple("button", "زر تفاعلي Button", Icons.Default.SmartButton),
                    Triple("container", "صندوق حاوية Div", Icons.Default.CropFree),
                    Triple("link", "رابط تشعبي 🔗", Icons.Default.Link),
                    Triple("image", "لوحة صورة Image", Icons.Default.Image),
                    Triple("input", "حقل إدخال Input", Icons.Default.Input),
                    Triple("divider", "فاصل خطي Divider", Icons.Default.HorizontalRule),
                    Triple("card_hero", "ترويج ذكي Hero", Icons.Default.Lightbulb),
                    Triple("card_pricing", "أسعار باقات Pricing", Icons.Default.CallToAction)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(widgets) { widget ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepSlateBg),
                            modifier = Modifier
                                .clickable {
                                    onAdd(widget.first, widget.second, selectedParentId)
                                }
                                .border(1.dp, LightBorderColor, RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = widget.third,
                                    contentDescription = widget.second,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = widget.second,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("رجوع", color = Color.Gray)
            }
        }
    )
}

// ==================== DIALOG 4: CODE EXPORTER DIALOG ====================

@Composable
fun ExportDialog(
    htmlContent: String,
    project: ProjectEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EditorCardBg,
        title = {
            Text(
                "📂 حزمة أكواد المشروع الجاهزة للتصدير",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "كود صفحة الويب المترجم بالكامل ومدمج مع أدوات التنسيق والحركات التفاعلية:",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                
                // Rich view copyable block
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSlateBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        item {
                            Text(
                                text = htmlContent,
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("WebSketch Code", htmlContent)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ كود الويب البرمجي للحافظة 📋!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ كود HTML")
                }
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("رجوع للخلف", color = Color.White)
                }
            }
        }
    )
}

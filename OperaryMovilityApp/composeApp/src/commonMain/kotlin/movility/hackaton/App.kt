package movility.hackaton

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private enum class AppTab(val label: String) {
    RUTA("Ruta"),
    TECNICOS("Técnicos"),
    MENSAJES("Mensajes"),
}

@Composable
@Preview
fun App() {
    AppTheme {
        val uriHandler = LocalUriHandler.current
        val backendDataSource = remember { BackendDataSource() }
        var baseTasks by remember { mutableStateOf<List<RouteTask>>(emptyList()) }
        var technicians by remember { mutableStateOf<List<Technician>>(emptyList()) }
        var isBackendLoading by remember { mutableStateOf(true) }
        var backendLoadError by remember { mutableStateOf<String?>(null) }
        var doneTaskIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
        var taskTypeOverrides by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
        var selectedTypeFilterName by rememberSaveable { mutableStateOf<String?>(null) }
        var selectedSortOptionName by rememberSaveable { mutableStateOf(TaskSortOption.PRIORIDAD.name) }
        var selectedTask by remember { mutableStateOf<RouteTask?>(null) }
        var selectedTabName by rememberSaveable { mutableStateOf(AppTab.RUTA.name) }
        var activeTaskSession by remember { mutableStateOf<ActiveTaskSession?>(null) }
        var requestedConversationId by rememberSaveable { mutableStateOf<String?>(null) }

        LaunchedEffect(backendDataSource) {
            isBackendLoading = true
            runCatching {
                val loadedTechnicians = backendDataSource.getTechnicians()
                val routeTechnicianId = loadedTechnicians.firstOrNull()?.backendId
                val loadedRoute = if (routeTechnicianId != null) {
                    backendDataSource.getTechnicianRoute(routeTechnicianId)
                } else {
                    emptyList()
                }
                loadedTechnicians to loadedRoute
            }.onSuccess { (loadedTechnicians, loadedRoute) ->
                technicians = loadedTechnicians
                baseTasks = loadedRoute
                backendLoadError = null
            }.onFailure {
                technicians = sampleTechnicians()
                baseTasks = sampleDailyRoute()
                backendLoadError = "No se pudo conectar con el backend. Mostrando datos de respaldo."
            }
            isBackendLoading = false
        }

        val selectedTypeFilter = selectedTypeFilterName?.let { name -> TaskType.entries.firstOrNull { it.name == name } }
        val selectedSortOption = TaskSortOption.entries.firstOrNull { it.name == selectedSortOptionName }
            ?: TaskSortOption.PRIORIDAD
        val selectedTab = AppTab.entries.firstOrNull { it.name == selectedTabName } ?: AppTab.RUTA

        val tasks = baseTasks.map { task ->
            val overrideType = taskTypeOverrides[task.id]?.let { overrideName ->
                TaskType.entries.firstOrNull { it.name == overrideName }
            }
            task.copy(type = overrideType ?: task.type, isDone = doneTaskIds.contains(task.id))
        }

        Scaffold(
            modifier = Modifier.safeContentPadding(),
            bottomBar = {
                if (activeTaskSession == null) {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTabName = tab.name },
                                icon = {
                                    val icon = when (tab) {
                                        AppTab.RUTA -> Icons.Default.Home
                                        AppTab.TECNICOS -> Icons.Default.People
                                        AppTab.MENSAJES -> Icons.AutoMirrored.Filled.Message
                                    }
                                    Icon(imageVector = icon, contentDescription = tab.label)
                                },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            if (activeTaskSession != null) {
                val sessionTask = tasks.firstOrNull { it.id == activeTaskSession?.taskId }
                if (sessionTask != null) {
                    ActiveTaskScreen(
                        modifier = Modifier.padding(innerPadding),
                        task = sessionTask,
                        onFinishTask = {
                            doneTaskIds = updateDoneTaskIds(doneTaskIds, sessionTask.id, true)
                            activeTaskSession = null
                        },
                        onOpenGps = { uriHandler.openUri(googleMapsNavigationUrl(sessionTask.address)) },
                    )
                }
            } else {
                when (selectedTab) {
                    AppTab.RUTA -> RouteTabContent(
                        modifier = Modifier.padding(innerPadding),
                        tasks = tasks,
                        isLoading = isBackendLoading,
                        loadError = backendLoadError,
                        selectedTypeFilter = selectedTypeFilter,
                        selectedSortOption = selectedSortOption,
                        onTypeFilterChange = { selectedTypeFilterName = it?.name },
                        onSortOptionChange = { selectedSortOptionName = it.name },
                        onTaskChecked = { task, isChecked ->
                            doneTaskIds = updateDoneTaskIds(doneTaskIds, task.id, isChecked)
                        },
                        onTaskClick = { selectedTask = it },
                    )

                    AppTab.TECNICOS -> TechniciansTabContent(
                        modifier = Modifier.padding(innerPadding),
                        technicians = technicians,
                        isLoading = isBackendLoading,
                        loadError = backendLoadError,
                        onCallTechnician = { technician ->
                            technician.phone
                                ?.takeIf { it.isNotBlank() }
                                ?.let { uriHandler.openUri(phoneDialUri(it)) }
                        },
                        onSendMessage = { technician ->
                            requestedConversationId = conversationIdForTechnician(technician)
                            selectedTabName = AppTab.MENSAJES.name
                        },
                    )

                    AppTab.MENSAJES -> MessagesTabContent(
                        modifier = Modifier.padding(innerPadding),
                        technicians = technicians,
                        isLoading = isBackendLoading,
                        loadError = backendLoadError,
                        selectedConversationIdRequest = requestedConversationId,
                        onConversationRequestConsumed = { requestedConversationId = null },
                    )
                }
            }

            selectedTask?.let { task ->
                TaskDetailDialog(
                    task = task,
                    onDismiss = { selectedTask = null },
                    onStartTask = {
                        activeTaskSession = startTaskSession(task)
                        selectedTask = null
                    },
                    onTaskTypeChanged = { newType ->
                        taskTypeOverrides = taskTypeOverrides + (task.id to newType.name)
                        selectedTask = task.copy(type = newType)
                    },
                    onPhotoClick = { photoUrl -> uriHandler.openUri(photoUrl) },
                )
            }
        }
    }
}

@Composable
private fun RouteTabContent(
    modifier: Modifier,
    tasks: List<RouteTask>,
    isLoading: Boolean,
    loadError: String?,
    selectedTypeFilter: TaskType?,
    selectedSortOption: TaskSortOption,
    onTypeFilterChange: (TaskType?) -> Unit,
    onSortOptionChange: (TaskSortOption) -> Unit,
    onTaskChecked: (RouteTask, Boolean) -> Unit,
    onTaskClick: (RouteTask) -> Unit,
) {
    val filteredTasks = filterTasksByType(tasks, selectedTypeFilter)
    val visibleTasks = sortTasks(filteredTasks, selectedSortOption)
    val pendingCount = tasks.count { !it.isDone }
    val visiblePendingCount = visibleTasks.count { !it.isDone }
    val completedCount = tasks.size - pendingCount

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.widthIn(max = 18.dp))
                        Text(
                            text = "Cargando ruta desde backend...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (!loadError.isNullOrBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                        ),
                    ) {
                        Text(
                            text = loadError,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            item {
                HeaderSummary(
                    pendingCount = pendingCount,
                    completedCount = completedCount,
                    visiblePendingCount = visiblePendingCount,
                    visibleTotalCount = visibleTasks.size,
                    totalCount = tasks.size,
                )
            }

            item {
                Text(
                    text = "Tipo de tarea",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                TaskTypeFilterDropdown(
                    selectedType = selectedTypeFilter,
                    onTypeSelected = onTypeFilterChange,
                )
            }

            item {
                Text(
                    text = "Orden",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                TaskSortRow(
                    selectedSortOption = selectedSortOption,
                    onSortSelected = onSortOptionChange,
                )
            }

            items(items = visibleTasks, key = { it.id }) { task ->
                RouteTaskRow(
                    task = task,
                    onTaskChecked = { checked -> onTaskChecked(task, checked) },
                    onTaskClick = { onTaskClick(task) },
                )
            }
        }
    }
}

@Composable
private fun TechniciansTabContent(
    modifier: Modifier,
    technicians: List<Technician>,
    isLoading: Boolean,
    loadError: String?,
    onCallTechnician: (Technician) -> Unit,
    onSendMessage: (Technician) -> Unit,
) {
    var selectedTechnicianByMap by remember { mutableStateOf<Technician?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isLoading) {
                item {
                    Text(
                        text = "Actualizando técnicos...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!loadError.isNullOrBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                        ),
                    ) {
                        Text(
                            text = loadError,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Ubicación de técnicos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                Text(
                    text = "Consulta dónde está cada técnico y contáctalo rápido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                TechniciansRealMap(
                    modifier = Modifier.fillMaxWidth(),
                    technicians = technicians,
                    onTechnicianClick = { selectedTechnicianByMap = it },
                )
            }

            items(technicians, key = { it.id }) { technician ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = technician.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = technician.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onCallTechnician(technician) },
                                enabled = !technician.phone.isNullOrBlank(),
                            ) {
                                Text("Llamar")
                            }
                            OutlinedButton(onClick = { onSendMessage(technician) }) {
                                Text("Enviar mensaje")
                            }
                        }
                    }
                }
            }
        }

        selectedTechnicianByMap?.let { technician ->
            AlertDialog(
                onDismissRequest = { selectedTechnicianByMap = null },
                title = { Text(technician.name) },
                text = {
                    Text(
                        text = technician.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        onCallTechnician(technician)
                        selectedTechnicianByMap = null
                    }, enabled = !technician.phone.isNullOrBlank()) {
                        Text("Llamar")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        onSendMessage(technician)
                        selectedTechnicianByMap = null
                    }) {
                        Text("Enviar mensaje")
                    }
                },
            )
        }
    }
}

@Composable
private fun ActiveTaskScreen(
    modifier: Modifier,
    task: RouteTask,
    onFinishTask: () -> Unit,
    onOpenGps: () -> Unit,
) {
    var elapsedSeconds by remember { mutableStateOf(0) }
    var isGeneratingReport by remember { mutableStateOf(false) }
    var isReportGenerated by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportData by remember { mutableStateOf(createEmptyReport(task.id, task.type)) }

    LaunchedEffect(task.id) {
        while (true) {
            delay(1_000)
            elapsedSeconds += 1
        }
    }

    LaunchedEffect(isGeneratingReport) {
        if (isGeneratingReport) {
            delay(2_000)
            isGeneratingReport = false
            isReportGenerated = true
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Tarea en curso",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = task.siteName,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = task.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = formatElapsedTime(elapsedSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Cronómetro activo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ),
            ) {
                Text(
                    text = if (isReportGenerated) {
                        "Informe generado correctamente. Ya puedes finalizar la tarea."
                    } else {
                        "Debes generar informe antes de finalizar la tarea."
                    },
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedButton(
                onClick = onOpenGps,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Abrir GPS")
            }

            OutlinedButton(
                onClick = { showReportDialog = true },
                enabled = !isGeneratingReport && !isReportGenerated,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (isGeneratingReport) "Generando informe..."
                    else if (isReportGenerated) "Informe generado"
                    else "Generar informe",
                )
            }

            Button(
                onClick = onFinishTask,
                enabled = isReportGenerated,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Finalizar tarea")
            }
        }
    }

    if (showReportDialog) {
        ReportFormDialog(
            task = task,
            reportData = reportData,
            onReportDataChanged = { reportData = it },
            onConfirm = {
                showReportDialog = false
                isGeneratingReport = true
            },
            onDismiss = { showReportDialog = false },
        )
    }
}

@Composable
private fun ReportFormDialog(
    task: RouteTask,
    reportData: TaskReport,
    onReportDataChanged: (TaskReport) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generar informe - ${task.type.label}") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    when (task.type) {
                        TaskType.CORRECTIVO_CRITICO -> {
                            OutlinedTextField(
                                value = reportData.correctivoProblem,
                                onValueChange = { newValue ->
                                    onReportDataChanged(reportData.copy(correctivoProblem = newValue))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Descripción del problema") },
                                minLines = 3,
                            )
                        }

                        TaskType.CORRECTIVO_NO_CRITICO -> {
                            OutlinedTextField(
                                value = reportData.correctivoProblem,
                                onValueChange = { newValue ->
                                    onReportDataChanged(reportData.copy(correctivoProblem = newValue))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Descripción del problema") },
                                minLines = 2,
                            )
                        }

                        TaskType.MANTENIMIENTO_PREVENTIVO_PROGRAMADO -> {
                            OutlinedTextField(
                                value = reportData.mantenimientoActivities,
                                onValueChange = { newValue ->
                                    onReportDataChanged(reportData.copy(mantenimientoActivities = newValue))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Actividades realizadas") },
                                minLines = 3,
                            )
                            OutlinedTextField(
                                value = reportData.proximoMantenimiento,
                                onValueChange = { newValue ->
                                    onReportDataChanged(reportData.copy(proximoMantenimiento = newValue))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Próximo mantenimiento programado") },
                            )
                        }

                        TaskType.PUESTA_EN_MARCHA -> {
                            OutlinedTextField(
                                value = reportData.puestaEnMarchaPruebas,
                                onValueChange = { newValue ->
                                    onReportDataChanged(reportData.copy(puestaEnMarchaPruebas = newValue))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Pruebas realizadas") },
                                minLines = 3,
                            )
                            OutlinedTextField(
                                value = reportData.puestaEnMarchaParametros,
                                onValueChange = { newValue ->
                                    onReportDataChanged(reportData.copy(puestaEnMarchaParametros = newValue))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Parámetros configurados") },
                                minLines = 2,
                            )
                        }

                        TaskType.VISITA_DE_DIAGNOSTICO -> {
                            OutlinedTextField(
                                value = reportData.diagnosticoHallazgos,
                                onValueChange = { newValue ->
                                    onReportDataChanged(reportData.copy(diagnosticoHallazgos = newValue))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Hallazgos y diagnóstico") },
                                minLines = 3,
                            )
                            OutlinedTextField(
                                value = reportData.diagnosticoRecomendaciones,
                                onValueChange = { newValue ->
                                    onReportDataChanged(reportData.copy(diagnosticoRecomendaciones = newValue))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Recomendaciones") },
                                minLines = 2,
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = reportData.observations,
                        onValueChange = { newValue ->
                            onReportDataChanged(reportData.copy(observations = newValue))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Observaciones adicionales") },
                        minLines = 2,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Guardar informe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}



@Composable
private fun HeaderSummary(
    pendingCount: Int,
    completedCount: Int,
    visiblePendingCount: Int,
    visibleTotalCount: Int,
    totalCount: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Ruta diaria del técnico",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Pendientes: $pendingCount/$totalCount  |  Completadas: $completedCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Vista actual: $visiblePendingCount pendientes de $visibleTotalCount tareas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TaskSortRow(selectedSortOption: TaskSortOption, onSortSelected: (TaskSortOption) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TaskSortOption.entries) { sortOption ->
            FilterChip(
                selected = selectedSortOption == sortOption,
                onClick = { onSortSelected(sortOption) },
                label = { Text(sortOption.label) },
            )
        }
    }
}

@Composable
private fun TaskTypeFilterDropdown(selectedType: TaskType?, onTypeSelected: (TaskType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = selectedType?.label ?: "Todos")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Todos") },
                onClick = {
                    onTypeSelected(null)
                    expanded = false
                },
            )
            TaskType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun RouteTaskRow(task: RouteTask, onTaskChecked: (Boolean) -> Unit, onTaskClick: () -> Unit) {
    val animatedContainerColor by animateColorAsState(
        targetValue = if (task.isDone) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            taskCardColor(task.type)
        },
        label = "task-container-color",
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        label = "task-title-color",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (task.isDone) 0.72f else 1f,
        label = "task-done-alpha",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTaskClick),
        colors = CardDefaults.cardColors(containerColor = animatedContainerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = animatedAlpha }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = onTaskChecked,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = task.siteName,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = animatedTextColor,
                )
                Text(
                    text = "${task.scheduledTime} - ${task.address}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = task.type.label,
                    modifier = Modifier
                        .widthIn(max = 120.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TaskDetailDialog(
    task: RouteTask,
    onDismiss: () -> Unit,
    onStartTask: () -> Unit,
    onTaskTypeChanged: (TaskType) -> Unit,
    onPhotoClick: (String) -> Unit,
) {
    var isTaskTypeMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = task.siteName,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ubicación: ${task.address}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = task.scheduledTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box {
                        OutlinedButton(onClick = { isTaskTypeMenuExpanded = true }) {
                            Text(task.type.label)
                        }
                        DropdownMenu(
                            expanded = isTaskTypeMenuExpanded,
                            onDismissRequest = { isTaskTypeMenuExpanded = false },
                        ) {
                            TaskType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    onClick = {
                                        onTaskTypeChanged(type)
                                        isTaskTypeMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                if (task.photoUrls.isEmpty()) {
                    Text(
                        text = "Fotos: no hay evidencias adjuntas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Fotos (${task.photoUrls.size})",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(task.photoUrls) { photoUrl ->
                            Card(
                                modifier = Modifier
                                    .clickable { onPhotoClick(photoUrl) }
                                    .padding(vertical = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                ),
                            ) {
                                Text(
                                    text = photoUrl,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onStartTask) {
                Text("Empezar tarea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
    )
}

@Composable
private fun taskCardColor(type: TaskType): Color {
    return when (type) {
        TaskType.CORRECTIVO_CRITICO -> MaterialTheme.colorScheme.errorContainer
        TaskType.CORRECTIVO_NO_CRITICO -> MaterialTheme.colorScheme.tertiaryContainer
        TaskType.MANTENIMIENTO_PREVENTIVO_PROGRAMADO -> MaterialTheme.colorScheme.secondaryContainer
        TaskType.PUESTA_EN_MARCHA -> MaterialTheme.colorScheme.primaryContainer
        TaskType.VISITA_DE_DIAGNOSTICO -> MaterialTheme.colorScheme.surfaceVariant
    }
}
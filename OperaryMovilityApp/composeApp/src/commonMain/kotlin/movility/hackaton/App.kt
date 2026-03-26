package movility.hackaton

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private enum class AppTab(val label: String) {
    RUTA("Ruta"),
    TECNICOS("Tecnicos"),
    MENSAJES("Mensajes"),
}

@Composable
@Preview
fun App() {
    AppTheme {
        val uriHandler = LocalUriHandler.current
        val baseTasks = remember { sampleDailyRoute() }
        val technicians = remember { sampleTechnicians() }
        var doneTaskIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
        var taskTypeOverrides by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
        var selectedTypeFilterName by rememberSaveable { mutableStateOf<String?>(null) }
        var selectedSortOptionName by rememberSaveable { mutableStateOf(TaskSortOption.PRIORIDAD.name) }
        var selectedTask by remember { mutableStateOf<RouteTask?>(null) }
        var selectedTabName by rememberSaveable { mutableStateOf(AppTab.RUTA.name) }

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
            },
        ) { innerPadding ->
            when (selectedTab) {
                AppTab.RUTA -> RouteTabContent(
                    modifier = Modifier.padding(innerPadding),
                    tasks = tasks,
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
                    onCallTechnician = { technician -> uriHandler.openUri(phoneDialUri(technician.phone)) },
                    onOpenTechnicianLocation = { technician ->
                        uriHandler.openUri(googleMapsPinUrl(technician.latitude, technician.longitude, technician.name))
                    },
                )

                AppTab.MENSAJES -> MessagesTabContent(modifier = Modifier.padding(innerPadding))
            }

            selectedTask?.let { task ->
                TaskDetailDialog(
                    task = task,
                    onDismiss = { selectedTask = null },
                    onStartTask = {
                        uriHandler.openUri(googleMapsNavigationUrl(task.address))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderSummary(
                pendingCount = pendingCount,
                completedCount = completedCount,
                visiblePendingCount = visiblePendingCount,
                visibleTotalCount = visibleTasks.size,
                totalCount = tasks.size,
            )

            Text(
                text = "Tipo de tarea",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TaskTypeFilterDropdown(
                selectedType = selectedTypeFilter,
                onTypeSelected = onTypeFilterChange,
            )

            Text(
                text = "Orden",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TaskSortRow(
                selectedSortOption = selectedSortOption,
                onSortSelected = onSortOptionChange,
            )

            AnimatedContent(targetState = visibleTasks, label = "tasks-filter-animation") { tasksForView ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items = tasksForView, key = { it.id }) { task ->
                        RouteTaskRow(
                            task = task,
                            onTaskChecked = { checked -> onTaskChecked(task, checked) },
                            onTaskClick = { onTaskClick(task) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TechniciansTabContent(
    modifier: Modifier,
    technicians: List<Technician>,
    onCallTechnician: (Technician) -> Unit,
    onOpenTechnicianLocation: (Technician) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Ubicacion de tecnicos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Consulta donde esta cada tecnico y contactalo rapido.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                                Button(onClick = { onCallTechnician(technician) }) {
                                    Text("Llamar")
                                }
                                OutlinedButton(onClick = { onOpenTechnicianLocation(technician) }) {
                                    Text("Ver en mapa")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagesTabContent(modifier: Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Mensajes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Aqui puedes integrar el chat operativo del equipo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
            )
        }
    }
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
                text = "Ruta diaria del tecnico",
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
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
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
                    text = "Ubicacion: ${task.address}",
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
package movility.hackaton

enum class TaskType(val label: String, val priorityOrder: Int) {
    CORRECTIVO_CRITICO("Correctivo critico", 0),
    CORRECTIVO_NO_CRITICO("Correctivo no critico", 1),
    MANTENIMIENTO_PREVENTIVO_PROGRAMADO("Mantenimiento preventivo programado", 2),
    PUESTA_EN_MARCHA("Puesta en marcha", 3),
    VISITA_DE_DIAGNOSTICO("Visita de diagnostico", 4),
}

enum class TaskSortOption(val label: String) {
    PRIORIDAD("Prioridad"),
    HORA("Hora"),
}

data class RouteTask(
    val id: String,
    val siteName: String,
    val address: String,
    val scheduledTime: String,
    val type: TaskType,
    val isDone: Boolean = false,
)

fun filterTasksByType(tasks: List<RouteTask>, selectedType: TaskType?): List<RouteTask> {
    return if (selectedType == null) tasks else tasks.filter { it.type == selectedType }
}

fun sortTasks(tasks: List<RouteTask>, sortOption: TaskSortOption): List<RouteTask> {
    return when (sortOption) {
        TaskSortOption.PRIORIDAD -> tasks.sortedWith(
            compareBy<RouteTask> { it.type.priorityOrder }
                .thenBy { it.scheduledTime }
                .thenBy { it.id },
        )

        TaskSortOption.HORA -> tasks.sortedWith(
            compareBy<RouteTask> { it.scheduledTime }
                .thenBy { it.type.priorityOrder }
                .thenBy { it.id },
        )
    }
}

fun updateDoneTaskIds(doneTaskIds: List<String>, taskId: String, isDone: Boolean): List<String> {
    return if (isDone) {
        (doneTaskIds + taskId).distinct()
    } else {
        doneTaskIds.filterNot { it == taskId }
    }
}

fun sampleDailyRoute(): List<RouteTask> {
    val tasks = listOf(
        RouteTask(
            id = "T-001",
            siteName = "Estacion Centro",
            address = "Av. Principal 123",
            scheduledTime = "08:00",
            type = TaskType.CORRECTIVO_CRITICO,
        ),
        RouteTask(
            id = "T-002",
            siteName = "Parque Norte",
            address = "Calle 45 #12-20",
            scheduledTime = "09:15",
            type = TaskType.CORRECTIVO_NO_CRITICO,
        ),
        RouteTask(
            id = "T-003",
            siteName = "Torre Empresarial",
            address = "Cra 10 #80-15",
            scheduledTime = "11:00",
            type = TaskType.MANTENIMIENTO_PREVENTIVO_PROGRAMADO,
        ),
        RouteTask(
            id = "T-004",
            siteName = "Conjunto Alameda",
            address = "Calle 120 #7-50",
            scheduledTime = "13:30",
            type = TaskType.PUESTA_EN_MARCHA,
        ),
        RouteTask(
            id = "T-005",
            siteName = "Centro Comercial Sur",
            address = "Av. Sur 88",
            scheduledTime = "15:00",
            type = TaskType.VISITA_DE_DIAGNOSTICO,
        ),
    )

    return sortTasks(tasks, TaskSortOption.PRIORIDAD)
}


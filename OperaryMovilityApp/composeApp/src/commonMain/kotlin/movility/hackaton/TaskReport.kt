package movility.hackaton

data class TaskReport(
    val taskId: String,
    val taskType: TaskType,
    val observations: String = "",
    val correctivoProblem: String = "", // CORRECTIVO_CRITICO / CORRECTIVO_NO_CRITICO
    val correctivoSolution: String = "",
    val mantenimientoActivities: String = "", // MANTENIMIENTO_PREVENTIVO_PROGRAMADO
    val proximoMantenimiento: String = "",
    val puestaEnMarchaPruebas: String = "", // PUESTA_EN_MARCHA
    val puestaEnMarchaParametros: String = "",
    val diagnosticoHallazgos: String = "", // VISITA_DE_DIAGNOSTICO
    val diagnosticoRecomendaciones: String = "",
    val photoUrls: List<String> = emptyList(),
)

fun createEmptyReport(taskId: String, taskType: TaskType): TaskReport {
    return TaskReport(taskId = taskId, taskType = taskType)
}


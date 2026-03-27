package movility.hackaton

data class ActiveTaskSession(
    val taskId: String,
)

fun startTaskSession(task: RouteTask): ActiveTaskSession {
    return ActiveTaskSession(taskId = task.id)
}

fun formatElapsedTime(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    val hh = hours.toString().padStart(2, '0')
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return "$hh:$mm:$ss"
}


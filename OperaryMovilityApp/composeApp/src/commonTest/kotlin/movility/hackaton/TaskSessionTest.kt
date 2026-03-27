package movility.hackaton

import kotlin.test.Test
import kotlin.test.assertEquals

class TaskSessionTest {

    @Test
    fun formatElapsedTime_formatsAsHhMmSs() {
        assertEquals("00:00:00", formatElapsedTime(0))
        assertEquals("00:01:05", formatElapsedTime(65))
        assertEquals("01:01:01", formatElapsedTime(3661))
    }

    @Test
    fun startTaskSession_usesTaskId() {
        val task = RouteTask(
            id = "T-200",
            siteName = "Sitio Demo",
            address = "Calle 10",
            scheduledTime = "10:00",
            type = TaskType.PUESTA_EN_MARCHA,
        )

        val session = startTaskSession(task)

        assertEquals("T-200", session.taskId)
    }
}


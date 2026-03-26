package movility.hackaton

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteTaskTest {

    @Test
    fun sampleRoute_containsAllTaskTypes() {
        val route = sampleDailyRoute()

        val expectedTypes = setOf(
            TaskType.CORRECTIVO_CRITICO,
            TaskType.CORRECTIVO_NO_CRITICO,
            TaskType.MANTENIMIENTO_PREVENTIVO_PROGRAMADO,
            TaskType.PUESTA_EN_MARCHA,
            TaskType.VISITA_DE_DIAGNOSTICO,
        )

        assertEquals(expectedTypes, route.map { it.type }.toSet())
    }

    @Test
    fun sampleRoute_isSortedByPriorityOrder() {
        val route = sampleDailyRoute()

        assertTrue(route.zipWithNext().all { (left, right) -> left.type.priorityOrder <= right.type.priorityOrder })
    }

    @Test
    fun filterTasksByType_whenTypeIsNull_returnsAllTasks() {
        val route = sampleDailyRoute()

        val filtered = filterTasksByType(route, null)

        assertEquals(route, filtered)
    }

    @Test
    fun filterTasksByType_whenTypeIsProvided_returnsOnlyThatType() {
        val route = sampleDailyRoute()

        val filtered = filterTasksByType(route, TaskType.CORRECTIVO_CRITICO)

        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.all { it.type == TaskType.CORRECTIVO_CRITICO })
    }

    @Test
    fun sortTasks_byPriority_ordersByCriticalityFirst() {
        val tasks = listOf(
            RouteTask("A", "Sitio A", "Dir A", "12:00", TaskType.VISITA_DE_DIAGNOSTICO),
            RouteTask("B", "Sitio B", "Dir B", "09:00", TaskType.CORRECTIVO_CRITICO),
            RouteTask("C", "Sitio C", "Dir C", "08:00", TaskType.CORRECTIVO_NO_CRITICO),
        )

        val sorted = sortTasks(tasks, TaskSortOption.PRIORIDAD)

        assertEquals(listOf("B", "C", "A"), sorted.map { it.id })
    }

    @Test
    fun sortTasks_byTime_ordersByScheduledTimeFirst() {
        val tasks = listOf(
            RouteTask("A", "Sitio A", "Dir A", "12:00", TaskType.CORRECTIVO_CRITICO),
            RouteTask("B", "Sitio B", "Dir B", "08:30", TaskType.VISITA_DE_DIAGNOSTICO),
            RouteTask("C", "Sitio C", "Dir C", "09:15", TaskType.CORRECTIVO_NO_CRITICO),
        )

        val sorted = sortTasks(tasks, TaskSortOption.HORA)

        assertEquals(listOf("B", "C", "A"), sorted.map { it.id })
    }

    @Test
    fun filterThenSort_byTime_appliesBothRules() {
        val tasks = listOf(
            RouteTask("A", "Sitio A", "Dir A", "10:00", TaskType.CORRECTIVO_CRITICO),
            RouteTask("B", "Sitio B", "Dir B", "08:00", TaskType.CORRECTIVO_CRITICO),
            RouteTask("C", "Sitio C", "Dir C", "09:00", TaskType.PUESTA_EN_MARCHA),
        )

        val filtered = filterTasksByType(tasks, TaskType.CORRECTIVO_CRITICO)
        val sorted = sortTasks(filtered, TaskSortOption.HORA)

        assertEquals(listOf("B", "A"), sorted.map { it.id })
    }

    @Test
    fun updateDoneTaskIds_whenChecksTask_addsIdWithoutDuplicates() {
        val result = updateDoneTaskIds(listOf("T-001"), "T-001", isDone = true)

        assertEquals(listOf("T-001"), result)
    }

    @Test
    fun updateDoneTaskIds_whenUnchecksTask_removesId() {
        val result = updateDoneTaskIds(listOf("T-001", "T-002"), "T-001", isDone = false)

        assertEquals(listOf("T-002"), result)
    }

    @Test
    fun routeTask_photoUrls_isEmptyByDefault() {
        val task = RouteTask("X", "Sitio X", "Dir X", "10:00", TaskType.PUESTA_EN_MARCHA)

        assertTrue(task.photoUrls.isEmpty())
    }

    @Test
    fun googleMapsNavigationUrl_buildsNavigationLink() {
        val url = googleMapsNavigationUrl("Calle 45 #12-20")

        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=Calle%2045%20%2312-20&travelmode=driving",
            url,
        )
    }
}


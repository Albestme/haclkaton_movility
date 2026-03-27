package movility.hackaton

actual class BackendDataSource actual constructor(
    @Suppress("UNUSED_PARAMETER") private val baseUrl: String,
) {
    actual suspend fun getTechnicians(): List<Technician> = sampleTechnicians()

    actual suspend fun getTechnicianRoute(technicianId: Int): List<RouteTask> = sampleDailyRoute()
}

actual fun defaultBackendBaseUrl(): String = "http://10.0.2.2:8000"


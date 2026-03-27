package movility.hackaton

expect class BackendDataSource(baseUrl: String = defaultBackendBaseUrl()) {
    suspend fun getTechnicians(): List<Technician>
    suspend fun getTechnicianRoute(technicianId: Int): List<RouteTask>
}

expect fun defaultBackendBaseUrl(): String


package movility.hackaton

import java.net.HttpURLConnection
import java.net.URI

actual class BackendDataSource actual constructor(
    private val baseUrl: String,
) {
    actual suspend fun getTechnicians(): List<Technician> {
        val body = httpGet(buildUrl("/api/v1/app/technicians"))
        return parseJsonArrayObjects(body).mapNotNull { it.toTechnicianOrNull() }
    }

    actual suspend fun getTechnicianRoute(technicianId: Int): List<RouteTask> {
        return parseJsonArrayObjects(httpGet(buildUrl("/api/v1/app/technicians/$technicianId/route")))
            .mapNotNull { it.toRouteTaskOrNull() }
    }

    private fun buildUrl(path: String): String {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        return "$normalizedBaseUrl/$path".replace("//api", "/api")
    }
}

actual fun defaultBackendBaseUrl(): String {
    val override = System.getProperty("operary.backend.baseUrl")?.trim().orEmpty()
    return if (override.isNotEmpty()) override else "http://localhost:8000"
}

private fun httpGet(url: String): String {
    val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8_000
        readTimeout = 12_000
        setRequestProperty("Accept", "application/json")
    }

    return try {
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
        if (statusCode !in 200..299) {
            error("Backend error $statusCode for $url: $body")
        }
        body
    } finally {
        connection.disconnect()
    }
}

private fun String.toTechnicianOrNull(): Technician? {
    val backendId = jsonInt("technician_id") ?: return null
    val name = jsonString("name") ?: return null
    val zone = jsonString("zone").orEmpty().ifBlank { "Zona sin asignar" }
    val lat = jsonDouble("latitude") ?: return null
    val lon = jsonDouble("longitude") ?: return null

    return Technician(
        id = "tec-$backendId",
        backendId = backendId,
        name = name,
        phone = null,
        address = zone,
        latitude = lat,
        longitude = lon,
    )
}

private fun String.toRouteTaskOrNull(): RouteTask? {
    val visitId = jsonInt("visit_id") ?: return null
    val incidenceId = jsonInt("incidence_id")
    val contractId = jsonInt("contract_id")
    val priority = jsonString("priority")
    val plannedDate = jsonString("planned_date")
    val address = jsonString("address")?.takeIf { it.isNotBlank() }
        ?: fallbackAddress(jsonDouble("latitude"), jsonDouble("longitude"))

    val siteName = when {
        incidenceId != null -> "Incidencia #$incidenceId"
        contractId != null -> "Contrato #$contractId"
        else -> "Visita #$visitId"
    }

    return RouteTask(
        id = "visit-$visitId",
        siteName = siteName,
        address = address,
        scheduledTime = scheduledTimeFromIso(plannedDate),
        type = taskTypeFromBackendPriority(priority),
        photoUrls = emptyList(),
    )
}

private fun fallbackAddress(latitude: Double?, longitude: Double?): String {
    return if (latitude != null && longitude != null) {
        "Lat $latitude, Lon $longitude"
    } else {
        "Ubicación sin dirección"
    }
}

private fun parseJsonArrayObjects(rawJson: String): List<String> {
    val objects = mutableListOf<String>()
    var depth = 0
    var inQuotes = false
    var escaped = false
    var objectStart = -1

    rawJson.forEachIndexed { index, ch ->
        if (inQuotes) {
            if (escaped) {
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == '"') {
                inQuotes = false
            }
            return@forEachIndexed
        }

        when (ch) {
            '"' -> inQuotes = true
            '{' -> {
                if (depth == 0) objectStart = index
                depth += 1
            }

            '}' -> {
                depth -= 1
                if (depth == 0 && objectStart >= 0) {
                    objects += rawJson.substring(objectStart, index + 1)
                    objectStart = -1
                }
            }
        }
    }

    return objects
}

private fun String.jsonString(key: String): String? {
    val pattern = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\\\"])*)\"")
    val value = pattern.find(this)?.groupValues?.getOrNull(1) ?: return null
    return value
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\/", "/")
        .replace("\\\\", "\\")
}

private fun String.jsonInt(key: String): Int? {
    val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
    return pattern.find(this)?.groupValues?.getOrNull(1)?.toIntOrNull()
}

private fun String.jsonDouble(key: String): Double? {
    val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
    return pattern.find(this)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
}


package movility.hackaton

data class Technician(
    val id: String,
    val name: String,
    val phone: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

fun phoneDialUri(phone: String): String {
    val normalized = phone.filter { it.isDigit() || it == '+' }
    return "tel:$normalized"
}

fun googleMapsPinUrl(latitude: Double, longitude: Double, label: String): String {
    val encodedLabel = encodeForUriComponent(label)
    return "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude%20($encodedLabel)"
}

private fun encodeForUriComponent(value: String): String {
    val trimmed = value.trim()
    return buildString {
        for (char in trimmed) {
            if (char.isLetterOrDigit() || char == '-' || char == '_' || char == '.' || char == '~') {
                append(char)
            } else {
                append('%')
                append(char.code.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }
}

fun sampleTechnicians(): List<Technician> {
    return listOf(
        Technician(
            id = "TEC-01",
            name = "Carlos Rojas",
            phone = "+573001112233",
            address = "Zona Norte - Calle 100 #15-20",
            latitude = 4.684,
            longitude = -74.049,
        ),
        Technician(
            id = "TEC-02",
            name = "Luisa Díaz",
            phone = "+573004445566",
            address = "Zona Centro - Av. 7 #45-10",
            latitude = 4.638,
            longitude = -74.069,
        ),
        Technician(
            id = "TEC-03",
            name = "Andrés Mejía",
            phone = "+573007778899",
            address = "Zona Sur - Calle 30 Sur #52-11",
            latitude = 4.578,
            longitude = -74.111,
        ),
    )
}


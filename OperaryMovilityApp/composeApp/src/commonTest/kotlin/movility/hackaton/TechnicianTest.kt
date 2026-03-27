package movility.hackaton

import kotlin.test.Test
import kotlin.test.assertEquals

class TechnicianTest {

    @Test
    fun phoneDialUri_removesFormattingChars() {
        val uri = phoneDialUri("+57 300-111-2233")

        assertEquals("tel:+573001112233", uri)
    }

    @Test
    fun googleMapsPinUrl_buildsQueryWithCoordinates() {
        val uri = googleMapsPinUrl(4.684, -74.049, "Carlos Rojas")

        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=4.684,-74.049%20(Carlos%20Rojas)",
            uri,
        )
    }
}


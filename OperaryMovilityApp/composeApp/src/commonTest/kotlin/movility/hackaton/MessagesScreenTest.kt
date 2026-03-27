package movility.hackaton

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessagesScreenTest {

    @Test
    fun sampleConversations_returnsSeededData() {
        val conversations = sampleConversations()

        assertEquals(3, conversations.size)
        assertTrue(conversations.all { it.messages.isNotEmpty() })
    }

    @Test
    fun sampleConversations_firstConversationIsOperaciones() {
        val firstConversation = sampleConversations().first()

        assertEquals("conv-operaciones", firstConversation.id)
        assertEquals("Centro de Operaciones", firstConversation.title)
    }
}


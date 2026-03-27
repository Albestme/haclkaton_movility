package movility.hackaton

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ChatMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val time: String,
    val isFromMe: Boolean,
)

data class ChatConversation(
    val id: String,
    val title: String,
    val subtitle: String,
    val lastMessage: String,
    val messages: List<ChatMessage>,
)

fun sampleConversations(): List<ChatConversation> {
    return listOf(
        ChatConversation(
            id = "conv-tec-01",
            title = "Carlos Rojas",
            subtitle = "Tecnico en campo",
            lastMessage = "Ya revise el cargador 2. Te comparto fotos en 5 min.",
            messages = listOf(
                ChatMessage("t1", "Carlos", "Estoy en Estacion Centro revisando la alarma critica.", "08:40", false),
                ChatMessage("t2", "Yo", "Perfecto, cuando termines me mandas estado y repuestos usados.", "08:42", true),
                ChatMessage("t3", "Carlos", "Ya revise el cargador 2. Te comparto fotos en 5 min.", "08:45", false),
            ),
        ),
        ChatConversation(
            id = "conv-tec-02",
            title = "Luisa Diaz",
            subtitle = "Tecnica en campo",
            lastMessage = "Voy para la estacion norte. Llego en 15 minutos.",
            messages = listOf(
                ChatMessage("t4", "Luisa", "Tengo modulo de repuesto disponible.", "09:10", false),
                ChatMessage("t5", "Yo", "Genial, prioriza la visita de diagnostico en Parque Norte.", "09:12", true),
                ChatMessage("t6", "Luisa", "Voy para la estacion norte. Llego en 15 minutos.", "09:14", false),
            ),
        ),
        ChatConversation(
            id = "conv-tec-03",
            title = "Andres Mejia",
            subtitle = "Tecnico en campo",
            lastMessage = "Termine preventivo. Quedo pendiente ajuste de firmware.",
            messages = listOf(
                ChatMessage("t7", "Andres", "En sitio todo estable despues de pruebas.", "10:02", false),
                ChatMessage("t8", "Yo", "Perfecto, deja evidencia en el informe.", "10:03", true),
                ChatMessage("t9", "Andres", "Termine preventivo. Quedo pendiente ajuste de firmware.", "10:06", false),
            ),
        ),
        ChatConversation(
            id = "conv-operaciones",
            title = "Centro de Operaciones",
            subtitle = "Coordinacion de rutas",
            lastMessage = "Perfecto. Tecnico Carlos esta cerca por soporte.",
            messages = listOf(
                ChatMessage("m1", "Despacho", "Alberto, prioriza el correctivo critico de Estacion Centro.", "07:30", false),
                ChatMessage("m2", "Yo", "Recibido. Voy para Estacion Centro y actualizo en 20 minutos.", "07:32", true),
                ChatMessage("m3", "Despacho", "Perfecto. Tecnico Carlos esta cerca por soporte.", "07:34", false),
            ),
        ),
        ChatConversation(
            id = "conv-tecnicos",
            title = "Tecnicos - Zona Norte",
            subtitle = "Apoyo en campo",
            lastMessage = "Tengo repuesto de modulo. Si quieren, paso por alla.",
            messages = listOf(
                ChatMessage("m4", "Carlos", "Ya llegue al punto, hay falla de comunicacion en el cargador 2.", "08:12", false),
                ChatMessage("m5", "Yo", "Voy saliendo de Torre Empresarial. Te llamo en 10 min.", "08:14", true),
                ChatMessage("m6", "Luisa", "Tengo repuesto de modulo. Si quieren, paso por alla.", "08:15", false),
            ),
        ),
        ChatConversation(
            id = "conv-clientes",
            title = "Atencion Cliente",
            subtitle = "Incidencias y evidencias",
            lastMessage = "Abro visita de diagnostico y envio fotos al cerrar.",
            messages = listOf(
                ChatMessage("m7", "Soporte", "Cliente reporta carga intermitente en Parque Norte.", "09:01", false),
                ChatMessage("m8", "Yo", "Abro visita de diagnostico y envio fotos al cerrar.", "09:03", true),
            ),
        ),
    )
}

fun conversationIdForTechnician(technician: Technician): String {
    return "conv-${technician.id.lowercase()}"
}

@Composable
fun MessagesTabContent(
    modifier: Modifier,
    selectedConversationIdRequest: String? = null,
    onConversationRequestConsumed: () -> Unit = {},
) {
    val conversations = remember { sampleConversations() }
    var selectedConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedConversationIdRequest) {
        if (selectedConversationIdRequest != null) {
            selectedConversationId = selectedConversationIdRequest
            onConversationRequestConsumed()
        }
    }
    val selectedConversation = conversations.firstOrNull { it.id == selectedConversationId }

    if (selectedConversation == null) {
        ConversationListView(
            conversations = conversations,
            onConversationSelected = { selectedConversationId = it.id },
            modifier = modifier,
        )
    } else {
        ChatDetailView(
            conversation = selectedConversation,
            onBackClick = { selectedConversationId = null },
            modifier = modifier,
        )
    }
}

@Composable
private fun ConversationListView(
    conversations: List<ChatConversation>,
    onConversationSelected: (ChatConversation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = "Mensajes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            state = rememberLazyListState(),
        ) {
            items(conversations, key = { it.id }) { conversation ->
                    ConversationListItem(
                        conversation = conversation,
                        onSelected = { onConversationSelected(conversation) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationListItem(
    conversation: ChatConversation,
    onSelected: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.15f)
                .aspectRatio(1f),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = conversation.title.first().toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = conversation.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = conversation.lastMessage.take(50),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ChatDetailView(
    conversation: ChatConversation,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var messageText by rememberSaveable { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatHeaderBarWithBack(
                conversation = conversation,
                onBackClick = onBackClick,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = rememberLazyListState(),
            ) {
                items(conversation.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }

            ChatInputBar(
                messageText = messageText,
                onMessageTextChanged = { messageText = it },
            )
        }
    }
}

@Composable
private fun ChatHeaderBarWithBack(
    conversation: ChatConversation,
    onBackClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Atrás",
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = conversation.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


@Composable
private fun ChatInputBar(
    messageText: String,
    onMessageTextChanged: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                placeholder = { Text("Mensaje") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                shape = RoundedCornerShape(24.dp),
            )
            IconButton(
                onClick = {},
                enabled = messageText.isNotEmpty(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (messageText.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromMe) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                },
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!message.isFromMe) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromMe) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = message.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isFromMe) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                )
            }
        }
    }
}


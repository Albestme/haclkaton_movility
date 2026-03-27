package movility.hackaton

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            id = "conv-operaciones",
            title = "Centro de Operaciones",
            subtitle = "Coordinación de rutas",
            lastMessage = "Perfecto. Técnico Carlos está cerca por soporte.",
            messages = listOf(
                ChatMessage("m1", "Despacho", "Alberto, prioriza el correctivo crítico de Estación Centro.", "07:30", false),
                ChatMessage("m2", "Yo", "Recibido. Voy para Estación Centro y actualizo en 20 minutos.", "07:32", true),
                ChatMessage("m3", "Despacho", "Perfecto. Técnico Carlos está cerca por soporte.", "07:34", false),
            ),
        ),
        ChatConversation(
            id = "conv-tecnicos",
            title = "Técnicos - Zona Norte",
            subtitle = "Apoyo en campo",
            lastMessage = "Tengo repuesto de módulo. Si quieren, paso por allá.",
            messages = listOf(
                ChatMessage("m4", "Carlos", "Ya llegué al punto, hay falla de comunicación en el cargador 2.", "08:12", false),
                ChatMessage("m5", "Yo", "Voy saliendo de Torre Empresarial. Te llamo en 10 min.", "08:14", true),
                ChatMessage("m6", "Luisa", "Tengo repuesto de módulo. Si quieren, paso por allá.", "08:15", false),
            ),
        ),
        ChatConversation(
            id = "conv-clientes",
            title = "Atención Cliente",
            subtitle = "Incidencias y evidencias",
            lastMessage = "Abro visita de diagnóstico y envío fotos al cerrar.",
            messages = listOf(
                ChatMessage("m7", "Soporte", "Cliente reporta carga intermitente en Parque Norte.", "09:01", false),
                ChatMessage("m8", "Yo", "Abro visita de diagnóstico y envío fotos al cerrar.", "09:03", true),
            ),
        ),
    )
}

fun conversationsForTechnicians(technicians: List<Technician>): List<ChatConversation> {
    val technicianConversations = technicians.map { technician ->
        ChatConversation(
            id = conversationIdForTechnician(technician),
            title = technician.name,
            subtitle = "Técnico en campo",
            lastMessage = "Estoy en ${technician.address}. Te actualizo en cuanto termine.",
            messages = listOf(
                ChatMessage(
                    id = "${technician.id}-m1",
                    senderName = technician.name,
                    text = "Estoy en ${technician.address}. Te actualizo en cuanto termine.",
                    time = "08:40",
                    isFromMe = false,
                ),
            ),
        )
    }

    return technicianConversations + sampleConversations()
}

fun conversationIdForTechnician(technician: Technician): String {
    return "conv-${technician.id.lowercase()}"
}

@Composable
fun MessagesTabContent(
    modifier: Modifier,
    technicians: List<Technician>,
    isLoading: Boolean,
    loadError: String?,
    selectedConversationIdRequest: String? = null,
    onConversationRequestConsumed: () -> Unit = {},
) {
    val conversations = remember(technicians) { conversationsForTechnicians(technicians) }
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
            isLoading = isLoading,
            loadError = loadError,
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
    isLoading: Boolean,
    loadError: String?,
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
            Text(
                text = "Canales de coordinación y soporte en campo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (isLoading) {
                Text(
                    text = "Sincronizando conversaciones...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (!loadError.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = loadError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (conversations.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                ) {
                    Text(
                        text = "No hay conversaciones disponibles.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
}

@Composable
private fun ConversationListItem(
    conversation: ChatConversation,
    onSelected: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected)
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = conversation.title.first().toString(),
                        style = MaterialTheme.typography.titleMedium,
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
                    text = conversation.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = conversation.lastMessage.take(60),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Text(
                text = conversation.messages.lastOrNull()?.time ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .height(64.dp)
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
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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


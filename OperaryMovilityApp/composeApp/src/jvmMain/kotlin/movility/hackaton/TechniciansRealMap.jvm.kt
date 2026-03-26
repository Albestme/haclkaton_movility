package movility.hackaton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun TechniciansRealMap(
    modifier: Modifier,
    technicians: List<Technician>,
    onTechnicianClick: (Technician) -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Mapa de tecnicos", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "En desktop se muestra lista clicable. El mapa real esta disponible en Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            technicians.forEach { technician ->
                Text(
                    text = "- ${technician.name} (${technician.latitude}, ${technician.longitude})",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTechnicianClick(technician) }
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}


package movility.hackaton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
actual fun TechniciansRealMap(
    modifier: Modifier,
    technicians: List<Technician>,
    onTechnicianClick: (Technician) -> Unit,
) {
    val defaultLatLng = technicians.firstOrNull()?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(4.65, -74.09)
    val cameraPositionState = rememberCameraPositionState {
        move(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 11f))
    }

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
                text = "Toca un punto para ver acciones del tecnico.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true),
                properties = MapProperties(isMyLocationEnabled = false),
                onMapClick = { },
            ) {
                technicians.forEach { technician ->
                    androidx.compose.runtime.key(technician.id) {
                        val markerState = com.google.maps.android.compose.rememberMarkerState(
                            position = LatLng(technician.latitude, technician.longitude),
                        )
                        com.google.maps.android.compose.Marker(
                            state = markerState,
                            title = technician.name,
                            snippet = technician.address,
                            onClick = {
                                onTechnicianClick(technician)
                                true
                            },
                        )
                    }
                }
            }
        }
    }
}


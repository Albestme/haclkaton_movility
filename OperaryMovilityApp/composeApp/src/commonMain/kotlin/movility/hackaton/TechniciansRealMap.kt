package movility.hackaton

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun TechniciansRealMap(
    modifier: Modifier = Modifier,
    technicians: List<Technician>,
    onTechnicianClick: (Technician) -> Unit,
)


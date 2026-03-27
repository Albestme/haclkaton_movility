package movility.hackaton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.awt.Color as AwtColor
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures

@Composable
actual fun TechniciansRealMap(
    modifier: Modifier,
    technicians: List<Technician>,
    onTechnicianClick: (Technician) -> Unit,
) {
    val initialCenter = remember(technicians) { centerFromTechnicians(technicians) }
    var viewport by remember(technicians) {
        mutableStateOf(
            MapViewport(
                centerLatitude = initialCenter.first,
                centerLongitude = initialCenter.second,
                zoom = chooseZoomLevel(technicians),
            ),
        )
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
            Text(text = "Mapa de técnicos", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "OpenStreetMap integrado. Pulsa un punto para abrir la ficha del técnico.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MAP_HEIGHT)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), MaterialTheme.shapes.medium),
            ) {
                TechnicianMapWithMarkers(
                    technicians = technicians,
                    viewport = viewport,
                    onViewportChanged = { viewport = it },
                    onTechnicianClick = onTechnicianClick,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("UI_COMPOSABLE_EXPECTED")
private fun TechnicianMapWithMarkers(
    technicians: List<Technician>,
    viewport: MapViewport,
    onViewportChanged: (MapViewport) -> Unit,
    onTechnicianClick: (Technician) -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }.roundToInt().coerceAtLeast(256)
        val heightPx = with(density) { maxHeight.toPx() }.roundToInt().coerceAtLeast(256)

        var dynamicMapState by remember { mutableStateOf<OsmMapState?>(null) }
        LaunchedEffect(technicians, viewport, widthPx, heightPx) {
            dynamicMapState = withContext(Dispatchers.IO) {
                buildOsmMapState(
                    technicians = technicians,
                    viewport = viewport,
                    widthPx = widthPx,
                    heightPx = heightPx,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(viewport) {
                    var dragViewport = viewport
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragViewport = panViewport(dragViewport, panX = dragAmount.x, panY = dragAmount.y)
                            onViewportChanged(dragViewport)
                        },
                    )
                }
                .pointerInput(viewport) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        var nextViewport = panViewport(viewport, panX = pan.x, panY = pan.y)
                        nextViewport = when {
                            zoom > 1.08f -> zoomViewport(nextViewport, +1)
                            zoom < 0.92f -> zoomViewport(nextViewport, -1)
                            else -> nextViewport
                        }
                        if (nextViewport != viewport) onViewportChanged(nextViewport)
                    }
                }
                .pointerInput(viewport) {
                    detectTapGestures(
                        onDoubleTap = {
                            onViewportChanged(zoomViewport(viewport, +1))
                        },
                    )
                }
                .onPointerEvent(PointerEventType.Scroll) { pointerEvent ->
                    val scrollY = pointerEvent.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                    if (scrollY != 0f) {
                        val zoomDelta = if (scrollY < 0f) +1 else -1
                        onViewportChanged(zoomViewport(viewport, zoomDelta))
                    }
                    pointerEvent.changes.forEach { it.consume() }
                },
        ) {
            if (dynamicMapState == null) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                return@Box
            }

            Image(
                bitmap = dynamicMapState!!.bitmap,
                contentDescription = "Mapa OpenStreetMap",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )

            technicians.forEach { technician ->
                val point = dynamicMapState!!.markerPixelsByTechnicianId[technician.id] ?: return@forEach
                val markerX = point.x.roundToInt()
                val markerY = point.y.roundToInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(markerX - 9, markerY - 9) }
                        .size(18.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color(0xFFE53935))
                        .border(2.dp, Color.White, MaterialTheme.shapes.small)
                        .clickable { onTechnicianClick(technician) },
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalIconButton(onClick = { onViewportChanged(zoomViewport(viewport, -1)) }) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Alejar")
                }
                FilledTonalIconButton(onClick = { onViewportChanged(zoomViewport(viewport, +1)) }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Acercar")
                }
            }
        }
    }
}

private data class PixelPoint(val x: Float, val y: Float)

private data class MapViewport(
    val centerLatitude: Double,
    val centerLongitude: Double,
    val zoom: Int,
)

private data class TileKey(val zoom: Int, val x: Int, val y: Int)

private data class OsmMapState(
    val bitmap: ImageBitmap,
    val widthPx: Float,
    val heightPx: Float,
    val markerPixelsByTechnicianId: Map<String, PixelPoint>,
)

private const val OSM_TILE_BASE_URL = "https://tile.openstreetmap.org"
private const val DEFAULT_OSM_USER_AGENT =
    "OperaryMovilityApp/1.0 (desktop; contact: soporte@operary.local)"

private val tileCacheLock = Any()
private val tileImageCache = object : LinkedHashMap<TileKey, BufferedImage>(96, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<TileKey, BufferedImage>?): Boolean {
        return size > 64
    }
}

private fun buildOsmMapState(
    technicians: List<Technician>,
    viewport: MapViewport,
    widthPx: Int,
    heightPx: Int,
): OsmMapState {
    val zoom = viewport.zoom
    val tileSize = 256
    val worldTileLimit = 1 shl zoom
    val worldSizePx = tileSize * worldTileLimit.toDouble()
    val centerWorldX = lonToWorldPixelX(viewport.centerLongitude, zoom)
    val centerWorldY = latToWorldPixelY(viewport.centerLatitude, zoom)
    val originPixelX = centerWorldX - widthPx / 2.0
    val originPixelY = centerWorldY - heightPx / 2.0

    val startTileX = floor(originPixelX / tileSize).toInt()
    val endTileX = floor((originPixelX + widthPx - 1) / tileSize).toInt()
    val startTileY = floor(originPixelY / tileSize).toInt()
    val endTileY = floor((originPixelY + heightPx - 1) / tileSize).toInt()

    val imageWidth = widthPx
    val imageHeight = heightPx
    val mosaic = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
    val graphics = mosaic.createGraphics()
    graphics.color = AwtColor(235, 235, 235)
    graphics.fillRect(0, 0, imageWidth, imageHeight)

    for (tileX in startTileX..endTileX) {
        for (tileY in startTileY..endTileY) {
            val clampedTileY = tileY.coerceIn(0, worldTileLimit - 1)
            val image = loadOsmTileImage(zoom = zoom, tileX = tileX, tileY = tileY)
            if (image != null) {
                val drawX = (tileX * tileSize - originPixelX).roundToInt()
                val drawY = (clampedTileY * tileSize - originPixelY).roundToInt()
                graphics.drawImage(image, drawX, drawY, null)
            }
        }
    }
    graphics.dispose()

    val markerPixels = technicians.associate { technician ->
        val markerPixelX = wrapWorldPixelX(lonToWorldPixelX(technician.longitude, zoom), worldSizePx) - originPixelX
        val markerPixelY = latToWorldPixelY(technician.latitude, zoom) - originPixelY
        technician.id to PixelPoint(markerPixelX.toFloat(), markerPixelY.toFloat())
    }

    val bitmapBytes = ByteArrayOutputStream().use { output ->
        ImageIO.write(mosaic, "png", output)
        output.toByteArray()
    }

    return OsmMapState(
        bitmap = Image.makeFromEncoded(bitmapBytes).toComposeImageBitmap(),
        widthPx = imageWidth.toFloat(),
        heightPx = imageHeight.toFloat(),
        markerPixelsByTechnicianId = markerPixels,
    )
}

private fun centerFromTechnicians(technicians: List<Technician>): Pair<Double, Double> {
    val fallbackLat = 4.65
    val fallbackLon = -74.09
    val centerLat = technicians.map { it.latitude }.average().takeUnless { it.isNaN() } ?: fallbackLat
    val centerLon = technicians.map { it.longitude }.average().takeUnless { it.isNaN() } ?: fallbackLon
    return centerLat to centerLon
}

private fun chooseZoomLevel(technicians: List<Technician>): Int {
    if (technicians.size < 2) return 13
    val latRange = technicians.maxOf { it.latitude } - technicians.minOf { it.latitude }
    val lonRange = technicians.maxOf { it.longitude } - technicians.minOf { it.longitude }
    val maxRange = maxOf(latRange, lonRange)
    return when {
        maxRange < 0.01 -> 14
        maxRange < 0.03 -> 13
        maxRange < 0.08 -> 12
        else -> 11
    }
}

private fun panViewport(viewport: MapViewport, panX: Float, panY: Float): MapViewport {
    val zoom = viewport.zoom
    val worldSizePx = 256.0 * 2.0.pow(zoom)
    val centerX = lonToWorldPixelX(viewport.centerLongitude, zoom)
    val centerY = latToWorldPixelY(viewport.centerLatitude, zoom)
    val nextCenterX = wrapWorldPixelX(centerX - panX, worldSizePx)
    val nextCenterY = (centerY - panY).coerceIn(0.0, worldSizePx)
    return viewport.copy(
        centerLongitude = worldPixelXToLon(nextCenterX, zoom),
        centerLatitude = worldPixelYToLat(nextCenterY, zoom),
    )
}

private fun zoomViewport(viewport: MapViewport, delta: Int): MapViewport {
    val nextZoom = (viewport.zoom + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)
    return viewport.copy(zoom = nextZoom)
}

private fun loadOsmTileImage(zoom: Int, tileX: Int, tileY: Int): BufferedImage? {
    val worldTileLimit = 1 shl zoom
    val wrappedTileX = wrapTile(tileX, worldTileLimit)
    val clampedTileY = tileY.coerceIn(0, worldTileLimit - 1)
    val key = TileKey(zoom, wrappedTileX, clampedTileY)
    synchronized(tileCacheLock) {
        tileImageCache[key]?.let { return it }
    }

    val tileUri = URI("$OSM_TILE_BASE_URL/$zoom/$wrappedTileX/$clampedTileY.png")
    val url = tileUri.toURL()
    val connection = (url.openConnection() as? HttpURLConnection) ?: return null
    return try {
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", openStreetMapUserAgent())
        connection.setRequestProperty("Accept", "image/png")
        connection.inputStream.use { input ->
            ImageIO.read(input)
        }?.also { downloadedImage ->
            synchronized(tileCacheLock) {
                tileImageCache[key] = downloadedImage
            }
        }
    } catch (_: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}

private fun openStreetMapUserAgent(): String {
    val override = System.getenv("OSM_USER_AGENT")?.trim().orEmpty()
    return if (override.isNotEmpty()) override else DEFAULT_OSM_USER_AGENT
}

private fun wrapTile(tile: Int, tileLimit: Int): Int {
    val mod = tile % tileLimit
    return if (mod < 0) mod + tileLimit else mod
}

private fun lonToTileX(lon: Double, zoom: Int): Double {
    return (lon + 180.0) / 360.0 * 2.0.pow(zoom)
}

private fun latToTileY(lat: Double, zoom: Int): Double {
    val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
    return (1.0 - ln(tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / PI) / 2.0 * 2.0.pow(zoom)
}

private fun lonToWorldPixelX(lon: Double, zoom: Int): Double {
    return lonToTileX(lon, zoom) * 256.0
}

private fun latToWorldPixelY(lat: Double, zoom: Int): Double {
    val clippedLat = lat.coerceIn(-85.0511, 85.0511)
    val latRad = Math.toRadians(clippedLat)
    val mercatorY = ln(tan(PI / 4.0 + latRad / 2.0))
    val normalized = (1.0 - mercatorY / PI) / 2.0
    return normalized * 2.0.pow(zoom) * 256.0
}

private fun worldPixelXToLon(worldX: Double, zoom: Int): Double {
    val worldSize = 256.0 * 2.0.pow(zoom)
    return worldX / worldSize * 360.0 - 180.0
}

private fun worldPixelYToLat(worldY: Double, zoom: Int): Double {
    val worldSize = 256.0 * 2.0.pow(zoom)
    val normalized = (0.5 - (worldY / worldSize)) * 2.0 * PI
    return Math.toDegrees(atan(0.5 * (exp(normalized) - exp(-normalized))))
}

private fun wrapWorldPixelX(worldX: Double, worldSize: Double): Double {
    val mod = worldX % worldSize
    return if (mod < 0) mod + worldSize else mod
}

private const val MIN_ZOOM = 3
private const val MAX_ZOOM = 18
private val MAP_HEIGHT: Dp = 280.dp


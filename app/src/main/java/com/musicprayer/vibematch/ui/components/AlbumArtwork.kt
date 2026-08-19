package com.musicprayer.vibematch.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.musicprayer.vibematch.R
import com.musicprayer.vibematch.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumArtwork(
    track: Track?,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val artwork by produceState<ImageBitmap?>(
        null,
        track?.uri,
        track?.sourceModifiedMs,
        track?.artworkUri,
        track?.artworkModifiedMs,
    ) {
        value = track?.let { loadArtwork(context, it) }
    }
    Box(
        modifier = modifier.background(containerColor ?: MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = artwork
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                alignment = Alignment.Center,
            )
        } else {
            Icon(painterResource(R.drawable.ic_music_note), contentDescription = null, modifier = Modifier.fillMaxSize(.34f), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.72f))
        }
    }
}

private suspend fun loadArtwork(context: Context, track: Track): ImageBitmap? = withContext(Dispatchers.IO) {
    val embeddedKey = "embedded:${track.uri}:${track.sourceModifiedMs}"
    ARTWORK_CACHE.get(embeddedKey)?.let { return@withContext it.asImageBitmap() }

    val embedded = if (EMBEDDED_MISS_CACHE.get(embeddedKey) == true) null
    else runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, track.uri)
            retriever.embeddedPicture?.let(::decodeSampledByteArray)
        }
    }.getOrNull()
    if (embedded != null) {
        ARTWORK_CACHE.put(embeddedKey, embedded)
        return@withContext embedded.asImageBitmap()
    }
    EMBEDDED_MISS_CACHE.put(embeddedKey, true)

    val externalUri = track.artworkUri ?: return@withContext null
    val externalKey = "external:$externalUri:${track.artworkModifiedMs ?: 0L}"
    ARTWORK_CACHE.get(externalKey)?.let { return@withContext it.asImageBitmap() }
    runCatching { decodeSampledUri(context, externalUri) }.getOrNull()
        ?.also { ARTWORK_CACHE.put(externalKey, it) }
        ?.asImageBitmap()
}

private fun decodeSampledByteArray(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private fun decodeSampledUri(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
    return resolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input, null, options) }
}

private fun sampleSize(width: Int, height: Int): Int {
    val largestDimension = maxOf(width, height)
    var size = 1
    while (largestDimension / size > MAX_ARTWORK_DIMENSION_PX) size *= 2
    return size
}

private const val MAX_ARTWORK_DIMENSION_PX = 1_280
private val ARTWORK_CACHE = object : LruCache<String, Bitmap>(
    (Runtime.getRuntime().maxMemory() / 1_024L / 16L).coerceIn(2_048L, 49_152L).toInt(),
) {
    override fun sizeOf(key: String, value: Bitmap): Int = (value.allocationByteCount / 1_024).coerceAtLeast(1)
}
private val EMBEDDED_MISS_CACHE = LruCache<String, Boolean>(2_048)

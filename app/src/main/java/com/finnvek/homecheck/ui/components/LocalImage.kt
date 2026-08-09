package com.finnvek.homecheck.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LocalImage(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    file: File? = null,
    uri: Uri? = null,
    contentScale: ContentScale = ContentScale.Crop,
    maxDimension: Int = 1200,
    placeholder: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, file?.path, uri) {
        value =
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }

                    fun open() =
                        when {
                            file != null -> file.inputStream()
                            uri != null -> context.contentResolver.openInputStream(uri)
                            else -> null
                        }
                    open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (bounds.outWidth / sample > maxDimension * 2 || bounds.outHeight / sample > maxDimension * 2) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
                    open()?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
                }.getOrNull()
            }
    }
    if (image != null) {
        Image(requireNotNull(image), contentDescription, modifier, contentScale = contentScale)
    } else {
        androidx.compose.material3.Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant) { placeholder() }
    }
}

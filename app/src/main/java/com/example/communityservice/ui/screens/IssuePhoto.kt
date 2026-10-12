package com.example.communityservice.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import coil.compose.rememberAsyncImagePainter

/**
 * Photos saved by the app and the staff portal are stored inside the Firestore report as text
 * ("data:image/jpeg;base64,..."). Coil cannot open that, so decode it here.
 * Anything else (web links, phone file addresses) still goes through Coil as before.
 */
@Composable
fun rememberIssuePainter(source: String?): Painter {
    val bitmap = remember(source) {
        if (source != null && source.startsWith("data:image")) {
            runCatching {
                val bytes = Base64.decode(source.substringAfter(","), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else null
    }
    return if (bitmap != null) BitmapPainter(bitmap) else rememberAsyncImagePainter(source)
}

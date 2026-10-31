package dev.j6n.still

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore

/** Export to a NEW MediaStore entry. Never writes to or deletes the source URI. */
object PhotoExporter {
    fun export(context: Context, original: Uri, look: CinematicLook, intensity: Float = 1f, custom: LookSettings? = null): Uri {
        require(look != CinematicLook.ORIGINAL) { "Choose an edited look" }
        val bitmap = PhotoDecoder.decode(context, original, 4096)
        val rendered = CinematicProcessor.render(bitmap, look, maxEdge = 4096, intensity = intensity, custom = custom)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Still_${look.name}_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Still")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create export")
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                check(rendered.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    "JPEG compression failed"
                }
            } ?: error("Unable to write export")
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
            return uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}

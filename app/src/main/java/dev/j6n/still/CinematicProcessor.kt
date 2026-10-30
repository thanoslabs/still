package dev.j6n.still

import android.graphics.Bitmap
import android.graphics.Color

/** Version 1: deterministic, offline, non-destructive preview transforms. */
enum class CinematicLook(val title: String, val contrast: Float, val warmth: Float, val saturation: Float) {
    ORIGINAL("Original", 1f, 0f, 1f),
    STILL("Still", 1.12f, 0.04f, 0.85f),
    DUSK("Dusk", 1.18f, 0.09f, 0.78f),
    NOIR("Noir", 1.25f, 0f, 0f),
    COOL("Blue Hour", 1.1f, -0.08f, 0.82f)
}

object CinematicProcessor {
    /** Render a bounded-size preview; the original full-resolution Bitmap is never modified. */
    fun render(source: Bitmap, look: CinematicLook, maxEdge: Int = 1280, intensity: Float = 1f): Bitmap {
        val strength = intensity.coerceIn(0f, 1f)
        val scale = minOf(1f, maxEdge.toFloat() / maxOf(source.width, source.height))
        val width = maxOf(1, (source.width * scale).toInt())
        val height = maxOf(1, (source.height * scale).toInt())
        val input = Bitmap.createScaledBitmap(source, width, height, true)
        if (look == CinematicLook.ORIGINAL || strength == 0f) return input
        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel).toFloat()
            val g = Color.green(pixel).toFloat()
            val b = Color.blue(pixel).toFloat()
            val gray = 0.2126f * r + 0.7152f * g + 0.0722f * b
            fun adjust(value: Float, warmth: Float): Int {
                val saturated = gray + (value - gray) * look.saturation
                return ((saturated - 127.5f) * look.contrast + 127.5f + warmth)
                    .toInt().coerceIn(0, 255)
            }
            fun blend(original: Float, graded: Int): Int =
                (original + (graded - original) * strength).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(
                Color.alpha(pixel),
                blend(r, adjust(r, look.warmth * 255)),
                blend(g, adjust(g, 0f)),
                blend(b, adjust(b, -look.warmth * 255))
            )
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}

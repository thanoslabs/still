package dev.j6n.still

import android.graphics.Bitmap
import android.graphics.Color

/** Cheap, deterministic, offline scene heuristic; no personal data leaves the device. */
object LookRecommender {
    fun recommend(source: Bitmap): CinematicLook {
        val stepX = maxOf(1, source.width / 48)
        val stepY = maxOf(1, source.height / 48)
        var luminance = 0.0
        var warmth = 0.0
        var count = 0
        for (y in 0 until source.height step stepY) {
            for (x in 0 until source.width step stepX) {
                val color = source.getPixel(x, y)
                luminance += (Color.red(color) * 0.2126 +
                    Color.green(color) * 0.7152 + Color.blue(color) * 0.0722) / 255.0
                warmth += (Color.red(color) - Color.blue(color)) / 255.0
                count++
            }
        }
        if (count == 0) return CinematicLook.STILL
        val light = luminance / count
        val temperature = warmth / count
        return when {
            light < 0.30 -> CinematicLook.DUSK
            temperature > 0.16 -> CinematicLook.COOL
            light > 0.76 -> CinematicLook.STILL
            else -> CinematicLook.STILL
        }
    }
}

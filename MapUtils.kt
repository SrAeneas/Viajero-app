package com.viajero.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.viajero.app.data.Place
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/** Guarda referencias al mapa y a la capa de ubicación para poder controlarlos desde la pantalla. */
class MapHolder {
    var map: MapView? = null
    var loc: MyLocationNewOverlay? = null
    var centered = false
    val icons = HashMap<String, Drawable>()
}

fun markerIcon(ctx: Context, p: Place, cache: HashMap<String, Drawable>): Drawable {
    val key = "${p.category.name}-${p.visited}"
    return cache.getOrPut(key) {
        val d = ctx.resources.displayMetrics.density
        val size = (40 * d).toInt()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        paint.color = if (p.visited) 0xFF9AA5A8.toInt() else p.category.color.toInt()
        c.drawCircle(size / 2f, size / 2f, size / 2f - 3 * d, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3 * d
        paint.color = android.graphics.Color.WHITE
        c.drawCircle(size / 2f, size / 2f, size / 2f - 3 * d, paint)
        paint.style = Paint.Style.FILL
        paint.textSize = 18 * d
        paint.textAlign = Paint.Align.CENTER
        paint.color = android.graphics.Color.BLACK
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2
        c.drawText(if (p.visited) "✅" else p.category.emoji, size / 2f, y, paint)
        BitmapDrawable(ctx.resources, bmp)
    }
}

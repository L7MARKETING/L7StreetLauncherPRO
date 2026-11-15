package com.l7.streetlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Papel de parede personalizado: a imagem escolhida é copiada (reduzida e já rotacionada)
 * para o armazenamento interno do app. Assim não depende de permissão de galeria depois.
 */
object WallpaperStore {

    fun file(context: Context): File = File(context.filesDir, "wallpaper.jpg")

    fun hasCustom(context: Context): Boolean = file(context).exists()

    /** Executar fora da thread principal. */
    fun importImage(context: Context, uri: Uri): Boolean {
        return try {
            val resolver = context.contentResolver
            val metrics = context.resources.displayMetrics
            val target = max(max(metrics.widthPixels, metrics.heightPixels), 1280)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val opened = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
                true
            } ?: false
            if (!opened || bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, target)
            }
            var bitmap: Bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return false

            val rotation = try {
                resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
            } catch (e: Exception) {
                0
            }
            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated !== bitmap) bitmap.recycle()
                bitmap = rotated
            }

            val tmp = File(context.filesDir, "wallpaper.tmp")
            FileOutputStream(tmp).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            bitmap.recycle()
            tmp.renameTo(file(context))
        } catch (e: Exception) {
            false
        } catch (e: OutOfMemoryError) {
            false
        }
    }

    /** Executar fora da thread principal. */
    fun load(context: Context, maxSide: Int): Bitmap? {
        val f = file(context)
        if (!f.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
            }
            BitmapFactory.decodeFile(f.absolutePath, options)
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        val longest = max(width, height)
        while (longest / (sample * 2) >= target) sample *= 2
        return sample
    }
}

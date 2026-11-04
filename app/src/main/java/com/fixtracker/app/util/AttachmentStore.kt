package com.fixtracker.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

object AttachmentStore {

    private const val MAX_DIMENSION = 1600

    fun directory(context: Context): File =
        File(context.filesDir, "attachments").apply { mkdirs() }

    fun file(context: Context, name: String): File =
        File(directory(context), File(name).name)

    fun saveImage(context: Context, uri: Uri): String? {
        val bitmap = decodeSampled(context, uri, MAX_DIMENSION) ?: return null
        val oriented = applyExifRotation(context, uri, bitmap)
        val width = oriented.width
        val height = oriented.height
        val scale = min(1f, MAX_DIMENSION.toFloat() / max(width, height))
        val finalBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                oriented,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else oriented

        val name = "att_${UUID.randomUUID()}.jpg"
        val target = file(context, name)
        runCatching {
            FileOutputStream(target).use { output ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)
            }
        }.onFailure {
            target.delete()
            return null
        }
        if (finalBitmap !== oriented) finalBitmap.recycle()
        if (oriented !== bitmap) oriented.recycle()
        bitmap.recycle()
        return name
    }

    fun loadBitmap(context: Context, name: String, maxWidth: Int = 1200): Bitmap? {
        val target = file(context, name)
        if (!target.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(target.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > maxWidth * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return runCatching { BitmapFactory.decodeFile(target.absolutePath, options) }.getOrNull()
    }

    fun delete(context: Context, name: String) {
        runCatching { file(context, name).delete() }
    }

    private fun decodeSampled(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

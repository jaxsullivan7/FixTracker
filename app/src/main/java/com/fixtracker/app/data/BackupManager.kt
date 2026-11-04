package com.fixtracker.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.fixtracker.app.util.AttachmentStore
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed interface BackupReadResult {
    data class Ready(
        val state: AppState,
        val tempDirectory: File?,
        val attachmentCount: Int,
    ) : BackupReadResult

    data class Error(val message: String) : BackupReadResult
}

object BackupManager {

    fun writeJson(json: String, output: OutputStream) {
        output.use { it.write(json.toByteArray(Charsets.UTF_8)) }
    }

    fun writeZip(context: Context, json: String, output: OutputStream) {
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry("fixtracker.json"))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            AttachmentStore.directory(context).listFiles()?.forEach { attachment ->
                if (!attachment.isFile) return@forEach
                zip.putNextEntry(ZipEntry("attachments/${attachment.name}"))
                attachment.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    fun readBackup(context: Context, uri: Uri, repository: FixRepository): BackupReadResult {
        val displayName = displayName(context, uri) ?: ""
        val mime = context.contentResolver.getType(uri).orEmpty()
        val isZip = displayName.endsWith(".zip", ignoreCase = true) || mime.contains("zip")

        return runCatching {
            if (isZip) readZip(context, uri, repository) else readJson(context, uri, repository)
        }.getOrElse { error ->
            BackupReadResult.Error(error.message ?: "No se pudo leer el archivo.")
        }
    }

    fun apply(context: Context, result: BackupReadResult.Ready, repository: FixRepository) {
        result.tempDirectory?.listFiles()?.forEach { attachment ->
            if (attachment.isFile) {
                attachment.copyTo(
                    AttachmentStore.file(context, attachment.name),
                    overwrite = true,
                )
            }
        }
        repository.replaceState(result.state)
        result.tempDirectory?.deleteRecursively()
    }

    fun discard(result: BackupReadResult.Ready) {
        result.tempDirectory?.deleteRecursively()
    }

    private fun readJson(context: Context, uri: Uri, repository: FixRepository): BackupReadResult {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: return BackupReadResult.Error("No se pudo abrir el archivo.")
        if (text.length > 20_000_000) return BackupReadResult.Error("El archivo es demasiado grande.")
        val state = repository.parseBackup(text)
            ?: return BackupReadResult.Error("El archivo no es una copia válida de FixTracker.")
        return BackupReadResult.Ready(state, null, 0)
    }

    private fun readZip(context: Context, uri: Uri, repository: FixRepository): BackupReadResult {
        val temp = File(context.cacheDir, "import_${System.currentTimeMillis()}").apply { mkdirs() }
        var json: String? = null
        var attachments = 0

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "fixtracker.json" -> {
                            json = zip.readBytes().toString(Charsets.UTF_8)
                        }
                        name.startsWith("attachments/") && !name.contains("..") -> {
                            val target = File(temp, name.removePrefix("attachments/"))
                            if (target.parentFile?.canonicalPath?.startsWith(temp.canonicalPath) == true) {
                                target.parentFile?.mkdirs()
                                target.outputStream().use { zip.copyTo(it) }
                                attachments++
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: return BackupReadResult.Error("No se pudo abrir el archivo.")

        val text = json ?: run {
            temp.deleteRecursively()
            return BackupReadResult.Error("La copia no contiene fixtracker.json.")
        }
        if (text.length > 20_000_000) {
            temp.deleteRecursively()
            return BackupReadResult.Error("La copia es demasiado grande.")
        }
        val state = repository.parseBackup(text) ?: run {
            temp.deleteRecursively()
            return BackupReadResult.Error("Los datos de la copia no son válidos.")
        }
        return BackupReadResult.Ready(state, temp, attachments)
    }

    private fun displayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
    }
}

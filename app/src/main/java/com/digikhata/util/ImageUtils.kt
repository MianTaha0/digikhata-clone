package com.digikhata.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    suspend fun saveImageToAppDir(context: Context, srcUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.getExternalFilesDir(null), "receipts").apply { mkdirs() }
            val destFile = File(dir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(srcUri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            } ?: return@withContext null
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun createCameraOutputFile(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "receipts").apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }
}

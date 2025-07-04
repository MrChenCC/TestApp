package cn.cb.testapp.module.uploadprogress

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import timber.log.Timber
import java.io.File

interface UploadProgressListener {
    fun onProgress(percent: Int)
}

class ProgressRequestBody(
    private val files: List<File>,
    private val listener: UploadProgressListener
) : RequestBody() {

    private val contentType = "application/octet-stream".toMediaTypeOrNull()

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = files.sumOf { it.length() }

    override fun writeTo(sink: BufferedSink) {
        var uploaded = 0L
        val total = contentLength()

        files.forEach { file ->
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    uploaded += read
                    val progress = (100 * uploaded / total).toInt()
                    listener.onProgress(progress)
                }
            }
        }
    }
}

fun Uri.toFileOrNull(context: Context): File? {
    return when (scheme) {
        "file" -> File(path ?: return null)
        "content" -> {
            try {
                val cursor = context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                val name = cursor?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                } ?: return null

                val inputStream = context.contentResolver.openInputStream(this) ?: return null
                val tempFile = File(context.cacheDir, name)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                tempFile
            } catch (e: Exception) {
                Timber.e(e, "无法将 Uri 转换为 File")
                null
            }
        }
        else -> null
    }
}

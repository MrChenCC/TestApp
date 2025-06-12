package cn.cb.testapp.common.log

import android.annotation.SuppressLint
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志文件管理器
 * 1. 日志根目录：/sdcard/MyTest/logs
 * 2. 相对路径下文件名：日志根目录/日期路径/用户路径/日期路径_文件序号.log
 * 3. 单个日志文件最大：10M
 * 4. 历史日志文件目录：/sdcard/MyTest/logs/old
 * 5. 归档历史日志时候，会删除old目录，超过一个月日志【通过文件最后修改时间排序，取巧删除】
 */
class LogFileManager(context: Context) {
    companion object {
        const val LOG_FILE_NAME = "yyyy-MM-dd"
        private const val LOG_FILE_MAX_SIZE = 1024 * 1024 * 10

        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: LogFileManager

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): LogFileManager {
            if (::instance.isInitialized) {
                return instance
            }
            return LogFileManager(context).apply { instance = this }
        }
    }

    /**日志根目录*/
    val logRootFile =
        File(context.externalCacheDir?.absolutePath + File.separator + "MyTest" + File.separator + "logs")

    /**获取当前日志路径*/
    fun getCurrentFilePath(): String {
        val datePath = SimpleDateFormat(LOG_FILE_NAME, Locale.CHINA).format(Date())
        val filePath =
            File(logRootFile, datePath + "_" + getFileIndex(logRootFile, datePath) + ".log")
        return filePath.absolutePath
    }


    /**获取日志文件序号*/
    private fun getFileIndex(fileParentPath: File, datePath: String, fileIndex: Int = 0): Int {
        val file = File(fileParentPath, datePath + "_" + fileIndex + ".log")
        if (file.exists()) {
            if (file.length() >= LOG_FILE_MAX_SIZE) {
                return getFileIndex(fileParentPath, datePath, fileIndex + 1)
            }
        }
        return fileIndex
    }

    /**
     * 删除历史文件，超过30个的日志
     * */
    fun deleteHistoryFiles() {
        //删除old文件夹下的文件，超过一个月日志【此处通过文件最后修改时间排序，取巧删除】
        logRootFile.listFiles()?.let { files ->
            files.sortByDescending { it.lastModified() }
            files.forEachIndexed { index, file ->
                if (index > 30) {
                    file.deleteRecursively()
                }
            }
        }
    }

    fun writerLog(log: String) {
        try {
            val file = File(getCurrentFilePath())
            if (file.parentFile?.exists()?.not() == true) {
                file.parentFile!!.mkdirs()
            }
            if (file.exists().not()) {
                file.createNewFile()
            }
            OutputStreamWriter(FileOutputStream(file, true), "UTF-8").use { it.write(log) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
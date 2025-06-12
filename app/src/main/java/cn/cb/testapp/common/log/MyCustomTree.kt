package cn.cb.testapp.common.log

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File

/**
 * 自定义日志，日志逻辑：
 * 1. 调试模式，DEBUG，日志仅打印到logcat，不保存到文件
 * 2. 先归档日志文件
 * 3. 添加文件头信息
 * 4. 添加日志信息
 *
 * @property isDebug
 */
class MyCustomTree(context: Context, private val isDebug: Boolean) : Timber.Tree() {

    private val logFileManager = LogFileManager.getInstance(context)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (isDebug && priority <= Log.DEBUG) {//调试日志仅打印到logcat
            Log.d(tag, message)
            return
        }
        logFileManager.deleteHistoryFiles()
        val logBody = LogPackaging.logBody(priority, tag, message, t)
        logFileManager.writerLog(logBody)
    }


}
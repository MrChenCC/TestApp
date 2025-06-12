package cn.cb.testapp.common.log

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 *
 * 包装日志内容
 */
object LogPackaging {
    private const val LOG_TEXT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    private val DATETIME_FORMATTER = SimpleDateFormat(LOG_TEXT_DATE_FORMAT, Locale.CHINA)

    fun logBody(priority: Int, tag: String?, message: String, t: Throwable?): String {
        val sb = StringBuilder().append("[$tag] ")
        when (priority) {
            Log.VERBOSE -> sb.append("Verbose: ")
            Log.DEBUG -> sb.append("Debug: ")
            Log.INFO -> sb.append("Info: ")
            Log.WARN -> sb.append("Warn: ")
            Log.ERROR -> sb.append("Error: ")
            Log.ASSERT -> sb.append("Assert: ")
            else -> sb.append("Other: ")
        }
        sb.append(DATETIME_FORMATTER.format(Date()))
            .append("(")
            .append(System.currentTimeMillis())
            .append(")")
            .appendLine()
        sb.append(message)
            .appendLine()
        if (t != null) {
            sb.append(getStackTraceString(t))
                .appendLine()
        }
        sb.appendLine()
        Log.println(priority, tag, sb.toString())
        return sb.toString()
    }

    private fun getStackTraceString(t: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }
}
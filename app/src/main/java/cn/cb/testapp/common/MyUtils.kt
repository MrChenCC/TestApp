package cn.cb.testapp.common

import android.view.View
import android.widget.ScrollView
import android.widget.TextView

object MyUtils {
    fun appendLog(textView: TextView, message: String) {
        textView.append("● $message\n")

        // 可选：滚动到底部（如果 TextView 放在 ScrollView 中）
        val parent = textView.parent
        if (parent is ScrollView) {
            parent.post {
                parent.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

}
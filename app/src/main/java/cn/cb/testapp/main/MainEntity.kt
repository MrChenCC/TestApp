package cn.cb.testapp.main

import android.app.Activity

/**
 * @property title 测试标题
 * @property desc 测试描述
 * @property clazz 跳转类
 */
data class MainItem(
    val title: String,
    val desc: String,
    val clazz: Class<out Activity>
)
package cn.cb.testapp

import android.app.Application
import cn.cb.testapp.common.log.MyCustomTree
import timber.log.Timber

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(MyCustomTree(this, false/*BuildConfig.DEBUG*/)) // file log
    }
}
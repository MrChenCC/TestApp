package cn.cb.testapp.module.info

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.cb.testapp.R
import cn.cb.testapp.databinding.ActivityInfoBinding

class InfoActivity : AppCompatActivity() {
    private val banding by lazy { ActivityInfoBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(banding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val sb = StringBuilder()
        sb.append("程序报名：").append(packageName).appendLine()
            .append("设备型号：").append(Build.MODEL).appendLine()
            .append("厂家信息：").append(Build.MANUFACTURER).appendLine()
            .append("DeviceID：").append(getSerialNumber()).appendLine()

        banding.textView.text = sb
    }

    @SuppressLint("HardwareIds")
    private fun getSerialNumber(): String {
        return Settings.Secure.getString(
            this.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }
}
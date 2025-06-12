package cn.cb.testapp.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.cb.testapp.R
import cn.cb.testapp.databinding.ActivityMainBinding
import cn.cb.testapp.module.camera.DualCameraActivity
import cn.cb.testapp.module.camera.MainJavaActivity
import cn.cb.testapp.module.tts.MainTtsActivity
import cn.cb.testapp.module.usbstorage.UsbMonitorActivity
import cn.cb.testapp.module.videovoice.VideoVoiceActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val itemList = mutableListOf<MainItem>()
        itemList.add(
            MainItem("音视频", "录制音视频时，同时另外录制音频", VideoVoiceActivity::class.java)
        )
        itemList.add(
            MainItem("多摄像头", "多摄像头同时预览", MainJavaActivity::class.java)
        )
        itemList.add(
            MainItem("多摄像头Kt", "多摄像头同时预览", DualCameraActivity::class.java)
        )
        itemList.add(
            MainItem("文字朗读", "使用系统自带TTS进行文字朗读", MainTtsActivity::class.java)
        )
        itemList.add(
            MainItem("U盘监听", "通过广播（BroadcastReceiver）结合 UsbManager 来实现U盘监听", UsbMonitorActivity::class.java)
        )
        binding.mainList.adapter = MyAdapter().apply {
            list.addAll(itemList)
            setOnItemClickListener { mainItem, _ ->
                startActivity(Intent(this@MainActivity, mainItem.clazz))
            }
        }
    }
}
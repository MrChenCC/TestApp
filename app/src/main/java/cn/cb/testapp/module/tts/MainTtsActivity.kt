package cn.cb.testapp.module.tts

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import cn.cb.testapp.common.MyUtils
import cn.cb.testapp.databinding.ActivityMainTtsBinding
import java.util.Locale

class MainTtsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var binding: ActivityMainTtsBinding
    private var initTimes: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainTtsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 列出当前可用引擎（可选，调试用）
        val tempTTS = TextToSpeech(this) { }
        val engines = tempTTS.engines
        engines.forEach {
            MyUtils.appendLog(binding.ttsLog, "发现引擎：${it.name}, 标签：${it.label}")
        }

        // 初始化讯飞 TTS（系统安装的 TTS 引擎包名）
        //tts = TextToSpeech(this, this, "com.iflytek.speechcloud")
        tts = TextToSpeech(this, this)

        binding.ttsInputBtn.setOnClickListener {
            if (binding.ttsInputValue.text.isNullOrEmpty()) {
                speakText("输入内容为空")
                return@setOnClickListener
            }
            speakText(binding.ttsInputValue.text.toString())
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            MyUtils.appendLog(binding.ttsLog, "TTS初始化成功，当前引擎为：${tts.defaultEngine}")
            val result = tts.setLanguage(Locale.CHINESE)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                MyUtils.appendLog(binding.ttsLog, "语言数据缺失或不支持")

                // 引导安装中文语音数据
                val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                //intent.setPackage("com.iflytek.speechcloud") // 指定讯飞引擎
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    MyUtils.appendLog(binding.ttsLog, "未找到语音数据安装页面")
                    //Toast.makeText(this, "未找到语音数据安装页面", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            } else {
                speakText("讯飞语音合成引擎初始化成功")
            }
        } else {
            MyUtils.appendLog(binding.ttsLog, "TTS 初始化失败，状态码：$status")
            val checkIntent = Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)
            //checkIntent.setPackage("com.iflytek.speechcloud")
            if (initTimes > 2) {
                return
            }
            try {
                startActivityForResult(checkIntent, 1000)
            } catch (e: Exception) {
                MyUtils.appendLog(binding.ttsLog, "无法检查语音引擎数据")
                e.printStackTrace()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        initTimes++
        if (requestCode == 1000) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                //tts = TextToSpeech(this, this, "com.iflytek.speechcloud")
                tts = TextToSpeech(this, this)
            } else {
                try {
                    val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    //installIntent.setPackage("com.iflytek.speechcloud")
                    startActivity(installIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    MyUtils.appendLog(binding.ttsLog, "无法引导安装语音引擎数据")
                }
            }
        }
    }

    private fun speakText(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}

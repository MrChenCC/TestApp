package cn.cb.testapp.module.uploadprogress

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.cb.testapp.common.MyUtils
import cn.cb.testapp.common.RetrofitClient
import cn.cb.testapp.databinding.ActivityUploadProgressBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import java.io.File

class UploadProgressActivity : AppCompatActivity() {

    private val binding by lazy { ActivityUploadProgressBinding.inflate(layoutInflater) }

    private val fileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            lifecycleScope.launch {
                val start = System.currentTimeMillis()
                binding.btnSelectFiles.isEnabled = false
                runOnUiThread {
                    MyUtils.appendLog(
                        binding.tvUploadLog,
                        "准备复制文件... $start"
                    )
                }

                try {
                    // 并发复制所有文件，避免一个个阻塞UI
                    val files = withContext(Dispatchers.IO) {
                        uris.map { uri ->
                            async {
                                uri.toFileOrNull(applicationContext)  // 复制操作，参考你之前的toFileOrNull函数
                            }
                        }.awaitAll().filterNotNull()
                    }

                    runOnUiThread {
                        MyUtils.appendLog(
                            binding.tvUploadLog,
                            "复制完成，开始上传... ${System.currentTimeMillis()} = ${System.currentTimeMillis() - start}"
                        )
                    }


                    if (files.isNotEmpty()) {
                        uploadFilesInternal(files)
                    } else {
                        runOnUiThread {
                            MyUtils.appendLog(
                                binding.tvUploadLog,
                                "未能转换文件 ${System.currentTimeMillis()}"
                            )
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        MyUtils.appendLog(
                            binding.tvUploadLog,
                            "复制文件失败 ${System.currentTimeMillis()}"
                        )
                    }
                } finally {
                    binding.btnSelectFiles.isEnabled = true
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnSelectFiles.setOnClickListener {
            fileLauncher.launch(arrayOf("*/*"))
        }
    }

    private suspend fun uploadFilesInternal(files: List<File>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = ProgressRequestBody(files, object : UploadProgressListener {
                    private var lastProgress = -1
                    override fun onProgress(percent: Int) {
                        if (percent != lastProgress) {
                            lastProgress = percent
                            runOnUiThread {
                                binding.progressBar.progress = percent
                                binding.tvProgress.text = "上传进度: $percent%"
                            }
                        }
                    }
                })

                val multipart =
                    MultipartBody.Part.createFormData("files", "upload.bin", requestBody)

                val start = System.currentTimeMillis()
                runOnUiThread {
                    MyUtils.appendLog(
                        binding.tvUploadLog,
                        "开始上传文件 $start"
                    )
                }

                val response = RetrofitClient.api.uploadFiles(files = multipart).execute()

                runOnUiThread {
                    if (response.isSuccessful) {
                        MyUtils.appendLog(
                            binding.tvUploadLog,
                            "上传结束 ${System.currentTimeMillis()} = ${System.currentTimeMillis() - start}"
                        )
                    } else {
                        MyUtils.appendLog(
                            binding.tvUploadLog,
                            "上传失败 ${System.currentTimeMillis()} = ${System.currentTimeMillis() - start}"
                        )
                    }
                }
                response.isSuccessful
            } catch (e: Exception) {
                runOnUiThread {
                    MyUtils.appendLog(
                        binding.tvUploadLog,
                        "上传异常: ${e.message} ${System.currentTimeMillis()}"
                    )
                }
                false
            }
        }
    }
}


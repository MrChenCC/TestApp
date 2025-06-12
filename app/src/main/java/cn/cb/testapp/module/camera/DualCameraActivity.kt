package cn.cb.testapp.module.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import cn.cb.testapp.R

class DualCameraActivity : AppCompatActivity() {

    private lateinit var previewViewBack: PreviewView
    private lateinit var previewViewFront: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mian_java)

        previewViewBack = findViewById(R.id.previewView1)
        previewViewFront = findViewById(R.id.previewView2)

        if (allPermissionsGranted()) {
            startDualCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startDualCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 获取后置摄像头
            val backCameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            // 获取前置摄像头
            val frontCameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

            val previewBack = Preview.Builder().build()
            val previewFront = Preview.Builder().build()

            previewBack.surfaceProvider = previewViewBack.surfaceProvider
            previewFront.surfaceProvider = previewViewFront.surfaceProvider

            try {
                cameraProvider.unbindAll()

                // 分别绑定两个摄像头
                cameraProvider.bindToLifecycle(this, backCameraSelector, previewBack)
                cameraProvider.bindToLifecycle(this, frontCameraSelector, previewFront)

            } catch (e: Exception) {
                Log.e("DualCamera", "Use case binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startDualCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}

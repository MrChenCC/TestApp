package cn.cb.testapp.module.usbstorage

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.renderscript.ScriptGroup.Binding
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import cn.cb.testapp.R
import cn.cb.testapp.common.log.LogFileManager
import cn.cb.testapp.databinding.ActivityMainBinding
import cn.cb.testapp.databinding.ActivityUsbMonitorBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.jahnen.libaums.core.UsbMassStorageDevice
import me.jahnen.libaums.core.fs.UsbFile
import me.jahnen.libaums.core.fs.UsbFileInputStream
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class UsbMonitorActivity : AppCompatActivity() {

    private val binding: ActivityUsbMonitorBinding by lazy {
        ActivityUsbMonitorBinding.inflate(layoutInflater)
    }
    private val logTextView: TextView by lazy { binding.logTextView }
    private val nestedScrollView: NestedScrollView by lazy { binding.nestedScrollView }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val action = intent.action
            val logBuilder = StringBuilder()

            val description = when (action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> "USB设备已插入"
                UsbManager.ACTION_USB_DEVICE_DETACHED -> "USB设备已拔出"
                UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> "USB accessory 已插入"
                UsbManager.ACTION_USB_ACCESSORY_DETACHED -> "USB accessory 已移除"
                "android.hardware.usb.action.USB_STATE" -> "USB连接状态变化（连接电脑/MTP等）"
                Intent.ACTION_MEDIA_MOUNTED -> "外部存储设备挂载"
                Intent.ACTION_MEDIA_UNMOUNTED -> "外部存储设备已卸载"
                Intent.ACTION_MEDIA_REMOVED -> "外部存储设备已移除"
                Intent.ACTION_MEDIA_BAD_REMOVAL -> "外部存储设备未正常移除"
                Intent.ACTION_MEDIA_CHECKING -> "外部存储设备正在检查"
                Intent.ACTION_MEDIA_EJECT -> "外部存储设备被请求卸载"
                Intent.ACTION_POWER_CONNECTED -> "电源已连接（可能为USB供电）"
                Intent.ACTION_POWER_DISCONNECTED -> "电源已断开"
                else -> "未知USB相关广播"
            }

            logBuilder.append("广播类型: $action\n")
            logBuilder.append("中文说明: $description\n")

            intent.extras?.keySet()?.forEach { key ->
                val value = intent.extras?.get(key)
                logBuilder.append("  Extra: $key = $value\n")
            }

            val log = logBuilder.toString()
            Timber.tag("UsbMonitor").d(log)
            showLog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //checkAndRequestPermissions()

        val filter = IntentFilter().apply {
            // USB 插拔
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)

            // 外部存储挂载状态
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            addAction(Intent.ACTION_MEDIA_CHECKING)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addDataScheme("file")
        }

        registerReceiver(usbReceiver, filter)
        Timber.tag("UsbMonitor").d("注册广播")

        binding.btn.setOnClickListener {
            val logFile = File(LogFileManager.getInstance(this).getCurrentFilePath())
            if (logFile.exists()) logFile.delete()
            showLog()
        }

        binding.usb.setOnClickListener {
            Timber.tag("UsbMonitor").d("获取挂载USB存储路径：${getUsbStoragePath()}")
            showLog()
        }

        binding.scan.setOnClickListener {
            Timber.tag("UsbMonitor").d("发广播通知系统重新扫描存储设备")
            scanStorage()
            showLog()
        }

        binding.cable.setOnClickListener {
            Timber.tag("UsbMonitor").d("反射获取所有存储路径")
            getUsbStoragePathByReflection()
            showLog()
        }

        binding.unmount.setOnClickListener {
            Timber.tag("UsbMonitor").d("卸载USB存储")
            unmountAllUsbStorage(this)
            showLog()
        }

        binding.mount.setOnClickListener {
            Timber.tag("UsbMonitor").d("shell脚本挂载USB存储")
            mountUsbDevice()
            showLog()
        }

        binding.allUsb.setOnClickListener {
            Timber.tag("UsbMonitor").d("查询所有USB存储设备")
            getAllUsbDevices(this)
            showLog()
        }

        binding.initUSB.setOnClickListener {
            Timber.tag("UsbMonitor").d("使用第三方库读取U盘")
            accessUsbDevice(this)
            showLog()
        }

        binding.copyToLocal.setOnClickListener {
            Timber.tag("UsbMonitor").d("复制U盘文件到本地")
            copy()
            showLog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        Timber.tag("UsbMonitor").d("卸载广播")
    }

    override fun onResume() {
        super.onResume()
        showLog()
    }

    private fun showLog() {
        runOnUiThread {
            logTextView.text = readLog(this)
            nestedScrollView.post { nestedScrollView.fullScroll(NestedScrollView.FOCUS_DOWN) }
        }
    }

    private fun readLog(context: Context): String {
        val logFile = File(LogFileManager.getInstance(context).getCurrentFilePath())
        return if (logFile.exists()) logFile.readText() else ""
    }

    private fun getUsbStoragePath(): String? {
        try {
            val file = File("/proc/mounts")
            if (!file.exists()) return null

            file.bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (
                        (line.contains("vfat")
                                || line.contains("exfat")
                                || line.contains("sdcardfs")
                                || line.contains("fuse"))
                        && (line.contains("/mnt/")
                                || line.contains("/storage/"))
                        && (line.contains("usb", ignoreCase = true)
                                || line.contains("udisk", ignoreCase = true))
                    ) {
//                        val parts = line.split(" ")
//                        if (parts.size >= 2) {
//                            return parts[1] // 返回挂载路径
//                        }
                        return line
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun scanStorage() {
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.data = Uri.fromFile(File("/storage"))
        sendBroadcast(intent)
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun getUsbStoragePathByReflection(): String? {
        try {
            val storageManager = getSystemService(STORAGE_SERVICE) as StorageManager
            val volumeListMethod = storageManager.javaClass.getDeclaredMethod("getVolumeList")
            volumeListMethod.isAccessible = true
            val volumes = volumeListMethod.invoke(storageManager) as Array<*>

            val sb = StringBuilder()
            for (volume in volumes) {
                val path = volume?.javaClass?.getMethod("getPath")?.invoke(volume) as String
                val state = volume.javaClass.getMethod("getState").invoke(volume) as String
                val isRemovable =
                    volume.javaClass.getMethod("isRemovable").invoke(volume) as Boolean

                sb.append("[Path=$path, State=$state, Removable=$isRemovable]").appendLine()
            }
            Timber.tag("UsbMonitor").d(sb.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun unmountAllUsbStorage(context: Context): Boolean {
        try {
            val storageManager = context.getSystemService(STORAGE_SERVICE) as StorageManager
            val storageVolumeClass = Class.forName("android.os.storage.StorageVolume")
            val getVolumeListMethod = StorageManager::class.java.getMethod("getVolumeList")
            val volumes = getVolumeListMethod.invoke(storageManager) as Array<*>

            val getPathMethod = storageVolumeClass.getMethod("getPath")
            val getStateMethod = storageVolumeClass.getMethod("getState")
            val isRemovableMethod = storageVolumeClass.getMethod("isRemovable")

            val unmountMethod = StorageManager::class.java.getMethod(
                "unmount",
                String::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )

            var success = false
            for (volume in volumes) {
                val path = getPathMethod.invoke(volume) as? String ?: continue
                val state = getStateMethod.invoke(volume) as? String ?: continue
                val removable = isRemovableMethod.invoke(volume) as? Boolean ?: continue

                if (removable && state == Environment.MEDIA_MOUNTED) {
                    // 只卸载已挂载的可移除设备
                    unmountMethod.invoke(storageManager, path, true, false)
                    Timber.tag("USB_UNMOUNT_ALL").d("已卸载: $path")
                    success = true
                }
            }
            return success
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun getAllUsbDevices(context: Context) {
        val usbManager = context.getSystemService(USB_SERVICE) as UsbManager
        val list = usbManager.deviceList.values.toList()
        val sb = StringBuilder("共 ${list.size} 个设备:").appendLine()

        for (device in list) {
            val deviceType = when (device.deviceClass) {
                UsbConstants.USB_CLASS_COMM -> "串口通信类"
                UsbConstants.USB_CLASS_HID -> "HID设备：鼠标或键盘"
                UsbConstants.USB_CLASS_MASS_STORAGE -> "存储设备"
                UsbConstants.USB_CLASS_CDC_DATA -> "串口类设备"
                UsbConstants.USB_CLASS_VENDOR_SPEC -> "厂商自定义"
                else -> "未知/复合设备"
            }
            sb.append("设备名称: ${device.deviceName}, 厂商ID: ${device.vendorId}, 产品ID: ${device.productId}")
                .appendLine()
                .append("设备级别类型（声明式/但不可靠）: $deviceType")
                .appendLine()
                .append(
                    "接口级别类型（实际用途/识别真实功能）:(${device.interfaceCount}) ${
                        getInterfaceType(device)
                    }"
                )
                .appendLine()
                .appendLine()
        }
        Timber.tag("allUsb").d(sb.toString())
    }

    private fun getInterfaceType(device: UsbDevice): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            when (intf.interfaceClass) {
                UsbConstants.USB_CLASS_MASS_STORAGE -> list.add("存储设备")
                UsbConstants.USB_CLASS_HID -> list.add("HID设备：鼠标或键盘")
                UsbConstants.USB_CLASS_COMM -> list.add("串口通信类")
                UsbConstants.USB_CLASS_AUDIO -> list.add("音频设备")
                UsbConstants.USB_CLASS_VIDEO -> list.add("摄像头")
                else -> list.add("其他类型：" + intf.interfaceClass)
            }
        }
        return list
    }

    private fun accessUsbDevice(context: Context) {
        val usbManager = context.getSystemService(USB_SERVICE) as UsbManager
        val list = usbManager.deviceList.values.toList()
        var storageDevice: UsbDevice? = null
        for (device in list) {
            for (i in 0 until device.interfaceCount) {
                when (device.getInterface(i).interfaceClass) {
                    UsbConstants.USB_CLASS_MASS_STORAGE -> storageDevice = device
                }
            }
        }
        if (storageDevice == null) {
            Timber.tag("USB").d("未找到存储设备")
            return
        }
        // 获取 UsbMassStorageDevice 列表
        val massStorageDevices = UsbMassStorageDevice.getMassStorageDevices(context)
        for (massStorageDevice in massStorageDevices) {
            if (massStorageDevice.usbDevice == storageDevice) {
                try {
                    // 打开设备并初始化
                    massStorageDevice.init()

                    // 获取分区
                    val partition = massStorageDevice.partitions[0]
                    val fileSystem = partition.fileSystem
                    val root = fileSystem.rootDirectory

                    listFilesRecursively(root)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun listFilesRecursively(dir: UsbFile, depth: Int = 0) {
        val indent = "  ".repeat(depth)
        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                Timber.tag("USB").d("$indent[Dir] ${file.absolutePath} ${file.name}")
                listFilesRecursively(file, depth + 1)
            } else {
                Timber.tag("USB")
                    .d("$indent[File] ${file.absolutePath} ${file.name} - ${file.length} bytes")
            }
        }
    }

    private fun mountUsbDevice() {
        val command = """
        for block in /dev/block/sd*; do
            fs_type=\$(blkid ${'$'}block | sed -n 's/.*TYPE="\([^"]*\)".*/\1/p')
            case "${'$'}fs_type" in
                vfat|exfat|ntfs|ext4)
                    mount_point="/mnt/media_rw/usb1"
                    mkdir -p "${'$'}mount_point"
                    mount -t "${'$'}fs_type" -o utf8 ${'$'}block ${'$'}mount_point
                    echo "Mounted ${'$'}block at ${'$'}mount_point"
                    ;;
            esac
        done
    """.trimIndent().replace("\n", "; ")

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor()
            val result = process.inputStream.bufferedReader().readText()
            Timber.tag("USB_MOUNT").d("Result: $result")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copy() {
        val massStorageDevices = UsbMassStorageDevice.getMassStorageDevices(this)
        val device = massStorageDevices.firstOrNull() ?: return
        try {
            device.init()
            val partition = device.partitions[0]
            partition.fileSystem.rootDirectory.listFiles().forEach {
                if (it.isDirectory.not()) {
                    val file = File(externalCacheDir, it.name)
                    it.copyToLocalWithProgress(
                        localFile = file,
                        onProgress = { bytesCopied, totalBytes ->
                            if (bytesCopied == totalBytes) {
                                Timber.tag("USB_COPY").d("Copy completed [${file.absolutePath}]")
                            }
                        }
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun UsbFile.copyToLocalWithProgress(
        localFile: File,
        bufferSize: Int = 8192,
        onProgress: (bytesCopied: Long, totalBytes: Long) -> Unit
    ) {
        UsbFileInputStream(this).use { input ->
            localFile.outputStream().use { output ->
                val buffer = ByteArray(bufferSize)
                var bytesCopied = 0L
                val totalBytes = length

                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break

                    output.write(buffer, 0, read)
                    bytesCopied += read
                    onProgress(bytesCopied, totalBytes)
                }
            }
        }
        localFile.setLastModified(lastModified())
    }

}
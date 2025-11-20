package cn.cb.testapp.module.netcamera
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import cn.cb.testapp.databinding.ActivityNetCameraBinding

class NetCameraActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    // 替换为你的摄像头 RTSP 地址
    private val rtspUrl = "rtsp://admin:Bossien1@192.168.1.161:554/stream"

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityNetCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerView = binding.playerView

        // 初始化 ExoPlayer
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer

            val mediaItem = MediaItem.Builder()
                .setUri(rtspUrl.toUri())
                .setMimeType("application/x-rtsp") // 告诉 Media3 这是 RTSP
                .build()

            val mediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(true) // 强制 TCP，避免 UDP 丢包
                .createMediaSource(mediaItem)

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    override fun onStart() {
        super.onStart()
        player?.play()
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
package cn.cb.testapp.module.videovoice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.cb.testapp.R;

public class VideoVoiceActivity extends ComponentActivity {
    private PreviewView previewView;
    private Button buttonRecordVideo, buttonRecordAudio;
    private ProcessCameraProvider cameraProvider;
    private VideoCapture<Recorder> videoCapture;
    private Recording videoRecording;
    private MediaRecorder audioRecorder;
    private boolean isVideoRecording = false;
    private boolean isAudioRecording = false;
    private File audioOutputFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_voice);

        previewView = findViewById(R.id.textureView);
        buttonRecordVideo = findViewById(R.id.btnVideoRecord);
        buttonRecordAudio = findViewById(R.id.btnAudioRecord);

        // 检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 0);
        } else {
            startCamera();
        }

        // 音视频录制按钮
        buttonRecordVideo.setOnClickListener(v -> {
            if (isVideoRecording) {
                stopVideoRecording();
            } else {
                startVideoRecording();
            }
        });

        // 音频录制按钮
        buttonRecordAudio.setOnClickListener(v -> {
            if (isAudioRecording) {
                stopAudioRecording();
            } else {
                startAudioRecording();
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("MissingPermission")
    private void startVideoRecording() {
        if (videoCapture == null) {
            return;
        }
        buttonRecordVideo.setText("结束音视频录制");
        isVideoRecording = true;

        MediaStoreOutputOptions outputOptions = new MediaStoreOutputOptions.Builder(getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(getContentValues("video/mp4"))
                .build();
        videoRecording = videoCapture.getOutput().prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        if (((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                            Toast.makeText(this, "音视频录制失败", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "音视频录制成功", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void stopVideoRecording() {
        if (videoRecording != null) {
            videoRecording.stop();
            videoRecording = null;
        }
        buttonRecordVideo.setText("开始音视频录制");
        isVideoRecording = false;
    }

    @SuppressLint("MissingPermission")
    private void startAudioRecording() {
        buttonRecordAudio.setText("结束音频录制");
        isAudioRecording = true;

        audioRecorder = new MediaRecorder();
        audioOutputFile = getOutputFile("audio", ".aac");

        try {
            audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            audioRecorder.setOutputFile(audioOutputFile.getAbsolutePath());
            audioRecorder.prepare();
            audioRecorder.start();
        } catch (IOException e) {
            Log.e("MediaRecorder", "Error starting audio recording", e);
        }
    }

    private void stopAudioRecording() {
        if (audioRecorder != null) {
            try {
                audioRecorder.stop();
            } catch (RuntimeException e) {
                Log.e("MediaRecorder", "Error stopping audio", e);
            }
            audioRecorder.reset();
            audioRecorder.release();
            audioRecorder = null;
            Toast.makeText(this, "Audio saved: " + audioOutputFile, Toast.LENGTH_SHORT).show();
        }
        buttonRecordAudio.setText("开始音频录制");
        isAudioRecording = false;
    }

    private File getOutputFile(String prefix, String extension) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String fileName = prefix + "_" + timeStamp + extension;
        return new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName);
    }

    private ContentValues getContentValues(String mimeType) {
        String name = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/CameraX");
        }
        return contentValues;
    }
}
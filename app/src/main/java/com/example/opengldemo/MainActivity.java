package com.example.opengldemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.opengldemo.transition.TransitionVideoRender;
import com.example.opengldemo.transition.encoder.GLMovieRecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ArrayList<String> pathList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            test();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1) {
            test();
        }
    }

    private void test() {

        GLSurfaceView glSurfaceView = findViewById(R.id.gl_surface_view);

        pathList = new ArrayList<>();
        pathList.add("/storage/emulated/0/DCIM/Camera/1.jpg");
        pathList.add("/storage/emulated/0/DCIM/Camera/2.jpg");
        pathList.add("/storage/emulated/0/DCIM/Camera/3.jpg");

        final TransitionVideoRender render = new TransitionVideoRender(this, pathList);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(render);
        findViewById(R.id.btn_make_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encoderVideo();
            }
        });
    }

    private void encoderVideo() {

        //生成一个全新的MovieRender，不然与现有的GL环境不一致，相互干扰容易出问题
        final TransitionVideoRender render = new TransitionVideoRender(this, pathList);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm:ss", Locale.ENGLISH);
        String format = simpleDateFormat.format(new Date());
        File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "movie-" + format + ".mp4");

        GLMovieRecorder recorder = new GLMovieRecorder(this);
        recorder.setDataSource(render);
        recorder.setMusic("/storage/emulated/0/DCIM/Camera/audio.mp3");
        recorder.configOutput(720, 1080, 10000000, 30, 1, outputFile.getAbsolutePath());
        recorder.startRecord(new GLMovieRecorder.OnRecordListener() {
            @Override
            public void onRecordFinish(boolean success) {
                Log.d(TAG, "onRecordFinish");
            }

            @Override
            public void onRecordProgress(long recordedDuration, long totalDuration) {
                Log.d(TAG, "onRecordProgress: " + (recordedDuration / totalDuration));
            }
        });
    }
}
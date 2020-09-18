package com.example.opengldemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.opengldemo.transition.TransitionVideoRender;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

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

        ArrayList<String> pathList = new ArrayList<>();
        pathList.add("/storage/emulated/0/DCIM/update/image1.jpg");
        pathList.add("/storage/emulated/0/DCIM/update/image2.jpg");
        pathList.add("/storage/emulated/0/DCIM/update/image3.jpg");

        final TransitionVideoRender render = new TransitionVideoRender(this, pathList);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(render);
        findViewById(R.id.btn_make_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                render.startMakeVideo();
            }
        });
    }
}
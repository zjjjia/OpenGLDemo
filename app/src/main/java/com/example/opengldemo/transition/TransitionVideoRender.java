package com.example.opengldemo.transition;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.math.BigDecimal;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.example.opengldemo.transition.IDrawer.ONE_BILLION;
import static com.example.opengldemo.transition.TransitionDrawer.TransitionType.PUSH_AWAY;
import static com.example.opengldemo.transition.TransitionDrawer.TransitionType.ZOOM_IN;

/**
 * @author : Jiabo
 * @date : 2020/9/8
 * @decription : 图片合成转场视频的Render
 */
public class TransitionVideoRender implements GLSurfaceView.Renderer {

    private static final String TAG = "TransitionVideoRender";
    private final Context mContext;
    private ArrayList<IDrawer> mDrawerList;
    private ArrayList<Bitmap> mBitmapList;
    private ArrayList<String> mImgPathList;

    private boolean isPause = false;
    private long[] timeSections;
    private long mCurPlayTime;
    private long totalDuration;
    private int frameIndex;
    private float mTransitionProgress;

    private MovieEngine mEngine;

    public TransitionVideoRender(Context context) {
        mContext = context;
        mDrawerList = new ArrayList<>();
        mBitmapList = new ArrayList<>();
    }

    public TransitionVideoRender(Context context, ArrayList<String> imgPathList) {
        mContext = context;
        mDrawerList = new ArrayList<>();
        mBitmapList = new ArrayList<>();

        mImgPathList = imgPathList;
    }

    public void reload(ArrayList<String> imgPathList) {
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        frameIndex = 0;
        totalDuration = 0;
        mCurPlayTime = 0;
        mTransitionProgress = 0;
        mBitmapList.clear();
        timeSections = null;
        mImgPathList = imgPathList;
    }

    public void startMakeVideo() {
        mEngine.make();
    }

    public void release() {
        mEngine.quit();
    }

    public void pause() {
        isPause = true;
    }

    public void start() {
        if (mCurPlayTime >= totalDuration) {
            mCurPlayTime = 0;
            frameIndex = 0;
        }
        isPause = false;
    }

    public boolean isPlaying() {
        return !isPause;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

    }

    @SuppressLint("CheckResult")
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged");
        GLES20.glViewport(0, 0, width, height);
        initDrawerList();
        initMovieEngine();
        for (int i = 0; i < mDrawerList.size(); i++) {
            IDrawer drawer = mDrawerList.get(i);
            GLES20.glViewport(0, 0, width, height);
            drawer.setWorldSize(width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (isPause) {
            return;
        }
        if (mCurPlayTime <= (totalDuration + ONE_BILLION / 30)) {
            generateFrame(mCurPlayTime);
            long presentationTimeNsec = computePresentationTimeNsec(frameIndex);
            frameIndex++;
            mCurPlayTime = presentationTimeNsec;
        } else {
            isPause = true;
        }
    }

    //fps 30
    private long computePresentationTimeNsec(int frameIndex) {
        return frameIndex * ONE_BILLION / 30;
    }

    private void generateFrame(long tempTime) {
        int movieIndex = 0;
        boolean find = false;
        for (int i = 0; i < timeSections.length; i++) {
            if (i + 1 < timeSections.length && tempTime >= timeSections[i] && tempTime < timeSections[i + 1]) {
                find = true;
                mDrawerList.get(movieIndex).release();
                movieIndex = i;
                mTransitionProgress = 0.0f;
                break;
            }
        }
        if (!find) {
            mDrawerList.get(movieIndex).release();
            movieIndex = timeSections.length - 1;
            mTransitionProgress = 0.0f;
        }
        long curTime = tempTime - timeSections[movieIndex];
        IDrawer drawer = mDrawerList.get(movieIndex);
        calculateProgress(mDrawerList.get(movieIndex).getDurationAsNano(), curTime);
        drawer.setProgress(mTransitionProgress);
        drawer.draw(false);
    }

    /**
     * 计算转场效果的进度
     *
     * @param duration 当前drawer的播放的时长
     * @param curTime  当前drawer已播放时长
     */
    private void calculateProgress(long duration, long curTime) {
        float progress = (float) curTime / duration;
        BigDecimal bigDecimal = new BigDecimal(progress);
        mTransitionProgress = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    private void initMovieEngine() {
        mEngine = new MovieEngine.MovieBuilder()
                .maker(generaDrawerData())
                .width(mBitmapList.get(0).getWidth())
                .height(mBitmapList.get(0).getHeight())
                .listener(new MovieEngine.ProgressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onCompleted(String path) {
                        Log.d(TAG, "onCompleted: " + path);
                    }

                    @Override
                    public void onProgress(long current, long totalDuration) {
                    }
                })
                .build();

    }

    private void initDrawerList() {

        mDrawerList = null;
        mDrawerList = generaDrawerData();

        //计算时长
        if (timeSections == null) {
            timeSections = new long[mDrawerList.size()];
            for (int i = 0; i < mDrawerList.size(); i++) {
                timeSections[i] = totalDuration;
                totalDuration += mDrawerList.get(i).getDurationAsNano();
            }
        }
    }

    private ArrayList<IDrawer> generaDrawerData() {

        mBitmapList.clear();
        for (int i = 0; i < mImgPathList.size(); i++) {
            Bitmap bitmap = rotateBitmap(BitmapHelper.decodeBitmap(720, mImgPathList.get(i)));
            mBitmapList.add(bitmap);
        }
        ArrayList<IDrawer> drawerList = new ArrayList<>();

        Bitmap bitmap1 = mBitmapList.get(0);
        Bitmap bitmap2 = mBitmapList.get(1);
        Bitmap bitmap3 = mBitmapList.get(2);

        int textureId1;
        int textureId2;

        textureId1 = TextureHelper.loadTextureByBitmap(bitmap1);
        drawerList.add(new ImageDrawer(bitmap1, textureId1));

        textureId1 = TextureHelper.loadTextureByBitmap(bitmap1);
        textureId2 = TextureHelper.loadTextureByBitmap(bitmap2);
        drawerList.add(new TransitionDrawer(mContext, ZOOM_IN, textureId1, textureId2));

        textureId1 = TextureHelper.loadTextureByBitmap(bitmap2);
        drawerList.add(new ImageDrawer(bitmap2, textureId1));

        textureId1 = TextureHelper.loadTextureByBitmap(bitmap2);
        textureId2 = TextureHelper.loadTextureByBitmap(bitmap3);
        drawerList.add(new TransitionDrawer(mContext, PUSH_AWAY, textureId1, textureId2));

        textureId1 = TextureHelper.loadTextureByBitmap(bitmap2);
        drawerList.add(new ImageDrawer(bitmap3, textureId1));


        int width = bitmap1.getWidth();
        int height = bitmap1.getHeight();
        for (int i = 0; i < drawerList.size(); i++) {
            drawerList.get(i).setVideoSize(width, height);
        }

        return drawerList;
    }

    private Bitmap rotateBitmap(Bitmap bitmap) {
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static int getExternalOESTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }
}

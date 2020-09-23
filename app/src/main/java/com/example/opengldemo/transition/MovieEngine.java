package com.example.opengldemo.transition;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.example.opengldemo.transition.ref.VideoEncoderCore;
import com.example.opengldemo.transition.ref.gles.EglCore;
import com.example.opengldemo.transition.ref.gles.WindowSurface;

import java.io.File;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.example.opengldemo.transition.IDrawer.ONE_BILLION;

public class MovieEngine extends HandlerThread {

    private static final String TAG = "MovieEngine";

    private final UiHandler uiHandler;
    int width;
    int height;
    int bitRate;
    File outputFile;
    ProgressListener mListener;
    private EglCore mEglCore;
    private WindowSurface mWindowSurface;
    private MovieHandler mMovieHandler;
    private VideoEncoderCore mVideoEncoder;
    private long[] timeSections;
    private boolean stop = false;

    private ArrayList<IDrawer> mDrawerList;
    private float mTransitionProgress = 0.0f;

    private MovieEngine() {
        super("MovieEngine-thread");
        uiHandler = new UiHandler();
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        //初始化handler
        getMovieHandler();
    }

    private MovieHandler getMovieHandler() {
        if (mMovieHandler == null) {
            mMovieHandler = new MovieHandler(getLooper(), this);
        }
        return mMovieHandler;
    }

    public void make() {
        synchronized (this) {
            getMovieHandler().sendEmptyMessage(MovieHandler.MSG_MAKE_MOVIES);
        }
    }

    public void setMovieProgress(ProgressListener listener) {
        mListener = listener;
    }

    private void makeMovie() {
        Log.d(TAG, "makeMovie");
        //不断绘制。
        boolean isCompleted = false;
        try {
            //初始化GL环境
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);

            mVideoEncoder = new VideoEncoderCore(width, height, bitRate, outputFile);
            Surface encoderInputSurface = mVideoEncoder.getInputSurface();
            mWindowSurface = new WindowSurface(mEglCore, encoderInputSurface, true);
            mWindowSurface.makeCurrent();

            //绘制
//            计算时长
            long totalDuration = 0;

            timeSections = new long[mDrawerList.size()];
            for (int i = 0; i < mDrawerList.size(); i++) {
                IDrawer drawer = mDrawerList.get(i);

                timeSections[i] = totalDuration;
                totalDuration += drawer.getDurationAsNano();
            }
            if (mListener != null) {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onStart();
                    }
                });
            }
            long tempTime = 0;
            int frameIndex = 0;
            while (tempTime <= totalDuration + ONE_BILLION / 30) {
                mVideoEncoder.drainEncoder(false);
                generateFrame(tempTime);
                long presentationTimeNsec = computePresentationTimeNsec(frameIndex);
                submitFrame(presentationTimeNsec);
                updateProgress(tempTime, totalDuration);
                frameIndex++;
                tempTime = presentationTimeNsec;

                if (stop) {
                    break;
                }
            }
            //finish
            mVideoEncoder.drainEncoder(true);
            isCompleted = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //结束
            try {
                releaseEncoder();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (isCompleted && mListener != null) {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onCompleted(outputFile.getAbsolutePath());
                    }
                });
            }
        }

    }

    private void updateProgress(final long tempTime, final long totalDuration) {
        if (mListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onProgress(tempTime, totalDuration);
                }
            });
        }
    }

    private void submitFrame(long presentationTimeNsec) {
        mWindowSurface.setPresentationTime(presentationTimeNsec);
        mWindowSurface.swapBuffers();
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
                movieIndex = i;
                mTransitionProgress = 0.0f;
                break;
            }
        }
        if (!find) {
            movieIndex = timeSections.length - 1;
            mTransitionProgress = 0.0f;
        }
        long curTime = tempTime - timeSections[movieIndex];
        IDrawer drawer = mDrawerList.get(movieIndex);
        calculateProgress(mDrawerList.get(movieIndex).getDurationAsNano(), curTime);
        drawer.setProgress(mTransitionProgress);
        drawer.draw(true);
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

    private void releaseEncoder() {
        mVideoEncoder.release();
        if (mWindowSurface != null) {
            mWindowSurface.release();
            mWindowSurface = null;
        }

        for (int i = 0; i < mDrawerList.size(); i++) {
            mDrawerList.get(i).release();
        }

        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        mDrawerList.clear();
    }

    @Override
    public boolean quit() {
        stop = true;

        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        return super.quit();
    }

    public interface ProgressListener {

        void onStart();

        void onCompleted(String path);

        void onProgress(long current, long totalDuration);

    }

    public static class MovieBuilder {
        private int width = 1280;
        private int height = 720;
        private int bitRate = 10000000;
        private File outputFile;
        private ProgressListener listener;
        private ArrayList<IDrawer> drawerList;

        public MovieBuilder width(int width) {
            this.width = width;
            return this;
        }

        public MovieBuilder height(int height) {
            this.height = height;
            return this;
        }

        public MovieBuilder bitRate(int bitRate) {
            this.bitRate = bitRate;
            return this;
        }

        public MovieBuilder outputFile(File outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public MovieBuilder listener(ProgressListener listener) {
            this.listener = listener;
            return this;
        }


        public MovieBuilder maker(ArrayList<IDrawer> drawerList) {
            this.drawerList = drawerList;
            return this;
        }

        public MovieEngine build() {
            MovieEngine engine = new MovieEngine();

            engine.width = width;
            engine.height = height;
            engine.bitRate = bitRate;

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm:ss", Locale.ENGLISH);
            String format = simpleDateFormat.format(new Date());

            if (outputFile == null) {
                outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        "movie-" + format + ".mp4");
            }
            if (!outputFile.getParentFile().exists()) {
                boolean mkdir = outputFile.getParentFile().mkdir();
            }

            engine.outputFile = outputFile;
            engine.mListener = listener;
            engine.mDrawerList = drawerList;
            engine.start();
            return engine;
        }
    }

    public static class MovieHandler extends Handler {

        public static final int MSG_MAKE_MOVIES = 1;
        private final WeakReference<MovieEngine> engineRef;


        public MovieHandler(Looper looper, MovieEngine engine) {
            super(looper);
            engineRef = new WeakReference<>(engine);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            MovieEngine engine = engineRef.get();
            if (engine == null) {
                return;
            }
            switch (msg.what) {
                case MSG_MAKE_MOVIES:
                    engine.makeMovie();
                    break;
            }
        }
    }

    public static class UiHandler extends Handler {
        public UiHandler() {
            super(Looper.getMainLooper());
        }
    }
}

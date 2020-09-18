package com.example.opengldemo.transition;

import static com.example.opengldemo.transition.MatrixUtils.TYPE_FITXY;

/**
 * @author : Jiabo
 * @date : 2020/9/5
 * @decription :
 */
public abstract class IDrawer {

    public static long ONE_BILLION = 1000000000;

    //纹理id
    public int mTextureId1;
    public int mTextureId2;

    protected int mVideoWidth = -1;
    protected int mVideoHeight = -1;
    protected int mWorldWidth = -1;
    protected int mWorldHeight = -1;
    protected float[] mMatrix;

    public abstract void draw();

    public abstract long getDurationAsNano();

    public abstract void setProgress(float progress);

    public abstract void release();

    public void setVideoSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
    }

    public void setWorldSize(int width, int height) {
        mWorldWidth = width;
        mWorldHeight = height;
    }

    protected void initDefMatrix() {
        if (mMatrix != null) {
            return;
        }
        mMatrix = new float[16];
        MatrixUtils.getMatrix(mMatrix, TYPE_FITXY, mVideoWidth, mVideoHeight, mWorldWidth, mWorldHeight);

    }

}

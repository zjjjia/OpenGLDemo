package com.example.opengldemo.transition;

import android.opengl.Matrix;

import static com.example.opengldemo.transition.MatrixUtils.TYPE_FITXY;

/**
 * @author : Jiabo
 * @date : 2020/9/5
 * @decription :
 */
public abstract class IDrawer {

    private static final String TAG = "IDrawer";
    public static long ONE_BILLION = 1000000000;

    //纹理id
    public int mTextureId1;
    public int mTextureId2;

    protected int mVideoWidth = -1;
    protected int mVideoHeight = -1;
    protected int mWorldWidth = -1;
    protected int mWorldHeight = -1;
    protected float[] mMatrix;

    public void draw(boolean isMakeVideo) {
        initDefMatrix(isMakeVideo);
        //创建、编译并启动OpenGL着色器
        createGLPrg();
        //激活并绑定纹理单元
        activateTexture();
        //绑定图片到纹理单元
        bindBitmapToTexture();
        //开始渲染绘制
        doDraw();
    }

    public void setVideoSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
    }

    public void setWorldSize(int width, int height) {
        mWorldWidth = width;
        mWorldHeight = height;
    }

    protected void initDefMatrix(boolean isMakeVideo) {
        if (mMatrix != null) {
            return;
        }
        mMatrix = new float[16];
        if (isMakeVideo) {
            Matrix.setIdentityM(mMatrix, 0);
//            Matrix.rotateM(mMatrix, 0, 90, 1, 0, 0);
        } else {
            MatrixUtils.getMatrix(mMatrix, TYPE_FITXY, mVideoWidth, mVideoHeight, mWorldWidth, mWorldHeight);
        }
    }

    public abstract long getDurationAsNano();

    public abstract void setProgress(float progress);

    public abstract void release();

    protected abstract void createGLPrg();

    protected abstract void activateTexture();

    protected abstract void bindBitmapToTexture();

    protected abstract void doDraw();

}

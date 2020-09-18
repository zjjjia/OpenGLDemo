package com.example.opengldemo.transition;

import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLUtils;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @author : Jiabo
 * @date : 2020/9/5
 * @decription :
 */
public class ImageDrawer extends IDrawer {

    private final Bitmap mBitmap;
    //顶点坐标
    private float[] mVertexCoors = new float[]{
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
    };

    //纹理坐标
    private float[] mTextureCoors = new float[]{
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };

    //OpenGL程序ID
    private int mProgram = -1;

    //顶点坐标接受者
    private int mVertexPosHandler = -1;
    //纹理坐标接受者
    private int mTexturePosHandler = -1;
    //纹理接受者
    private int mTextureHandler = -1;
    //顶点着色器的矩阵变量
    private int mVertexMatrixHandler = -1;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;

    public ImageDrawer(Bitmap bitmap) {
        mBitmap = bitmap;
        initPos();
    }

    public ImageDrawer(Bitmap bitmap, int textureId) {
        mBitmap = bitmap;
        mTextureId1 = textureId;
        initPos();
    }

    private void initPos() {
        ByteBuffer bb = ByteBuffer.allocateDirect(mVertexCoors.length * 4);
        bb.order(ByteOrder.nativeOrder());
        //将坐标数据转换为FloatBuffer,用以传入OpenGL ES程序
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(mVertexCoors);
        mVertexBuffer.position(0);

        ByteBuffer cc = ByteBuffer.allocateDirect(mTextureCoors.length * 4);
        cc.order(ByteOrder.nativeOrder());
        mTextureBuffer = cc.asFloatBuffer();
        mTextureBuffer.put(mTextureCoors);
        mTextureBuffer.position(0);
    }


    @Override
    public void draw() {
        if (mTextureId1 != -1) {
            initDefMatrix();
            //创建、编译并启动OpenGL着色器
            createGLPrg();
            //激活并绑定纹理单元
            activateTexture();
            //绑定图片到纹理单元
            bindBitmapToTexture();
            //开始渲染绘制
            doDraw();
        }
    }

    @Override
    public void release() {
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTexturePosHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDeleteTextures(1, new int[]{mTextureId1}, 0);
        GLES20.glDeleteProgram(mProgram);
    }

    private void createGLPrg() {
        if (mProgram == -1) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexShader());
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShader());

            //创建OpenGL ES程序，注意：需要再OpenGL渲染线程中创建，否则无法渲染
            mProgram = GLES20.glCreateProgram();
            //将顶点着色器加入到程序
            GLES20.glAttachShader(mProgram, vertexShader);
            //将片元着色器加入到程序
            GLES20.glAttachShader(mProgram, fragmentShader);
            //连接到着色器程序
            GLES20.glLinkProgram(mProgram);

            mVertexPosHandler = GLES20.glGetAttribLocation(mProgram, "aPosition");
            mTexturePosHandler = GLES20.glGetAttribLocation(mProgram, "aCoordinate");
            mTextureHandler = GLES20.glGetUniformLocation(mProgram, "uTexture");
            mVertexMatrixHandler = GLES20.glGetUniformLocation(mProgram, "uMatrix");
        }

        GLES20.glUseProgram(mProgram);
    }

    private void activateTexture() {
        //激活指定纹理单元
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //绑定纹理ID到纹理单元
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId1);
        //将激活的纹理单元传递到着色器里面
        GLES20.glUniform1i(mTextureHandler, 0);
        //配置边缘过度参数
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private void bindBitmapToTexture() {
        if (!mBitmap.isRecycled()) {
            //绑定图片到被激活的纹理单元
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
        }
    }

    private void doDraw() {
        //启动顶点句柄
        GLES20.glEnableVertexAttribArray(mVertexPosHandler);
        GLES20.glEnableVertexAttribArray(mTexturePosHandler);

        GLES20.glUniformMatrix4fv(mVertexMatrixHandler, 1, false, mMatrix, 0);

        //设置着色器参数,第二个参数表示一个顶点包含的数据数量，这里为xy，所以为2
        GLES20.glVertexAttribPointer(mVertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glVertexAttribPointer(mTexturePosHandler, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    private int loadShader(int type, String shaderCode) {
        //根据type创建顶点着色器或者片元着色器
        int shader = GLES20.glCreateShader(type);
        //将资源加入到着色器中，并编译
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    @Override
    public long getDurationAsNano() {
        return 3 * ONE_BILLION;
    }


    private String getVertexShader() {
        return "attribute vec4 aPosition;" +
                "uniform mat4 uMatrix;" +
                "attribute vec2 aCoordinate;" +
                "varying vec2 vCoordinate;" +
                "void main() {" +
                "  gl_Position = aPosition * uMatrix;" +
                "  vCoordinate = aCoordinate;" +
                "}";
    }

    private String getFragmentShader() {
        return "precision mediump float;" +
                "uniform sampler2D uTexture;" +
                "varying vec2 vCoordinate;" +
                "void main() {" +
                "  vec4 color = texture2D(uTexture, vCoordinate);" +
                "  gl_FragColor = color;" +
                "}";
    }

    @Override
    public void setProgress(float progress) {

    }
}

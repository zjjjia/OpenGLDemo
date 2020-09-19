package com.example.opengldemo.transition;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * @author : Jiabo
 * @date : 2020/9/8
 * @decription : 绘制转场效果个drawer
 */

public class TransitionDrawer extends IDrawer {

    private static final String TAG = "TransitionDrawer";
    public enum TransitionType {
        CLOCKWISE_ROTATION, //顺时针旋转
        ANTICLOCKWISE_ROTATION,  //逆时针旋转
        ZOOM_IN,   //拉近
        PUSH_AWAY  //推远
    }

    private final Context mContext;
    private int mProgram;//自定义渲染管线着色器程序id
    private int muMVPMatrixHandle;//总变换矩阵引用
    private int maPositionHandle;//顶点位置属性引用
    private int maTexCoorHandle;//顶点纹理坐标属性引用
    private int mRotationHandle;//旋转方向属性值引用
    private int mZoomTypeHandle;//拉近和推远布尔值引用

    private int muProgressHandle;

    public int mTexture1;
    private int mTexture2;

    private FloatBuffer mVertexBuffer;//顶点坐标数据缓冲
    private FloatBuffer mTextureBuffer;//顶点着色器缓冲

    private int vCount;//顶点数量

    private float mProgress;
    private TransitionType mTransitionType;
    private float mRotation; //正数为顺时针，复数为逆时针
    private float isZoomIn; //0.0f：表示拉近；1.0f：表示推远


    public TransitionDrawer(Context context, TransitionType type, int textureId1, int textureId2) {
        mContext = context;
        mTextureId1 = textureId1;
        mTextureId2 = textureId2;

        mTransitionType = type;
        initTransitionType(type);
        initVertexData();
    }

    private void initTransitionType(TransitionType type) {
        switch (type) {
            case CLOCKWISE_ROTATION:
                mRotation = 1;
                break;
            case ANTICLOCKWISE_ROTATION:
                mRotation = -1;
                break;
            case ZOOM_IN:
                isZoomIn = 0.0f;
                //拉近的参数的设置
                break;
            case PUSH_AWAY:
                isZoomIn = 1.0f;
                //推远的参数设置
                break;
        }
    }

    @Override
    public void setProgress(float progress) {
        mProgress = progress;
    }

    @Override
    protected void createGLPrg() {
        initShader(mContext);
        //指定使用某套着色器程序
        GLES20.glUseProgram(mProgram);

        GLES20.glUniform1f(muProgressHandle, mProgress);
        GLES20.glUniform1f(mRotationHandle, mRotation);
        GLES20.glUniform1f(mZoomTypeHandle, isZoomIn);
    }

    @Override
    protected void activateTexture() {
        //绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId1);
        GLES20.glUniform1i(mTexture1, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId2);
        GLES20.glUniform1i(mTexture2, 1);

        //配置边缘过度参数
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    @Override
    protected void bindBitmapToTexture() {

    }

    @Override
    protected void doDraw() {
        //启用顶点位置数据数组
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        //启用顶点纹理坐标数据数组
        GLES20.glEnableVertexAttribArray(maTexCoorHandle);

        //将最终变换矩阵传入渲染管线
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMatrix, 0);
        //将顶点位置数据传入渲染管线
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, mVertexBuffer);
        //将纹理数据传入渲染管线
        GLES20.glVertexAttribPointer(maTexCoorHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, mTextureBuffer);

        //绘制纹理矩形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount);
    }

    @Override
    public long getDurationAsNano() {
        //一个转场动效持续1s
        return ONE_BILLION;
    }

    @Override
    public void release() {
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTexCoorHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDeleteTextures(2, new int[]{mTextureId1, mTextureId2}, 0);
        GLES20.glDeleteProgram(mProgram);
    }

    private void initVertexData() {
        //顶点坐标数据初始化
        vCount = 6; //每个格子两个三角形，每个三角形3个顶点
        float width = 2.0f;
        float height = 2.0f;
        float[] vertices =
                {
                        -width / 2, height / 2, 0,
                        -width / 2, -height / 2, 0,
                        width / 2, height / 2, 0,

                        -width / 2, -height / 2, 0,
                        width / 2, -height / 2, 0,
                        width / 2, height / 2, 0
                };
        //创建顶点坐标数据缓冲
        //vertices.length*4是因为一个整数四个字节
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());//设置字节顺序
        mVertexBuffer = vbb.asFloatBuffer();//转换为int型缓冲
        mVertexBuffer.put(vertices);//向缓冲区中放入顶点坐标数据
        mVertexBuffer.position(0);//设置缓冲区起始位置
        float[] textures =
                {
                        0f, 0f, 0f, 1, 1, 0f,
                        0f, 1, 1, 1, 1, 0f
                };
        //创建顶点纹理数据缓冲
        ByteBuffer tbb = ByteBuffer.allocateDirect(textures.length * 4);
        tbb.order(ByteOrder.nativeOrder());//设置字节顺序
        mTextureBuffer = tbb.asFloatBuffer();//转换为Float型缓冲
        mTextureBuffer.put(textures);//向缓冲区中放入顶点着色数据
        mTextureBuffer.position(0);//设置缓冲区起始位置
        //特别提示：由于不同平台字节顺序不同数据单元不是字节的一定要经过ByteBuffer
        //转换，关键是要通过ByteOrder设置nativeOrder()，否则有可能会出问题
        //顶点纹理数据的初始化================end============================
    }

    private void initShader(Context context) {
        //加载顶点着色器脚本内容
        //顶点着色器
        String vertexShader = TextResourceReader.readTextFileFromAsset(context, "transition_vertex.glsl");
        //片元着色器
        //加载片元着色器的脚本内容
        String fragmentShader = initFragmentShader(context);
        //基于顶点着色器与片元着色器创建程序
        mProgram = ShaderHelper.buildProgram(vertexShader, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        //获取程序中顶点位置属性引用
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        //获取程序中顶点纹理坐标属性引用
        maTexCoorHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoor");
        //获取程序中总变换矩阵id
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        muProgressHandle = GLES20.glGetUniformLocation(mProgram, "progress");
        mRotationHandle = GLES20.glGetUniformLocation(mProgram, "rotations");
        mZoomTypeHandle = GLES20.glGetUniformLocation(mProgram, "isZoomIn");

        mTexture1 = GLES20.glGetUniformLocation(mProgram, "sTexture1");
        mTexture2 = GLES20.glGetUniformLocation(mProgram, "sTexture2");

    }

    private String initFragmentShader(Context context) {
        String fragmentShader = null;
        switch (mTransitionType) {
            case CLOCKWISE_ROTATION:
            case ANTICLOCKWISE_ROTATION:
                fragmentShader = TextResourceReader.readTextFileFromAsset(context, "transition_rotation_fragment.glsl");
                break;
            case ZOOM_IN:
            case PUSH_AWAY:
                fragmentShader = TextResourceReader.readTextFileFromAsset(context, "transition_zoom_fragment.glsl");
                break;
        }
        return fragmentShader;
    }
}

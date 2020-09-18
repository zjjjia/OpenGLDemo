package com.example.opengldemo.transition;

import android.opengl.GLES20;
import android.util.Log;

/**
 * @author : Jiabo
 * @date : 2020/9/8
 * @decription :
 */
public class ShaderHelper {

    private static final String TAG = "ShaderHelper";

    public static int buildProgram(String vertexShaderSource, String fragmentShaderSource) {
        int program;

        Log.d(TAG, "vertex is: " + vertexShaderSource + "frag is " + fragmentShaderSource);
        int vertexShader = compileVertexShader(vertexShaderSource);
        int fragmentShader = compileFragmentShader(fragmentShaderSource);

        program = linkProgram(vertexShader, fragmentShader);

        validateProgram(program);
        return program;
    }

    public static boolean validateProgram(int programObjectId) {
        GLES20.glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0);

        Log.d(TAG, "validateProgram: " + validateStatus[0] + "\nLog" + GLES20.glGetProgramInfoLog(programObjectId));

        return validateStatus[0] != 0;
    }

    private static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        final int programObjectId = GLES20.glCreateProgram();
        if (programObjectId == 0) {
            Log.d(TAG, "linkProgram: could not create new program");
            return 0;
        }
        GLES20.glAttachShader(programObjectId, vertexShaderId);
        GLES20.glAttachShader(programObjectId, fragmentShaderId);
        GLES20.glLinkProgram(programObjectId);

        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_LINK_STATUS, linkStatus, 0);

        Log.d(TAG, "linkProgram: \n" + GLES20.glGetProgramInfoLog(programObjectId));
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(programObjectId);
            Log.e(TAG, "linkProgram: linking of program failed");
            return 0;
        }

        return programObjectId;
    }

    private static int compileFragmentShader(String shaderCode) {
        return compileShader(GLES20.GL_FRAGMENT_SHADER, shaderCode);
    }

    //编译顶点着色器
    private static int compileVertexShader(String shaderCode) {
        return compileShader(GLES20.GL_VERTEX_SHADER, shaderCode);
    }

    /**
     * 根据类型编译着色器
     */
    private static int compileShader(int type, String shaderCode) {
        final int shaderObjectId = GLES20.glCreateShader(type);
        if (shaderObjectId == 0) {
            Log.d(TAG, "compileShader: could not create new shader");
            return 0;
        }

        GLES20.glShaderSource(shaderObjectId, shaderCode);
        GLES20.glCompileShader(shaderObjectId);
        final int[] compileStatue = new int[1];
        GLES20.glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS, compileStatue, 0);
        if ((compileStatue[0] == 0)) {
            GLES20.glDeleteShader(shaderObjectId);
            Log.e(TAG, "compileShader: compilation of shader failed");
            return 0;
        }

        return shaderObjectId;
    }
}

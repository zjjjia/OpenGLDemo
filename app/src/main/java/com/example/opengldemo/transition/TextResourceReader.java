package com.example.opengldemo.transition;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author : Jiabo
 * @date : 2020/9/8
 * @decription : 读取GLSL语言
 */
public class TextResourceReader {

    private static final String TAG = "TextResourceReader";

    public static String readTextFileFromAsset(Context context, String fileName) {
        String result = null;
        String loadPath = "transition/" + fileName;
        try {
            InputStream in = context.getResources().getAssets().open(loadPath);
            int ch;
            ByteArrayOutputStream baStream = new ByteArrayOutputStream();
            while ((ch = in.read()) != -1) {
                baStream.write(ch);
            }
            byte[] buff = baStream.toByteArray();
            baStream.close();
            in.close();
            result = new String(buff, "UTF-8");
            result = result.replace("\\r\\n", "\n");
            Log.d(TAG, "read result is: " + result);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "readTextFileFromAsset: ", e);
        }

        return result;
    }
}

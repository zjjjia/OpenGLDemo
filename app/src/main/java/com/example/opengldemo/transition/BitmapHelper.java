package com.example.opengldemo.transition;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * @author : Jiabo
 * @date : 2020/9/14
 * @decription :
 */
public class BitmapHelper {

    public static Bitmap decodeBitmap(int targetWidth, String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        int outWith = options.outWidth;
        options.inJustDecodeBounds = false;
        options.inSampleSize = ((int) (outWith * 1f / targetWidth * 1f)) >> 1 << 1;
        return BitmapFactory.decodeFile(filePath, options);
    }
}

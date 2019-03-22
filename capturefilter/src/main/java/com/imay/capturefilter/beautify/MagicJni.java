package com.imay.capturefilter.beautify;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public class MagicJni {
    static{
        System.loadLibrary("MagicBeautify");
    }

    public static native void jniInitMagicBeautify(ByteBuffer handler);
    public static native void jniUnInitMagicBeautify();

    public static native void jniStartSkinSmooth(float denoiseLevel);
    public static native void jniStartWhiteSkin(float whitenLevel);

    public static native ByteBuffer jniStoreBitmapData(Bitmap bitmap);
    public static native void jniFreeBitmapData(ByteBuffer handler);
    public static native Bitmap jniGetBitmapFromStoredBitmapData(ByteBuffer handler);
}

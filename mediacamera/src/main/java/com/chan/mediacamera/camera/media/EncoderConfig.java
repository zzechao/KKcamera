package com.chan.mediacamera.camera.media;

import android.content.Context;
import android.opengl.EGLContext;

public class EncoderConfig {
    final int width;
    final int height;
    final String outputFile;
    final Context context;
    final EGLContext mEglContext;

    public EncoderConfig(int width, int height, String outputFile, Context context, EGLContext mEglContext) {
        this.width = width;
        this.height = height;
        this.outputFile = outputFile;
        this.context = context;
        this.mEglContext = mEglContext;
    }
}

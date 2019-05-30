package com.chan.mediacamera.camera.decoder;

import android.view.Surface;

public class DecoderConfig {
    public int fps;
    public String mPath;
    public Surface mSurface;

    public DecoderConfig(int fps, String mPath, Surface mSurface) {
        this.fps = fps;
        this.mPath = mPath;
        this.mSurface = mSurface;
    }
}

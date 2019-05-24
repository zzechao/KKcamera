package com.chan.mediacamera.camera;

import android.graphics.Point;
import android.graphics.SurfaceTexture;

/**
 * Description:
 */
public interface ICamera {

    boolean open(int cameraId);
    void setConfig(Config config);
    boolean preview();
    boolean switchTo(int cameraId);
    void takePhoto(TakePhotoCallback callback);
    boolean close();
    void setPreviewTexture(SurfaceTexture texture);

    Point getPreviewSize();
    Point getPictureSize();

    void setOnPreviewFrameCallback(PreviewFrameCallback callback);

    class Config{
        float rate; //宽高比
        int minPreviewWidth;
        int minPictureWidth;
    }

    interface TakePhotoCallback{
        void onTakePhoto(byte[] bytes);
    }

    interface PreviewFrameCallback{
        void onPreviewFrame(byte[] bytes, int width, int height);
    }

}

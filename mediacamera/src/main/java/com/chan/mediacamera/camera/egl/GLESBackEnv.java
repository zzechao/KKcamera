package com.chan.mediacamera.camera.egl;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.IntBuffer;


/**
 * Description:
 */
public class GLESBackEnv {

    private int mWidth;
    private int mHeight;
    private EGLHelper mEGLHelper;

    final static String TAG = "GLES20BackEnv";
    final static boolean LIST_CONFIGS = false;

    String mThreadOwner;

    public GLESBackEnv(int width, int height){
        this.mWidth=width;
        this.mHeight=height;
        mEGLHelper=new EGLHelper();
        mEGLHelper.eglInit(width,height);
    }

    public void setThreadOwner(String threadOwner){
        this.mThreadOwner=threadOwner;
    }

    public Bitmap getBitmap() {
        if (!Thread.currentThread().getName().equals(mThreadOwner)) {
            Log.e(TAG, "getBitmap: This thread does not own the OpenGL context.");
            return null;
        }

        return convertToBitmap();
    }

    public void destroy() {
        mEGLHelper.destroy();
    }


    private Bitmap convertToBitmap() {
        int[] iat = new int[mWidth * mHeight];
        IntBuffer ib = IntBuffer.allocate(mWidth * mHeight);
        mEGLHelper.mGL.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
            ib);
        int[] ia = ib.array();

        // Convert upside down mirror-reversed image to right-side up normal
        // image.
        for (int i = 0; i < mHeight; i++) {
            System.arraycopy(ia, i * mWidth, iat, (mHeight - i - 1) * mWidth, mWidth);
        }
        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(iat));
        return bitmap;
    }

}

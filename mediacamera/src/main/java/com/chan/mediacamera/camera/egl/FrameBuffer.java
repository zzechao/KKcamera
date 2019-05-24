package com.chan.mediacamera.camera.egl;

import android.opengl.GLES20;
import android.util.Log;


public class FrameBuffer {
    public final static String TAG = "FrameBuffer";

    private int mFrameBufferId = -1;
    private int mFrameBufferTextureId = -1;

    private int mWidth = 0;
    private int mHeight = 0;

    private long mThreadId = 0;

    /**
     * @param width 纹理的 width
     * @param height 纹理的 height
     * @return 是否创建成功
     */
    public boolean create(int width, int height) {
        long curThreadId = Thread.currentThread().getId();
        if (mThreadId != curThreadId && mThreadId > 0) {
            Log.e("slack","ERROR: FrameBuffer can't cross thread");
            release(false);
        }
        else {
            if (mWidth == width && mHeight == height &&
                    mFrameBufferTextureId != -1) {
                return true;
            } else {
                this.release(true);
            }
        }
        mThreadId = curThreadId;

        return createInternal(width, height);
    }

    private boolean createInternal(int width, int height) {
        int[] frameBuffer = new int[1];
        int[] frameBufferTexture = new int[1];

        // generate frame buffer
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);

        // generate texture
        GLES20.glGenTextures(1, frameBufferTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture[0]);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // set texture as colour attachment
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, frameBufferTexture[0], 0);

        // unbind
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        mFrameBufferId = frameBuffer[0];
        mFrameBufferTextureId = frameBufferTexture[0];

        mWidth = width;
        mHeight = height;

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "create framebuffer failed");
            return false;
        }
        Log.e(TAG, "Java create framebuffer success: (" +
                width + ", " + height + "), FB: " + mFrameBufferId + " , Tex: " + mFrameBufferTextureId);
        return true;
    }

    public void beginDrawToFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
//        GLES20.glClearColor(0, 0, 0, 1);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    public void endDrawToFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void drawToFrameBuffer(Runnable runnable) {
        beginDrawToFrameBuffer();
        runnable.run();
        endDrawToFrameBuffer();
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getId() {
        return mFrameBufferId;
    }

    public int getTextureId() {
        return mFrameBufferTextureId;
    }

    /**
     * 只删除 framebuffer 不删除纹理
     */
    public void releaseBufferOnly() {
        if (mFrameBufferId != -1) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                    GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, 0, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            GLES20.glDeleteFramebuffers(1, new int[] {mFrameBufferId}, 0);
        }

        mThreadId = 0;
        mFrameBufferId = -1;
    }

    public void release(boolean deleteBuffer) {
        if (deleteBuffer && mFrameBufferId != -1) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                    GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, 0, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            GLES20.glDeleteFramebuffers(1, new int[] {mFrameBufferId}, 0);
        }

        if (mFrameBufferTextureId != -1) {
            GLES20.glDeleteTextures(1, new int[] {mFrameBufferTextureId}, 0);
        }

        mWidth = 0;
        mHeight = 0;
        mThreadId = 0;
        mFrameBufferId = -1;
        mFrameBufferTextureId = -1;
    }
}

/*
 * Copyright 2014-2016 Media for Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chan.mediacamera.clip;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

import com.chan.mediacamera.camera.FBOVideoRenderer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * Holds state associated with a Surface used for MediaCodec decoder output.
 * <p/>
 * The (width,height) constructor for this class will prepare GL, create a SurfaceTexture,
 * and then create a Surface for that SurfaceTexture.  The Surface can be passed to
 * MediaCodec.setMediaFormat() to receive decoder output.  When a frame arrives, we latch the
 * texture with updateTexImage, then render the texture with GL to a pbuffer.
 * <p/>
 * The no-arg constructor skips the GL preparation step and doesn't allocate a pbuffer.
 * Instead, it just creates the Surface and SurfaceTexture, and when a frame arrives
 * we just draw it on whatever surface is current.
 * <p/>
 * By default, the Surface will be using a BufferQueue in asynchronous mode, so we
 * can potentially drop frames.
 */
public class OutputSurface implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "OutputSurface";
    private EGL10 mEGL;
    private EGLDisplay eglDisplay;
    private EGLContext eglContext;
    private EGLSurface eglSurface;

    private int mTextureId;

    private int mScreenWidth;
    private int mScreenHeight;

    private Surface surface;

    private final byte[] mFrameSyncObject = new byte[0];
    private boolean mFrameAvailable;

    private FBOVideoRenderer mRenderer;

    /**
     * Creates an OutputSurface using the current EGL context.  Creates a Surface that can be
     * passed to MediaCodec.setMediaFormat().
     */
    public OutputSurface(Context context) {
        mRenderer = new FBOVideoRenderer(context);
        // Even if we don't access the SurfaceTexture after the constructor returns, we
        // still need to keep a reference to it.  The Surface doesn't retain a reference
        // at the Java level, so if we don't either then the object can get GCed, which
        // causes the native finalizer to run.
        //if (VERBOSE) //Log.d(TAG, "textureID=" + mTextureRender.getTextureId());
//        textureId = this.eglUtil.createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
//        surfaceTexture = new SurfaceTexture(textureId);

        mRenderer.onSurfaceCreated();

        // This doesn't work if OutputSurface is created on the thread that CTS started for
        // these test cases.
        //
        // The CTS-created thread has a Looper, and the SurfaceTexture constructor will
        // create a Handler that uses it.  The "frame available" message is delivered
        // there, but since we're not a Looper-based thread we'll never see it.  For
        // this to do anything useful, OutputSurface must be created on a thread without
        // a Looper, so that SurfaceTexture uses the main application Looper instead.
        //
        // Java language note: passing "this" out of a constructor is generally unwise,
        // but we should be able to get away with it here.
        mRenderer.getSurfaceTexture().setOnFrameAvailableListener(this);
        surface = new Surface(mRenderer.getSurfaceTexture());
    }

    public void setInputSize(int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;
        mRenderer.onSurfaceChanged(width, height);
    }

    public void setVideoSize(int videoWidth, int videoHeight) {
        mRenderer.setVideoSize(videoWidth, videoHeight);
    }

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    public void release() {
        if (mEGL != null) {
            if (mEGL.eglGetCurrentContext().equals(eglContext)) {
                // Clear the current context and surface to ensure they are discarded immediately.
                mEGL.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT);
            }
            mEGL.eglDestroySurface(eglDisplay, eglSurface);
            mEGL.eglDestroyContext(eglDisplay, eglContext);
            //mEGL.eglTerminate(mEGLDisplay);

            // added line
            //egl.eglTerminate(eglDisplay);
            // added line
        }

        surface.release();

        // this causes a bunch of warnings that appear harmless but might confuse someone:
        //  W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
        //mSurfaceTexture.release();
        mRenderer.releaseSurfaceTexture();

        // null everything out so future attempts to use this object will cause an NPE
        eglDisplay = null;
        eglContext = null;
        eglSurface = null;
        mEGL = null;

        //textureRender = null;
        surface = null;
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent() {

        String message = "Failed to set up EGL context and surface.";

        if (mEGL == null) {
            throw new RuntimeException(message);
        }
        checkEglError("before makeCurrent");
        if (!mEGL.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw new RuntimeException(message);
        }
    }

    /**
     * Returns the Surface that we draw onto.
     */
    public Surface getSurface() {
        return surface;
    }


    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the OutputSurface object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    public void awaitNewImage() {
        final int TIMEOUT_MS = 500;
        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }
        // Latch the data.
//        mTextureRender.checkGlError("before updateTexImage");
//        mDrawer.checkGlError("before updateTexImage");
        mRenderer.getSurfaceTexture().updateTexImage();
    }


    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    public void onDrawFrame() {
        mRenderer.onDrawFrame();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        //if (VERBOSE) //Log.d(TAG, "new frame available");
        synchronized (mFrameSyncObject) {
            if (mFrameAvailable) {
//                throw new RuntimeException("Failed to notify on a new frame available.");
            }
            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }

    /**
     * Che cks for EGL errors.
     */
    private void checkEglError(String msg) {
        boolean failed = false;
        int error;
        while ((error = mEGL.eglGetError()) != EGL10.EGL_SUCCESS) {
            Log.e(TAG, msg + ": EGL error: 0x" + Integer.toHexString(error));
            failed = true;
        }
        if (failed) {
            throw new RuntimeException("EGL error encountered (see log)");
        }
    }

}

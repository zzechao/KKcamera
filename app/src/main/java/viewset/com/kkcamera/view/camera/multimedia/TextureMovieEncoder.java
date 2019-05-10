/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package viewset.com.kkcamera.view.camera.multimedia;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import viewset.com.kkcamera.view.camera.filter.NoFilter;
import viewset.com.kkcamera.view.camera.gles.EglCore;
import viewset.com.kkcamera.view.camera.gles.WindowSurface;

/**
 * Encode a movie from frames rendered from an external texture image.
 * <p>
 * The object wraps an encoder running on a dedicated thread.  The various control messages
 * may be sent from arbitrary threads (typically the app UI thread).  The encoder thread
 * manages both sides of the encoder (feeding and draining); the only external input is
 * the GL texture.
 * <p>
 * The design is complicated slightly by the need to create an EGL context that shares state
 * with a view that gets restarted if (say) the device orientation changes.  When the view
 * in question is a GLSurfaceView, we don't have full control over the EGL context creation
 * on that side, so we have to bend a bit backwards here.
 * <p>
 * To use:
 * <ul>
 * <li>create TextureMovieEncoder object
 * <li>create an EncoderConfig
 * <li>call TextureMovieEncoder#startRecording() with the config
 * <li>call TextureMovieEncoder#setTextureId() with the texture object that receives frames
 * <li>for each frame, after latching it with SurfaceTexture#updateTexImage(),
 * call TextureMovieEncoder#frameAvailable().
 * </ul>
 * <p>
 * TODO: tweak the API (esp. textureId) so it's less awkward for simple use cases.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class TextureMovieEncoder implements Runnable {
    private static final String TAG = "";
    private static final boolean VERBOSE = false;

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private NoFilter showFilter;
    private int mTextureId;

    private VideoEncoderCore mVideoEncoder;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;

    private Object mReadyFence = new Object();      // guards ready/running
    private boolean mReady;
    private boolean mRunning;
    private Context mContext;

    public TextureMovieEncoder(Context context) {
        mContext = context;
    }

    public static class EncoderConfig {
        final File mOutputFile;
        final int mWidth;
        final int mHeight;
        final int mBitRate;
        final EGLContext mEglContext;

        public EncoderConfig(File outputFile, int width, int height, int bitRate,
                             EGLContext sharedEglContext) {
            mOutputFile = outputFile;
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mEglContext = sharedEglContext;
        }
    }

    /**
     * startRecording 先通过mReadyFence.wait()先将线程锁死，
     * 并开启TextureMovieEncoder线程，执行runnable，
     * 创建线程对应的Looper，再解锁mReadyFence.notify(),
     * 执行 mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
     * @param config
     */
    public void startRecording(EncoderConfig config) {
        Log.d(TAG, "Encoder: startRecording()");
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "TextureMovieEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ignore) {
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }

    public void stopRecording() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
    }

    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    public void frameAvailable(SurfaceTexture st) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }

        float[] transform = new float[16];
        st.getTransformMatrix(transform);
        long timestamp = st.getTimestamp();
        if (timestamp == 0) {
            return;
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE,
                (int) (timestamp >> 32), (int) timestamp, transform));
    }

    public void setTextureId(int id) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
    }

    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }


    private static class EncoderHandler extends Handler {
        private WeakReference<TextureMovieEncoder> mWeakEncoder;

        public EncoderHandler(TextureMovieEncoder encoder) {
            mWeakEncoder = new WeakReference<TextureMovieEncoder>(encoder);
        }

        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            TextureMovieEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) inputMessage.arg1) << 32) |
                            (((long) inputMessage.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable((float[]) obj, timestamp);
                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(inputMessage.arg1);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private void handleStartRecording(EncoderConfig config) {
        prepareEncoder(config.mEglContext, config.mWidth, config.mHeight, config.mBitRate,
                config.mOutputFile);
    }

    private void handleFrameAvailable(float[] transform, long timestampNanos) {
        mVideoEncoder.drainEncoder(false);

        showFilter.setTextureId(mTextureId);
        showFilter.onDrawFrame();

        mInputWindowSurface.setPresentationTime(timestampNanos);
        mInputWindowSurface.swapBuffers();
    }

    private void handleStopRecording() {
        mVideoEncoder.drainEncoder(true);
        releaseEncoder();
    }

    private void handleSetTexture(int id) {
        mTextureId = id;
    }

    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        mInputWindowSurface.releaseEglSurface();
        showFilter.release();
        mEglCore.release();

        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        showFilter = new NoFilter(mContext);
        showFilter.onSurfaceCreated();
    }

    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate,
                                File outputFile) {
        mVideoEncoder = new VideoEncoderCore(width, height, bitRate, outputFile);
        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();

        showFilter = new NoFilter(mContext);
        showFilter.onSurfaceCreated();
        showFilter.setSize(width, height);
    }

    private void releaseEncoder() {
        mVideoEncoder.release();
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (showFilter != null) {
            showFilter.release();
            showFilter = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }
}

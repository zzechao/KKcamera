package com.chan.mediacamera.camera.media;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;

import java.lang.ref.WeakReference;


/**
 * Looper基础类
 */
public abstract class Encoder implements Runnable{

    protected static final int MSG_START_RECORDING = 1;
    protected static final int MSG_STOP_RECORDING = 2;
    protected static final int MSG_FRAME_AVAILABLE = 3;
    protected static final int MSG_SET_TEXTURE_ID = 4;
    protected static final int MSG_UPDATE_SHARED_CONTEXT = 5;
    protected static final int MSG_QUIT = 6;
    protected static final int MSG_PAUSE_RECORDING = 7;
    protected static final int MSG_RESUME_RECORDING = 8;
    protected static final int MSG_AUDIO_STEP = 9;

    /**
     * 锁，同步EncoderHandler创建，再释放锁
     */
    protected final byte[] mReadyFence = new byte[0]; //

    protected EncoderHandler mHandler;
    protected boolean mReady = false;
    protected boolean mRunning;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected static class EncoderHandler extends Handler {
        private WeakReference<Encoder> mWeakEncoder;

        public EncoderHandler(Encoder encoder) {
            mWeakEncoder = new WeakReference<Encoder>(encoder);
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Object obj = msg.obj;
            Encoder encoder = mWeakEncoder.get();
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
                    long timestamp = (((long) msg.arg1) << 32) |
                            (((long) msg.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable(timestamp);
                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(msg.arg1);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EncoderConfig) obj);
                    break;
                case MSG_PAUSE_RECORDING:
                    encoder.handlePauseRecording();
                    break;
                case MSG_RESUME_RECORDING:
                    encoder.handleResumeRecording();
                    break;
                case MSG_AUDIO_STEP:
                    encoder.handleAudioStep();
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
                default:
            }
        }
    }



    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    protected abstract void start(EncoderConfig config);

    protected abstract void stop();

    protected abstract void pause();

    protected abstract void resume();

    protected abstract void handleResumeRecording();

    protected abstract void handlePauseRecording();

    protected abstract void handleUpdateSharedContext(EncoderConfig obj);

    protected abstract void handleSetTexture(int textureId);

    protected abstract void handleFrameAvailable(long timestamp);

    protected abstract void handleStopRecording();

    protected abstract void handleStartRecording(EncoderConfig obj);

    protected abstract void handleAudioStep();
}

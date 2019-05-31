package com.chan.mediacamera.camera.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public abstract class Decoder implements Runnable {

    protected byte[] mReadyFence = new byte[0];
    protected int mDelayTime;

    protected static final int MSG_START_DECORDING = 1;
    protected static final int MSG_STOP_DECORDING = 2;
    protected static final int MSG_PAUSE_DECORDING = 3;
    protected static final int MSG_RESUME_DECORDING = 4;
    protected static final int MSG_DECORD_STEP = 5;
    protected static final int MSG_QUIT = 6;

    public static final int DECODER_VIDEO = 1;
    public static final int DECODER_AUDIO = 2;

    public static class DecoderHandler extends Handler {

        WeakReference<Decoder> weakReference;

        public DecoderHandler(Decoder decoder) {
            weakReference = new WeakReference<>(decoder);
        }

        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            Decoder decoder = weakReference.get();
            if (decoder == null) {
                return;
            }
            Object object = msg.obj;
            switch (msg.what) {
                case MSG_START_DECORDING:
                    decoder.handleStartDecoder((DecoderConfig) object);
                    break;
                case MSG_STOP_DECORDING:
                    decoder.handleStopDecoder();
                    break;
                case MSG_DECORD_STEP:
                    if (object != null) {
                        DecoderConfig config = (DecoderConfig) object;
                        decoder.setFPS(config.fps);
                    }
                    decoder.handleStepDecoder();
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
            }
        }
    }

    private void setFPS(int fps) {
        mDelayTime = 1000 / fps;
    }

    public interface DecoderListener {
        void onVideoSizeChanged(int width, int height);

        void onStart(int decoderStats, MediaCodec decoder, MediaFormat mediaFormat, DecoderConfig config);

        void onStop(int decoderStats);

        void queueAudio(ByteBuffer copyBuffer, int outputIndex, long presentationTimeUs);
    }

    protected abstract void handleStartDecoder(DecoderConfig config);

    protected abstract void handleStopDecoder();

    protected abstract void handleStepDecoder();
}

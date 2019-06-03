package com.chan.mediacamera.camera.decoder;

import android.os.Looper;

public class MediaDecoder extends Decoder {

    private DecoderHandler mHandler;
    private boolean mReady = false;
    private boolean mRunning;
    private boolean mVideoStop;
    private boolean mAudioStop;
    private VideoWapper videoWapper;
    private AudioWapper audioWapper;

    @Override
    public void run() {
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new DecoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();
    }

    public void start(DecoderConfig config) {
        synchronized (mReadyFence) {
            if (mRunning) {
                return;
            }
            mRunning = true;
            new Thread(this, "VideoDecoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException e) {

                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_DECORDING, config));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_DECORD_STEP, config));
    }

    public void stop() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_DECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
    }

    @Override
    protected void handleStartDecoder(DecoderConfig config) {
        videoWapper = new VideoWapper(config.mPath);
        audioWapper = new AudioWapper(config.mPath);

        videoWapper.start(config.mSurface);
        audioWapper.start();
    }

    @Override
    protected void handleStopDecoder() {

    }

    @Override
    protected void handleStepDecoder() {
        intputDecord();
        outputDecord();
    }

    private void intputDecord() {
        if (!mVideoStop) {
            videoWapper.intputDecord();
        }
        if (!mAudioStop) {
            audioWapper.intputDecord();
        }
    }

    private void outputDecord() {
        if ((mVideoStop = videoWapper.outputDecord()) && (mAudioStop = audioWapper.outputDecord())) {
            videoWapper.release();
            audioWapper.release();
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_DECORD_STEP));
        }
    }


}

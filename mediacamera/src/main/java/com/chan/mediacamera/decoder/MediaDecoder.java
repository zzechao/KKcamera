package com.chan.mediacamera.decoder;

import android.os.Looper;

public class MediaDecoder extends Decoder implements MediaTimeProvider {

    private VideoFrameReleaseTimeHelper mFrameReleaseTimeHelper;
    private DecoderHandler mHandler;
    private boolean mReady = false;
    private boolean mRunning;
    private boolean mVideoStop;
    private boolean mAudioStop;
    private VideoWapper videoWapper;
    private AudioWapper audioWapper;
    private long mDeltaTimeUs;

    public MediaDecoder() {
        mFrameReleaseTimeHelper = new VideoFrameReleaseTimeHelper();
    }

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
        mHandler.removeMessages(MSG_DECORD_STEP);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_DECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
    }

    @Override
    protected void handleStartDecoder(DecoderConfig config) {
        mDeltaTimeUs = -1;
        mFrameReleaseTimeHelper.enable();

        videoWapper = new VideoWapper(config.mPath, this);
        audioWapper = new AudioWapper(config.mPath);

        videoWapper.start(config.mSurface);
        audioWapper.start();
    }

    @Override
    protected void handleStopDecoder() {
        videoWapper.release();
        audioWapper.release();
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
        if ((mVideoStop = videoWapper.outputDecord()) & (mAudioStop = audioWapper.outputDecord())) {
            videoWapper.release();
            audioWapper.release();
            if (mFrameReleaseTimeHelper != null) {
                mFrameReleaseTimeHelper.disable();
                mFrameReleaseTimeHelper = null;
            }
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_DECORD_STEP));
        }
    }


    @Override
    public long getNowUs() {
        if (audioWapper == null) {
            return System.currentTimeMillis() * 1000;
        }

        return audioWapper.getAudioTimeUs();
    }

    @Override
    public long getRealTimeUsForMediaTime(long mediaTimeUs) {
        if (mDeltaTimeUs == -1) {
            long nowUs = getNowUs();
            mDeltaTimeUs = nowUs - mediaTimeUs;
        }
        long earlyUs = mDeltaTimeUs + mediaTimeUs - getNowUs();
        long unadjustedFrameReleaseTimeNs = System.nanoTime() + (earlyUs * 1000);
        long adjustedReleaseTimeNs = mFrameReleaseTimeHelper.adjustReleaseTime(
                mDeltaTimeUs + mediaTimeUs, unadjustedFrameReleaseTimeNs);
        return adjustedReleaseTimeNs / 1000;
    }

    @Override
    public long getVsyncDurationNs() {
        if (mFrameReleaseTimeHelper != null) {
            return mFrameReleaseTimeHelper.getVsyncDurationNs();
        } else {
            return -1;
        }
    }
}

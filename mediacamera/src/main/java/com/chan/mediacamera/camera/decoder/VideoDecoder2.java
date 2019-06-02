package com.chan.mediacamera.camera.decoder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder2 extends Decoder {

    private final String TAG = VideoDecoder2.class.getSimpleName();

    private final String VIDEO = "video/";
    private final int TIMEOUT_USEC = 10000;

    private DecoderHandler mHandler;
    private boolean mReady = false;
    private boolean mRunning = false;
    private boolean mStop = false;

    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private DecoderListener mListener;

    public VideoDecoder2(DecoderListener listener) {
        mListener = listener;
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
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_DECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
    }

    public void release() {
        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
        }
        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
        }
        mBufferInfo = null;
        mListener = null;
    }

    @Override
    protected void handleStartDecoder(DecoderConfig config) {
        try {
            mBufferInfo = new MediaCodec.BufferInfo();
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(config.mPath);
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = mExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(VIDEO)) {
                    mExtractor.selectTrack(i);
                    int videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                    int videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    long videoTime = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    if (mListener != null) {
                        mListener.onVideoSizeChanged(videoWidth, videoHeight);
                    }
                    mDecoder = MediaCodec.createDecoderByType(mime);

                    Log.d(TAG, "format : " + mediaFormat);
                    if (mListener != null) {
                        mListener.onStart(Decoder.DECODER_VIDEO, mDecoder, mediaFormat, config);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void handleStopDecoder() {
        mStop = true;
    }

    @Override
    protected void handleStepDecoder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mDecoder.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(MediaCodec codec, int index) {

                }

                @Override
                public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {

                }

                @Override
                public void onError(MediaCodec codec, MediaCodec.CodecException e) {

                }

                @Override
                public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {

                }
            }, mHandler);
        }


        if (!mStop) {
            intputDecord();
            outputDecord();
            mHandler.sendMessage(mHandler.obtainMessage(MSG_DECORD_STEP));
        } else {
            if (mListener != null) {
                mListener.onStop(Decoder.DECODER_VIDEO);
            }
        }
    }

    /**
     * mExtractor中读取数据
     */
    private void intputDecord() {
        int inputBufIndex = mDecoder.dequeueInputBuffer(TIMEOUT_USEC);
        if (inputBufIndex >= 0) {
            ByteBuffer mInputBuffer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mInputBuffer = mDecoder.getInputBuffer(inputBufIndex);
            } else {
                mInputBuffer = mDecoder.getInputBuffers()[inputBufIndex];
            }
            assert mInputBuffer != null;
            mInputBuffer.clear();
            int sampleSize = mExtractor.readSampleData(mInputBuffer, 0);
            if (mExtractor.advance() && sampleSize > 0) {
                mDecoder.queueInputBuffer(inputBufIndex, 0, sampleSize, mExtractor.getSampleTime(), 0);
            } else {
                mDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }
        }
    }

    private void outputDecord() {
        int outputIndex = mDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) { //等待超时，需要再次等待，通常忽略

        } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

        } else if (outputIndex < 0) {

        } else {
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                    && mBufferInfo.size > 0) {
                mDecoder.releaseOutputBuffer(outputIndex, true);
            }

            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                mStop = true;
            }
        }
    }
}

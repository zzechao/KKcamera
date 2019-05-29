package com.chan.mediacamera.camera.decoder;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoderThread extends Thread {
    private static final String VIDEO = "video/";
    private static final String TAG = "VideoDecoder";
    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;

    private boolean eosReceived;

    private int videoWidth;
    private int videoHeight;
    private long videoTime;

    public boolean init(Surface surface, String filePath) {
        eosReceived = false;
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(filePath);
            //分离出音轨和视轨
            Log.d(TAG, "getTrackCount: " + mExtractor.getTrackCount());
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = mExtractor.getTrackFormat(i);

                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(VIDEO)) {
                    mExtractor.selectTrack(i);
                    videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                    videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    videoTime = mediaFormat.getLong(MediaFormat.KEY_DURATION);

                    mDecoder = MediaCodec.createDecoderByType(mime);
                    try {

                        Log.d(TAG, "format : " + mediaFormat + "--width : " + videoWidth + "--height : " + videoHeight);
                        mDecoder.configure(mediaFormat, surface, null, 0 /* Decoder */);

                    } catch (IllegalStateException e) {
                        Log.e(TAG, "codec '" + mime + "' failed configuration. " + e);
                        return false;
                    }

                    mDecoder.start();
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void run() {
        BufferInfo info = new BufferInfo();
        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();

        boolean isInput = true;
        boolean first = false;
        long startWhen = 0;

        while (!eosReceived) {
            if (isInput) {
                int inputIndex = mDecoder.dequeueInputBuffer(10000);
                if (inputIndex >= 0) {
                    // fill inputBuffers[inputBufferIndex] with valid data
                    ByteBuffer inputBuffer = inputBuffers[inputIndex];

                    int sampleSize = mExtractor.readSampleData(inputBuffer, 0);

                    if (mExtractor.advance() && sampleSize > 0) {
                        mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, mExtractor.getSampleTime(), 0);

                    } else {
                        Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        mDecoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isInput = false;
                    }
                }
            }

            int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    mDecoder.getOutputBuffers();
                    break;

                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " + mDecoder.getOutputFormat());
                    break;

                case MediaCodec.INFO_TRY_AGAIN_LATER:
//                Log.d(TAG, "INFO_TRY_AGAIN_LATER");
                    break;

                default:
                    if (!first) {
                        startWhen = System.currentTimeMillis();
                        first = true;
                    }
                    try {
                        long sleepTime = (info.presentationTimeUs / 1000) - (System.currentTimeMillis() - startWhen);
                        //Log.d(TAG, "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - startWhen) + " sleepTime : " + sleepTime);

                        if (sleepTime > 0)
                            Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    mDecoder.releaseOutputBuffer(outIndex, true /* Surface init */);
                    break;
            }

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }

        mDecoder.release();
        mExtractor.release();
    }

    public void close() {
        eosReceived = true;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public long getVideoTime() {
        return videoTime;
    }
}
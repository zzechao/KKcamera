package com.chan.mediacamera.camera.decoder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoWapper {
    private static final String TAG = "VideoWapper";
    private static final long TIMEOUT_USEC = 0;
    private final String VIDEO = "video/";

    private final MediaCodec.BufferInfo mBufferInfo;
    private final MediaExtractor mExtractor;
    private MediaCodec mDecoder;
    private boolean mStop;

    public VideoWapper(String path) {
        mBufferInfo = new MediaCodec.BufferInfo();
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(Surface surface) {
        for (int i = 0; i < mExtractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = mExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(VIDEO)) {
                mExtractor.selectTrack(i);
                int videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                int videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                long videoTime = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                try {
                    mDecoder = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "format : " + mediaFormat);
                mDecoder.configure(mediaFormat, surface, null, 0);

                mStop = false;
                mDecoder.start();
                break;
            }
        }
    }

    /**
     * mExtractor中读取数据
     */
    public void intputDecord() {
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

    public boolean outputDecord() {
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
        return mStop;
    }

    public void release() {
        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.release();
        }
        if(mExtractor != null){
            mExtractor.release();
        }
    }
}

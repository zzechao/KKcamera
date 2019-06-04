package com.chan.mediacamera.camera.decoder;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioWapper {
    private static final long TIMEOUT_USEC = 0;
    private static final String TAG = "AudioWapper";
    private final MediaCodec.BufferInfo mBufferInfo;
    private final MediaExtractor mExtractor;
    private final String AUDIO = "audio/";
    private final int APPLICATION_AUDIO_PERIOD_MS = 200;
    private NonBlockingAudioTrack mAudioTrack;
    private MediaCodec mDecoder;
    private boolean mStop;

    public AudioWapper(String path) {
        mBufferInfo = new MediaCodec.BufferInfo();
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        // MediaExtractor的初始化
        int selectTrack = 0;
        int sampleRateInHz = 65200;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int channelMask = AudioFormat.CHANNEL_OUT_STEREO;
        int channelCount = 0;

        String mime = "audio/mp4a-latm";
        MediaFormat mediaFormat = null;

        for (int i = 0; i < mExtractor.getTrackCount(); i++) {
            mediaFormat = mExtractor.getTrackFormat(i);
            mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(AUDIO)) {
                selectTrack = i;
                sampleRateInHz = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);// 采样率
                channelConfig = (mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1) ?
                        AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO; // 声道数
                channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                break;
            }
        }
        mExtractor.selectTrack(selectTrack);

        // AudioTrack的初始化
        int minBufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz,
                channelConfig,
                audioFormat);
        final int frameCount = APPLICATION_AUDIO_PERIOD_MS * sampleRateInHz / 1000;
        final int frameSizeInBytes = Integer.bitCount(channelConfig)
                * getBytesPerSample(audioFormat);

        Log.e("ttt", "minBufferSizeInBytes:" + minBufferSizeInBytes + "--frameCount * frameSizeInBytes :" + frameCount * frameSizeInBytes);

        mAudioTrack = new NonBlockingAudioTrack(sampleRateInHz, channelCount);

        // MediaCodec的初始化
        try {
            mDecoder = MediaCodec.createDecoderByType(mime);
            mDecoder.configure(mediaFormat, null, null, 0);

            mStop = false;
            mAudioTrack.play();
            mDecoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getBytesPerSample(int audioFormat) {
        switch (audioFormat) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 1;
            case AudioFormat.ENCODING_PCM_16BIT:
            case AudioFormat.ENCODING_IEC61937:
            case AudioFormat.ENCODING_DEFAULT:
                return 2;
            case AudioFormat.ENCODING_PCM_FLOAT:
                return 4;
            case AudioFormat.ENCODING_INVALID:
            default:
                throw new IllegalArgumentException("Bad audio format " + audioFormat);
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
            ByteBuffer mOutputBuffer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mOutputBuffer = mDecoder.getOutputBuffer(outputIndex);
            } else {
                mOutputBuffer = mDecoder.getOutputBuffers()[outputIndex];
            }
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                    && mBufferInfo.size > 0 && mOutputBuffer != null) {
                ByteBuffer copyBuffer = ByteBuffer.allocate(mOutputBuffer.remaining());
                copyBuffer.put(mOutputBuffer);
                copyBuffer.flip();
                mAudioTrack.write(copyBuffer, 0, mBufferInfo.size);
            }

            mDecoder.releaseOutputBuffer(outputIndex, false);

            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                mStop = true;
            }
        }
        return mStop;
    }

    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.flush();
            mAudioTrack.stop();
            mAudioTrack.release();
        }
        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.release();
        }
        if (mExtractor != null) {
            mExtractor.release();
        }
    }

    public long getAudioTimeUs() {
        if (mAudioTrack == null) {
            return 0;
        }

        return mAudioTrack.getAudioTimeUs();
    }
}

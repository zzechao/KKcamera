package com.chan.mediacamera.camera.decoder;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDecoder2 extends Decoder {

    private final String TAG = AudioDecoder2.class.getSimpleName();

    private final String AUDIO = "audio/";
    private final int TIMEOUT_USEC = 10000;

    //采样率
    private final int SAMPLE_RATE = 65200;
    //声道数
    private final int CHANNEL_COUNT = AudioFormat.CHANNEL_OUT_MONO;

    private final int APPLICATION_AUDIO_PERIOD_MS = 200;
    private final int TEST_MAX_SPEED = 1;

    private DecoderHandler mHandler;
    private boolean mReady = false;
    private boolean mRunning = false;
    private boolean mStop = false;

    private MediaExtractor mExtractor;
    private AudioTrack mAudioTrack;
    private MediaCodec mDecoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private DecoderListener mListener;

    public AudioDecoder2(DecoderListener listener) {
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

    public AudioTrack getAudioTrack() {
        return mAudioTrack;
    }

    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.flush();
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
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
            // MediaExtractor的初始化
            int selectTrack = 0;
            int sampleRateInHz = SAMPLE_RATE;
            int channelConfig = CHANNEL_COUNT;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int channelMask = AudioFormat.CHANNEL_OUT_STEREO;
            String mime = "audio/mp4a-latm";
            MediaFormat mediaFormat = null;

            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(config.mPath);
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                mediaFormat = mExtractor.getTrackFormat(i);
                mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(AUDIO)) {
                    selectTrack = i;
                    sampleRateInHz = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);// 采样率
                    channelConfig = (mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1) ?
                            AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO; // 声道数
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

            minBufferSizeInBytes = TEST_MAX_SPEED /* speed influences buffer size , default is 2*/
                    * Math.max(minBufferSizeInBytes, frameCount * frameSizeInBytes);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                AudioFormat af = new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setChannelMask(channelMask)
                        .setSampleRate(sampleRateInHz)
                        .build();
                mAudioTrack =
                        new AudioTrack(audioAttributes, af, minBufferSizeInBytes,
                                AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            } else {
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBufferSizeInBytes,
                        AudioTrack.MODE_STREAM);
            }

            // MediaCodec的初始化
            mDecoder = MediaCodec.createDecoderByType(mime);
            if (mListener != null) {
                mListener.onStart(Decoder.DECODER_AUDIO, mDecoder, mediaFormat, config);
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
        if (!mStop) {
            intputDecord();
            outputDecord();
            mHandler.sendMessage(mHandler.obtainMessage(MSG_DECORD_STEP));
        } else {
            if (mListener != null) {
                mListener.onStop(Decoder.DECODER_AUDIO);
            }
        }
    }

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
            ByteBuffer mOutputBuffer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mOutputBuffer = mDecoder.getOutputBuffer(outputIndex);
            } else {
                mOutputBuffer = mDecoder.getOutputBuffers()[outputIndex];
            }
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                    && mBufferInfo.size > 0 && mOutputBuffer != null) {
                if (mListener != null) {
                    Log.e("ttt","queueAudio");
                    mListener.queueAudio(mOutputBuffer, outputIndex, mBufferInfo.presentationTimeUs);
                }
            }

            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                mStop = true;
            }
        }
    }

    public int getBytesPerSample(int audioFormat) {
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

    public DecoderHandler getAndioHandler() {
        return mHandler;
    }

    /**
     * @param bufferId
     */
    public void releaseOutputBuffer(int bufferId) {
        Log.e("ttt","releaseOutputBuffer");
        mDecoder.releaseOutputBuffer(bufferId, false);
    }
}

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

public class AudioDecoder extends Decoder {

    private final String TAG = AudioDecoder.class.getSimpleName();

    private final String AUDIO = "audio/";
    private final int TIMEOUT_USEC = 10000;

    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    //采样率
    private final int SAMPLE_RATE = 65200;
    //声道数
    private final int CHANNEL_COUNT = AudioFormat.CHANNEL_OUT_MONO;


    private DecoderHandler mHandler;
    private boolean mReady = false;
    private boolean mRunning = false;
    private boolean mStop = false;

    private MediaExtractor mExtractor;
    private AudioTrack mAudioTrack;
    private MediaCodec mDecoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private DecoderListener mListener;
    private boolean mFirstTime;
    private long mStartTime;

    public AudioDecoder(DecoderListener listener) {
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
            int samplerate = SAMPLE_RATE;
            int changelConfig = CHANNEL_COUNT;
            int pmc = AUDIO_FORMAT;
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
                    samplerate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);// 采样率
                    changelConfig = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT); // 声道数
                    break;
                }
            }
            mExtractor.selectTrack(selectTrack);

            // AudioTrack的初始化

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final int minBufferSize = AudioTrack.getMinBufferSize(samplerate,
                        changelConfig,
                        pmc);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                AudioFormat audioFormat = new AudioFormat.Builder()
                        .setEncoding(pmc)
                        .setChannelMask(channelMask)
                        .setSampleRate(samplerate)
                        .build();
                mAudioTrack =
                        new AudioTrack(audioAttributes, audioFormat, minBufferSize,
                                AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            } else {
                final int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBufferSize,
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
            if (!mFirstTime) {
                mStartTime = System.currentTimeMillis();
                mFirstTime = true;
            }
            long sleepTime = (mBufferInfo.presentationTimeUs / 1000) - (System.currentTimeMillis() - mStartTime);
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
                ByteBuffer copyBuffer = ByteBuffer.allocate(mOutputBuffer.remaining());
                copyBuffer.put(mOutputBuffer);
                copyBuffer.flip();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (mListener != null) {
                        mListener.queueAudio(copyBuffer, outputIndex, mBufferInfo.presentationTimeUs);
                    }
                } else {

                }
                //mAudioTrack.write(copyBuffer.array(), 0, mBufferInfo.size);

                mDecoder.releaseOutputBuffer(outputIndex, false);
            }

            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                mStop = true;
            }
        }
    }
}

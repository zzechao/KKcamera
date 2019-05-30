package com.chan.mediacamera.camera.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioEncoder extends Encoder {

    private final String MIME_TYPE = "audio/mp4a-latm";   //音频编码的Mime
    private final int OUTPUT_AUDIO_BIT_RATE = 64 * 1024;
    private final int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    private final int OUTPUT_AUDIO_CHANNEL_COUNT = 1;
    private final int OUTPUT_AUDIO_SAMPLE_RATE_HZ = 16000;
    private final int OUTPUT_AUDIO_SAMPLE_PER_RATE = 1024;
    private final int AUDIO_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; //音频录制格式，默认为PCM16Bit
    private final int fps = 25; //

    private int bufferSize;
    private AudioRecord mAudioRecorder;   //录音器
    private MediaCodec mAudioCodes;   //编码器，用于音频编码
    private MediaCodec.BufferInfo mBufferInfo;

    private MuxerEncoderListener mListener;

    private volatile boolean isPause = true;
    private volatile boolean isStop = true;
    private int mTrackIndex;

    public AudioEncoder(MuxerEncoderListener listener) {
        super();
        mListener = listener;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void run() {
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();
    }

    public void release() {
        if (mAudioRecorder != null) {
            mAudioRecorder.stop();
            mAudioRecorder.release();
            mAudioRecorder = null;
        }
        if (mAudioCodes != null) {
            mAudioCodes.stop();
            mAudioCodes.release();
            mAudioCodes = null;
        }
    }

    private void init() throws IOException {
        int minBufferSize = AudioRecord.getMinBufferSize(OUTPUT_AUDIO_SAMPLE_RATE_HZ,
                AUDIO_CONFIG, AUDIO_FORMAT);

        bufferSize = OUTPUT_AUDIO_SAMPLE_PER_RATE * fps;

        if (bufferSize < minBufferSize) {
            bufferSize = (minBufferSize / OUTPUT_AUDIO_SAMPLE_PER_RATE + 1) * OUTPUT_AUDIO_SAMPLE_PER_RATE * 2;
        }

        mBufferInfo = new MediaCodec.BufferInfo();

        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, OUTPUT_AUDIO_SAMPLE_RATE_HZ, AUDIO_CONFIG,
                AUDIO_FORMAT, bufferSize);//初始化录音器

        MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, OUTPUT_AUDIO_SAMPLE_RATE_HZ, OUTPUT_AUDIO_CHANNEL_COUNT);//创建音频的格式,参数 MIME,采样率,通道数
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE);//编码方式
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BIT_RATE);//比特率
        mAudioCodes = MediaCodec.createEncoderByType(MIME_TYPE);//创建音频编码器
        mAudioCodes.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);//配置

        mAudioCodes.start();
        mAudioRecorder.startRecording();

        Log.e("ttt", "init");
    }

    private void recordEncorder() {
        int inputBufIndex = mAudioCodes.dequeueInputBuffer(10000);
        if (inputBufIndex >= 0) {
            ByteBuffer mInputBuffer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mInputBuffer = mAudioCodes.getInputBuffer(inputBufIndex);
            } else {
                mInputBuffer = mAudioCodes.getInputBuffers()[inputBufIndex];
            }
            assert mInputBuffer != null;
            mInputBuffer.clear();
            int length = mAudioRecorder.read(mInputBuffer, bufferSize); //读入数据
            if (length > 0) {
                mAudioCodes.queueInputBuffer(inputBufIndex, 0, length, mListener.getPTSUs() - 2000, isStop ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
            }
        }
    }

    private void drainEncoder() {
        final int TIMEOUT_USEC = 10000;
        int outputIndex = mAudioCodes.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {

        } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat newFormat = mAudioCodes.getOutputFormat();
            mTrackIndex = mListener.onFormatChanged(MuxerWapper.DATA_AUDIO, newFormat);
            mListener.onStart(MuxerWapper.DATA_AUDIO);
        } else if (outputIndex < 0) {
            drainEncoder();
        } else {
            ByteBuffer encodedData;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                encodedData = mAudioCodes.getOutputBuffer(outputIndex);
            } else {
                encodedData = mAudioCodes.getOutputBuffers()[outputIndex];
            }
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                    && mBufferInfo.size > 0 && encodedData != null) {
                mBufferInfo.presentationTimeUs = mListener.getPTSUs() - 2000;
                encodedData.position(mBufferInfo.offset);
                encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                mListener.writeData(MuxerWapper.DATA_AUDIO, mTrackIndex, encodedData, mBufferInfo);
                mAudioCodes.releaseOutputBuffer(outputIndex, false);
            }
        }

//        此流程已经deprecated
//        ByteBuffer[] encoderOutputBuffers = mAudioCodes.getOutputBuffers();
//        while (true) {
//            int encoderStatus = mAudioCodes.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
//            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) { //等待超时，需要再次等待，通常忽略
//                return;
//            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) { //输出缓冲区改变，通常忽略
//                encoderOutputBuffers = mAudioCodes.getOutputBuffers();
//            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                MediaFormat newFormat = mAudioCodes.getOutputFormat();
//                mTrackIndex = mListener.onFormatChanged(MuxerWapper.DATA_AUDIO, newFormat);
//                mListener.onStart(MuxerWapper.DATA_AUDIO);
//            } else if (encoderStatus < 0) {
//
//            } else {
//                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
//
//                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
//                        && mBufferInfo.size > 0) {
//                    mBufferInfo.presentationTimeUs = mListener.getPTSUs() - 2000;
//                    encodedData.position(mBufferInfo.offset);
//                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
//                    mListener.writeData(MuxerWapper.DATA_AUDIO, mTrackIndex, encodedData, mBufferInfo);
//                    mAudioCodes.releaseOutputBuffer(encoderStatus, false);
//                }
//
//                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                    break;
//                }
//            }
//        }
    }

    @Override
    protected void start(EncoderConfig config) {
        synchronized (mReadyFence) {
            if (mRunning) {
                return;
            }
            mRunning = true;
            new Thread(this, "AudioEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ignore) {
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_AUDIO_STEP));
    }

    @Override
    protected void stop() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
    }

    @Override
    protected void pause() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_PAUSE_RECORDING));
    }

    @Override
    protected void resume() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_RESUME_RECORDING));
    }

    @Override
    protected void handleResumeRecording() {
        isPause = false;
        mAudioRecorder.startRecording();
    }

    @Override
    protected void handlePauseRecording() {
        isPause = true;
        mAudioRecorder.stop();
    }

    @Override
    protected void handleUpdateSharedContext(EncoderConfig obj) {

    }

    @Override
    protected void handleSetTexture(int textureId) {

    }

    @Override
    protected void handleFrameAvailable(long timestamp) {

    }

    @Override
    protected void handleStopRecording() {
        isStop = true;
        mListener.onStop(MuxerWapper.DATA_AUDIO);
    }

    @Override
    protected void handleStartRecording(EncoderConfig obj) {
        try {
            isPause = false;
            isStop = false;
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loop
     */
    @Override
    protected void handleAudioStep() {
        if (!isStop) {
            if (!isPause) {
                recordEncorder();
                drainEncoder();
                mHandler.sendMessage(mHandler.obtainMessage(MSG_AUDIO_STEP));
            } else {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_AUDIO_STEP));
            }
        }
    }
}

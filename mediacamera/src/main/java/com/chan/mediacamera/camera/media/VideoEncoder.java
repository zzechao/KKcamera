package com.chan.mediacamera.camera.media;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import com.chan.mediacamera.camera.filter.NoFilter;
import com.chan.mediacamera.camera.gles.EglCore;
import com.chan.mediacamera.camera.gles.WindowSurface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoEncoder extends Encoder {

    private EglCore mEglCore;
    private WindowSurface mInputWindowSurface;
    private NoFilter showFilter;
    private int mTextureId;

    private final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private final int FRAME_RATE = 25;
    private final float BPP = 0.25f;
    private final int I_FRAME_INTERVAL = 5; // I帧间隔
    private int mBitRate;
    private int mWidth;
    private int mHeight;

    // 高清录制时的帧率倍数
    private static final int HDValue = 4;
    // 是否允许高清
    private boolean isEnableHD = false;

    protected MediaCodec mMediaCodec;
    private MediaCodec.BufferInfo mBufferInfo;
    private Surface mSurface;

    private MuxerEncoderListener mListener;
    private int mTrackIndex;

    public VideoEncoder(MuxerEncoderListener listener) {
        mListener = listener;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void run() {
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void init(int width, int height) throws IOException {
        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
        if (videoCodecInfo == null) {
            return;
        }
        mWidth = width;
        mHeight = height;
        mBitRate = (int) (mWidth * mHeight * FRAME_RATE * BPP / 2);

        mBufferInfo = new MediaCodec.BufferInfo();

        int videoWidth = width % 2 == 0 ? width : width - 1;
        int videoHeight = height % 2 == 0 ? height : height - 1;
        final MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, videoWidth, videoHeight);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);    // API >= 18

        //设置视频宽度
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, width);
        //设置视频高度
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, height);

        if (mBitRate > 0) {
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        } else {
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
        }
        //设置视频fps
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        //设置视频关键帧间隔，这里设置两秒一个关键帧
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            /**
             * 可选配置，设置码率模式
             * BITRATE_MODE_VBR：恒定质量
             * BITRATE_MODE_VBR：可变码率
             * BITRATE_MODE_CBR：恒定码率
             */
            mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            /**
             * 可选配置，设置H264 Profile
             * 需要做兼容性检查
             */
            mediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            /**
             * 可选配置，设置H264 Level
             * 需要做兼容性检查
             */
            mediaFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel31);
        }


        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // get Surface for encoder input
        // this method only can call between #configure and #start
        mSurface = mMediaCodec.createInputSurface();    // API >= 18
        mMediaCodec.start();
    }

    public void signalEndOfInputStream() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mMediaCodec.signalEndOfInputStream();
        }
    }

    private void drainEncoder() {
        final int TIMEOUT_USEC = 10000;

        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        while (true) {
            int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) { //等待超时，需要再次等待，通常忽略
                return;
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) { //输出缓冲区改变，通常忽略
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                mTrackIndex = mListener.onFormatChanged(MuxerWapper.DATA_VIDEO, newFormat);
                mListener.onStart(MuxerWapper.DATA_VIDEO);
            } else if (encoderStatus < 0) {

            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                        && mBufferInfo.size > 0) {
                    mBufferInfo.presentationTimeUs = mListener.getPTSUs();
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    mListener.writeData(MuxerWapper.DATA_VIDEO, mTrackIndex, encodedData, mBufferInfo);
                    mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }
    }

    public void release() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (showFilter != null) {
            showFilter.release();
            showFilter = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        mListener = null;
    }

    private int calcBitRate() {
        int bitrate = (int) (BPP * FRAME_RATE * mWidth * mHeight);
        if (isEnableHD) {
            bitrate *= HDValue;
        } else {
            bitrate *= 2;
        }
        return bitrate;
    }

    /**
     * 是否允许录制高清视频
     *
     * @param enable
     */
    public void enableHighDefinition(boolean enable) {
        isEnableHD = enable;
    }

    private Surface getInputSurface() {
        return mSurface;
    }

    public void updateSharedContext(EncoderConfig config) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, config));
    }

    public void setTextureId(int id) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
    }

    public void frameAvailable(SurfaceTexture st) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }

        float[] transform = new float[16];
        st.getTransformMatrix(transform);
        long timestamp = st.getTimestamp();
        if (timestamp == 0) {
            return;
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE,
                (int) (timestamp >> 32), (int) timestamp, transform));
    }

    protected static final MediaCodecInfo selectVideoCodec(final String mimeType) {
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    final int format = selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    protected static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
        int result = 0;
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
            colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
                result = colorFormat;
                break;
            }
        }
        return result;
    }

    protected static int[] recognizedFormats;

    static {
        recognizedFormats = new int[]{
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
        };
    }

    private static final boolean isRecognizedViewoFormat(final int colorFormat) {
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void start(EncoderConfig config) {
        synchronized (mReadyFence) {
            if (mRunning) {
                return;
            }
            mRunning = true;
            new Thread(this, "VideoEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ignore) {
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
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
        mListener.onResume();
    }

    @Override
    protected void handlePauseRecording() {
        mListener.onPause();
    }

    @Override
    protected void handleUpdateSharedContext(EncoderConfig config) {
        mInputWindowSurface.releaseEglSurface();
        showFilter.release();
        mEglCore.release();

        mEglCore = new EglCore(config.mEglContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        showFilter = new NoFilter(config.context);
        showFilter.onSurfaceCreated();
    }

    @Override
    protected void handleSetTexture(int textureId) {
        mTextureId = textureId;
    }

    @Override
    protected void handleFrameAvailable(long timestamp) {
        drainEncoder();

        showFilter.setTextureId(mTextureId);
        showFilter.onDrawFrame();

        mInputWindowSurface.setPresentationTime(timestamp);
        mInputWindowSurface.swapBuffers();
    }

    @Override
    protected void handleStopRecording() {
        mListener.onStop(MuxerWapper.DATA_VIDEO);
    }

    @Override
    protected void handleStartRecording(EncoderConfig config) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                init(config.width, config.height);

                mEglCore = new EglCore(config.mEglContext, EglCore.FLAG_RECORDABLE);
                mInputWindowSurface = new WindowSurface(mEglCore, getInputSurface(), true);
                mInputWindowSurface.makeCurrent();

                showFilter = new NoFilter(config.context);
                showFilter.onSurfaceCreated();
                showFilter.setSize(config.width, config.height);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void handleAudioStep() {

    }
}

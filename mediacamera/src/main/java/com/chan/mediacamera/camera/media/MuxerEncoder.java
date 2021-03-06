package com.chan.mediacamera.camera.media;//package com.chan.mediacamera.camera.media;
//
//import android.graphics.SurfaceTexture;
//import android.media.MediaCodec;
//import android.media.MediaFormat;
//import android.media.MediaMuxer;
//import android.os.Build;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.Message;
//import android.support.annotation.RequiresApi;
//
//import java.io.IOException;
//import java.lang.ref.WeakReference;
//import java.nio.ByteBuffer;
//
//import com.chan.mediacamera.camera.filter.NoFilter;
//import com.chan.mediacamera.camera.gles.EglCore;
//import com.chan.mediacamera.camera.gles.WindowSurface;
//
//public class MuxerEncoder implements Runnable, MuxerEncoderListener {
//
//    private static final int MSG_START_RECORDING = 1;
//    private static final int MSG_STOP_RECORDING = 2;
//    private static final int MSG_FRAME_AVAILABLE = 3;
//    private static final int MSG_SET_TEXTURE_ID = 4;
//    private static final int MSG_UPDATE_SHARED_CONTEXT = 5;
//    private static final int MSG_QUIT = 6;
//    private static final int MSG_PAUSE_RECORDING = 7;
//    private static final int MSG_RESUME_RECORDING = 8;
//
//    /**
//     * 锁，同步EncoderHandler创建，再释放锁
//     */
//    private byte[] mReadyFence = new byte[0];
//
//    private EncoderHandler mHandler;
//
//    private boolean mReady = false;
//    private boolean mRunning = false;
//
//    private VideoEncoder3 mVideoEncoder;
//
//    /**
//     * 输入surface
//     */
//    private EglCore mEglCore;
//    private WindowSurface mInputWindowSurface;
//    private NoFilter showFilter;
//    private int mTextureId;
//
//    private boolean mMuxerStarted;
//    private long pauseBeginNans;
//    private MediaMuxer mMediaMuxer;
//    private long pauseTotalTime;
//
//    /**
//     * 创建专门视频录制的loop（message信息传送带）
//     */
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    @Override
//    public void run() {
//        Looper.prepare();
//        synchronized (mReadyFence) {
//            mHandler = new EncoderHandler(this);
//            mReady = true;
//            mReadyFence.notify();
//        }
//        Looper.loop();
//    }
//
//    /**
//     * 开启录制
//     *
//     * @param config
//     */
//    public void startRecording(EncoderConfig config) {
//        synchronized (mReadyFence) {
//            if (mRunning) {
//                return;
//            }
//            mRunning = true;
//            new Thread(this, "MuxerEncoder").start();
//            while (!mReady) {
//                try {
//                    mReadyFence.wait();
//                } catch (InterruptedException ignore) {
//                }
//            }
//        }
//
//        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
//    }
//
//    /**
//     * 停止录制
//     */
//    public void stopRecording() {
//        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
//        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
//    }
//
//    /**
//     * 暂停录制
//     */
//    public void pauseRecording() {
//        mHandler.sendMessage(mHandler.obtainMessage(MSG_PAUSE_RECORDING));
//    }
//
//    /**
//     * 继续录制
//     */
//    public void resumeRecording() {
//        mHandler.sendMessage(mHandler.obtainMessage(MSG_RESUME_RECORDING));
//    }
//
//    /**
//     * @return
//     */
//    public boolean isRecording() {
//        synchronized (mReadyFence) {
//            return mRunning;
//        }
//    }
//
//    /**
//     * 更新EGLContext
//     *
//     * @param config
//     */
//    public void updateSharedContext(EncoderConfig config) {
//        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, config));
//    }
//
//    public void frameAvailable(SurfaceTexture st) {
//        synchronized (mReadyFence) {
//            if (!mReady) {
//                return;
//            }
//        }
//
//        float[] transform = new float[16];
//        st.getTransformMatrix(transform);
//        long timestamp = st.getTimestamp();
//        if (timestamp == 0) {
//            return;
//        }
//
//        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE,
//                (int) (timestamp >> 32), (int) timestamp, transform));
//    }
//
//    public void setTextureId(int id) {
//        synchronized (mReadyFence) {
//            if (!mReady) {
//                return;
//            }
//        }
//        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
//    }
//
//
//    /**
//     * 是否已经开始start
//     *
//     * @return
//     */
//    @Override
//    public boolean isStart() {
//        return mMuxerStarted;
//    }
//
//    /**
//     * 改变muxer输入渠道
//     *
//     * @param newFormat
//     */
//    @Override
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    public int onFormatChanged(MediaFormat newFormat) {
//        int trackIndex = mMediaMuxer.addTrack(newFormat);
//        mMediaMuxer.start();
//        mMuxerStarted = true;
//        return trackIndex;
//    }
//
//    /**
//     * 写信息
//     *
//     * @param mTrackIndex
//     * @param encodedData
//     * @param mBufferInfo
//     */
//    @Override
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    public void writeData(int mTrackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo mBufferInfo) {
//        mMediaMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
//    }
//
//    /**
//     * 获取录制时间
//     * @return
//     */
//    @Override
//    public long getPTSUs() {
//        long result = System.nanoTime();
//        return (result - pauseTotalTime) / 1000L;
//    }
//
//    @Override
//    public void onStart() {
//
//    }
//
//    @Override
//    public void onStop() {
//
//    }
//
//    @Override
//    public void onResume() {
//
//    }
//
//    @Override
//    public void onPause() {
//
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    private static class EncoderHandler extends Handler {
//        private WeakReference<MuxerEncoder> mWeakEncoder;
//
//        public EncoderHandler(MuxerEncoder encoder) {
//            mWeakEncoder = new WeakReference<MuxerEncoder>(encoder);
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            int what = msg.what;
//            Object obj = msg.obj;
//            MuxerEncoder encoder = mWeakEncoder.get();
//            if (encoder == null) {
//                return;
//            }
//
//            switch (what) {
//                case MSG_START_RECORDING:
//                    encoder.handleStartRecording((EncoderConfig) obj);
//                    break;
//                case MSG_STOP_RECORDING:
//                    encoder.handleStopRecording();
//                    break;
//                case MSG_FRAME_AVAILABLE:
//                    long timestamp = (((long) msg.arg1) << 32) |
//                            (((long) msg.arg2) & 0xffffffffL);
//                    encoder.handleFrameAvailable(timestamp);
//                    break;
//                case MSG_SET_TEXTURE_ID:
//                    encoder.handleSetTexture(msg.arg1);
//                    break;
//                case MSG_UPDATE_SHARED_CONTEXT:
//                    encoder.handleUpdateSharedContext((EncoderConfig) obj);
//                    break;
//                case MSG_PAUSE_RECORDING:
//                    encoder.handlePauseRecording();
//                    break;
//                case MSG_RESUME_RECORDING:
//                    encoder.handleResumeRecording();
//                    break;
//                case MSG_QUIT:
//                    Looper.myLooper().quit();
//                    break;
//                default:
//            }
//        }
//    }
//
//
//    /**
//     *
//     */
//    private void handleResumeRecording() {
//        pauseTotalTime += System.nanoTime() - pauseBeginNans;
//    }
//
//    /**
//     *
//     */
//    private void handlePauseRecording() {
//        pauseBeginNans = System.nanoTime();
//    }
//
//    /**
//     * Handler回调
//     *
//     * @param config
//     */
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    private void handleStartRecording(EncoderConfig config) {
//        try {
//            mMediaMuxer = new MediaMuxer(config.outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//
//            mVideoEncoder = new VideoEncoder3(this);
//            mVideoEncoder.start(config.width, config.height);
//
//            mEglCore = new EglCore(config.mEglContext, EglCore.FLAG_RECORDABLE);
//            mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
//            mInputWindowSurface.makeCurrent();
//
//            showFilter = new NoFilter(config.context);
//            showFilter.onSurfaceCreated();
//            showFilter.setSize(config.width, config.height);
//        } catch (IOException e) {
//
//        }
//    }
//
//    private void handleSetTexture(int id) {
//        mTextureId = id;
//    }
//
//    /**
//     * @param timestampNanos
//     */
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    private void handleFrameAvailable(long timestampNanos) {
//        mVideoEncoder.drainEncoder();
//
//        showFilter.setTextureId(mTextureId);
//        showFilter.onDrawFrame();
//
//        mInputWindowSurface.setPresentationTime(timestampNanos);
//        mInputWindowSurface.swapBuffers();
//    }
//
//    /**
//     * @param config
//     */
//    private void handleUpdateSharedContext(EncoderConfig config) {
//        mInputWindowSurface.releaseEglSurface();
//        showFilter.release();
//        mEglCore.release();
//
//        mEglCore = new EglCore(config.mEglContext, EglCore.FLAG_RECORDABLE);
//        mInputWindowSurface.recreate(mEglCore);
//        mInputWindowSurface.makeCurrent();
//
//        showFilter = new NoFilter(config.context);
//        showFilter.onSurfaceCreated();
//    }
//
//    /**
//     *
//     */
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    private void handleStopRecording() {
//        if (mVideoEncoder != null) {
//            mVideoEncoder.signalEndOfInputStream();
//            mVideoEncoder.release();
//            mVideoEncoder = null;
//        }
//        releaseEncoder();
//    }
//
//    /**
//     *
//     */
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    private void releaseEncoder() {
//        if (mMediaMuxer != null) {
//            mMediaMuxer.stop();
//            mMediaMuxer.release();
//            mMediaMuxer = null;
//        }
//        if (mInputWindowSurface != null) {
//            mInputWindowSurface.release();
//            mInputWindowSurface = null;
//        }
//        if (showFilter != null) {
//            showFilter.release();
//            showFilter = null;
//        }
//        if (mEglCore != null) {
//            mEglCore.release();
//            mEglCore = null;
//        }
//    }
//}

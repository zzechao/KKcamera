package viewset.com.kkcamera.view.camera.media;

import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;

import java.io.IOException;
import java.lang.ref.WeakReference;

import viewset.com.kkcamera.view.camera.filter.NoFilter;
import viewset.com.kkcamera.view.camera.gles.EglCore;
import viewset.com.kkcamera.view.camera.gles.WindowSurface;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MuxerEncoder implements Runnable {

    private static final int MSG_START_RECORDING = 1;
    private static final int MSG_STOP_RECORDING = 2;
    private static final int MSG_FRAME_AVAILABLE = 3;
    private static final int MSG_SET_TEXTURE_ID = 4;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 5;
    private static final int MSG_QUIT = 6;

    /**
     * 锁，同步EncoderHandler创建，再释放锁
     */
    private byte[] mReadyFence = new byte[0];

    private EncoderHandler mHandler;

    private boolean mReady = false;
    private boolean mRunning = false;

    private VideoEncoder mVideoEncoder;

    /**
     * 输入surface
     */
    private EglCore mEglCore;
    private WindowSurface mInputWindowSurface;

    /**
     *
     */
    private NoFilter showFilter;

    private MediaMuxer mMediaMuxer;

    /**
     * 创建专门视频录制的loop（message信息传送带）
     */
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

    /**
     * 开启录制
     *
     * @param config
     */
    public void startRecording(EncoderConfig config) {
        synchronized (mReadyFence) {
            if (mRunning) {
                return;
            }
            mRunning = true;
            new Thread(this, "MuxerEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ignore) {
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }

    /**
     * Handler回调
     * @param config
     */
    private void handleStartRecording(EncoderConfig config) {
        try {
            mMediaMuxer = new MediaMuxer(config.outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            mVideoEncoder = new VideoEncoder(mMediaMuxer);
            mVideoEncoder.prepare(config.width, config.height);

            mEglCore = new EglCore(config.mEglContext, EglCore.FLAG_RECORDABLE);
            mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
            mInputWindowSurface.makeCurrent();

            showFilter = new NoFilter(config.context);
            showFilter.onSurfaceCreated();
            showFilter.setSize(config.width, config.height);
        }catch (IOException e){

        }
    }

    private static class EncoderHandler extends Handler {
        private WeakReference<MuxerEncoder> mWeakEncoder;

        public EncoderHandler(MuxerEncoder encoder) {
            mWeakEncoder = new WeakReference<MuxerEncoder>(encoder);
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Object obj = msg.obj;
            MuxerEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:

                    break;
                case MSG_FRAME_AVAILABLE:

                    break;
                case MSG_SET_TEXTURE_ID:

                    break;
                case MSG_UPDATE_SHARED_CONTEXT:

                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
                default:
            }
        }
    }
}

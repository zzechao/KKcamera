package viewset.com.kkcamera.view.camera.media;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;

import java.lang.ref.WeakReference;



public abstract class Encoder implements Runnable{

    private static final int MSG_START_RECORDING = 1;
    private static final int MSG_STOP_RECORDING = 2;
    private static final int MSG_FRAME_AVAILABLE = 3;
    private static final int MSG_SET_TEXTURE_ID = 4;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 5;
    private static final int MSG_QUIT = 6;
    private static final int MSG_PAUSE_RECORDING = 7;
    private static final int MSG_RESUME_RECORDING = 8;

    /**
     * 锁，同步EncoderHandler创建，再释放锁
     */
    private byte[] mReadyFence = new byte[0];

    private EncoderHandler mHandler;
    private boolean mReady = false;

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
    private static class EncoderHandler extends Handler {
        private WeakReference<Encoder> mWeakEncoder;

        public EncoderHandler(Encoder encoder) {
            mWeakEncoder = new WeakReference<Encoder>(encoder);
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Object obj = msg.obj;
            Encoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) msg.arg1) << 32) |
                            (((long) msg.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable(timestamp);
                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(msg.arg1);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EncoderConfig) obj);
                    break;
                case MSG_PAUSE_RECORDING:
                    encoder.handlePauseRecording();
                    break;
                case MSG_RESUME_RECORDING:
                    encoder.handleResumeRecording();
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
                default:
            }
        }
    }

    protected abstract void handleResumeRecording();

    protected abstract void handlePauseRecording();

    protected abstract void handleUpdateSharedContext(EncoderConfig obj);

    protected abstract void handleSetTexture(int textureId);

    protected abstract void handleFrameAvailable(long timestamp);

    protected abstract void handleStopRecording();

    protected abstract void handleStartRecording(EncoderConfig obj);
}

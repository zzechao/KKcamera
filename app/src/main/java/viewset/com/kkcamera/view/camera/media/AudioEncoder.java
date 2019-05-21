package viewset.com.kkcamera.view.camera.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.RequiresApi;

public class AudioEncoder extends Encoder{

    private final String MIME_TYPE = "audio/mp4a-latm";   //音频编码的Mime
    private final int OUTPUT_AUDIO_BIT_RATE = 64 * 1024;
    private final int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    private int OUTPUT_AUDIO_CHANNEL_COUNT = 1;
    private int OUTPUT_AUDIO_SAMPLE_RATE_HZ = 48000;



    private int audioRate = 128000;   //音频编码的密钥比特率
    private int sampleRate = 48000;   //音频采样率
    private int channelCount = 2;     //音频编码通道数
    private int channelConfig = AudioFormat.CHANNEL_IN_STEREO;   //音频录制通道,默认为立体声
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT; //音频录制格式，默认为PCM16Bit

    private int bufferSize;
    private AudioRecord mRecorder;   //录音器
    private MediaCodec mAudioEnc;   //编码器，用于音频编码

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

    private void init() {
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
    }

    @Override
    protected void stop() {

    }

    @Override
    protected void pause() {

    }

    @Override
    protected void resume() {

    }

    @Override
    protected void handleResumeRecording() {

    }

    @Override
    protected void handlePauseRecording() {

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

    }

    @Override
    protected void handleStartRecording(EncoderConfig obj) {
        init();
    }
}

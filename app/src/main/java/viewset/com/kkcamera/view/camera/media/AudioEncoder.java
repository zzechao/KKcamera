package viewset.com.kkcamera.view.camera.media;

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
    private MediaCodec mAudioEncorder;   //编码器，用于音频编码

    private volatile boolean isPause = true;

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

    private void init() throws IOException {
        int minBufferSize = AudioRecord.getMinBufferSize(OUTPUT_AUDIO_SAMPLE_RATE_HZ,
                AUDIO_CONFIG, AUDIO_FORMAT);

        bufferSize = OUTPUT_AUDIO_SAMPLE_PER_RATE * fps;

        if (bufferSize < minBufferSize) {
            bufferSize = (minBufferSize / OUTPUT_AUDIO_SAMPLE_PER_RATE + 1) * OUTPUT_AUDIO_SAMPLE_PER_RATE * 2;
        }

        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, OUTPUT_AUDIO_SAMPLE_RATE_HZ, AUDIO_CONFIG,
                AUDIO_FORMAT, bufferSize);//初始化录音器

        MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, OUTPUT_AUDIO_SAMPLE_RATE_HZ, OUTPUT_AUDIO_CHANNEL_COUNT);//创建音频的格式,参数 MIME,采样率,通道数
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE);//编码方式
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BIT_RATE);//比特率
        mAudioEncorder = MediaCodec.createEncoderByType(MIME_TYPE);//创建音频编码器
        mAudioEncorder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);//配置

        mAudioEncorder.start();
        mAudioRecorder.startRecording();

        Log.e("ttt", "init");
    }

    private void recordEncorder() {

    }

    private void drainEncoder(){
        
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
    }

    @Override
    protected void handlePauseRecording() {
        isPause = true;
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
        try {
            isPause = false;
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
        if (!isPause) {
            recordEncorder();
            drainEncoder();
            mHandler.sendMessage(mHandler.obtainMessage(MSG_AUDIO_STEP));
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_AUDIO_STEP));
        }
    }


}

package viewset.com.kkcamera.view.camera.media;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MuxerWapper implements MuxerEncoderListener {

    private MediaMuxer mMediaMuxer;
    private VideoEncoder mVideoEncoder;
    private AudioEncoder mAudioEncoder;
    private volatile boolean mMuxerStarted;

    private volatile long pauseBeginNans;
    private volatile long pauseTotalTime;

    public static final int DATA_VIDEO = 1;
    public static final int DATA_AUDIO = 2;

    private final byte[] mLock = new byte[0];
    private final byte[] mVideoLock = new byte[0];
    private final byte[] mAudioLock = new byte[0];

    private boolean mVideoTrack, mAudioTrack;

    /**
     * 开始播放
     */
    public void startRecording(EncoderConfig config) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mMediaMuxer = new MediaMuxer(config.outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                mVideoEncoder = new VideoEncoder(this);
                mVideoEncoder.start(config);
                mAudioEncoder = new AudioEncoder(this);
                mAudioEncoder.start(config);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止播放
     */
    public void stopRecording() {
        mVideoEncoder.stop();
        mAudioEncoder.stop();
    }

    /**
     * 暂停播放
     */
    public void pauseRecording() {
        mVideoEncoder.pause();
        mAudioEncoder.pause();
    }

    /**
     * 恢复播放
     */
    public void resumeRecording() {
        mVideoEncoder.resume();
        mAudioEncoder.resume();
    }


    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public int onFormatChanged(int dataStats, MediaFormat newFormat) {
        Log.e("ttt", dataStats + "---addTrack");
        int mIndexTrack = mMediaMuxer.addTrack(newFormat);
        if (dataStats == DATA_VIDEO) {
            mVideoTrack = true;
        } else if (dataStats == DATA_AUDIO) {
            mAudioTrack = true;
        }
        return mIndexTrack;
    }


    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void writeData(int dataStats, int mTrackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo mBufferInfo) {
        Log.e("ttt", dataStats + "---writeData");
        synchronized (mLock) { // 锁进行mMediaMuxer.start();和writeSampleData互斥
            while (!mMuxerStarted) {
                Log.e("ttt", "---mLock.wait();");
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mMediaMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
    }

    @Override
    public boolean isStart() {
        return mMuxerStarted;
    }

    @Override
    public long getPTSUs() {
        long result = System.nanoTime();
        return (result - pauseTotalTime) / 1000L;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onStart() {
        synchronized (mLock) {
            Log.e("ttt", "onStart");
            if (mAudioTrack && mVideoTrack) {
                Log.e("ttt", "start");
                mMediaMuxer.start();
                mMuxerStarted = true;
                mLock.notify();
            }
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onStop() {
        mVideoTrack = false;
        mAudioTrack = false;
        if (mVideoEncoder != null) {
            mVideoEncoder.signalEndOfInputStream();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.release();
            mAudioEncoder = null;
        }
        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    @Override
    public void onResume() {
        pauseTotalTime += System.nanoTime() - pauseBeginNans;
    }

    @Override
    public void onPause() {
        pauseBeginNans = System.nanoTime();
    }

    /**
     * 更新视频录制信息
     *
     * @param config
     */
    public void updateSharedContext(EncoderConfig config) {
        if (mVideoEncoder != null) {
            mVideoEncoder.updateSharedContext(config);
        }
    }

    public void setTextureId(int textureId) {
        if (mVideoEncoder != null) {
            mVideoEncoder.setTextureId(textureId);
        }
    }

    public void frameAvailable(SurfaceTexture mSurfaceTexture) {
        if (mVideoEncoder != null) {
            mVideoEncoder.frameAvailable(mSurfaceTexture);
        }
    }
}

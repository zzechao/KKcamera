package viewset.com.kkcamera.view.camera.media;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MuxerWapper implements MuxerEncoderListener {

    private MediaMuxer mMediaMuxer;

    private VideoEncoder mVideoEncoder;
    private boolean mMuxerStarted;

    private volatile long pauseBeginNans;
    private volatile long pauseTotalTime;

    /**
     * 开始播放
     */
    public void startRecording(EncoderConfig config) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mMediaMuxer = new MediaMuxer(config.outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                mVideoEncoder = new VideoEncoder(this);
                mVideoEncoder.start(config);
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
    }

    /**
     * 暂停播放
     */
    public void pauseRecording() {
        mVideoEncoder.pause();
    }

    /**
     * 恢复播放
     */
    public void resumeRecording() {
        mVideoEncoder.resume();
    }


    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public int onFormatChanged(MediaFormat newFormat) {
        int trackIndex = mMediaMuxer.addTrack(newFormat);
        mMediaMuxer.start();
        mMuxerStarted = true;
        return trackIndex;
    }


    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void writeData(int mTrackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo mBufferInfo) {
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
    public void onStart() {

    }


    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onStop() {
        if (mVideoEncoder != null) {
            mVideoEncoder.signalEndOfInputStream();
            mVideoEncoder.release();
            mVideoEncoder = null;
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

package viewset.com.kkcamera.view.camera.media;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MuxerWapper implements MuxerEncoderListener {

    private MediaMuxer mMediaMuxer;

    private VideoEncoder2 mVideoEncoder;

    /**
     * 开始播放
     */
    public void startRecording(EncoderConfig config) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mMediaMuxer = new MediaMuxer(config.outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                mVideoEncoder = new VideoEncoder2(this);
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
    public int onFormatChanged(MediaFormat newFormat) {
        return 0;
    }

    @Override
    public void writeData(int mTrackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo mBufferInfo) {

    }

    @Override
    public boolean isStart() {
        return false;
    }

    @Override
    public long getPTSUs() {
        return 0;
    }

    @Override
    public void onStart() {

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
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
        if (mSurfaceTexture != null) {

        }
    }
}

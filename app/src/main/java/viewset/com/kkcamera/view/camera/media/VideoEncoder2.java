package viewset.com.kkcamera.view.camera.media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;

public class VideoEncoder2 extends Encoder{

    private MuxerEncoderListener mListener;

    public VideoEncoder2(MuxerEncoderListener listener) {
        mListener = listener;
    }

    public void start(int width, int height) throws IOException {
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

    }
}

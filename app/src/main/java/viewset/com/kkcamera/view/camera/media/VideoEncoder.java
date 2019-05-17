package viewset.com.kkcamera.view.camera.media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VideoEncoder {

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 25;
    private static final float BPP = 0.25f;
    private static final int I_FRAME_INTERVAL = 5; // I帧间隔
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

    private WeakReference<MuxerEncoder> weakReferenceMuxer;
    private int mTrackIndex;

    public VideoEncoder(MuxerEncoder muxerEncoder) {
        weakReferenceMuxer = new WeakReference<>(muxerEncoder);
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
        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, videoWidth, videoHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);    // API >= 18

        if (mBitRate > 0) {
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        } else {
            format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
        }
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);

        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // get Surface for encoder input
        // this method only can call between #configure and #start
        mSurface = mMediaCodec.createInputSurface();    // API >= 18
        mMediaCodec.start();
    }

    public void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;

        if (endOfStream) {
            mMediaCodec.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        while (true) {
            int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break;
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MuxerEncoder muxerEncoder = weakReferenceMuxer.get();
                if (muxerEncoder.isStart()) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                mTrackIndex = muxerEncoder.changeFormat(newFormat);
            } else if (encoderStatus < 0) {

            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }

                MuxerEncoder muxerEncoder = weakReferenceMuxer.get();
                if (mBufferInfo.size != 0) {
                    if (!muxerEncoder.isStart()) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    muxerEncoder.writeData(mTrackIndex, encodedData, mBufferInfo);

                    Log.d("ttt", "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                            mBufferInfo.presentationTimeUs);
                }

                mMediaCodec.releaseOutputBuffer(encoderStatus, false);

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

    public Surface getInputSurface() {
        return mSurface;
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
}

package com.chan.mediacamera.camera.media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaMuxer;
import android.os.Build;
import android.view.Surface;

public class Media {

    /**
     * 视频
     */
    private static final int TIMEOUT_USEC = 10000;
    private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc";
    private static final int OUTPUT_VIDEO_BIT_RATE = 512 * 1024;
    private static final int OUTPUT_VIDEO_FRAME_RATE = 25;
    private static final int OUTPUT_VIDEO_IFRAME_INTERVAL = 10;
    private static int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;

    /**
     * 音频
     */
    private static final String OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private static final int OUTPUT_AUDIO_BIT_RATE = 64 * 1024;
    private static final int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    private int OUTPUT_AUDIO_CHANNEL_COUNT = 1;
    private int OUTPUT_AUDIO_SAMPLE_RATE_HZ = 48000;

    /**
     *
     */
    private MediaExtractor videoExtractor;
    private MediaExtractor audioExtractor;

    private Surface outputSurface;

    private MediaCodec videoDecoder;
    private MediaCodec audioDecoder;

    private MediaCodec videoEncoder;
    private MediaCodec audioEncoder;

    private MediaMuxer muxer;


    public Media(){
        getSupportColorFormat();
    }

    /**
     * 获取视频颜色格式
     */
    private void getSupportColorFormat() {
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo codecInfo = null;
        for (int i = 0; i < numCodecs && codecInfo == null; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (!info.isEncoder()) {
                continue;
            }
            String[] types = info.getSupportedTypes();
            boolean found = false;
            for (int j = 0; j < types.length && !found; j++) {
                if (types[j].equals(OUTPUT_VIDEO_MIME_TYPE)) {
                    found = true;
                }
            }
            if (!found)
                continue;
            codecInfo = info;
        }


        assert codecInfo != null;
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(OUTPUT_VIDEO_MIME_TYPE);

        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            if(capabilities.colorFormats[i] == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
                }
            }
        }
    }

    public void extractDecodeEncodeMux(){

    }
}

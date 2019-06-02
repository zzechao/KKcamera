package com.chan.mediacamera.camera.decoder;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MediaUtils {
    private static final String TAG = "MediaUtils";

    private static final MediaCodecList sMCL = new MediaCodecList(MediaCodecList.REGULAR_CODECS);

    private static boolean hasCodecForMime(boolean encoder, String mime) {
        for (MediaCodecInfo info : sMCL.getCodecInfos()) {
            if (encoder != info.isEncoder()) {
                continue;
            }

            for (String type : info.getSupportedTypes()) {
                if (type.equalsIgnoreCase(mime)) {
                    Log.i(TAG, "found codec " + info.getName() + " for mime " + mime);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasCodecForMimes(boolean encoder, String[] mimes) {
        for (String mime : mimes) {
            if (!hasCodecForMime(encoder, mime)) {
                Log.i(TAG, "no " + (encoder ? "encoder" : "decoder") + " for mime " + mime);
                return false;
            }
        }
        return true;
    }

    public static boolean hasDecoder(String... mimes) {
        return hasCodecForMimes(false /* encoder */, mimes);
    }

    public static MediaExtractor createMediaExtractorForMimeType(
            Context context, String path, String mimeTypePrefix)
            throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(path, null);
        int trackIndex;
        for (trackIndex = 0; trackIndex < extractor.getTrackCount(); trackIndex++) {
            MediaFormat trackMediaFormat = extractor.getTrackFormat(trackIndex);
            if (trackMediaFormat.getString(MediaFormat.KEY_MIME).startsWith(mimeTypePrefix)) {
                extractor.selectTrack(trackIndex);
                break;
            }
        }
        if (trackIndex == extractor.getTrackCount()) {
            extractor.release();
            throw new IllegalStateException("couldn't get a track for " + mimeTypePrefix);
        }

        return extractor;
    }
}

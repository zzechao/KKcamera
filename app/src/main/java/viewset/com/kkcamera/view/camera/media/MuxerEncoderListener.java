package viewset.com.kkcamera.view.camera.media;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public interface MuxerEncoderListener {
    int onFormatChanged(MediaFormat newFormat);

    void writeData(int mTrackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo mBufferInfo);

    boolean isStart();

    long getPTSUs();

    /**
     * 遵循Looper的MessageQueue顺序
     */
    void onStart();

    void onStop();

    void onResume();

    void onPause();
}

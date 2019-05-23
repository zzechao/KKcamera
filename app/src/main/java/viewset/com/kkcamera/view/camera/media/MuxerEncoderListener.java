package viewset.com.kkcamera.view.camera.media;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public interface MuxerEncoderListener {
    int onFormatChanged(int dataStats,MediaFormat newFormat);

    void writeData(int dataStats, int mTrackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo mBufferInfo);

    boolean isStart();

    long getPTSUs();

    /**
     * 遵循Looper的MessageQueue顺序
     */
    void onStart(int dataStats);

    void onStop(int dataStats);

    void onResume();

    void onPause();
}

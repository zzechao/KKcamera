package viewset.com.kkcamera.view.camera.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MuxerWapper implements MuxerEncoderListener{

    private MediaMuxer mMediaMuxer;

    private VideoEncoder2 videoEncoder2;

    /**
     * 开始播放
     */
    public void startRecording(EncoderConfig config){
        try {
            mMediaMuxer = new MediaMuxer(config.outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            videoEncoder2 = new VideoEncoder2(this);
            videoEncoder2.start(config);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止播放
     */
    public void stopRecording(){

    }

    /**
     * 暂停播放
     */
    public void pauseRecording(){

    }

    /**
     * 恢复播放
     */
    public void resumeRecording(){

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
}

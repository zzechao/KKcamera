package com.chan.mediacamera.clip;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.chan.mediacamera.decoder.Decoder;
import com.chan.mediacamera.decoder.DecoderConfig;
import com.seu.magicfilter.filter.base.gpuimage.GPUImageFilter;
import com.seu.magicfilter.filter.helper.MagicFilterFactory;
import com.seu.magicfilter.filter.helper.MagicFilterType;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.media.MediaExtractor.SEEK_TO_PREVIOUS_SYNC;

/**
 * 重构
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaClipper {

    private VideoClipper mVideoClipper;
    private AudioClipper mAudioClipper;
    private String mInputPath;
    private String mOutputPath;
    private int mOutputWidth;
    private int mOutputHeight;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoRotation;
    private MediaMuxer mMediaMuxer;

    /**
     * 开始剪辑
     * @param context
     * @param startPostion
     * @param clipDur
     */
    public void startClip(Context context, long startPostion, long clipDur) {
        try {
            mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            ClipperConfig config = new ClipperConfig(context, mInputPath, startPostion, clipDur,
                    mOutputWidth, mOutputHeight, mVideoWidth, mVideoHeight);
            mVideoClipper = new VideoClipper(mMediaMuxer);
            mVideoClipper.start(config);

            mAudioClipper = new AudioClipper(mMediaMuxer);
            mAudioClipper.start(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOutputPath(String outputPath) {
        mOutputPath = outputPath;
    }

    public void setInputPath(String inputPath) {
        mInputPath = inputPath;
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        retr.setDataSource(mInputPath);
        String width = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String rotation = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        mVideoWidth = Integer.parseInt(width);
        mVideoHeight = Integer.parseInt(height);
        mVideoRotation = Integer.parseInt(rotation);
    }

    public void setOutputWidht(int mOutputWidht) {
        this.mOutputWidth = mOutputWidht;
    }

    public void setOutputHeight(int mOutputHeight) {
        this.mOutputHeight = mOutputHeight;
    }

    /**
     * 视频剪辑
     */
    class VideoClipper extends Clipper {

        private MediaExtractor mVideoExtractor;
        private MediaMuxer mMediaMuxer;
        private MediaCodec mVideoEncoder;
        private MediaCodec mVideoDecoder;
        private InputSurface mInputSurface;
        private OutputSurface mOutputSurface;

        public VideoClipper(MediaMuxer mediaMuxer) {
            mMediaMuxer = mediaMuxer;
        }

        @Override
        public String getThreadName() {
            return getClass().getName();
        }

        @Override
        protected void handleMsgStart(ClipperConfig config) {
            try {
                mVideoExtractor = new MediaExtractor();
                mVideoExtractor.setDataSource(config.mPath);
                int mVideoTrackIndex = -1;
                MediaFormat mVideoFormat = null;
                //音轨和视轨初始化
                for (int i = 0; i < mVideoExtractor.getTrackCount(); i++) {
                    MediaFormat format = mVideoExtractor.getTrackFormat(i);
                    if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                        mVideoTrackIndex = i;
                        mVideoFormat = format;
                        break;
                    }
                }

                assert mVideoTrackIndex != -1;
                mVideoExtractor.selectTrack(mVideoTrackIndex);
                long firstVideoTime = mVideoExtractor.getSampleTime();
                mVideoExtractor.seekTo(firstVideoTime + config.startPosition, SEEK_TO_PREVIOUS_SYNC);

                mVideoEncoder = MediaCodec.createDecoderByType("video/avc");
                mVideoDecoder = MediaCodec.createDecoderByType("video/avc");

                // decoder
                Context context = config.mWeakReference.get();
                assert context != null;
                mOutputSurface = new OutputSurface(context);
                mOutputSurface.setInputSize(config.mOutputWidth, config.mOutputHeight);
                mOutputSurface.setVideoSize(config.mVideoWidth, config.mVideoHeight);
                mVideoDecoder.configure(mVideoFormat, mOutputSurface.getSurface(), null, 0);
                mVideoDecoder.start();//解码器启动

                // Encoder
                MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", config.mOutputWidth, config.mOutputHeight);
                int mBitRate = (int) (config.mVideoWidth * config.mVideoHeight * 25 * 0.25 / 6);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
                mVideoEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mInputSurface = new InputSurface(mVideoEncoder.createInputSurface());
                mInputSurface.makeCurrent();
                mVideoEncoder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void handleMsgStop() {

        }
    }

    /**
     * 音频剪辑
     */
    class AudioClipper extends Clipper {

        private MediaExtractor mAudioExtractor;
        private MediaMuxer mMediaMuxer;
        private MediaCodec mAudioEncoder;
        private MediaCodec mAudioDecoder;

        public AudioClipper(MediaMuxer mediaMuxer) {
            mMediaMuxer = mediaMuxer;
        }

        @Override
        public String getThreadName() {
            return getClass().getName();
        }

        @Override
        protected void handleMsgStart(ClipperConfig config) {
            try {
                mAudioExtractor = new MediaExtractor();
                mAudioExtractor.setDataSource(config.mPath);
                int mAudioTrackIndex = -1;
                MediaFormat mAudioFormat = null;
                //音轨和视轨初始化
                for (int i = 0; i < mAudioExtractor.getTrackCount(); i++) {
                    MediaFormat format = mAudioExtractor.getTrackFormat(i);
                    if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                        mAudioTrackIndex = i;
                        mAudioFormat = format;
                        break;
                    }
                }

                assert mAudioTrackIndex != -1;
                mAudioExtractor.selectTrack(mAudioTrackIndex);
                long firstVideoTime = mAudioExtractor.getSampleTime();
                mAudioExtractor.seekTo(firstVideoTime + config.startPosition, SEEK_TO_PREVIOUS_SYNC);



            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void handleMsgStop() {

        }
    }

    static abstract class Clipper implements Runnable {

        final byte[] mReadyFence = new byte[0];
        private boolean mReady;
        private boolean mRunning;
        private ClipperHandler mHandler;

        private static final int MSG_START_CLIPING = 1;
        private static final int MSG_STOP_CLIPING = 2;


        @Override
        public void run() {
            Looper.prepare();
            synchronized (mReadyFence) {
                mHandler = new ClipperHandler(this);
                mReady = true;
                mReadyFence.notify();
            }
            Looper.loop();
        }

        public void start(ClipperConfig config) {
            synchronized (mReadyFence) {
                if (mRunning) {
                    return;
                }
                mRunning = true;
                new Thread(this, getThreadName()).start();
                while (!mReady) {
                    try {
                        mReadyFence.wait();
                    } catch (InterruptedException e) {

                    }
                }
            }

            mHandler.sendMessage(mHandler.obtainMessage(MSG_START_CLIPING, config));
        }

        public void stop() {
            synchronized (mReadyFence) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_CLIPING));
            }
        }

        public abstract String getThreadName();

        public static class ClipperHandler extends Handler {

            WeakReference<Clipper> mWeakReference;

            public ClipperHandler(Clipper clipper) {
                mWeakReference = new WeakReference<>(clipper);
            }

            @Override
            public void dispatchMessage(Message msg) {
                Object obj = msg.obj;
                Clipper clipper = mWeakReference.get();
                assert clipper != null;
                switch (msg.what) {
                    case MSG_START_CLIPING:
                        clipper.handleMsgStart((ClipperConfig) obj);
                        break;
                    case MSG_STOP_CLIPING:
                        clipper.handleMsgStop();
                        break;
                }
            }
        }

        protected abstract void handleMsgStart(ClipperConfig obj);

        protected abstract void handleMsgStop();
    }

    public class ClipperConfig {
        String mPath;
        long startPosition;
        long clipDur;
        int mOutputWidth;
        int mOutputHeight;
        int mVideoWidth;
        int mVideoHeight;
        WeakReference<Context> mWeakReference;

        public ClipperConfig(Context context, String mPath, long startPosition, long clipDur, int outputWidth, int currentHeight, int videoWidth, int videoHeight) {
            this.mPath = mPath;
            this.startPosition = startPosition;
            this.clipDur = clipDur;
            this.mOutputWidth = outputWidth;
            this.mOutputHeight = currentHeight;
            this.mVideoWidth = videoWidth;
            this.mVideoHeight = videoHeight;
            this.mWeakReference = new WeakReference<>(context);
        }

    }
}

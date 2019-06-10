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
    private MediaMuxer mMediaMuxer;

    public void setOutputPath(String outputPath) {
        mOutputPath = outputPath;
    }

    public void setInputPath(String inputPath) {
        mInputPath = inputPath;
    }

    public void startClip(long startPostion, long clipDur) {
        try {
            mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            ClipperConfig config = new ClipperConfig(mInputPath, startPostion, clipDur);
            mVideoClipper = new VideoClipper();
            mVideoClipper.start(config);

            mAudioClipper = new AudioClipper();
            mAudioClipper.start(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    class VideoClipper extends Clipper {

        private MediaExtractor mVideoExtractor;

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
                MediaFormat mVideoFormat;
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


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void handleMsgStop() {

        }
    }

    class AudioClipper extends Clipper {

        private MediaExtractor mAudioExtractor;

        @Override
        public String getThreadName() {
            return getClass().getName();
        }

        @Override
        protected void handleMsgStart(ClipperConfig obj) {
            try {
                mAudioExtractor = new MediaExtractor();
                mAudioExtractor.setDataSource(obj.mPath);
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

        public ClipperConfig(String mPath, long startPosition, long clipDur) {
            this.mPath = mPath;
            this.startPosition = startPosition;
            this.clipDur = clipDur;
        }
    }
}

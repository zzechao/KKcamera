package com.chan.mediacamera.camera.decoder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaSync;
import android.media.MediaTimestamp;
import android.media.PlaybackParams;
import android.media.SyncParams;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for the MediaSync API and local video/audio playback.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class MediaSyncTest {
    private static final String LOG_TAG = "MediaSyncTest";

    private final long NO_TIMESTAMP = -1;
    private final float FLOAT_PLAYBACK_RATE_TOLERANCE = .02f;
    private final long TIME_MEASUREMENT_TOLERANCE_US = 20000;
    private final int APPLICATION_AUDIO_PERIOD_MS = 200;
    private final int TEST_MAX_SPEED = 2;
    private Context mContext;
    private MediaSync mMediaSync = null;
    private Surface mSurfaceHolder = null;
    private Surface mSurface = null;
    private AudioTrack mAudioTrack = null;

    private Decoder mDecoderVideo = null;
    private Decoder mDecoderAudio = null;
    private boolean mHasAudio = false;
    private boolean mHasVideo = false;
    private boolean mEosAudio = false;
    private boolean mEosVideo = false;
    private final Object mConditionEos = new Object();
    private final Object mConditionEosAudio = new Object();


    public MediaSyncTest(Surface mSurface) {
        mSurfaceHolder = mSurface;
        mMediaSync = new MediaSync();
        mDecoderVideo = new Decoder(this, mMediaSync, false);
        mDecoderAudio = new Decoder(this, mMediaSync, true);
    }

    public void tearDown() throws Exception {
        if (mMediaSync != null) {
            onEos(mDecoderAudio);
            onEos(mDecoderVideo);
            mMediaSync.flush();
            mMediaSync.release();
            mMediaSync = null;
        }
        if (mDecoderAudio != null) {
            mDecoderAudio.release();
            mDecoderAudio = null;
        }
        if (mDecoderVideo != null) {
            mDecoderVideo.release();
            mDecoderVideo = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }

    private boolean reachedEos_l() {
        return ((!mHasVideo || mEosVideo) && (!mHasAudio || mEosAudio));
    }

    public void onEos(Decoder decoder) {
        synchronized (mConditionEosAudio) {
            if (decoder == mDecoderAudio) {
                mEosAudio = true;
                mConditionEosAudio.notify();
            }
        }

        synchronized (mConditionEos) {
            if (decoder == mDecoderVideo) {
                mEosVideo = true;
            }
            if (reachedEos_l()) {
                mConditionEos.notify();
            }
        }
    }

    /**
     * Tests playing back audio and video successfully.
     */
    public void testPlayAudioAndVideo(String fileUri) throws InterruptedException {
        playAV(fileUri, 5 * 60 * 1000 /* lastBufferTimestampMs */,
                true /* audio */, true /* video */, 10 * 60 * 1000 /* timeOutMs */);
    }

    private void playAV(
            final String inputResourceId,
            final long lastBufferTimestampMs,
            final boolean audio,
            final boolean video,
            int timeOutMs) throws InterruptedException {
        playAV(inputResourceId, lastBufferTimestampMs, audio, video, timeOutMs, 1.0f);
    }

    private void playAV(
            final String inputResourceId,
            final long lastBufferTimestampMs,
            final boolean audio,
            final boolean video,
            int timeOutMs,
            final float playbackRate) throws InterruptedException {
        final AtomicBoolean completed = new AtomicBoolean();
        Thread decodingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                completed.set(runPlayAV(inputResourceId, lastBufferTimestampMs * 1000,
                        audio, video, playbackRate));
            }
        });
        decodingThread.start();
        decodingThread.join(timeOutMs);
        if (!completed.get()) {
            throw new RuntimeException("timed out decoding to end-of-stream");
        }
    }

    private boolean runPlayAV(
            String inputResourceId,
            long lastBufferTimestampUs,
            boolean audio,
            boolean video,
            float playbackRate) {
        // allow 250ms for playback to get to stable state.
        final int PLAYBACK_RAMP_UP_TIME_MS = 250;

        final Object conditionFirstAudioBuffer = new Object();

        if (video) {
            mMediaSync.setSurface(mSurfaceHolder);
            mSurface = mMediaSync.createInputSurface();

            if (mDecoderVideo.setup(
                    inputResourceId, mSurface, lastBufferTimestampUs) == false) {
                return true;
            }
            mHasVideo = true;
        }

        if (audio) {
            if (mDecoderAudio.setup(inputResourceId, null, lastBufferTimestampUs) == false) {
                return true;
            }

            // get audio track.
            mAudioTrack = mDecoderAudio.getAudioTrack();

            mMediaSync.setAudioTrack(mAudioTrack);

            mMediaSync.setCallback(new MediaSync.Callback() {
                @Override
                public void onAudioBufferConsumed(
                        MediaSync sync, ByteBuffer byteBuffer, int bufferIndex) {
                    Decoder decoderAudio = mDecoderAudio;
                    if (decoderAudio != null) {
                        decoderAudio.releaseOutputBuffer(bufferIndex, NO_TIMESTAMP);
                    }
                    synchronized (conditionFirstAudioBuffer) {
                        conditionFirstAudioBuffer.notify();
                    }
                }
            }, null);

            mHasAudio = true;
        }

        SyncParams sync = new SyncParams().allowDefaults();
        mMediaSync.setSyncParams(sync);
        sync = mMediaSync.getSyncParams();

        mMediaSync.setPlaybackParams(new PlaybackParams().setSpeed(playbackRate));

        synchronized (conditionFirstAudioBuffer) {
            if (video) {
                mDecoderVideo.start();
            }
            if (audio) {
                mDecoderAudio.start();

                // wait for the first audio output buffer returned by media sync.
                try {
                    conditionFirstAudioBuffer.wait();
                } catch (InterruptedException e) {
                    Log.i(LOG_TAG, "worker thread is interrupted.");
                    return true;
                }
            }
        }

        if (audio) {
            try {
                Thread.sleep(PLAYBACK_RAMP_UP_TIME_MS);
            } catch (InterruptedException e) {
                Log.i(LOG_TAG, "worker thread is interrupted during sleeping.");
                return true;
            }

            MediaTimestamp mediaTimestamp = mMediaSync.getTimestamp();
            if (mediaTimestamp == null) {
                Log.d(LOG_TAG, "No timestamp available for starting");
                return true;
            }
            long checkStartTimeRealUs = System.nanoTime() / 1000;
            long checkStartTimeMediaUs = mediaTimestamp.getAnchorMediaTimeUs();

            synchronized (mConditionEosAudio) {
                if (!mEosAudio) {
                    try {
                        mConditionEosAudio.wait();
                    } catch (InterruptedException e) {
                        Log.i(LOG_TAG, "worker thread is interrupted when waiting for audio EOS.");
                        return true;
                    }
                }
            }
            mediaTimestamp = mMediaSync.getTimestamp();
            if (mediaTimestamp == null) {
                Log.d(LOG_TAG, "No timestamp available for ending");
                return true;
            }
            long playTimeUs = System.nanoTime() / 1000 - checkStartTimeRealUs;
            long mediaDurationUs = mediaTimestamp.getAnchorMediaTimeUs() - checkStartTimeMediaUs;
            if (!(Math.abs(mediaDurationUs - playTimeUs * playbackRate) <=
                    (mediaDurationUs * (sync.getTolerance() + FLOAT_PLAYBACK_RATE_TOLERANCE)
                            + TIME_MEASUREMENT_TOLERANCE_US))) {
                // sync.getTolerance() is MediaSync's tolerance of the playback rate, whereas
                // FLOAT_PLAYBACK_RATE_TOLERANCE is our test's tolerance.
                // We need to add both to get an upperbound for allowable error.
                Log.d(LOG_TAG, "Mediasync may have error in playback rate " + playbackRate
                        + ", play time is " + playTimeUs + " vs expected " + mediaDurationUs);
                return true;
            }
        }

        boolean completed = false;
        synchronized (mConditionEos) {
            if (!reachedEos_l()) {
                try {
                    mConditionEos.wait();
                } catch (InterruptedException e) {
                }
            }
            completed = reachedEos_l();
        }
        return completed;
    }

    private class Decoder extends MediaCodec.Callback {
        private MediaSyncTest mMediaSyncTest = null;
        private MediaSync mMediaSync = null;
        private boolean mIsAudio = false;
        private long mLastBufferTimestampUs = 0;

        private Surface mSurface = null;

        private AudioTrack mAudioTrack = null;

        private final Object mConditionCallback = new Object();
        private MediaExtractor mExtractor = null;
        private MediaCodec mDecoder = null;

        private final Object mAudioBufferLock = new Object();
        private List<AudioBuffer> mAudioBuffers = new LinkedList<AudioBuffer>();

        // accessed only on callback thread.
        private boolean mEos = false;
        private boolean mSignaledEos = false;

        private class AudioBuffer {
            public ByteBuffer mByteBuffer;
            public int mBufferIndex;

            public AudioBuffer(ByteBuffer byteBuffer, int bufferIndex) {
                mByteBuffer = byteBuffer;
                mBufferIndex = bufferIndex;
            }
        }

        private HandlerThread mHandlerThread;
        private Handler mHandler;

        Decoder(MediaSyncTest test, MediaSync sync, boolean isAudio) {
            mMediaSyncTest = test;
            mMediaSync = sync;
            mIsAudio = isAudio;
        }

        public boolean setup(String inputResourceId, Surface surface, long lastBufferTimestampUs) {
            if (!mIsAudio) {
                mSurface = surface;
                // handle video callback in a separate thread as releaseOutputBuffer is blocking
                mHandlerThread = new HandlerThread("SyncViewVidDec");
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper());
            }
            mLastBufferTimestampUs = lastBufferTimestampUs;
            try {
                // get extrator.
                String type = mIsAudio ? "audio/" : "video/";
                mExtractor = MediaUtils.createMediaExtractorForMimeType(
                        mContext, inputResourceId, type);

                // get decoder.
                MediaFormat mediaFormat =
                        mExtractor.getTrackFormat(mExtractor.getSampleTrackIndex());
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (!MediaUtils.hasDecoder(mimeType)) {
                    Log.i(LOG_TAG, "No decoder found for mimeType= " + mimeType);
                    return false;
                }
                mDecoder = MediaCodec.createDecoderByType(mimeType);
                mDecoder.configure(mediaFormat, mSurface, null, 0);
                mDecoder.setCallback(this, mHandler);

                return true;
            } catch (IOException e) {
                throw new RuntimeException("error reading input resource", e);
            }
        }

        public void start() {
            if (mDecoder != null) {
                mDecoder.start();
            }
        }

        public void release() {
            synchronized (mConditionCallback) {
                if (mDecoder != null) {
                    try {
                        mDecoder.stop();
                    } catch (IllegalStateException e) {
                    }
                    mDecoder.release();
                    mDecoder = null;
                }
                if (mExtractor != null) {
                    mExtractor.release();
                    mExtractor = null;
                }
            }

            if (mAudioTrack != null) {
                mAudioTrack.release();
                mAudioTrack = null;
            }
        }

        public int getBytesPerSample(int audioFormat) {
            switch (audioFormat) {
                case AudioFormat.ENCODING_PCM_8BIT:
                    return 1;
                case AudioFormat.ENCODING_PCM_16BIT:
                case AudioFormat.ENCODING_IEC61937:
                case AudioFormat.ENCODING_DEFAULT:
                    return 2;
                case AudioFormat.ENCODING_PCM_FLOAT:
                    return 4;
                case AudioFormat.ENCODING_INVALID:
                default:
                    throw new IllegalArgumentException("Bad audio format " + audioFormat);
            }
        }

        public AudioTrack getAudioTrack() {
            if (!mIsAudio) {
                throw new RuntimeException("can not create audio track for video");
            }

            if (mExtractor == null) {
                throw new RuntimeException("extrator is null");
            }

            if (mAudioTrack == null) {
                MediaFormat mediaFormat =
                        mExtractor.getTrackFormat(mExtractor.getSampleTrackIndex());
                int sampleRateInHz = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                int channelConfig = (mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1 ?
                        AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO);
                int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                int minBufferSizeInBytes = AudioTrack.getMinBufferSize(
                        sampleRateInHz,
                        channelConfig,
                        audioFormat);
                final int frameCount = APPLICATION_AUDIO_PERIOD_MS * sampleRateInHz / 1000;
                final int frameSizeInBytes = Integer.bitCount(channelConfig)
                        * getBytesPerSample(audioFormat);
                // ensure we consider application requirements for writing audio data
                minBufferSizeInBytes = TEST_MAX_SPEED /* speed influences buffer size , default is 2*/
                        * Math.max(minBufferSizeInBytes, frameCount * frameSizeInBytes);
                mAudioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRateInHz,
                        channelConfig,
                        audioFormat,
                        minBufferSizeInBytes,
                        AudioTrack.MODE_STREAM);
            }

            return mAudioTrack;
        }

        public void releaseOutputBuffer(int bufferIndex, long renderTimestampNs) {
            synchronized (mConditionCallback) {
                if (mDecoder != null) {
                    if (renderTimestampNs == NO_TIMESTAMP) {
                        mDecoder.releaseOutputBuffer(bufferIndex, false /* render */);
                    } else {
                        mDecoder.releaseOutputBuffer(bufferIndex, renderTimestampNs);
                    }
                }
            }
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
        }

        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            synchronized (mConditionCallback) {
                if (mExtractor == null || mExtractor.getSampleTrackIndex() == -1
                        || mSignaledEos || mDecoder != codec) {
                    return;
                }

                ByteBuffer buffer = codec.getInputBuffer(index);
                int size = mExtractor.readSampleData(buffer, 0);
                long timestampUs = mExtractor.getSampleTime();
                mExtractor.advance();
                mSignaledEos = mExtractor.getSampleTrackIndex() == -1
                        || timestampUs >= mLastBufferTimestampUs;
                codec.queueInputBuffer(
                        index,
                        0,
                        size,
                        timestampUs,
                        mSignaledEos ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
            }
        }

        @Override
        public void onOutputBufferAvailable(
                MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            synchronized (mConditionCallback) {
                if (mEos || mDecoder != codec) {
                    return;
                }

                mEos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

                if (info.size > 0) {
                    if (mIsAudio) {
                        ByteBuffer outputByteBuffer = codec.getOutputBuffer(index);
                        synchronized (mAudioBufferLock) {
                            mAudioBuffers.add(new AudioBuffer(outputByteBuffer, index));
                        }
                        mMediaSync.queueAudio(
                                outputByteBuffer,
                                index,
                                info.presentationTimeUs);
                    } else {
                        codec.releaseOutputBuffer(index, info.presentationTimeUs * 1000);
                    }
                } else {
                    codec.releaseOutputBuffer(index, false);
                }
            }

            if (mEos) {
                mMediaSyncTest.onEos(this);
            }
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
        }
    }
}

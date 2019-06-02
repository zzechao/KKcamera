package com.chan.mediacamera.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaSync;
import android.media.PlaybackParams;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import com.chan.mediacamera.camera.FBOVideoRenderer;
import com.chan.mediacamera.camera.decoder.AudioDecoder;
import com.chan.mediacamera.camera.decoder.Decoder;
import com.chan.mediacamera.camera.decoder.DecoderConfig;
import com.chan.mediacamera.camera.decoder.VideoDecoder;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * https://blog.csdn.net/nonmarking/article/details/78747210 音视频同步的文章
 * 先用MediaSync处理音视频同步
 */
public class VideoHardware3GLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, Decoder.DecoderListener {

    private static final String TAG = "VideoHardwareGLSurfaceView";

    private FBOVideoRenderer mRenderer;
    private String mPath;
    private VideoDecoder mVideoDecoder;
    private AudioDecoder mAudioDecoder;
    private MediaSync mSync;
    private boolean mVideoStart = false;
    private boolean mAudioStart = false;

    public VideoHardware3GLSurfaceView(Context context) {
        this(context, null);
    }

    public VideoHardware3GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
        initDecoder();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
        if (mVideoDecoder != null) {
            mVideoDecoder.stop();
        }
        if (mAudioDecoder != null) {
            mAudioDecoder.stop();
        }
        if (mRenderer != null) {
            mRenderer.releaseSurfaceTexture();
        }
    }


    private void init() {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        setPreserveEGLContextOnPause(true);//保存Context当pause时
        setCameraDistance(100);

        mRenderer = new FBOVideoRenderer(getContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mSync = new MediaSync();
        }
    }

    private void initDecoder() {
        mVideoDecoder = new VideoDecoder(this);
        mAudioDecoder = new AudioDecoder(this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        mRenderer.onSurfaceCreated(gl, config);
        mRenderer.getSurfaceTexture().setOnFrameAvailableListener(this);
        Surface mSurface = new Surface(mRenderer.getSurfaceTexture());
        if (mVideoDecoder != null) {
            mVideoDecoder.start(new DecoderConfig(25, mPath, mSurface));
        }
        if (mAudioDecoder != null) {
            mAudioDecoder.start(new DecoderConfig(25, mPath, null));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mSync.setPlaybackParams(new PlaybackParams().setSpeed(1.0f));
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //创建视频格式信息
        mRenderer.onSurfaceChanged(gl, width, height);
        mRenderer.setVideoSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mRenderer.onDrawFrame(gl);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    public void setPath(String path) {
        mPath = path;
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        mRenderer.setVideoSize(width, height);
    }

    @Override
    public void onStart(int decoderStats, MediaCodec decoder, MediaFormat mediaFormat, DecoderConfig config) {
        Log.e("ttt", "decoderStats : " + decoderStats);
        if (decoderStats == Decoder.DECODER_VIDEO && !mVideoStart) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mSync.setSurface(config.mSurface);
                decoder.configure(mediaFormat, mSync.createInputSurface(), null, 0);
            } else {
                decoder.configure(mediaFormat, config.mSurface, null, 0);
            }
            mVideoStart = true;
            decoder.start();
        } else if (decoderStats == Decoder.DECODER_AUDIO && !mAudioStart) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mSync.setAudioTrack(mAudioDecoder.getAudioTrack());
                mSync.setCallback(new MediaSync.Callback() {
                    @Override
                    public void onAudioBufferConsumed(MediaSync sync, ByteBuffer audioBuffer, int bufferId) {
                        audioBuffer.clear();
                        mAudioDecoder.releaseOutputBuffer(bufferId);
                    }
                },mAudioDecoder.getAndioHandler());
            } else {
                mAudioDecoder.getAudioTrack().play();
            }
            decoder.configure(mediaFormat, null, null, 0);
            mAudioStart = true;
            decoder.start();
        }
    }

    @Override
    public void onStop(int decoderStats) {
        if (decoderStats == Decoder.DECODER_VIDEO) {
            mVideoDecoder.release();
            mVideoStart = false;
        } else if (decoderStats == Decoder.DECODER_AUDIO) {
            mAudioDecoder.release();
            mAudioStart = false;
        }
        if (!mVideoStart && !mAudioStart && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mSync.setPlaybackParams(new PlaybackParams().setSpeed(0.f));
            mSync.release();
        }
    }

    @Override
    public void queueAudio(ByteBuffer copyBuffer, int outputIndex, long presentationTimeUs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mSync.queueAudio(copyBuffer, outputIndex, presentationTimeUs);
        }
    }
}

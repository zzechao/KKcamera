package com.chan.mediacamera.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaSync;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Surface;

import com.chan.mediacamera.camera.FBOVideoRenderer;
import com.chan.mediacamera.decoder.AudioDecoder;
import com.chan.mediacamera.decoder.Decoder;
import com.chan.mediacamera.decoder.DecoderConfig;
import com.chan.mediacamera.decoder.VideoDecoder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * https://blog.csdn.net/nonmarking/article/details/78747210 音视频同步的文章
 * 先用MediaSync处理音视频同步
 */
public class VideoHardwareGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, Decoder.DecoderListener {

    private static final String TAG = "VideoHardwareGLSurfaceView";

    private FBOVideoRenderer mRenderer;
    private String mPath;
    private VideoDecoder mVideoDecoder;
    private AudioDecoder mAudioDecoder;
    private MediaSync mSync;
    private boolean mVideoStart = false;
    private boolean mAudioStart = false;

    public VideoHardwareGLSurfaceView(Context context) {
        this(context, null);
    }

    public VideoHardwareGLSurfaceView(Context context, AttributeSet attrs) {
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

        mRenderer.onSurfaceCreated();
        mRenderer.getSurfaceTexture().setOnFrameAvailableListener(this);
        Surface mSurface = new Surface(mRenderer.getSurfaceTexture());
        if (mVideoDecoder != null) {
            mVideoDecoder.start(new DecoderConfig(25, mPath, mSurface));
        }
        if (mAudioDecoder != null) {
            mAudioDecoder.start(new DecoderConfig(25, mPath, null));
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //创建视频格式信息
        mRenderer.onSurfaceChanged(width, height);
        mRenderer.setVideoSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mRenderer.onDrawFrame();
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
        if (decoderStats == Decoder.DECODER_VIDEO && !mVideoStart) {
            decoder.configure(mediaFormat, config.mSurface, null, 0);
            mVideoStart = true;
            decoder.start();
        } else if (decoderStats == Decoder.DECODER_AUDIO && !mAudioStart) {
            mAudioDecoder.getAudioTrack().play();
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

    }
}

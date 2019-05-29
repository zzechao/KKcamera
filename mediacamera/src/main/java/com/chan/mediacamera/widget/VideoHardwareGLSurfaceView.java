package com.chan.mediacamera.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;

import com.chan.mediacamera.camera.FBOVideoRenderer;
import com.chan.mediacamera.camera.decoder.VideoDecoderThread;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VideoHardwareGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, MediaPlayer.OnVideoSizeChangedListener, SurfaceTexture.OnFrameAvailableListener {

    private FBOVideoRenderer mRenderer;
    private String mPath;
    private VideoDecoderThread mVideoDecoder;
    private Surface mSurface;

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
            mVideoDecoder.close();
            mVideoDecoder = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mRenderer != null) {
            mRenderer.releaseSurfaceTexture();
            mRenderer = null;
        }
    }

    private void init() {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        setPreserveEGLContextOnPause(true);//保存Context当pause时
        setCameraDistance(100);

        mRenderer = new FBOVideoRenderer(getContext());
    }

    private void initDecoder() {
        mVideoDecoder = new VideoDecoderThread();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        mRenderer.onSurfaceCreated(gl, config);
        mRenderer.getSurfaceTexture().setOnFrameAvailableListener(this);
        mSurface = new Surface(mRenderer.getSurfaceTexture());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //创建视频格式信息
        mRenderer.onSurfaceChanged(gl, width, height);

        if (mVideoDecoder != null) {
            if (mVideoDecoder.init(mSurface, mPath)) {
                mVideoDecoder.start();
            } else {
                mVideoDecoder = null;
            }
        }
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
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        mRenderer.setVideoSize(width, height);
    }
}

package com.chan.mediacamera.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import com.chan.mediacamera.camera.KKRenderer;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class KKVideoGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, MediaPlayer.OnVideoSizeChangedListener, SurfaceTexture.OnFrameAvailableListener {

    private KKRenderer mRenderer;
    private MediaPlayer mediaPlayer;
    private String mPath;

    public KKVideoGLSurfaceView(Context context) {
        this(context, null);
    }

    public KKVideoGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
        initMediaPlayer();
    }

    @Override
    public void onResume() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
        super.onPause();
    }

    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
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

        mRenderer = new KKRenderer(getContext());
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setLooping(true);
        mediaPlayer.setOnVideoSizeChangedListener(this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        try {
            mediaPlayer.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mRenderer.onSurfaceCreated(gl, config);
        mRenderer.getSurfaceTexture().setOnFrameAvailableListener(this);
        Surface surface = new Surface(mRenderer.getSurfaceTexture());
        mediaPlayer.setSurface(surface);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mediaPlayer.prepareAsync();
        Log.e("ttt","prepareAsync");
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                Log.e("ttt","onPrepared");
                mediaPlayer.start();
            }
        });
        mRenderer.onSurfaceChanged(gl, width, height);
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

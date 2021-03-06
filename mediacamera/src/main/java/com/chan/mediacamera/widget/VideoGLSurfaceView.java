package com.chan.mediacamera.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.chan.mediacamera.camera.FBOVideoRenderer;
import com.chan.mediacamera.clip.VideoClipper;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VideoGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, MediaPlayer.OnVideoSizeChangedListener, SurfaceTexture.OnFrameAvailableListener {

    private FBOVideoRenderer mRenderer;
    private MediaPlayer mediaPlayer;
    private String mPath;
    private VideoClipper videoClipper;


    public VideoGLSurfaceView(Context context) {
        this(context, null);
    }

    public VideoGLSurfaceView(Context context, AttributeSet attrs) {
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
        setKeepScreenOn(false);
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
        setCameraDistance(100);

        mRenderer = new FBOVideoRenderer(getContext());

        setKeepScreenOn(true);

        videoClipper = new VideoClipper(getContext());
        videoClipper.setOnVideoCutFinishListener(new VideoClipper.OnVideoCutFinishListener() {
            @Override
            public void onFinish() {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "完成", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build());
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
        mediaPlayer.setLooping(true);
        mediaPlayer.setOnVideoSizeChangedListener(this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        try {
            mediaPlayer.setDataSource(mPath);
            videoClipper.setInputVideoPath(mPath);

            videoClipper.setOutputVideoPath("/storage/emulated/0/VOD_CCTV6--裸露在狼群-" + System.currentTimeMillis() + ".mp4");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mRenderer.onSurfaceCreated();
        mRenderer.getSurfaceTexture().setOnFrameAvailableListener(this);
        Surface surface = new Surface(mRenderer.getSurfaceTexture());
        mediaPlayer.setSurface(surface);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d("ttt", "width : " + width + "---height : " + height);
        videoClipper.setScreenSize(width, height);
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.prepareAsync();
            Log.e("ttt", "prepareAsync");
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    Log.e("ttt", "onPrepared");
                    mediaPlayer.start();
                }
            });
        }
        mRenderer.onSurfaceChanged(width, height);
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
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.e("ttt", "width : " + width + "---height :" + height);
        mRenderer.setVideoSize(width, height);
    }

    public void clipVideo() {
        try {
            videoClipper.clipVideo(0, mediaPlayer.getDuration() * 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

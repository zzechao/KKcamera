package com.chan.mediacamera.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.Surface;

import com.chan.mediacamera.avplayer.MediaCodecPlayer;
import com.chan.mediacamera.camera.FBOVideoRenderer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * https://blog.csdn.net/nonmarking/article/details/78747210 音视频同步的文章
 * 先用MediaSync处理音视频同步
 */
public class VideoHardware2GLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "VideoHardwareGLSurfaceView";

    private FBOVideoRenderer mRenderer;
    private String mPath;
    private MediaCodecPlayer mMediaCodecPlayer;
    private static final int SLEEP_TIME_MS = 1000;
    private static final long PLAY_TIME_MS = TimeUnit.MILLISECONDS.convert(4, TimeUnit.MINUTES);

    public VideoHardware2GLSurfaceView(Context context) {
        this(context, null);
    }

    public VideoHardware2GLSurfaceView(Context context, AttributeSet attrs) {
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
        setKeepScreenOn(false);
        if (mMediaCodecPlayer != null) {
            mMediaCodecPlayer.reset();
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

        setKeepScreenOn(true);
    }

    private void initDecoder() {

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        mRenderer.onSurfaceCreated();
        mRenderer.getSurfaceTexture().setOnFrameAvailableListener(this);
        Surface mSurface = new Surface(mRenderer.getSurfaceTexture());
        mMediaCodecPlayer = new MediaCodecPlayer(mSurface);

        DecodeTask task = new DecodeTask();
        task.execute();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //创建视频格式信息
        mRenderer.onSurfaceChanged(width, height);
        mRenderer.setVideoSize(mMediaCodecPlayer.getmMediaFormatWidth(), mMediaCodecPlayer.getmMediaFormatHeight());
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

    public class DecodeTask extends AsyncTask<Void, Void, Boolean> {

        @SuppressLint("WrongThread")
        @Override
        protected Boolean doInBackground(Void... params) {
            //this runs on a new thread
            initializePlayer();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            //this runs on ui thread
        }
    }

    private void initializePlayer() {

        /*取音频 取视频*/
        mMediaCodecPlayer.setAudioDataSource(Uri.parse(mPath), null);
        mMediaCodecPlayer.setVideoDataSource(Uri.parse(mPath), null);
        mMediaCodecPlayer.start(); //from IDLE to PREPARING
        try {
            mMediaCodecPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mRenderer.setVideoSize(mMediaCodecPlayer.getmMediaFormatWidth(), mMediaCodecPlayer.getmMediaFormatHeight());

        // 开始播放视频
        mMediaCodecPlayer.startThread();

        long timeOut = System.currentTimeMillis() + 4 * PLAY_TIME_MS;
        while (timeOut > System.currentTimeMillis() && !mMediaCodecPlayer.isEnded()) {
            try {
                Thread.sleep(SLEEP_TIME_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (mMediaCodecPlayer.getCurrentPosition() >= mMediaCodecPlayer.getDuration()) {
                break;
            }
        }

        if (timeOut > System.currentTimeMillis()) {
            return;
        }

        mMediaCodecPlayer.reset();

        initializePlayer();
    }
}

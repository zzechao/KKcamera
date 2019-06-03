package com.chan.mediacamera.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;

import com.chan.mediacamera.camera.FBOVideoRenderer;
import com.chan.mediacamera.camera.decoder.DecoderConfig;
import com.chan.mediacamera.camera.decoder.MediaDecoder;

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
    private MediaDecoder mMediaDecoder;

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
    }

    private void initDecoder() {
        mMediaDecoder = new MediaDecoder();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        mRenderer.onSurfaceCreated(gl, config);
        mRenderer.getSurfaceTexture().setOnFrameAvailableListener(this);
        Surface mSurface = new Surface(mRenderer.getSurfaceTexture());
        if (mMediaDecoder != null) {
            mMediaDecoder.start(new DecoderConfig(25, mPath, mSurface));
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
}

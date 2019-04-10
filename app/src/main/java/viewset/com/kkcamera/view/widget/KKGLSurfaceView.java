package viewset.com.kkcamera.view.widget;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class KKGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private KKRenderer renderer;

    private int mWidth, mHeight;
    private KKCamera kkCamera;


    public KKGLSurfaceView(Context context) {
        this(context, null);
    }

    public KKGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {
        renderer = new KKRenderer(this, getContext());
        setEGLContextClientVersion(2);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        kkCamera = new KKCamera(getContext());
        kkCamera.setCameraCallback(new KKCamera.CameraCallback() {
            @Override
            public void configureTransform(int viewWidth, int viewHeight, int previewWidth, int previewHeight) {

            }

            @Override
            public void deviceOpened() {
                kkCamera.startPreview(renderer.getSurfaceTexture());
            }
        });
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        renderer.onSurfaceCreated(gl, config);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        renderer.onSurfaceChanged(gl, width, height);
        mWidth = width;
        mHeight = height;
        kkCamera.openCamera(mWidth, mHeight);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        renderer.onDrawFrame(gl);
    }

    @Override
    public void onResume() {
        super.onResume();
        kkCamera.startBackgroundThread();
        if (renderer.isAvailable()) {
            int width = mWidth == 0 ? getWidth() : mWidth;
            int height = mHeight == 0 ? getHeight() : mHeight;
            kkCamera.openCamera(width, height);
        } else {
            setRenderer(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        kkCamera.stopBackgroundThread();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        renderer.releaseSurfaceTexture();
    }


}

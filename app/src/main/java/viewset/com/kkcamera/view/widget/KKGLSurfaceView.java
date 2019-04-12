package viewset.com.kkcamera.view.widget;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import viewset.com.kkcamera.view.activity.camera.KitkatCamera;

public class KKGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    /**
     * Camera1
     */
    private int cameraId = 0;
    private KitkatCamera mCamera1;

    /**
     * Camera2
     */
    private KKRenderer renderer;
    private KKCamera mCamera2;

    private boolean useCamera2 = true;

    public KKGLSurfaceView(Context context) {
        this(context, null);
    }

    public KKGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        // 大于21使用camera2
        if (useCamera2) {
            mCamera2 = new KKCamera(getContext());
            mCamera2.setCameraCallback(new KKCamera.CameraCallback() {
                @Override
                public void configureTransform(int previewWidth, int previewHeight) {
                    renderer.setPreviewSize(previewWidth, previewHeight);
                }

                @Override
                public void deviceOpened() {
                    mCamera2.startPreview(renderer.getSurfaceTexture());
                }
            });
            renderer = new KKRenderer(getContext());
        } else {
            /**初始化相机的管理类*/
            mCamera1 = new KitkatCamera();
            renderer = new KKRenderer(getContext());
        }


    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        if (useCamera2) {
            renderer.onSurfaceCreated(gl, config);
            mCamera2.openCamera();
            renderer.getSurfaceTexture().setOnFrameAvailableListener(this);
        } else {
            renderer.onSurfaceCreated(gl, config);
            mCamera1.open(cameraId);
            Point point = mCamera1.getPreviewSize();
            renderer.setPreviewSize(point.x, point.y);
            mCamera1.setPreviewTexture(renderer.getSurfaceTexture());
            renderer.getSurfaceTexture().setOnFrameAvailableListener(this);
            mCamera1.preview();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        if (useCamera2) {
            renderer.onSurfaceChanged(gl, width, height);
        } else {
            renderer.onSurfaceChanged(gl, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (useCamera2) {
            renderer.onDrawFrame(gl);
        } else {
            renderer.onDrawFrame(gl);
        }
    }

    @Override
    public void onResume() {
        if (mCamera2 != null) {
            mCamera2.startBackgroundThread();
            if (renderer.isAvailable()) {
                mCamera2.openCamera();
            }
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (!useCamera2) {
            mCamera1.close();
        } else {
            mCamera2.closeCamera();
            mCamera2.stopBackgroundThread();
        }
        super.onPause();
    }

    public void onDestroy() {
        if (mCamera2 != null) {
            mCamera2.closeCamera();
        }
        renderer.releaseSurfaceTexture();
    }


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }
}

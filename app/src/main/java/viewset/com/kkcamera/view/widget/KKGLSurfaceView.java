package viewset.com.kkcamera.view.widget;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import viewset.com.kkcamera.view.activity.camera.CameraController;

public class KKGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private KKRenderer renderer;

    private int mWidth, mHeight;

    private CameraController mCamera;
    private KKCamera kkCamera;

    private int dataWidth;
    private int dataHeight;

    private boolean isSetParm = false;

    private int cameraId;

    private boolean useCamera2 = false;

    public KKGLSurfaceView(Context context) {
        this(context, null);
    }

    public KKGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {
        renderer = new KKRenderer(getContext());
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setPreserveEGLContextOnPause(true);//保存Context当pause时
        setCameraDistance(100);//相机距离

        // 大于21使用camera2
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && useCamera2) {
            kkCamera = new KKCamera(getContext());
            kkCamera.setCameraCallback(new KKCamera.CameraCallback() {
                @Override
                public void configureTransform(int viewWidth, int viewHeight, int previewWidth, int previewHeight) {
                    renderer.setPreviewSize(previewWidth, previewHeight);
                }

                @Override
                public void deviceOpened() {
                    kkCamera.startPreview(renderer.getSurfaceTexture());
                }
            });
        } else {
            /**初始化相机的管理类*/
            mCamera = new CameraController();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        renderer.onSurfaceCreated(gl, config);
        if (mCamera != null && !isSetParm) {
            open(cameraId);
            stickerInit();
            renderer.setPreviewSize(dataWidth, dataHeight);
        }
        SurfaceTexture texture = renderer.getSurfaceTexture();
        if (texture != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && kkCamera != null) {
                texture.setOnFrameAvailableListener(this, kkCamera.getCameraHandler());
            } else {
                texture.setOnFrameAvailableListener(this);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        renderer.onSurfaceChanged(gl, width, height);
        if (kkCamera != null) {
            mWidth = width;
            mHeight = height;
            kkCamera.openCamera(mWidth, mHeight);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        renderer.onDrawFrame(gl);
    }

    @Override
    public void onResume() {
        if (kkCamera != null) {
            kkCamera.startBackgroundThread();
            if (renderer.isAvailable()) {
                int width = mWidth == 0 ? getWidth() : mWidth;
                int height = mHeight == 0 ? getHeight() : mHeight;
                kkCamera.openCamera(width, height);
            }
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (kkCamera != null) {
            kkCamera.closeCamera();
            kkCamera.stopBackgroundThread();
        }
        super.onPause();
    }

    public void onDestroy() {
        if (mCamera != null) {
            mCamera.close();
        }
        if (kkCamera != null) {
            kkCamera.closeCamera();
        }
        renderer.releaseSurfaceTexture();
    }

    private void open(int cameraId) {
        mCamera.close();
        mCamera.open(cameraId);
        final Point previewSize = mCamera.getPreviewSize();
        dataWidth = previewSize.x;
        dataHeight = previewSize.y;
        SurfaceTexture texture = renderer.getSurfaceTexture();
        mCamera.setPreviewTexture(texture);
        mCamera.preview();
    }

    private void stickerInit() {
        if (!isSetParm && dataWidth > 0 && dataHeight > 0) {
            isSetParm = true;
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }
}

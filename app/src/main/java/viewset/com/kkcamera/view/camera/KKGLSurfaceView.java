package viewset.com.kkcamera.view.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import viewset.com.kkcamera.view.camera.egl.FrameBuffer;
import viewset.com.kkcamera.view.camera.egl.GLESBackEnv;
import viewset.com.kkcamera.view.camera.filter.BaseFilter;
import viewset.com.kkcamera.view.camera.filter.ColorFilter;
import viewset.com.kkcamera.view.camera.filter.ImgShowFilter;
import viewset.com.kkcamera.view.camera.filter.NoFilter;
import viewset.com.kkcamera.view.camera.filter.ProcessFilter;
import viewset.com.kkcamera.view.image.opengl.util.Gl2Utils;

public class KKGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    /**
     * Camera1
     */
    private int cameraId = 1;
    private KitkatCamera mCamera1;

    /**
     * Camera2
     */
    private KKFBORenderer renderer;
    private KKCamera mCamera2;

    private boolean useCamera2 = true;

    private boolean isSetParm = false;

    private int mWidth, mHeight;

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
        setPreserveEGLContextOnPause(true);//保存Context当pause时
        setCameraDistance(100);//相机距离

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
                    mCamera2.setPreviewTexture(renderer.getSurfaceTexture());
                    renderer.setCameraId(cameraId);
                }
            });
            renderer = new KKFBORenderer(getContext());
        } else {
            /**初始化相机的管理类*/
            mCamera1 = new KitkatCamera();
            renderer = new KKFBORenderer(getContext());
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        renderer.setCameraId(cameraId);
        if (!isSetParm) {
            if (useCamera2) {
                renderer.onSurfaceCreated(gl, config);
                mCamera2.open(cameraId);
            } else {
                renderer.onSurfaceCreated(gl, config);
                mCamera1.open(cameraId);
                Point point = mCamera1.getPreviewSize();
                renderer.setPreviewSize(point.x, point.y);
                mCamera1.setPreviewTexture(renderer.getSurfaceTexture());
                mCamera1.preview();
            }
            renderer.getSurfaceTexture().setOnFrameAvailableListener(this);
            isSetParm = true;
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        GLES20.glViewport(0, 0, width, height);
        if (useCamera2) {
            renderer.onSurfaceChanged(gl, width, height);
        } else {
            renderer.onSurfaceChanged(gl, width, height);
        }
    }

    public void switchCamera() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                cameraId = cameraId == 0 ? 1 : 0;
                renderer.setCameraId(cameraId);
                if (useCamera2) {
                    mCamera2.close();
                    mCamera2.open(cameraId);
                } else {
                    mCamera1.switchTo(cameraId);
                    Point point = mCamera1.getPreviewSize();
                    renderer.setPreviewSize(point.x, point.y);
                    mCamera1.setPreviewTexture(renderer.getSurfaceTexture());
                    mCamera1.preview();
                }
            }
        });
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        renderer.onDrawFrame(gl);
    }

    @Override
    public void onResume() {
        Log.e("ttt", "onResume");
        if (useCamera2) {
            mCamera2.startBackgroundThread();
        }
        openCamera();
        super.onResume();
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.e("ttt", "onDetachedFromWindow");
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        Log.e("ttt", "onAttachedToWindow");
        super.onAttachedToWindow();
    }

    /**
     * 开启摄像头
     */
    private void openCamera() {
        if (isSetParm) {
            if (useCamera2) {
                mCamera2.open(cameraId);
            } else {
                mCamera1.open(cameraId);
                Point point = mCamera1.getPreviewSize();
                renderer.setPreviewSize(point.x, point.y);
                mCamera1.setPreviewTexture(renderer.getSurfaceTexture());
                mCamera1.preview();
            }
            renderer.getSurfaceTexture().setOnFrameAvailableListener(this);
        }
    }

    @Override
    public void onPause() {
        Log.e("ttt", "onPause");
        if (useCamera2) {
            mCamera2.stopBackgroundThread();
        }
        //renderer.onPause();
        //renderer.getSurfaceTexture().setOnFrameAvailableListener(null);
        closeCamera();
        super.onPause();
    }

    private void closeCamera() {
        if (useCamera2) {
            mCamera2.close();
        } else {
            mCamera1.close();
        }
    }

    public void onDestroy() {
        Log.e("ttt", "onDestroy");
        if (mCamera2 != null) {
            mCamera2.close();
        }
        renderer.releaseSurfaceTexture();
    }

    /**
     * 拍照
     */
    public void takePhoto(final Callback<Bitmap> callback) {
        ICamera.TakePhotoCallback photoCallback = new ICamera.TakePhotoCallback() {
            @Override
            public void onTakePhoto(byte[] data) {
                final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                int w = bmp.getWidth();
                int h = bmp.getHeight();

                GLESBackEnv backEnv = new GLESBackEnv(w, h);
                backEnv.setThreadOwner(Thread.currentThread().getName());

                renderer.drawBitmap(bmp, w, h, useCamera2);

                Bitmap result = backEnv.getBitmap();

                backEnv.destroy();

                callback.back(result);

                mCamera1.preview();
            }
        };
        if (useCamera2) { // 摄像机1、2拍照
            mCamera2.takePhoto(photoCallback);
        } else { //
            mCamera1.takePhoto(photoCallback);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    public interface Callback<T> {
        void back(T t);
    }
}

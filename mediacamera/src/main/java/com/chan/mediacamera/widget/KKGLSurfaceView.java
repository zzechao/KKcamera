package com.chan.mediacamera.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import com.chan.mediacamera.camera.ICamera;
import com.chan.mediacamera.camera.KKCamera;
import com.chan.mediacamera.camera.KKFBORenderer;
import com.chan.mediacamera.camera.KitkatCamera;
import com.chan.mediacamera.camera.egl.GLESBackEnv;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

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

    private boolean useCamera2 = false;

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
                public void configureTransform(final int previewWidth, final int previewHeight) {
                    renderer.setPreviewSize(previewWidth, previewHeight);
                }

                @Override
                public void deviceOpened() {
                    Log.e("ttt", "setPreviewTexture");
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
                renderer.setCameraId(cameraId);
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
                if (useCamera2) {
                    mCamera2.close();
                    mCamera2.open(cameraId);
                } else {
                    mCamera1.switchTo(cameraId);
                    Point point = mCamera1.getPreviewSize();
                    renderer.setPreviewSize(point.x, point.y);
                    mCamera1.setPreviewTexture(renderer.getSurfaceTexture());
                    mCamera1.preview();
                    renderer.setCameraId(cameraId);
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
        if (useCamera2) {
            mCamera2.startBackgroundThread();
        }
        openCamera();
        super.onResume();
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

                if (!useCamera2) {
                    mCamera1.preview();
                }
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

    /**
     * 是否再暂停中
     *
     * @return
     */
    public boolean isPause() {
        return renderer.isPause();
    }

    /**
     * 开始录制
     */
    public void startRecord() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.startRecord();
            }
        });
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.stopRecord();
            }
        });
    }

    /**
     * 暂停录制
     */
    public void pauseRecord() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.pauseRecord();
            }
        });
    }

    /**
     * 恢复录制
     */
    public void resumeRecord() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.resumeRecord();
            }
        });
    }

    public interface Callback<T> {
        void back(T t);
    }

    public String getOutputPath() {
        return renderer.getOutputPath();
    }
}

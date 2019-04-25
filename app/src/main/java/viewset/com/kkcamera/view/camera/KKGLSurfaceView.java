package viewset.com.kkcamera.view.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import viewset.com.kkcamera.view.camera.egl.FrameBuffer;
import viewset.com.kkcamera.view.camera.egl.GLESBackEnv;
import viewset.com.kkcamera.view.camera.filter.BaseFilter;
import viewset.com.kkcamera.view.camera.filter.ColorFilter;
import viewset.com.kkcamera.view.camera.filter.ImgShowFilter;
import viewset.com.kkcamera.view.camera.filter.ProcessFilter;

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
                    renderer.setCameraId(cameraId);
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
        if (useCamera2) {

        } else {
            mCamera1.takePhoto(new ICamera.TakePhotoCallback() {
                @Override
                public void onTakePhoto(byte[] data, int width, int height) {
                    Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                    int w = bmp.getWidth();
                    int h = bmp.getHeight();
                    GLESBackEnv backEnv = new GLESBackEnv(w, h);
                    backEnv.setThreadOwner(Thread.currentThread().getName());

                    ImgShowFilter mFilter = new ImgShowFilter(getContext());
                    mFilter.setBitmap(bmp);
                    mFilter.onSurfaceCreated();
                    mFilter.setSize(w, h);

                    int[] mFrameBuffers = new int[1];
                    int[] mFrameBufferTextures = new int[1];

                    GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
                    GLES20.glGenTextures(1, mFrameBufferTextures, 0);

                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                            GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                            GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
                    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                            GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
                    mFilter.onDrawFrame();


                    BaseFilter processColorFilter = new ProcessFilter(getContext(), new ColorFilter(getContext()));
                    processColorFilter.onSurfaceCreated();
                    processColorFilter.setSize(w, h);
                    processColorFilter.setTextureId(mFrameBufferTextures[0]);
                    processColorFilter.onDrawFrame();


                    Bitmap result = backEnv.getBitmap();

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                    GLES20.glDeleteTextures(1, new int[]{mFilter.getTextureId()}, 0);
                    GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
                    GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);

                    backEnv.destroy();

                    callback.back(result);
                    mCamera1.preview();
//                    mCamera1.stopPreview();
//                    final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
//                    queueEvent(new Runnable() {
//                        @Override
//                        public void run() {
//                            int w = bmp.getWidth();
//                            int h = bmp.getHeight();
//                            GLESBackEnv backEnv = new GLESBackEnv(w, h);
//                            backEnv.setThreadOwner(Thread.currentThread().getName());
//
//                            ImgShowFilter mFilter = new ImgShowFilter(getContext());
//                            mFilter.setBitmap(bmp);
//                            mFilter.setSize(w, h);
//
//                            FrameBuffer buffer = new FrameBuffer();
//                            buffer.create(w, h);
//                            buffer.beginDrawToFrameBuffer();
//                            mFilter.onDrawFrame();
//                            buffer.endDrawToFrameBuffer();
//
//                            Bitmap result = backEnv.getBitmap();
//                            if (callback != null) {
//                                callback.back(result);
//                            }
//                            backEnv.destroy();
//
//                            mCamera1.preview();
//                        }
//                    });
                }
            });
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

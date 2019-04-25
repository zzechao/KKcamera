package com.imay.capturefilter.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.imay.capturefilter.camera.Camera2Engine;
import com.imay.capturefilter.camera.CameraEngine;
import com.imay.capturefilter.camera.utils.CameraInfo;
import com.imay.capturefilter.filter.advanced.MagicBeautyFilter;
import com.imay.capturefilter.filter.base.MagicCameraInputFilter;
import com.imay.capturefilter.filter.base.gpuimage.OpenGlUtils;
import com.imay.capturefilter.filter.helper.MagicFilterType;
import com.imay.capturefilter.helper.SavePictureTask;
import com.imay.capturefilter.utils.ICUtils;
import com.imay.capturefilter.utils.MagicParams;
import com.imay.capturefilter.utils.Rotation;
import com.imay.capturefilter.utils.TextureRotationUtil;
import com.imay.capturefilter.widget.base.MagicBaseView;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ThreadFactory;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MagicCameraView extends MagicBaseView {

    private MagicCameraInputFilter cameraInputFilter;
    private MagicBeautyFilter beautyFilter;

    private SurfaceTexture surfaceTexture;

    public MagicCameraView(Context context) {
        this(context, null);
    }

    private boolean recordingEnabled;
    private int recordingStatus;

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    private File outputFile;

    public MagicCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.getHolder().addCallback(this);
        //outputFile = new File(MagicParams.videoPath, MagicParams.videoName);
        recordingStatus = -1;
        recordingEnabled = false;
        scaleType = ScaleType.CENTER_CROP;


    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);
        if (recordingEnabled)
            recordingStatus = RECORDING_RESUMED;
        else
            recordingStatus = RECORDING_OFF;
        if (cameraInputFilter == null)
            cameraInputFilter = new MagicCameraInputFilter(getContext());
        cameraInputFilter.init();
        if (textureId == OpenGlUtils.NO_TEXTURE) {
            textureId = OpenGlUtils.getExternalOESTextureID();
            if (textureId != OpenGlUtils.NO_TEXTURE) {
                surfaceTexture = new SurfaceTexture(textureId);
                surfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, final int width, final int height) {
        try {
            super.onSurfaceChanged(gl, width, height);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Looper.getMainLooper().getQueue().addIdleHandler(new MessageQueue.IdleHandler() {
                    @Override
                    public boolean queueIdle() {
                        openCamera(width, height);
                        return false;
                    }
                });
            } else {
                openCamera(width, height);
            }
        } catch (Exception e) {

        }
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        super.onDrawFrame(gl);
        if (surfaceTexture == null)
            return;
        try {
            surfaceTexture.updateTexImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (recordingEnabled) {
            switch (recordingStatus) {
                case RECORDING_OFF:
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        } else {
            switch (recordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    recordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        }

        float[] mtx = new float[16];
        surfaceTexture.getTransformMatrix(mtx);
        cameraInputFilter.setTextureTransformMatrix(mtx);
        int id = textureId;
        if (filter == null) {
            cameraInputFilter.onDrawFrame(textureId, gLCubeBuffer, gLTextureBuffer);
        } else {
            id = cameraInputFilter.onDrawToTexture(textureId);
            filter.onDrawFrame(id, gLCubeBuffer, gLTextureBuffer);
        }
    }

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            requestRender();
        }
    };

    @Override
    public void setFilter(MagicFilterType type) {
        super.setFilter(type);
    }

    private void openCamera(int width, int height) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            Surface surface = new Surface(surfaceTexture);
            Camera2Engine.getInstance().openCamera(width, height, getContext(), surface);
        } else {
            try {
                if (CameraEngine.getInstance().getCamera() == null)
                    CameraEngine.getInstance().openCamera(MagicParams.OPEN_CAMERA_ID, width, height);
                CameraInfo info = CameraEngine.getInstance().getCameraInfo();
                if (info.orientation == 90 || info.orientation == 270) {
                    imageWidth = info.previewHeight;
                    imageHeight = info.previewWidth;
                } else {
                    imageWidth = info.previewWidth;
                    imageHeight = info.previewHeight;
                }
                if (cameraInputFilter != null) {
                    cameraInputFilter.onInputSizeChanged(imageWidth, imageHeight);
                }
                adjustSize(info.orientation, info.isFront, true);
                if (surfaceTexture != null)
                    CameraEngine.getInstance().startPreview(surfaceTexture);

                //这里是新加的代码，调整前置摄像头拍照后,画面翻转跟预览旋转问题.
                //要问我为什么,我也不知道,就是被我调试出来了.这样可以解决.adjustSize里面设置问题。
                if (info.isFront) {
                    setSwitchCamera();
                }
            } catch (Exception e) {

            }
        }
    }

    public void resumeCamera() {
        CameraEngine.getInstance().startPreview();
    }

    public void pauceCamera() {
        CameraEngine.getInstance().stopPreview();
    }

    public void switchCamera() {
        CameraEngine.getInstance().switchCamera(surfaceWidth, surfaceHeight);
        CameraInfo info = CameraEngine.getInstance().getCameraInfo();
        if (info.orientation == 90 || info.orientation == 270) {
            imageWidth = info.previewHeight;
            imageHeight = info.previewWidth;
        } else {
            imageWidth = info.previewWidth;
            imageHeight = info.previewHeight;
        }
        cameraInputFilter.onInputSizeChanged(imageWidth, imageHeight);
        adjustSize(info.orientation, info.isFront, true);
        if (surfaceTexture != null)
            CameraEngine.getInstance().startPreview(surfaceTexture);

        //这里是新加的代码，调整前置摄像头拍照后,画面翻转跟预览旋转问题.
        //要问我为什么,我也不知道,就是被我调试出来了.这样可以解决.adjustSize里面设置问题。
        if (info.isFront) {
            setSwitchCamera();
        }
    }

    public void setSwitchCamera() {
        adjustSize(90, false, true);
        if (surfaceTexture != null)
            CameraEngine.getInstance().startPreview(surfaceTexture);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        //holder.removeCallback(this);
//        if(CameraEngine.isOpen){
//            String string = (String) getTag();
//            if(TextUtils.isEmpty(string) || !string.equals("photopicker")){
//                CameraEngine.releaseCamera();
//            }
//        }
    }

    public void changeRecordingState(boolean isRecording) {
        recordingEnabled = isRecording;
    }

    protected void onFilterChanged() {
        super.onFilterChanged();
        cameraInputFilter.onDisplaySizeChanged(surfaceWidth, surfaceHeight);
        if (filter != null)
            cameraInputFilter.initCameraFrameBuffer(imageWidth, imageHeight);
        else
            cameraInputFilter.destroyFramebuffers();
    }

    @Override
    public void savePicture(final SavePictureTask savePictureTask) {
        CameraEngine.getInstance().takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                CameraEngine.getInstance().stopPreview();
                final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);    //原始图
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
                        final Bitmap photo = drawPhoto(bitmap, false);  //默认false，在savePictureTask里面判断图片旋转
                        savePictureTask.setSurface(surfaceWidth, surfaceHeight);
                        savePictureTask.execute(photo);
                        ICUtils.recycledBitmap(bitmap);
                    }
                });
                //TODO 不继续预览
//                CameraEngine.startPreview();
            }
        });
    }

    private Bitmap drawPhoto(Bitmap bitmap, boolean isRotated) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();


        if (beautyFilter == null)
            beautyFilter = new MagicBeautyFilter(getContext());
        beautyFilter.init();
        beautyFilter.onDisplaySizeChanged(width, height);
        beautyFilter.onInputSizeChanged(width, height);

        if (filter != null) {
            filter.onInputSizeChanged(width, height);
            filter.onDisplaySizeChanged(width, height);
        }

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

        GLES20.glViewport(0, 0, width, height);
        int textureId = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, true);

        FloatBuffer gLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        FloatBuffer gLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);
        if (isRotated)
            gLTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, false)).position(0);
        else
            gLTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, true)).position(0);

        int id = textureId;
        if (filter == null) {
            beautyFilter.onDrawFrame(textureId, gLCubeBuffer, gLTextureBuffer);
        } else {
            //beautyFilter.onDrawFrame(textureId);
            //id = cameraInputFilter.onDrawToTexture(textureId);
            // 测试
            //filter.onDrawFrame(mFrameBufferTextures[0], gLCubeBuffer, gLTextureBuffer);
            filter.onDrawFrame(textureId, gLCubeBuffer, gLTextureBuffer);
        }
        IntBuffer ib = IntBuffer.allocate(width * height);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(ib);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
        GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
        GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);

        beautyFilter.destroy();
        beautyFilter = null;
        if (filter != null) {
            filter.onDisplaySizeChanged(surfaceWidth, surfaceHeight);
            filter.onInputSizeChanged(imageWidth, imageHeight);
        }
        return result;
    }

    public void onBeautyLevelChanged() {
        cameraInputFilter.onBeautyLevelChanged();
    }

    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    public int getSurfaceHeight() {
        return surfaceHeight;
    }
}

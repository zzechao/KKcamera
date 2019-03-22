package com.imay.capturefilter.camera;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;

import com.imay.capturefilter.camera.utils.CameraUtils;
import com.imay.capturefilter.utils.ICCons;
import com.imay.capturefilter.widget.MagicCameraView;

import java.util.Arrays;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Engine {

    private static Camera2Engine instance;
    private final Handler mHandler;
    private ImageReader imageReader;


    public static Camera2Engine getInstance() {
        if (instance == null) {
            synchronized (Camera2Engine.class) {
                if (instance == null) {
                    instance = new Camera2Engine();
                }
            }
        }
        return instance;
    }

    public boolean isOpen = false;
    private final String mCameraId = "Camera2Engine";
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private Surface mSurface;
    /**
     * 预览请求的Builder
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    private CameraCaptureSession.StateCallback mSessionCallback;

    private CameraDevice.StateCallback mDeviceCallback;

    private CameraCaptureSession.CaptureCallback mCaptureCallback;

    public Camera2Engine() {
        HandlerThread handlerThread = new HandlerThread("camera");
        handlerThread.start();
        mHandler = new Handler(Looper.getMainLooper());

        mSessionCallback = new CameraCaptureSession.StateCallback() {

            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                Log.e("ttt", "onConfigured");
                try {
                    mCaptureSession = session;
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mHandler);
                } catch (CameraAccessException e) {
                    Log.e("ttt", e.getMessage());
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        };

        mDeviceCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.e("ttt", "onOpened");
                try {
                    mCameraDevice = camera;

                    mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    // 为相机预览，创建一个CameraCaptureSession对象
                    mCameraDevice.createCaptureSession(Arrays.asList(mSurface, imageReader.getSurface()), mSessionCallback, mHandler);
                } catch (CameraAccessException e) {
                    Log.e("ttt", e.getMessage());
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.e("ttt", "onDisconnected");
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.e("ttt", "onError");
            }
        };

        mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        };
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean openCamera(int width, int height, Context context, Surface surface) {
        try {
            imageReader = ImageReader.newInstance(width, height, ImageFormat.YV12, 1);//预览数据流最好用非JPEG
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            mSurface = surface;
            //获取到可用的相机
            for (String cameraId : manager.getCameraIdList()) {
                //获取到每个相机的参数对象，包含前后摄像头，分辨率等
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                //摄像头的方向
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null) {
                    continue;
                }
                //匹配方向,指定打开后摄像头
                if (facing != CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }
                //打开指定的摄像头
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
                Log.e("ttt", "openCamera2");
                manager.openCamera(cameraId, mDeviceCallback, mHandler);
                return true;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
}
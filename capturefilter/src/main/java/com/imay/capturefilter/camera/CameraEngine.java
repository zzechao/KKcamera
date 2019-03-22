package com.imay.capturefilter.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;

import com.imay.capturefilter.camera.utils.CameraUtils;
import com.imay.capturefilter.utils.ICCons;

public class CameraEngine {

    private static CameraEngine instance;

    public static CameraEngine getInstance() {
        if (instance == null) {
            synchronized (CameraEngine.class) {
                if (instance == null) {
                    instance = new CameraEngine();
                }
            }
        }
        return instance;
    }

    private Camera camera = null;
    private int cameraID = 0;
    public boolean isOpen = false;


    /**
     * 获取手机摄像头属性
     *
     * @return
     */
    public boolean getCameraBackStatus() {
        return cameraID == 0 ? true : false;
    }

    public Camera getCamera() {
        return camera;
    }

    public boolean openCamera(int id, int width, int height) {
        if (camera == null) {
            try {
                CameraUtils.getCurrentScreenSize(1, width, height);
                camera = Camera.open(id);
                if (camera == null) {
                    ICCons.cameraOrNull = false;
                    return false;
                } else {
                    ICCons.cameraOrNull = true;
                    cameraID = id;
                    setDefaultParameters();
                    isOpen = true;
                    return true;
                }
            } catch (Exception e) {
                //e.printStackTrace();
                ICCons.cameraOrNull = false;
                return false;
            }
        } else {
            ICCons.cameraOrNull = true;
            return true;
        }

    }

    public void releaseCamera() {
        try {
            if (camera != null) {
                if (isOpen) {
                    isOpen = false;
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                    camera.lock();
                    camera.release();
                    camera = null;
                }
            }
        } catch (Exception e) {

        }
    }


    public void switchCamera(int w, int h) {
        assert !isOpen;
        releaseCamera();
        cameraID = cameraID == 0 ? 1 : 0;
        openCamera(cameraID, w, h);
    }

    private void setDefaultParameters() {
        if (camera != null && isOpen) {
            Parameters parameters = camera.getParameters();
            if (parameters.getSupportedFocusModes().contains(
                    Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
//
            if (CameraUtils.currentScreenSize != null && CameraUtils.currentScreenSize.width != 0) {
                parameters.setPreviewSize(CameraUtils.currentScreenSize.width, CameraUtils.currentScreenSize.height);
            }

            if (CameraUtils.currentScreenPicSize != null && CameraUtils.currentScreenPicSize.width != 0) {
                parameters.setPictureSize(CameraUtils.currentScreenPicSize.width, CameraUtils.currentScreenPicSize.height);
            }

            parameters.setRotation(90);
            camera.setParameters(parameters);
        }
    }

    private Size getPreviewSize() {
        if (camera != null && isOpen) {
            return camera.getParameters().getPreviewSize();
        } else {
            return null;
        }
    }

    private Size getPictureSize() {
        if (camera != null && isOpen) {
            return camera.getParameters().getPictureSize();
        } else {
            return null;
        }
    }

    public void startPreview(SurfaceTexture surfaceTexture) {
        if (camera != null && isOpen)
            try {
                camera.setPreviewTexture(surfaceTexture);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public void startPreview() {
        if (camera != null && isOpen)
            camera.startPreview();
    }

    public void stopPreview() {
        if (camera != null && isOpen) {
            camera.stopPreview();
        }
    }

    public void setRotation(int rotation) {
        if (camera != null && isOpen) {
            Parameters params = camera.getParameters();
            params.setRotation(rotation);
            camera.setParameters(params);
        }
    }

    public void takePicture(Camera.ShutterCallback shutterCallback, Camera.PictureCallback rawCallback,
                            Camera.PictureCallback jpegCallback) {
        try {
            if (camera != null && isOpen) {
                camera.takePicture(shutterCallback, rawCallback, jpegCallback);
            }
        } catch (Exception e) {
            Log.e("CameraEngine", "native_takePicture can not do");
        }
    }

    public void setFlashMode(int flashMode) {
        if (camera != null && isOpen) {
            Parameters parameters = camera.getParameters();
            if (parameters == null) {
                return;
            }
            if (flashMode == 1) {
                parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);//自动
            } else if (flashMode == 2) {
                // --FLASH_MODE_TORCH 直接打开 FLASH_MODE_ON拍照的时候打开
                parameters.setFlashMode(Parameters.FLASH_MODE_ON); //开
            } else {
                parameters.setFlashMode(Parameters.FLASH_MODE_OFF); //关
            }
            camera.setParameters(parameters);
        }
    }

    public void setZoom(float scale) {
        if (camera != null && isOpen) {
            if (isSupportZoom(camera)) {
                Parameters params = camera.getParameters();
                final int max = params.getMaxZoom();
                int min = 0;
                if (max == 0) return;
                int zoomValue = params.getZoom();
                if (zoomValue < 2 && scale < 0) { //缩小
                    zoomValue = min;
                } else if (zoomValue == max && scale > 0) {
                    zoomValue = max;
                } else {
                    zoomValue += scale;
                }
                params.setZoom(zoomValue);
                camera.setParameters(params);
            } else {
                Log.d("ICCamera", "--------setZoom--------the phone not support zoom");
            }
        }
    }

    public boolean isSupportZoom(Camera camera) {
        boolean isSuppport = false;
        if (camera != null && isOpen) {
            if (camera.getParameters().isZoomSupported()) { //&&camera.getParameters().isSmoothZoomSupported()
                isSuppport = true;
            }
        }
        return isSuppport;
    }

    public void onDestroy() {
        releaseCamera();
        camera = null;
        cameraID = 0;
        instance = null;
    }

    public com.imay.capturefilter.camera.utils.CameraInfo getCameraInfo() {
        com.imay.capturefilter.camera.utils.CameraInfo info = new com.imay.capturefilter.camera.utils.CameraInfo();
        Size size = getPreviewSize();
        CameraInfo cameraInfo = new CameraInfo();
        if (cameraID == 0 || cameraID == 1) {
            try {
                Camera.getCameraInfo(cameraID, cameraInfo);
            } catch (Exception e) {
                Log.e("CameraEngine", "Unknown camera ID");
            }
        }
        if (size != null) {
            info.previewWidth = size.width;
            info.previewHeight = size.height;
        }

        info.orientation = cameraInfo.orientation;
        info.isFront = cameraID == 1 ? true : false;

//        size = getPictureSize();
        size = CameraUtils.getLargePictureSize(camera);

        if (size != null) {
            info.pictureWidth = size.width;
            info.pictureHeight = size.height;
        }

        return info;
    }

    public void pauceCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
        }
    }
}
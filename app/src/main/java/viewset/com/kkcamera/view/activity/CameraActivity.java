package viewset.com.kkcamera.view.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.imay.capturefilter.widget.SquareLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;

public class CameraActivity extends AppCompatActivity {

    @BindView(R.id.im_camera)
    AutoFitTextureView mTextureView;

    @BindView(R.id.im_square)
    SquareLayout imSquare;

    Unbinder unbinder;

    private String mCameraId;
    private Size mPreviewSize;
    private CameraDevice.StateCallback stateCallback;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mPreviewSession;
    private ImageReader mImageReader;

    private Handler backgroundHandler;
    private HandlerThread handlerThread;

    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private int mState = STATE_PREVIEW;

    private static final int STATE_PREVIEW = 0;

    private static final int STATE_WAITING_LOCK = 1;

    private static final int STATE_WAITING_PRECAPTURE = 2;

    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    private static final int STATE_PICTURE_TAKEN = 4;

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.e("ttt", "onSurfaceTextureAvailable");
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            Log.e("ttt", "onSurfaceTextureSizeChanged");
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };


    @OnClick({R.id.bt_screen, R.id.bt_1to1, R.id.bt_takephoto})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.bt_screen:
                imSquare.setStats(SquareLayout.FULL_SCREEN);
                break;
            case R.id.bt_1to1:
                imSquare.setStats(SquareLayout.ONCEONONCE);
                break;
            case R.id.bt_takephoto:
                takePhoto();
                break;
        }
    }

    private void takePhoto() {
        captureStillPicture();
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Activity activity = this;
            if (null == mTextureView || null == mPreviewSize || null == activity) {
                return;
            }
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max(
                        (float) viewHeight / mPreviewSize.getHeight(),
                        (float) viewWidth / mPreviewSize.getWidth());
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            } else if (Surface.ROTATION_180 == rotation) {
                matrix.postRotate(180, centerX, centerY);
            }
            mTextureView.setTransform(matrix);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        unbinder = ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            stateCallback = new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    //开启预览
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    cameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int error) {
                    cameraDevice.close();
                    mCameraDevice = null;
                }
            };
        }

    }


    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }


    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        handlerThread = new HandlerThread("CameraThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != mPreviewSession) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    private void setupCamera(int width, int height) {
        //获取摄像头的管理者CameraManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                //遍历所有摄像头
                for (String cameraId : manager.getCameraIdList()) {
                    CameraCharacteristics characteristics
                            = manager.getCameraCharacteristics(cameraId);
                    //默认打开后置摄像头
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        continue;
                    }
                    //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map == null) {
                        continue;
                    }
                    //根据TextureView的尺寸设置预览尺寸
                    Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
                    mPreviewSize = getOptimalSize(sizes, width, height);
                    mPreviewSize = new Size(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                    Log.e("ttt", width + "---" + height + "---" + mPreviewSize.getWidth() + "---" + mPreviewSize.getHeight());
                    mCameraId = cameraId;
                    break;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupImageReader(int width, int height) {
        //前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据，本例的2代表ImageReader中最多可以获取两帧图像流
        mImageReader = ImageReader.newInstance(width, height,
                ImageFormat.JPEG, /*maxImages*/2);
        //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.e("ttt", "onImageAvailable");
                backgroundHandler.post(new ImageSaver(reader.acquireNextImage()));
            }
        }, backgroundHandler);
    }

    private Size getOptimalSize(Size[] sizes, int width, int height) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        Log.e("ttt", Arrays.toString(sizes));
        List<Size> enough = new ArrayList<>();
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        float t = width > height ? width * 1f / height : height * 1f / width;
        for (Size option : sizes) {
            float k = option.getWidth() > option.getHeight() ? option.getWidth() * 1f / option.getHeight() : option.getHeight() * 1f / option.getWidth();
            if (k > t && option.getHeight() >= width) {
                bigEnough.add(option);
            } else {
                notBigEnough.add(option);
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        Log.e("ttt", Arrays.toString(bigEnough.toArray()));
        Log.e("ttt", Arrays.toString(notBigEnough.toArray()));
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            return new Size(width, height);
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * 打开摄像头
     *
     * @param width
     * @param height
     */
    private void openCamera(int width, int height) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            //检查权限
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    //requestCameraPermission();
                    return;
                }
                //打开相机，第一个参数指示打开哪个摄像头，第二个参数stateCallback为相机的状态回调接口，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
                setupCamera(width, height);
                setupImageReader(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                configureTransform(width, height);
                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                manager.openCamera(mCameraId, stateCallback, backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }


    private void startPreview() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SurfaceTexture mSurfaceTexture = mTextureView.getSurfaceTexture();
            //设置TextureView的缓冲区大小
            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            //获取Surface显示预览数据
            Surface mSurface = new Surface(mSurfaceTexture);
            try {
                //创建CaptureRequestBuilder，TEMPLATE_PREVIEW比表示预览请求
                mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                //设置Surface作为预览数据的显示界面
                //获取ImageReader的Surface
                //CaptureRequest添加imageReaderSurface，不加的话就会导致ImageReader的onImageAvailable()方法不会回调
                mCaptureRequestBuilder.addTarget(mSurface);
                //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
                mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        try {
                            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                            //创建捕获请求
                            mCaptureRequest = mCaptureRequestBuilder.build();
                            mPreviewSession = session;

                            //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                            mPreviewSession.setRepeatingRequest(mCaptureRequest, mCaptureCallback, backgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {

                    }
                }, backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }


    public static class ImageSaver implements Runnable {
        private Image mImage;

        public ImageSaver(Image image) {
            mImage = image;
        }

        public final String PIC_DIR_NAME = "1.jpg"; //在系统的图片文件夹下创建了一个相册文件夹，名为“myPhotos"，所有的图片都保存在该文件夹下。

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            File imageFile = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), PIC_DIR_NAME);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(imageFile);
                fos.write(data, 0, data.length);
                fos.flush();
            } catch (IOException e) {
                Log.e("ttt", e.getMessage());
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mPreviewSession.capture(mCaptureRequestBuilder.build(), mCaptureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillPicture() {
        try {
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            };

            mPreviewSession.stopRepeating();
            mPreviewSession.abortCaptures();
            mPreviewSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mPreviewSession.capture(mCaptureRequestBuilder.build(), mCaptureCallback,
                    backgroundHandler);
            mState = STATE_PREVIEW;
            mPreviewSession.setRepeatingRequest(mCaptureRequest, mCaptureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        Log.e("ttt", "1process-STATE_WAITING_LOCK" + afState);
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            Log.e("ttt", "2process-STATE_WAITING_LOCK" + afState);
                            captureStillPicture();
                        } else {
                            Log.e("ttt", "3process-STATE_WAITING_LOCK" + afState);
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices

                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    Log.e("ttt", "4process-STATE_WAITING_PRECAPTURE" + aeState);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);

            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            process(result);
        }
    };
}

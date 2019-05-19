package viewset.com.kkcamera.view.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.camera.egl.FrameBuffer;
import viewset.com.kkcamera.view.camera.filter.BaseFilter;
import viewset.com.kkcamera.view.camera.filter.ColorFilter;
import viewset.com.kkcamera.view.camera.filter.GroupFilter;
import viewset.com.kkcamera.view.camera.filter.ImgShowFilter;
import viewset.com.kkcamera.view.camera.filter.NoFilter;
import viewset.com.kkcamera.view.camera.filter.PkmFilter;
import viewset.com.kkcamera.view.camera.filter.ProcessBeautyFilter;
import viewset.com.kkcamera.view.camera.filter.ProcessFilter;
import viewset.com.kkcamera.view.camera.filter.ShowFilter;
import viewset.com.kkcamera.view.camera.filter.TimeWaterMarkFilter;
import viewset.com.kkcamera.view.camera.filter.WaterMarkFilter;
import viewset.com.kkcamera.view.camera.media.EncoderConfig;
import viewset.com.kkcamera.view.camera.media.MuxerEncoder;
import viewset.com.kkcamera.view.camera.multimedia.MediaEncoder;
import viewset.com.kkcamera.view.camera.multimedia.TextureMovieEncoder;
import viewset.com.kkcamera.view.camera.record.HardcodeEncoder;
import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;
import viewset.com.kkcamera.view.image.opengl.util.Gl2Utils;

public class KKFBORenderer implements GLSurfaceView.Renderer {

    /**
     * 显示
     */
    private BaseFilter showFilter;

    /**
     * 图像的
     */
    private BaseFilter drawFilter;
    private GroupFilter groupFilter;
    private BaseFilter processColorFilter;
    private WaterMarkFilter waterMarkFilter;
    private ProcessBeautyFilter beautyFilter;

    protected Context mContext;
    private int mTextureId = OpenGlUtils.NO_TEXTURE;
    private SurfaceTexture mSurfaceTexture;

    private int mPreviewWidth;
    private int mPreviewHeight;
    private int mWidth;
    private int mHeight;
    private int mImgWidth;
    private int mImgHeight;

    //创建离屏buffer，用于最后导出数据
    private int[] fFrame = new int[1];
    private int[] fTexture = {OpenGlUtils.NO_TEXTURE};

    private int width;
    private int height;
    private int mCameraId;

    private boolean recordingEnabled;
    private int recordingStatus;
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private static final int RECORDING_PAUSE = 3;
    private static final int RECORDING_RESUME = 4;

    private String mOutputPath;

    private MuxerEncoder muxerEncoder;

    /**
     * 获取视频缓存绝对路径
     *
     * @param context
     * @return
     */
    public static String getVideoCachePath(Context context) {
        String directoryPath;
        // 判断外部存储是否可用，如果不可用则使用内部存储路径
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else { // 使用内部存储缓存目录
            directoryPath = context.getCacheDir().getAbsolutePath();
        }
        String path = directoryPath + File.separator + "KkCamera_" + System.currentTimeMillis() + ".mp4";
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return path;
    }

    public KKFBORenderer(Context context) {
        mContext = context;

        mOutputPath = getVideoCachePath(context);

        showFilter = new NoFilter(context);
        drawFilter = new ShowFilter(context);
        processColorFilter = new ProcessFilter(context);
        ((ProcessFilter) processColorFilter).setFilter(new ColorFilter(context));
        //processPkmFilter = new ProcessFilter(context);
        //((ProcessFilter) processPkmFilter).setFilter(new ZipPkmAnimationFilter(context));
        beautyFilter = new ProcessBeautyFilter(context);

        setWaterMarkPosition();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mTextureId = OpenGlUtils.getExternalOESTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        drawFilter.onSurfaceCreated();
        drawFilter.setTextureId(mTextureId);

        showFilter.onSurfaceCreated();

        processColorFilter.onSurfaceCreated();

        //processPkmFilter.onSurfaceCreated();

        groupFilter.onSurfaceCreated();

        beautyFilter.onSurfaceCreated();

        // 一开始处于关闭状态
        if (recordingEnabled) {
            recordingStatus = RECORDING_RESUMED;
        } else {
            recordingStatus = RECORDING_OFF;
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (this.width != width && this.height != height) {
            this.width = width;
            this.height = height;
            GLES20.glDeleteFramebuffers(1, fFrame, 0);
            GLES20.glDeleteTextures(1, fTexture, 0);

            /**创建一个帧染缓冲区对象*/
            GLES20.glGenFramebuffers(1, fFrame, 0);
            /**根据纹理数量 返回的纹理索引*/
            GLES20.glGenTextures(1, fTexture, 0);

            /**将生产的纹理名称和对应纹理进行绑定*/
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fTexture[0]);
            /**根据指定的参数 生产一个2D的纹理 调用该函数前  必须调用glBindTexture以指定要操作的纹理*/
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mPreviewWidth, mPreviewHeight,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);


            processColorFilter.setSize(mPreviewWidth, mPreviewHeight);
            drawFilter.setSize(mPreviewWidth, mPreviewHeight);
            //processPkmFilter.setSize(mPreviewWidth, mPreviewHeight);
            groupFilter.setSize(mPreviewWidth, mPreviewHeight);
            beautyFilter.setSize(mPreviewWidth, mPreviewHeight);
            setViewSize(width, height);
        }
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onDrawFrame(GL10 gl) {
        if (mSurfaceTexture != null) {
            //更新数据，其实也是消耗数据，将上一帧的数据处理或者抛弃掉，要不然SurfaceTexture是接收不到最新数据
            mSurfaceTexture.updateTexImage();

            // 绘制
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fFrame[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, fTexture[0], 0);
            GLES20.glViewport(0, 0, mPreviewWidth, mPreviewHeight);
            drawFilter.onDrawFrame();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            // 滤镜组
            groupFilter.setTextureId(fTexture[0]);
            groupFilter.onDrawFrame();

            //processPkmFilter.setTextureId(groupFilter.getOutputTexture());
            //processPkmFilter.onDrawFrame();

            // 滤镜
            processColorFilter.setTextureId(groupFilter.getOutputTexture());
            processColorFilter.onDrawFrame();

            // 美颜
            beautyFilter.setTextureId(processColorFilter.getOutputTexture());
            beautyFilter.onDrawFrame();

            if (recordingEnabled) {
                /**说明是录制状态*/
                switch (recordingStatus) {
                    case RECORDING_OFF:
                        muxerEncoder = new MuxerEncoder();
                        Log.e("ttt", mOutputPath+"---startRecording");
                        muxerEncoder.startRecording(new EncoderConfig(mPreviewWidth,mPreviewHeight,mOutputPath,mContext,EGL14.eglGetCurrentContext()));
                        recordingStatus = RECORDING_ON;
                        break;
                    case RECORDING_RESUMED:
                        muxerEncoder.updateSharedContext(new EncoderConfig(0,0,null,mContext,EGL14.eglGetCurrentContext()));
                        recordingStatus = RECORDING_ON;
                        break;
                    case RECORDING_ON:
                        break;
                    case RECORDING_PAUSE:
                        recordingStatus = RECORDING_ON;
                        break;
                    case RECORDING_RESUME:
                        recordingStatus = RECORDING_ON;
                        break;
                }
            } else {
                switch (recordingStatus) {
                    case RECORDING_ON:
                    case RECORDING_RESUMED:
                    case RECORDING_PAUSE:
                    case RECORDING_RESUME:
                        Log.e("ttt", mOutputPath+"---stopRecording");
                        muxerEncoder.stopRecording();
                        recordingStatus = RECORDING_OFF;
                        break;
                    case RECORDING_OFF:
                        break;
                }
            }

            // 显示
            GLES20.glViewport(0, 0, mWidth, mHeight);
            showFilter.setTextureId(beautyFilter.getOutputTexture());
            showFilter.onDrawFrame();

            if (muxerEncoder != null && recordingEnabled && recordingStatus == RECORDING_ON) {
                muxerEncoder.setTextureId(beautyFilter.getOutputTexture());
                muxerEncoder.frameAvailable(mSurfaceTexture);
            }
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public boolean isAvailable() {
        return mSurfaceTexture != null;
    }

    /**
     * 释放surfaceTexture
     */
    public void releaseSurfaceTexture() {
        if (mSurfaceTexture != null) {
            boolean shouldRelease = true;
            if (shouldRelease) {
                mSurfaceTexture.release();
            }
            mSurfaceTexture = null;
        }
        groupFilter.clearAll();
    }

    public void setPreviewSize(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
        waterMarkFilter.setPosition(mPreviewWidth - mImgWidth, 0, mImgWidth, mImgHeight);
    }

    public void setViewSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        calculateMatrix();
    }

    //和拍照一样都是先绘制到drawFilter中，要跟随ImageShowFilter一样的逻辑
    private void calculateMatrix() {
        float[] matrix = Gl2Utils.getOriginalMatrix();
        Gl2Utils.getShowMatrix(matrix, mPreviewWidth, mPreviewHeight, mWidth, mHeight);
        if (mCameraId == 1) { // 前置摄像头矩阵
            Gl2Utils.rotate(matrix, 90);
            Gl2Utils.flip(matrix, true, true);
            drawFilter.setMatrix(matrix);
        } else { // 后置摄像头
            Gl2Utils.rotate(matrix, 90);
            Gl2Utils.flip(matrix, false, true);
            drawFilter.setMatrix(matrix);
        }
    }

    /**
     * 设置摄像头
     *
     * @param cameraId
     */
    public void setCameraId(int cameraId) {
        mCameraId = cameraId;
        calculateMatrix();
    }

    /**
     * 设置水印
     */
    private void setWaterMarkPosition() {
        groupFilter = new GroupFilter(mContext);

        waterMarkFilter = new WaterMarkFilter(mContext);
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.watermark);
        mImgWidth = bitmap.getWidth();
        mImgHeight = bitmap.getHeight();
        waterMarkFilter.setWaterMark(bitmap);
        waterMarkFilter.setPosition(30, 0, 0, 0);
        groupFilter.addFilter(waterMarkFilter);

        TimeWaterMarkFilter timeWaterMarkFilter = new TimeWaterMarkFilter(mContext);
        timeWaterMarkFilter.isBitmap(false);
        timeWaterMarkFilter.setPosition(0, 0, 0, 0);
        groupFilter.addFilter(timeWaterMarkFilter);

        PkmFilter pkmFilter = new PkmFilter(mContext);
        pkmFilter.setPosition(200, 100);
        pkmFilter.setAnimation("assets/etczip/cc.zip");
        groupFilter.addFilter(pkmFilter);
    }

    /**
     * 绘制拍照的图片滤镜
     *
     * @param bmp
     * @param width
     * @param height
     * @param useCamera2
     */
    public void drawBitmap(Bitmap bmp, int width, int height, boolean useCamera2) {
        ImgShowFilter mFilter = new ImgShowFilter(mContext);
        mFilter.setBitmap(bmp);
        mFilter.onSurfaceCreated();
        mFilter.setSize(width, height);

        // 绘制
        GLES20.glViewport(0, 0, width, height);
        FrameBuffer buffer = new FrameBuffer();
        buffer.create(width, height);
        buffer.beginDrawToFrameBuffer();
        if (mCameraId == 1) { // 前置摄像头矩阵
            if (useCamera2) {

            } else {
                Gl2Utils.rotate(mFilter.getMatrix(), 90);
                Gl2Utils.flip(mFilter.getMatrix(), true, true);
            }
            mFilter.setMatrix(mFilter.getMatrix());
        } else { // 后置摄像头
            if (useCamera2) {
                Gl2Utils.flip(mFilter.getMatrix(), false, true);
            } else {
                Gl2Utils.rotate(mFilter.getMatrix(), 90);
                Gl2Utils.flip(mFilter.getMatrix(), false, true);
            }
            mFilter.setMatrix(mFilter.getMatrix());
        }
        mFilter.onDrawFrame();
        buffer.endDrawToFrameBuffer();

        /**
         * 滤镜组
         */
        GLES20.glViewport(0, 0, width, height);
        GroupFilter groupFilter = new GroupFilter(mContext);

        // 图片水印
        WaterMarkFilter waterMarkFilter = new WaterMarkFilter(mContext);
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.watermark);
        int imgWidth = bitmap.getWidth();
        int imgHeight = bitmap.getHeight();
        waterMarkFilter.setWaterMark(bitmap);
        waterMarkFilter.setPosition(width - imgWidth, 0, imgWidth, imgHeight);
        groupFilter.addFilter(waterMarkFilter);

        // 时间水印
        TimeWaterMarkFilter timeWaterMarkFilter = new TimeWaterMarkFilter(mContext);
        timeWaterMarkFilter.setPosition(10, 0, 0, 0);
        groupFilter.addFilter(timeWaterMarkFilter);

        groupFilter.onSurfaceCreated();
        groupFilter.setSize(width, height);
        groupFilter.setTextureId(buffer.getTextureId());
        groupFilter.onDrawFrame();

        // 滤镜
        GLES20.glViewport(0, 0, width, height);
        BaseFilter processColorFilter = new ProcessFilter(mContext);
        ((ProcessFilter) processColorFilter).setFilter(new ColorFilter(mContext));
        processColorFilter.onSurfaceCreated();
        processColorFilter.setSize(width, height);
        processColorFilter.setTextureId(groupFilter.getOutputTexture());
        processColorFilter.onDrawFrame();

        // 美颜
        GLES20.glViewport(0, 0, width, height);
        ProcessBeautyFilter beautyFilter = new ProcessBeautyFilter(mContext);
        beautyFilter.onSurfaceCreated();
        beautyFilter.setSize(width, height);
        beautyFilter.setTextureId(processColorFilter.getOutputTexture());
        beautyFilter.onDrawFrame();

        // 显示
        NoFilter showFilter = new NoFilter(mContext);
        showFilter.onSurfaceCreated();
        showFilter.setSize(width, height);
        showFilter.setMatrix(Gl2Utils.getOriginalMatrix());
        showFilter.setTextureId(beautyFilter.getOutputTexture());
        showFilter.onDrawFrame();
    }

    public void startRecord() {
        recordingEnabled = true;
    }

    public void stopRecord() {
        recordingEnabled = false;
    }

    public String getOutputPath(){
        return mOutputPath;
    }

    public void pauseRecord() {
        if(recordingStatus==RECORDING_ON){
            recordingStatus=RECORDING_PAUSE;
        }
    }


    public void resumeRecord() {
        if(recordingStatus==RECORDING_ON){
            recordingStatus=RECORDING_PAUSE;
        }
    }
}

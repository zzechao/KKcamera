package viewset.com.kkcamera.view.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.camera.filter.BaseFilter;
import viewset.com.kkcamera.view.camera.filter.GroupFilter;
import viewset.com.kkcamera.view.camera.filter.NoFilter;
import viewset.com.kkcamera.view.camera.filter.ProcessFilter;
import viewset.com.kkcamera.view.camera.filter.ShowFilter;
import viewset.com.kkcamera.view.camera.filter.TimeWaterMarkFilter;
import viewset.com.kkcamera.view.camera.filter.WaterMarkFilter;
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

    private BaseFilter processFilter;

    private WaterMarkFilter waterMarkFilter;

    protected Context mContext;
    private int mTextureId = OpenGlUtils.NO_TEXTURE;
    private SurfaceTexture mSurfaceTexture;

    private float[] matrix;

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


    public KKFBORenderer(Context context) {
        mContext = context;

        matrix = Gl2Utils.getOriginalMatrix();

        showFilter = new NoFilter(context);
        drawFilter = new ShowFilter(context);
        processFilter = new ProcessFilter(context);

        setWaterMarkPosition();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mTextureId = OpenGlUtils.getExternalOESTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        drawFilter.onSurfaceCreated();
        drawFilter.setTextureId(mTextureId);


        showFilter.onSurfaceCreated();

        processFilter.onSurfaceCreated();

        groupFilter.onSurfaceCreated();
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
       /* GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width,
                height);*/


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

            //processFilter.setSize(width, height);
            drawFilter.setSize(width, height);
            groupFilter.setSize(width, height);
            setViewSize(width, height);
        }
        Log.e("ttt", "onSurfaceChanged---fTexture =" + fTexture[0] + "---mTextureId==" + mTextureId);
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        if (mSurfaceTexture != null) {
            //更新数据，其实也是消耗数据，将上一帧的数据处理或者抛弃掉，要不然SurfaceTexture是接收不到最新数据
            mSurfaceTexture.updateTexImage();

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fFrame[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, fTexture[0], 0);
            GLES20.glViewport(0, 0, mPreviewWidth, mPreviewHeight);
            drawFilter.onDrawFrame();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);


            groupFilter.setTextureId(fTexture[0]);
            groupFilter.onDrawFrame();


            processFilter.setTextureId(groupFilter.getOutputTexture());
            processFilter.onDrawFrame();


            GLES20.glViewport(0, 0, mWidth, mHeight);
            showFilter.setTextureId(groupFilter.getOutputTexture());
            showFilter.onDrawFrame();
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public boolean isAvailable() {
        return mSurfaceTexture != null;
    }

    public void releaseSurfaceTexture() {
        if (mSurfaceTexture != null) {
            boolean shouldRelease = true;
            if (shouldRelease) {
                mSurfaceTexture.release();
            }
            mSurfaceTexture = null;
        }
    }

    public void setPreviewSize(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
        //calculateMatrix();
    }

    public void setViewSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        waterMarkFilter.setPosition(width - mImgWidth, 50, 0, 0);
        calculateMatrix();
    }

    private void calculateMatrix() {
        Gl2Utils.getShowMatrix(matrix, mPreviewWidth, mPreviewHeight, mWidth, mHeight);
        showFilter.setMatrix(matrix);
    }

    public void setCameraId(int cameraId) {
        calculateMatrix();
        if (cameraId == 1) { // 前置摄像头矩阵
            float[] OM = Gl2Utils.getOriginalMatrix();
            Gl2Utils.rotate(OM, 90);
            Gl2Utils.flip(OM, true, true);
            drawFilter.setMatrix(OM);
        } else { // 后置摄像头
            float[] OM = Gl2Utils.getOriginalMatrix();
            Gl2Utils.rotate(OM, 90);
            Gl2Utils.flip(OM, false, true);
            drawFilter.setMatrix(OM);
        }
    }

    private void setWaterMarkPosition() {
        groupFilter = new GroupFilter(mContext);

        waterMarkFilter = new WaterMarkFilter(mContext);
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.watermark);
        mImgWidth = bitmap.getWidth();
        mImgHeight = bitmap.getHeight();
        waterMarkFilter.setWaterMark(bitmap);
        waterMarkFilter.setPosition(30, 50, 0, 0);
         groupFilter.addFilter(waterMarkFilter);

        TimeWaterMarkFilter timeWaterMarkFilter = new TimeWaterMarkFilter(mContext);
        timeWaterMarkFilter.setPosition(10, 50, 0, 0);
        groupFilter.addFilter(timeWaterMarkFilter);
    }

    public void onPause() {
        Log.e("ttt", "onSurfaceChanged---fTexture =" + fTexture[0] + "---mTextureId==" + mTextureId);
    }
}

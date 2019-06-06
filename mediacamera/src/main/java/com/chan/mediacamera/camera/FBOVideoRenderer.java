package com.chan.mediacamera.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;

import com.chan.mediacamera.R;
import com.chan.mediacamera.camera.egl.FrameBuffer;
import com.chan.mediacamera.camera.filter.BaseFilter;
import com.chan.mediacamera.camera.filter.ColorFilter;
import com.chan.mediacamera.camera.filter.GroupFilter;
import com.chan.mediacamera.camera.filter.NoFilter;
import com.chan.mediacamera.camera.filter.PkmFilter;
import com.chan.mediacamera.camera.filter.ProcessBeautyFilter;
import com.chan.mediacamera.camera.filter.ProcessFilter;
import com.chan.mediacamera.camera.filter.ShowFilter;
import com.chan.mediacamera.camera.filter.TimeWaterMarkFilter;
import com.chan.mediacamera.camera.filter.WaterMarkFilter;
import com.chan.mediacamera.util.Gl2Utils;
import com.seu.magicfilter.utils.OpenGlUtils;


public class FBOVideoRenderer {


    /**
     * 显示
     */
    private BaseFilter showFilter;

    private BaseFilter drawFilter;
    private ProcessFilter processColorFilter;
    private ProcessBeautyFilter beautyFilter;
    private GroupFilter groupFilter;

    protected Context mContext;
    private int mTextureId = OpenGlUtils.NO_TEXTURE;
    private SurfaceTexture mSurfaceTexture;

    private int mVideoWidth;
    private int mVideoHeight;
    private int mWidth;
    private int mHeight;

    private FrameBuffer mFBObuffer;

    private WaterMarkFilter waterMarkFilter;
    private int mImgWidth;
    private int mImgHeight;

    public FBOVideoRenderer(Context context) {
        mContext = context;

        drawFilter = new ShowFilter(context);
        showFilter = new NoFilter(context);
        processColorFilter = new ProcessFilter(context);
        processColorFilter.setFilter(new ColorFilter(context));

        beautyFilter = new ProcessBeautyFilter(context);

        setWaterMarkPosition();
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

    public void onSurfaceCreated() {
        mTextureId = OpenGlUtils.getExternalOESTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        drawFilter.setTextureId(mTextureId);
        drawFilter.onSurfaceCreated();

        showFilter.onSurfaceCreated();
        processColorFilter.onSurfaceCreated();
        beautyFilter.onSurfaceCreated();

        groupFilter.onSurfaceCreated();
    }

    public void onSurfaceChanged(int width, int height) {
        Log.e("ttt", "onSurfaceChanged");
        if (mWidth != width && mHeight != height) {
            mFBObuffer = new FrameBuffer();
            mFBObuffer.create(width, height);

            groupFilter.setSize(width, height);
            beautyFilter.setSize(width, height);
            processColorFilter.setSize(width, height);
            showFilter.setSize(width, height);
            setViewSize(width, height);
        }
    }

    public void onDrawFrame() {
        if (mSurfaceTexture != null) {
            //更新数据，其实也是消耗数据，将上一帧的数据处理或者抛弃掉，要不然SurfaceTexture是接收不到最新数据
            mSurfaceTexture.updateTexImage();

            GLES20.glViewport(0, 0, mWidth, mHeight);
            mFBObuffer.beginDrawToFrameBuffer();
            drawFilter.onDrawFrame();
            mFBObuffer.endDrawToFrameBuffer();

            groupFilter.setTextureId(mFBObuffer.getTextureId());
            groupFilter.onDrawFrame();

            processColorFilter.setTextureId(groupFilter.getOutputTexture());
            processColorFilter.onDrawFrame();

            beautyFilter.setTextureId(processColorFilter.getOutputTexture());
            beautyFilter.onDrawFrame();

            showFilter.setTextureId(beautyFilter.getOutputTexture());
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
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (showFilter != null) {
            showFilter.release();
            showFilter = null;
        }
        if (drawFilter != null) {
            drawFilter.release();
            drawFilter = null;
        }
        if (processColorFilter != null) {
            processColorFilter.release();
            processColorFilter = null;
        }
        if (beautyFilter != null) {
            beautyFilter.release();
            beautyFilter = null;
        }
        if (groupFilter != null) {
            groupFilter.release();
            groupFilter.clearAll();
            groupFilter = null;
        }
    }

    public void setVideoSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        calculateMatrix();
    }

    public void setViewSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        waterMarkFilter.setPosition(width - mImgWidth, 0, mImgWidth, mImgHeight);
    }

    private void calculateMatrix() {
        float[] matrix = Gl2Utils.getOriginalMatrix();
        Gl2Utils.getMatrix(matrix, Gl2Utils.TYPE_CENTERINSIDE, mVideoWidth, mVideoHeight, mWidth, mHeight);
        Gl2Utils.flip(matrix, false, true);
        drawFilter.setMatrix(matrix);
    }
}

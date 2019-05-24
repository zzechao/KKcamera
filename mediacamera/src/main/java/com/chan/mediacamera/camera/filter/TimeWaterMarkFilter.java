package com.chan.mediacamera.camera.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.chan.mediacamera.util.Gl2Utils;

import java.text.SimpleDateFormat;
import java.util.Date;



public class TimeWaterMarkFilter extends NoFilter {

    private SimpleDateFormat mDateFormat;
    private String mPreTimeStr;
    /**
     * 水印的放置位置和宽高
     */
    private int x, y, w, h;
    /**
     * 控件的大小
     */
    private int width, height;

    /***/
    private NoFilter mFilter;
    private Bitmap mBitmap;
    private boolean mStats = false;

    public TimeWaterMarkFilter(Context context) {
        super(context);
        mFilter = new NoFilter(context) {
            @Override
            protected void onClear() {

            }
        };
        float[] OM = Gl2Utils.getOriginalMatrix();
        Gl2Utils.flip(OM, false, true);
        setMatrix(OM);
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }


    @Override
    public void onDrawFrame() {
        super.onDrawFrame();
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        String timeStr = mDateFormat.format(date);
        mBitmap = Gl2Utils.text2Bitmap(timeStr);
        GLES20.glViewport(x, y, w == 0 ? mBitmap.getWidth() : w, h == 0 ? mBitmap.getHeight() : h);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
        createTexture();
        mFilter.onDrawFrame();
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glViewport(0, 0, width, height);
    }

    public void isBitmap(boolean stats) {
        mStats = stats;
    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();
        mFilter.onSurfaceCreated();

        Gl2Utils.flip(mFilter.getMatrix(), false, true);
    }

    private int[] textures = new int[1];

    private void createTexture() {
        if (mBitmap != null) {
            //生成纹理
            GLES20.glGenTextures(1, textures, 0);
            //生成纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);

            mFilter.setTextureId(textures[0]);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        this.width = width;
        this.height = height;
        mFilter.setSize(width, height);
    }

    public void setPosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.w = width;
        this.h = height;
    }
}

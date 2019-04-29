package viewset.com.kkcamera.view.camera.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import viewset.com.kkcamera.view.image.opengl.util.Gl2Utils;

/**
 * 针对水印首先初始化一个滤镜Nofilter来存放真正的水印绘制的filter
 */
public class WaterMarkFilter extends NoFilter {
    /**
     * 水印的放置位置和宽高
     */
    private int x, y, w, h;
    /**
     * 控件的大小
     */
    private int width, height;
    /**
     * 水印图片的bitmap
     */
    private Bitmap mBitmap;

    /**
     * 真正的水印绘制
     */
    private NoFilter mFilter;

    boolean mStats = false;

    public WaterMarkFilter(Context context) {
        super(context);
        mFilter = new NoFilter(context) {
            @Override
            protected void onClear() {

            }
        };
        float[] OM = Gl2Utils.getOriginalMatrix();
        Gl2Utils.flip(OM, false, true);
        setMatrix(OM);
    }

    public void setWaterMark(Bitmap bitmap) {
        if (this.mBitmap != null) {
            this.mBitmap.recycle();
        }
        this.mBitmap = bitmap;
    }

    @Override
    public void onDrawFrame() {
        super.onDrawFrame();
        GLES20.glViewport(x, y, w == 0 ? mBitmap.getWidth() : w, h == 0 ? mBitmap.getHeight() : h);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
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
        createTexture();
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

            //对画面进行矩阵旋转
            if (!mStats) {
                Gl2Utils.flip(mFilter.getMatrix(), false, true);
            }

            mFilter.setTextureId(textures[0]);

            mBitmap.recycle();
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

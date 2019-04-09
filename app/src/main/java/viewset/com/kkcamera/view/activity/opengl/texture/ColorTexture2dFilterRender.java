package viewset.com.kkcamera.view.activity.opengl.texture;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import viewset.com.kkcamera.view.activity.opengl.filter.BeautyFilter;
import viewset.com.kkcamera.view.activity.opengl.filter.ColorFilter;
import viewset.com.kkcamera.view.activity.opengl.filter.GrayFilter;
import viewset.com.kkcamera.view.activity.opengl.filter.N1977Filter;

public class ColorTexture2dFilterRender implements GLSurfaceView.Renderer {

    ColorFilter colorFilter;
    Context mContext;
    EGLConfig mConfig;
    int mWidth, mHeight;
    boolean isFilterChange;
    Bitmap mBitmap;

    public ColorTexture2dFilterRender(Context context) {
        mContext = context;
        isFilterChange = false;
        colorFilter = new ColorFilter(mContext) {
            @Override
            public void glOnSufaceCreated(int program) {

            }

            @Override
            protected void onDrawArraysPre() {

            }

            @Override
            protected void onDrawArraysAfter() {

            }

        };
        colorFilter.init(this);
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mConfig = config;
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        if (colorFilter == null) {
            return;
        }
        colorFilter.onSurfaceCreated(gl, config);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mWidth = width;
        mHeight = height;
        GLES20.glViewport(0, 0, width, height);
        if (colorFilter == null) {
            return;
        }
        colorFilter.onSurfaceChanged(gl, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (colorFilter == null) {
            return;
        }
        if (isFilterChange && mWidth != 0 && mHeight != 0) {
            colorFilter.onSurfaceCreated(gl, mConfig);
            colorFilter.onSurfaceChanged(gl, mWidth, mHeight);
            isFilterChange = false;
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        colorFilter.onDrawFrame(gl);
    }

    public void setFilter(FilterState.Filter filter) {
        if (colorFilter != null) {
            colorFilter.realse();
            colorFilter = null;
        }
        if (filter == FilterState.Filter.NONE) {
            colorFilter = new ColorFilter(mContext) {
                @Override
                public void glOnSufaceCreated(int program) {

                }

                @Override
                protected void onDrawArraysPre() {

                }

                @Override
                protected void onDrawArraysAfter() {

                }

            };
            colorFilter.init(this);
        } else if (filter == FilterState.Filter.GRAY) {
            colorFilter = new GrayFilter(mContext);
            colorFilter.init(this);
        } else if (filter == FilterState.Filter.N1977) {
            colorFilter = new N1977Filter(mContext);
            colorFilter.init(this);
        } else if (filter == FilterState.Filter.BEAUTY) {
            colorFilter = new BeautyFilter(mContext);
            colorFilter.init(this);
        }
        isFilterChange = true;
    }
}

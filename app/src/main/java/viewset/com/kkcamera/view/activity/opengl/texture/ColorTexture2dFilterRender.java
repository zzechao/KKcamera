package viewset.com.kkcamera.view.activity.opengl.texture;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import viewset.com.kkcamera.view.activity.opengl.filter.ColorFilter;
import viewset.com.kkcamera.view.activity.opengl.filter.GrayFilter;

public class ColorTexture2dFilterRender implements GLSurfaceView.Renderer {

    ColorFilter colorFilter;
    Context mContext;

    public ColorTexture2dFilterRender(Context context) {
        mContext = context;
        colorFilter = new ColorFilter(context) {
            @Override
            public void glOnSufaceCreated(int program) {

            }

            @Override
            public void glOnDrawFrame() {

            }
        };
        colorFilter.init();
    }

    public void setBitmap(Bitmap bitmap) {
        colorFilter.setBitmap(bitmap);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        colorFilter.onSurfaceCreated(gl, config);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        colorFilter.onSurfaceChanged(gl, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        colorFilter.onDrawFrame(gl);
    }

    public void setFilter(FilterState.Filter filter) {
        if (filter == FilterState.Filter.GRAY) {
            if (colorFilter != null) {
                colorFilter.realse();
                colorFilter = null;
            }
            colorFilter = new GrayFilter(mContext);
            colorFilter.init();
        }
    }
}

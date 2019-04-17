package viewset.com.kkcamera.view.camera.filter;

import android.content.Context;
import android.opengl.GLES20;

import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;

public class NoFilter extends BaseFilter {
    public NoFilter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("camera/show_vertex.glsl", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("camera/base_fragment.glsl", context.getResources()));
        mContext = context;
    }

    @Override
    protected void onClear() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    protected void onSizeChanged(int width, int height) {

    }

}

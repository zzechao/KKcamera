package viewset.com.kkcamera.view.camera.filter;

import android.content.Context;

import viewset.com.kkcamera.view.activity.opengl.texture.OpenGlUtils;

public class N1977Filter extends BaseFilter {
    public N1977Filter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("camera/base_vertex.glsl", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("camera/base_fragment.glsl", context.getResources()));
        mContext = context;
    }

    @Override
    protected void glOnSufaceCreated(int mProgram) {

    }

    @Override
    protected void onDrawArraysAfter() {

    }

    @Override
    protected void onDrawArraysPre() {

    }
}

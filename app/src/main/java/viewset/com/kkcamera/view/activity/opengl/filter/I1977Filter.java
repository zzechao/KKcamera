package viewset.com.kkcamera.view.activity.opengl.filter;

import android.content.Context;

import viewset.com.kkcamera.view.activity.opengl.texture.OpenGlUtils;

public class I1977Filter extends ColorFilter {

    public I1977Filter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("filter/half_color_vertex.sh", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("filter/1977_fragment.sh", context.getResources()));
    }

    @Override
    public void glOnSufaceCreated(int program) {

    }

    @Override
    protected void onDrawArraysPre() {

    }

    @Override
    protected void onDrawArraysAfter() {

    }

}

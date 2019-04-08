package viewset.com.kkcamera.view.activity.opengl.filter;


import android.content.Context;
import android.opengl.GLES20;

import viewset.com.kkcamera.view.activity.opengl.texture.FilterState;
import viewset.com.kkcamera.view.activity.opengl.texture.OpenGlUtils;

public class GrayFilter extends ColorFilter {

    private int glChangeType;
    private int glChangeColor;

    public GrayFilter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("filter/half_color_vertex.sh", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("filter/half_color_fragment.sh", context.getResources()));
    }

    @Override
    public void glOnSufaceCreated(int program) {
        glChangeType = GLES20.glGetUniformLocation(program, "vChangeType");
        glChangeColor = GLES20.glGetUniformLocation(program, "vChangeColor");
    }

    @Override
    protected void onDrawArraysPre() {
        GLES20.glUniform1i(glChangeType, FilterState.Filter.GRAY.getType());
        GLES20.glUniform3fv(glChangeColor, 1, FilterState.Filter.GRAY.data(), 0);
    }

    @Override
    protected void onDrawArraysAfter() {

    }
}

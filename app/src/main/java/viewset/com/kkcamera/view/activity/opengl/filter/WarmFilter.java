package viewset.com.kkcamera.view.activity.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import viewset.com.kkcamera.view.activity.opengl.texture.FilterState;
import viewset.com.kkcamera.view.activity.opengl.texture.OpenGlUtils;

public class WarmFilter extends ColorFilter{

    private int glChangeType;
    private int glChangeColor;

    public WarmFilter(Context context) {
            super(OpenGlUtils.loadShareFromAssetsFile("filter/half_color_vertex.glsl", context.getResources()),
                    OpenGlUtils.loadShareFromAssetsFile("filter/half_color_fragment.glsl", context.getResources()));
    }

    @Override
    public void glOnSufaceCreated(int program) {
        glChangeType = GLES20.glGetUniformLocation(program, "vChangeType");
        glChangeColor = GLES20.glGetUniformLocation(program, "vChangeColor");
    }

    @Override
    protected void onDrawArraysPre() {
        GLES20.glUniform1i(glChangeType, FilterState.Filter.WARM.getType());
        GLES20.glUniform3fv(glChangeColor, 1, FilterState.Filter.WARM.data(), 0);
    }

    @Override
    protected void onDrawArraysAfter() {

    }
}

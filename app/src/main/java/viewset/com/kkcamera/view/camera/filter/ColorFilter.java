package viewset.com.kkcamera.view.camera.filter;

import android.content.Context;
import android.opengl.GLES20;

import viewset.com.kkcamera.view.image.opengl.texture.FilterState;
import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;

public class ColorFilter extends BaseFilter {
    private int glIsHalf;
    private int glChangeType;
    private int glChangeColor;

    private int[] fFrame = new int[1];
    private int[] fRender = new int[1];
    private int[] fTexture = new int[1];

    public ColorFilter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("filter/half_color_vertex.glsl", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("filter/half_color_fragment.glsl", context.getResources()));
        mContext = context;
    }

    @Override
    protected void glOnSufaceCreated(int mProgram) {
        glIsHalf = GLES20.glGetUniformLocation(mProgram, "vIsHalf");
        glChangeType = GLES20.glGetUniformLocation(mProgram, "vChangeType");
        glChangeColor = GLES20.glGetUniformLocation(mProgram, "vChangeColor");
    }

    @Override
    protected void onDrawArraysAfter() {

    }

    @Override
    protected void onDrawArraysPre() {
        GLES20.glUniform1i(glIsHalf, 1);
        GLES20.glUniform1i(glChangeType, FilterState.Filter.GRAY.getType());
        GLES20.glUniform3fv(glChangeColor, 1, FilterState.Filter.GRAY.data(), 0);
    }

    @Override
    protected void onSizeChanged(int width, int height) {

    }

}

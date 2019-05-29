package com.chan.mediacamera.camera.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.chan.mediacamera.util.OpenGlUtils;


public class ColorFilter extends BaseFilter {
    private int glIsHalf;
    private int glChangeType;
    private int glChangeColor;

    public ColorFilter(Context context) {
        super(context);
    }

    @Override
    public void onCreateShare() {
       mVertexShader = OpenGlUtils.loadShareFromAssetsFile("camera/half_color_vertex.glsl", mContext.getResources());
       mFragmentShader = OpenGlUtils.loadShareFromAssetsFile("camera/half_color_fragment.glsl", mContext.getResources());
    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();
        glIsHalf = GLES20.glGetUniformLocation(mProgram, "vIsHalf");
        glChangeType = GLES20.glGetUniformLocation(mProgram, "vChangeType");
        glChangeColor = GLES20.glGetUniformLocation(mProgram, "vChangeColor");
    }

    @Override
    protected void onSetExpandData() {
        GLES20.glUniform1i(glIsHalf, 0);
        GLES20.glUniform1i(glChangeType, FilterState.Filter.WARM.getType());
        GLES20.glUniform3fv(glChangeColor, 1, FilterState.Filter.WARM.data(), 0);
        super.onSetExpandData();
    }

    @Override
    protected void onSizeChanged(int width, int height) {

    }
}

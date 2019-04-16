package viewset.com.kkcamera.view.camera.filter;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import viewset.com.kkcamera.view.image.opengl.texture.FilterState;
import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;
import viewset.com.kkcamera.view.image.opengl.util.Gl2Utils;

public class ColorFilter extends BaseFilter {
    private int glIsHalf;
    private int glChangeType;
    private int glChangeColor;

    public ColorFilter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("camera/half_color_vertex.glsl", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("camera/half_color_fragment.glsl", context.getResources()));
        mContext = context;

        float[] OM = Gl2Utils.getOriginalMatrix();
        setMatrix(OM);
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
    protected void onBindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + getTextureType());
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, getTextureId());
        GLES20.glUniform1i(glTexture, getTextureType());
    }
}

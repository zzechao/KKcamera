package viewset.com.kkcamera.view.camera.filter;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;

public class ShowFilter extends BaseFilter {
    public ShowFilter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("camera/base_vertex.glsl", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("camera/base_fragment.glsl", context.getResources()));
        mContext = context;
    }

    @Override
    protected void onBindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + getTextureType());
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, getTextureId());
        GLES20.glUniform1i(glTexture, getTextureType());
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

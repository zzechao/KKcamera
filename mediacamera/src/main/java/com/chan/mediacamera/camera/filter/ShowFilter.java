package com.chan.mediacamera.camera.filter;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.chan.mediacamera.util.OpenGlUtils;


public class ShowFilter extends BaseFilter {
    public ShowFilter(Context context) {
        super(context);
    }

    @Override
    public void onCreateShare() {
        mVertexShader = OpenGlUtils.loadShareFromAssetsFile("camera/show_vertex.glsl", mContext.getResources());
        mFragmentShader = OpenGlUtils.loadShareFromAssetsFile("camera/show_fragment.glsl", mContext.getResources());
    }

    @Override
    protected void onBindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + getTextureType());
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, getTextureId());
        GLES20.glUniform1i(glTexture, getTextureType());
    }


    @Override
    protected void onSizeChanged(int width, int height) {

    }
}

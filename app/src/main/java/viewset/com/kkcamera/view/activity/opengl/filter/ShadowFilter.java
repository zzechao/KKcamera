package viewset.com.kkcamera.view.activity.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import viewset.com.kkcamera.view.activity.opengl.texture.OpenGlUtils;

public class ShadowFilter extends ColorFilter {
    private int inputTextureUniformLocations2;
    private int inputTextureHandles2;

    public ShadowFilter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("filter/half_color_vertex.glsl", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("filter/shadow_fragment.glsl", context.getResources()));
        mContext = context;
    }

    @Override
    public void glOnSufaceCreated(int program) {
        inputTextureUniformLocations2 = GLES20.glGetUniformLocation(program, "inputImageTexture2");
    }

    @Override
    protected void onDrawArraysPre() {
        inputTextureHandles2 = OpenGlUtils.loadTexture(mContext, "image/shadow.png");
        if (inputTextureHandles2 != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureHandles2);
            GLES20.glUniform1i(inputTextureUniformLocations2, 3);
        }
    }

    @Override
    protected void onDrawArraysAfter() {

    }
}

package viewset.com.kkcamera.view.image.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;

public class ShadowFilter extends ColorFilter {
    private int inputTextureUniformLocation2;
    private int inputTextureHandle2;

    public ShadowFilter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("filter/half_color_vertex.glsl", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("filter/shadow_fragment.glsl", context.getResources()));
        mContext = context;
    }

    @Override
    public void glOnSufaceCreated(int program) {
        inputTextureUniformLocation2 = GLES20.glGetUniformLocation(program, "inputImageTexture2");
    }

    @Override
    protected void onDrawArraysPre() {
        inputTextureHandle2 = OpenGlUtils.loadTexture(mContext, "image/shadow.png");
        if (inputTextureHandle2 != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureHandle2);
            GLES20.glUniform1i(inputTextureUniformLocation2, 3);
        }
    }

    @Override
    protected void onDrawArraysAfter() {
        if (inputTextureHandle2 != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        }
    }
}

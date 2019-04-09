package viewset.com.kkcamera.view.activity.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import viewset.com.kkcamera.view.activity.opengl.texture.OpenGlUtils;

public class MeTestFilter extends ColorFilter {

    private int[] inputTextureUniformLocations = {-1, -1};
    private int[] inputTextureHandles = {-1, -1};

    public MeTestFilter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("filter/half_color_vertex.glsl", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("filter/mytest_fragment.glsl", context.getResources()));
        mContext = context;
    }

    @Override
    public void glOnSufaceCreated(int program) {
        for (int i = 0; i < inputTextureUniformLocations.length; i++) {
            inputTextureUniformLocations[i] = GLES20.glGetUniformLocation(program, "inputImageTexture" + (i + 2));
        }
    }

    @Override
    protected void onDrawArraysPre() {
        inputTextureHandles[0] = OpenGlUtils.loadTexture(mContext, "image/me.png");
        inputTextureHandles[1] = OpenGlUtils.loadTexture(mContext, "image/me2.png");
        for (int i = 0; i < inputTextureHandles.length
                && inputTextureHandles[i] != OpenGlUtils.NO_TEXTURE; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i + 3));
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureHandles[i]);
            GLES20.glUniform1i(inputTextureUniformLocations[i], (i + 3));
        }
    }

    @Override
    protected void onDrawArraysAfter() {
        for (int i = 0; i < inputTextureHandles.length
                && inputTextureHandles[i] != OpenGlUtils.NO_TEXTURE; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i + 3));
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        }
    }
}

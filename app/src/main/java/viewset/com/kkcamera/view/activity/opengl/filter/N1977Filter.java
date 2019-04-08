package viewset.com.kkcamera.view.activity.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import viewset.com.kkcamera.view.activity.opengl.texture.ColorTexture2dFilterRender;
import viewset.com.kkcamera.view.activity.opengl.texture.OpenGlUtils;

public class N1977Filter extends ColorFilter {

    private int[] inputTextureHandles = {-1, -1};
    private int[] inputTextureUniformLocations = {-1, -1};

    public N1977Filter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("filter/half_color_vertex.sh", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("filter/n1977_fragment.sh", context.getResources()));
        mContext = context;
    }

    @Override
    public void init(ColorTexture2dFilterRender render) {
        super.init(render);
    }

    @Override
    public void glOnSufaceCreated(int program) {
        for (int i = 0; i < inputTextureUniformLocations.length; i++) {
            Log.e("ttt", "--glOnSufaceCreated--");
            inputTextureUniformLocations[i] = GLES20.glGetUniformLocation(program, "inputImageTexture" + (2 + i));
        }
    }

    @Override
    protected void onDrawArraysPre() {
        inputTextureHandles[0] = OpenGlUtils.loadTexture(mContext, "image/n1977map.png");
        inputTextureHandles[1] = OpenGlUtils.loadTexture(mContext, "image/n1977blowout.png");
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

package viewset.com.kkcamera.view.image.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.seu.magicfilter.utils.MagicParams;

import java.nio.FloatBuffer;

import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;

/**
 * Created by Administrator on 2016/5/22.
 */
public class BeautyFilter extends ColorFilter {
    private int mSingleStepOffsetLocation;
    private int mParamsLocation;

    public BeautyFilter(Context context) {
        super(OpenGlUtils.loadShareFromAssetsFile("filter/half_color_vertex.glsl", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("filter/beauty_fragment.glsl", context.getResources()));
        mContext = context;
    }


    public void setBeautyLevel(int level) {
        float beauty = 1.0f;
        switch (level) {
            case 1:
                beauty = 1.0f;
                break;
            case 2:
                beauty = 0.8f;
                break;
            case 3:
                beauty = 0.6f;
                break;
            case 4:
                beauty = 0.4f;
                break;
            case 5:
                beauty = 0.33f;
                break;
            default:
                break;
        }
        GLES20.glUniform1f(mParamsLocation, beauty);
    }

    public void onBeautyLevelChanged() {
        setBeautyLevel(MagicParams.beautyLevel);
    }

    @Override
    public void glOnSufaceCreated(int program) {
        mSingleStepOffsetLocation = GLES20.glGetUniformLocation(program, "singleStepOffset");
        mParamsLocation = GLES20.glGetUniformLocation(program, "params");
    }

    @Override
    protected void onDrawArraysPre() {
        GLES20.glUniform2fv(mSingleStepOffsetLocation, 1, FloatBuffer.wrap(new float[]{2.0f / mInputWidth, 2.0f / mInputHeight}));
        onBeautyLevelChanged();
    }

    @Override
    protected void onDrawArraysAfter() {

    }
}

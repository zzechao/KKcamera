package viewset.com.kkcamera.view.camera.filter;

import android.content.Context;
import android.opengl.GLES20;

import viewset.com.kkcamera.view.camera.filter.pkm.ZipPkmReader;
import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;
import viewset.com.kkcamera.view.image.opengl.util.Gl2Utils;

public class PkmFilter extends NoFilter {

    private NoFilter mFilter;
    private int glAlphaTexture;

    private ZipPkmReader mPkmReader;
    private int fTextureSize = 2;
    private int[] textures = new int[fTextureSize];
    private int width, height;

    public PkmFilter(Context context) {
        super(context);
        mPkmReader = new ZipPkmReader(context.getAssets());
        mFilter = new NoFilter(context) {

            @Override
            public void onCreateShare() {
                mVertexShader = OpenGlUtils.loadShareFromAssetsFile("camera/pkm_vertex.glsl", mContext.getResources());
                mFragmentShader = OpenGlUtils.loadShareFromAssetsFile("camera/pkm_fragment.glsl", mContext.getResources());
            }

            @Override
            protected void onClear() {

            }

            @Override
            public void onSurfaceCreated() {
                super.onSurfaceCreated();
                glAlphaTexture = GLES20.glGetUniformLocation(mProgram, "inputImageTextureAlpha");
            }

            @Override
            protected void onBindTexture() {

            }
        };

        float[] OM = Gl2Utils.getOriginalMatrix();
        Gl2Utils.flip(OM, false, true);
        setMatrix(OM);
    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();
        mFilter.onSurfaceCreated();
        createTexture();
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * 生成纹理
     */
    private void createTexture() {
        GLES20.glGenTextures(fTextureSize, textures, 0);
        for (int i = 0; i < fTextureSize; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        }
        mFilter.setTextureId(textures[0]);
    }
}

package com.chan.mediacamera.camera.filter;

import android.content.Context;
import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.opengl.GLES20;

import com.chan.mediacamera.camera.filter.pkm.ZipPkmReader;
import com.chan.mediacamera.util.Gl2Utils;
import com.chan.mediacamera.util.OpenGlUtils;

import java.nio.ByteBuffer;

/**
 * 根据https://blog.csdn.net/junzia/article/details/53872303,重构的pkmFilter
 */
public class PkmFilter extends NoFilter {

    private NoFilter mFilter;
    private int glAlphaTexture;

    private ZipPkmReader mPkmReader;
    private int fTextureSize = 2;
    private int[] textures = new int[fTextureSize];
    private int width, height, pkmWidth, pkmHeight;
    private ByteBuffer emptyBuffer;
    private ByteBuffer emptyBuffer2;
    private int x, y;

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
                ETC1Util.ETC1Texture t = mPkmReader.getNextTexture();
                ETC1Util.ETC1Texture tAlpha = mPkmReader.getNextTexture();
                if (t != null && tAlpha != null) {
                    pkmWidth = t.getWidth();
                    pkmHeight = t.getHeight();
                    float[] OM = Gl2Utils.getOriginalMatrix();
                    Gl2Utils.getMatrix(OM, Gl2Utils.TYPE_FITEND, t.getWidth(), t.getHeight(), width, height);
                    Gl2Utils.flip(OM, false, true);
                    setMatrix(OM);

                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + getTextureType());
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
                    ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0, GLES20.GL_RGB, GLES20
                            .GL_UNSIGNED_SHORT_5_6_5, t);
                    GLES20.glUniform1i(glTexture, getTextureType());

                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + 1 + getTextureType());
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1]);
                    ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0, GLES20.GL_RGB, GLES20
                            .GL_UNSIGNED_SHORT_5_6_5, tAlpha);
                    GLES20.glUniform1i(glAlphaTexture, 1 + getTextureType());
                } else {
                    if (mPkmReader != null) {
                        mPkmReader.close();
                        mPkmReader.open();
                    }
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + getTextureType());
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
                    ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0, GLES20.GL_RGB, GLES20
                            .GL_UNSIGNED_SHORT_5_6_5, new ETC1Util.ETC1Texture(width, height, emptyBuffer));
                    GLES20.glUniform1i(glAlphaTexture, getTextureType());

                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + 1 + getTextureType());
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1]);
                    ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0, GLES20.GL_RGB, GLES20
                            .GL_UNSIGNED_SHORT_5_6_5, new ETC1Util.ETC1Texture(width, height, emptyBuffer2));
                    GLES20.glUniform1i(glAlphaTexture, 1 + getTextureType());
                }
            }
        };

        float[] OM = Gl2Utils.getOriginalMatrix();
        Gl2Utils.flip(OM, false, true);
        setMatrix(OM);
    }

    @Override
    protected void onClear() {

    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();
        mFilter.onSurfaceCreated();
        createTexture();
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            mFilter.setSize(width, height);
            emptyBuffer = ByteBuffer.allocateDirect(ETC1.getEncodedDataSize(width, height));
            emptyBuffer2 = ByteBuffer.allocateDirect(ETC1.getEncodedDataSize(width, height));
        }
    }

    @Override
    public void onDrawFrame() {
        super.onDrawFrame();
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glViewport(x - pkmWidth / 2, y - pkmHeight / 2, pkmWidth, pkmHeight * 2);
        mFilter.onDrawFrame();
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glViewport(0, 0, width, height);
    }

    /**
     * 生成纹理
     */
    private void createTexture() {
        GLES20.glGenTextures(fTextureSize, textures, 0);
        for (int i = 0; i < fTextureSize; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        }
        mFilter.setTextureId(textures[0]);
    }

    /**
     * 加载pkm动画
     *
     * @param path
     */
    public void setAnimation(String path) {
        mPkmReader.setZipPath(path);
        mPkmReader.open();
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

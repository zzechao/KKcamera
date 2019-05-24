package com.chan.mediacamera.camera.filter.pkm;

import android.content.Context;
import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.opengl.GLES20;

import com.chan.mediacamera.camera.filter.BaseFilter;
import com.chan.mediacamera.util.Gl2Utils;
import com.chan.mediacamera.util.OpenGlUtils;

import java.nio.ByteBuffer;


public class ZipPkmAnimationFilter extends BaseFilter {

    private ZipPkmReader mPkmReader;
    private int fTextureSize = 2;
    private int[] textures = new int[fTextureSize];

    private ByteBuffer emptyBuffer;

    private int glAlphaTexture;
    private int mWidth, mHeight;
    private int x;
    private int y;
    private int pkmWidth;
    private int pkmHeight;

    public ZipPkmAnimationFilter(Context context) {
        super(context);
        mPkmReader = new ZipPkmReader(context.getAssets());
    }

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
        genTextures();
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        mWidth = width;
        mHeight = height;
        emptyBuffer = ByteBuffer.allocateDirect(ETC1.getEncodedDataSize(width, height));
    }

    @Override
    public void onDrawFrame() {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glViewport(x - pkmWidth / 2, y - pkmHeight / 2, pkmWidth, pkmHeight * 2);
        super.onDrawFrame();
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    //生成Textures
    private void genTextures() {
        GLES20.glGenTextures(fTextureSize, textures, 0);
        for (int i = 0; i < fTextureSize; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        }
        setTextureId(textures[0]);
    }

    public void setAnimation(String path) {
        mPkmReader.setZipPath(path);
        mPkmReader.open();
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    protected void finalize() throws Throwable {
        if (mPkmReader != null) {
            mPkmReader.close();
        }
        super.finalize();
    }

    @Override
    protected void onBindTexture() {
        ETC1Util.ETC1Texture t = mPkmReader.getNextTexture();
        ETC1Util.ETC1Texture tAlpha = mPkmReader.getNextTexture();
        if (t != null && tAlpha != null) {
            pkmWidth = t.getWidth();
            pkmHeight = t.getHeight();
            Gl2Utils.getMatrix(super.getMatrix(), Gl2Utils.TYPE_FITEND, t.getWidth(), t.getHeight(), mWidth, mHeight);
            Gl2Utils.flip(super.getMatrix(), false, true);
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
                    .GL_UNSIGNED_SHORT_5_6_5, new ETC1Util.ETC1Texture(mWidth, mHeight, emptyBuffer));
            GLES20.glUniform1i(glAlphaTexture, getTextureType());

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + 1 + getTextureType());
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1]);
            ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0, GLES20.GL_RGB, GLES20
                    .GL_UNSIGNED_SHORT_5_6_5, new ETC1Util.ETC1Texture(mWidth, mHeight, emptyBuffer));
            GLES20.glUniform1i(glAlphaTexture, 1 + getTextureType());
        }
    }

}

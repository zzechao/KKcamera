package com.chan.mediacamera.camera.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.chan.mediacamera.util.Gl2Utils;

public class ProcessFilter extends BaseFilter {

    private BaseFilter mFilter;

    //创建离屏buffer
    private int[] fFrame = new int[1];
    private int[] fRender = new int[1];
    private int[] fTexture = new int[1];

    private int width;
    private int height;

    public ProcessFilter(Context context) {
        super(context);

        mFilter = new NoFilter(context);
    }

    public void setFilter(BaseFilter filter) {
        mFilter = filter;
        float[] OM = Gl2Utils.getOriginalMatrix();
        Gl2Utils.flip(OM, false, true);//矩阵上下翻转
        mFilter.setMatrix(OM);
    }

    @Override
    protected void initBuffer() {

    }

    @Override
    public void onDrawFrame() {
        boolean b = GLES20.glIsEnabled(GLES20.GL_CULL_FACE);
        if (b) {
            GLES20.glDisable(GLES20.GL_CULL_FACE);
        }
        GLES20.glViewport(0, 0, width, height);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fFrame[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fTexture[0], 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, fRender[0]);

        mFilter.setTextureId(getTextureId());
        mFilter.onDrawFrame();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        if (b) {
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        }
    }

    @Override
    public void onSurfaceCreated() {
        mFilter.onSurfaceCreated();
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        this.width = width;
        this.height = height;
        mFilter.setSize(width, height);
        deleteFrameBuffer();

        GLES20.glGenFramebuffers(1, fFrame, 0);
        GLES20.glGenRenderbuffers(1, fRender, 0);

        genTextures();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fFrame[0]);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, fRender[0]);

        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                width, height);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fTexture[0], 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, fRender[0]);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
    }

    //生成Textures
    private void genTextures() {
        GLES20.glGenTextures(1, fTexture, 0);
        for (int i = 0; i < 1; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fTexture[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
    }


    private void deleteFrameBuffer() {
        GLES20.glDeleteRenderbuffers(1, fRender, 0);
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
        GLES20.glDeleteTextures(1, fTexture, 0);
    }

    @Override
    public int getOutputTexture() {
        return fTexture[0];
    }
}

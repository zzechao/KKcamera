package viewset.com.kkcamera.view.camera.filter;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;
import viewset.com.kkcamera.view.image.opengl.util.Gl2Utils;

public abstract class BaseFilter {

    private FloatBuffer mVerBuffer, mTexBuffer;

    protected String mVertexShader;
    protected String mFragmentShader;

    private final float[] sPos = {
            -1.0f, 1.0f,    //左上角
            -1.0f, -1.0f,   //左下角
            1.0f, 1.0f,     //右上角
            1.0f, -1.0f     //右下角
    };

    private final float[] sCoord = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    public static final float[] OM = Gl2Utils.getOriginalMatrix();

    /**
     * 对于帧FBO来说，不能用new float[]的形式，因为如果这样会导致其他滤镜不存在matrix的对象，导致无效。
     */
    private float[] matrix = Arrays.copyOf(OM, 16);

    protected int mProgram;

    protected int glPosition;
    protected int glCoordinate;
    protected int glMatrix;
    protected int glTexture;

    protected Context mContext;

    private int mTextureType = 0;      //默认使用Texture2D0
    private int mTextureId = 0;

    public BaseFilter(Context context) {
        mContext = context;
        onCreateShare();
        initBuffer();
    }

    /**
     * 创建所有着色器，包括顶点着色器和片元着色器
     */
    public void onCreateShare() {
        mVertexShader = OpenGlUtils.loadShareFromAssetsFile("filter/default_vertex.glsl", mContext.getResources());
        mFragmentShader = OpenGlUtils.loadShareFromAssetsFile("filter/default_fragment.glsl", mContext.getResources());
    }

    /**
     * Buffer初始化
     */
    protected void initBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(sPos.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVerBuffer = bb.asFloatBuffer();
        mVerBuffer.put(sPos);
        mVerBuffer.position(0);

        ByteBuffer cc = ByteBuffer.allocateDirect(sCoord.length * 4);
        cc.order(ByteOrder.nativeOrder());
        mTexBuffer = cc.asFloatBuffer();
        mTexBuffer.put(sCoord);
        mTexBuffer.position(0);
    }

    /**
     * 获取所有句柄
     */
    public void onSurfaceCreated() {
        mProgram = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);

        glPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        glCoordinate = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        glMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        glTexture = GLES20.glGetUniformLocation(mProgram, "inputImageTexture");
    }

    public void onDrawFrame() {
        onClear();
        onUseProgram();
        onSetExpandData();
        onBindTexture();
        onDraw();
    }

    /**
     * 清除屏幕
     */
    protected void onClear() {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    protected void onUseProgram() {
        GLES20.glUseProgram(mProgram);
    }

    /**
     * 设置其他扩展数据
     */
    protected void onSetExpandData() {
        GLES20.glUniformMatrix4fv(glMatrix, 1, false, matrix, 0);
    }

    /**
     * 绑定默认纹理
     */
    protected void onBindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + getTextureType());
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getTextureId());
        GLES20.glUniform1i(glTexture, getTextureType());
    }

    /**
     * 启用顶点坐标和纹理坐标进行绘制
     */
    protected void onDraw() {
        GLES20.glEnableVertexAttribArray(glPosition);
        GLES20.glVertexAttribPointer(glPosition, 2, GLES20.GL_FLOAT, false, 0, mVerBuffer);

        GLES20.glEnableVertexAttribArray(glCoordinate);
        GLES20.glVertexAttribPointer(glCoordinate, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(glPosition);
        GLES20.glDisableVertexAttribArray(glCoordinate);
    }

    public final void setSize(int width, int height) {
        onSizeChanged(width, height);
    }

    protected abstract void onSizeChanged(int width, int height);

    public final void setTextureType(int type) {
        mTextureType = type;
    }

    public final int getTextureType() {
        return mTextureType;
    }

    public final int getTextureId() {
        return mTextureId;
    }

    public final void setTextureId(int textureId) {
        mTextureId = textureId;
    }

    /**
     * 销毁
     */
    public void realse() {
        GLES20.glDeleteProgram(mProgram);
    }

    /**
     * @param matrix
     */
    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }

    public int getOutputTexture() {
        return -1;
    }

    protected float[] getMatrix() {
        return matrix;
    }
}

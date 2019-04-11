package viewset.com.kkcamera.view.widget;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import viewset.com.kkcamera.view.activity.opengl.texture.OpenGlUtils;

public abstract class BaseFilter {

    private FloatBuffer bPos, bCoord;
    private String mVertexShader;
    private String mFragmentShader;

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

    private float[] mMVPMatrix = new float[16];

    private int mProgram;

    protected int glPosition;
    protected int glCoordinate;
    protected int glMatrix;
    protected int glTexture;
    protected int glIsHalf;

    protected Context mContext;

    private int mTextureType = 0;      //默认使用Texture2D0
    private int mTextureId = 0;

    public BaseFilter(Context context) {
        this(OpenGlUtils.loadShareFromAssetsFile("filter/default_vertex.glsl", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("filter/default_fragment.glsl", context.getResources()));
        mContext = context;
    }

    public BaseFilter(String vertexShader, String fragmentShader) {
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;

        ByteBuffer bb = ByteBuffer.allocateDirect(sPos.length * 4);
        bb.order(ByteOrder.nativeOrder());
        bPos = bb.asFloatBuffer();
        bPos.put(sPos);
        bPos.position(0);

        ByteBuffer cc = ByteBuffer.allocateDirect(sCoord.length * 4);
        cc.order(ByteOrder.nativeOrder());
        bCoord = cc.asFloatBuffer();
        bCoord.put(sCoord);
        bCoord.position(0);
    }

    /**
     * 设置其他着色器的变量
     *
     * @param program
     */
    public abstract void glOnSufaceCreated(int program);

    protected abstract void onDrawArraysPre();

    protected abstract void onDrawArraysAfter();

    public void onSurfaceCreated() {
        Log.e("ttt", "onSurfaceCreated");
        mProgram = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);

        glPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        glCoordinate = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        glMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        glTexture = GLES20.glGetUniformLocation(mProgram, "inputImageTexture");
        glIsHalf = GLES20.glGetUniformLocation(mProgram, "vIsHalf");

        glOnSufaceCreated(mProgram);
    }

    public void onDrawFrame() {
        GLES20.glUseProgram(mProgram);

        //指定vMatrix的值
        GLES20.glUniformMatrix4fv(glMatrix, 1, false, mMVPMatrix, 0);

        GLES20.glUniform1i(glIsHalf, 1);

        //启用三角形顶点的句柄
        GLES20.glEnableVertexAttribArray(glPosition);
        //传入顶点坐标
        GLES20.glVertexAttribPointer(glPosition, 2, GLES20.GL_FLOAT, false, 0, bPos);

        //启用纹理坐标的句柄
        GLES20.glEnableVertexAttribArray(glCoordinate);
        //传入纹理坐标
        GLES20.glVertexAttribPointer(glCoordinate, 2, GLES20.GL_FLOAT, false, 0, bCoord);

        onDrawArraysPre();

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + getTextureType());
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getTextureId());
        GLES20.glUniform1i(glTexture, getTextureType());

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(glPosition);
        GLES20.glDisableVertexAttribArray(glCoordinate);
        onDrawArraysAfter();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

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
        mMVPMatrix = matrix;
    }
}

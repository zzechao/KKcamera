package viewset.com.kkcamera.view.image.opengl.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import viewset.com.kkcamera.view.image.opengl.texture.ColorTexture2dFilterRender;
import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;

public abstract class ColorFilter implements GLSurfaceView.Renderer {

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

    private float[] mViewMatrix = new float[16];
    private float[] mProjectMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private int mProgram;

    protected int glPosition;
    protected int glCoordinate;
    protected int glMatrix;
    protected int glTexture;
    protected int glIsHalf;

    protected Context mContext;

    protected ColorTexture2dFilterRender mRender;
    protected int mTextureId;

    protected int mInputWidth;
    protected int mInputHeight;

    public ColorFilter(Context context) {
        this(OpenGlUtils.loadShareFromAssetsFile("filter/default_vertex.glsl", context.getResources()),
                OpenGlUtils.loadShareFromAssetsFile("filter/default_fragment.glsl", context.getResources()));
        mContext = context;
    }

    public ColorFilter(String vertexShader, String fragmentShader) {
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

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.e("ttt", "onSurfaceCreated");
        mProgram = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);

        glPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        glCoordinate = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        glMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        glTexture = GLES20.glGetUniformLocation(mProgram, "inputImageTexture");
        glIsHalf = GLES20.glGetUniformLocation(mProgram, "vIsHalf");

        glOnSufaceCreated(mProgram);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mInputWidth = width;
        mInputHeight = height;
        Bitmap bitmap = mRender.getBitmap();
        Log.e("ttt", "onSurfaceChanged--" + bitmap.isRecycled());
        if (!bitmap.isRecycled()) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            float sWH = w / (float) h;
            float sWidthHeight = width / (float) height;
            if (width > height) {
                if (sWH > sWidthHeight) {
                    Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight * sWH, sWidthHeight * sWH, -1, 1, 3, 7);
                } else {
                    Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight / sWH, sWidthHeight / sWH, -1, 1, 3, 7);
                }
            } else {
                if (sWH > sWidthHeight) {
                    Matrix.orthoM(mProjectMatrix, 0, -1, 1, -1 / sWidthHeight * sWH, 1 / sWidthHeight * sWH, 3, 7);
                } else {
                    Matrix.orthoM(mProjectMatrix, 0, -1, 1, -sWH / sWidthHeight, sWH / sWidthHeight, 3, 7);
                }
            }
            //设置相机位置
            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
            //计算变换矩阵
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.e("ttt", "onDrawFrame--" + mRender.getBitmap().isRecycled());
        GLES20.glUseProgram(mProgram);

        mTextureId = OpenGlUtils.loadTexture(mRender.getBitmap(), OpenGlUtils.NO_TEXTURE);

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

        if (mTextureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
            GLES20.glUniform1i(glTexture, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(glPosition);
        GLES20.glDisableVertexAttribArray(glCoordinate);
        onDrawArraysAfter();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /**
     * 销毁
     */
    public void realse() {
        GLES20.glDeleteProgram(mProgram);
        mRender = null;
    }

    /**
     * 初始化
     *
     * @param render
     */
    public void init(ColorTexture2dFilterRender render) {
        mRender = render;
    }
}

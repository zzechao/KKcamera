package viewset.com.kkcamera.view.image.opengl.texture;

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

public class EgTexture2dRender implements GLSurfaceView.Renderer {

    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "attribute vec2 vCoordinate;" +
                    "uniform mat4 vMatrix;" +
                    "varying vec2 aCoordinate;" +
                    "void main(){" +
                    "    gl_Position=vMatrix*vPosition;" +
                    "    aCoordinate=vCoordinate;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform sampler2D vTexture;" +
                    "varying vec2 aCoordinate;" +
                    "void main(){" +
                    "    gl_FragColor=texture2D(vTexture,aCoordinate);" +
                    "}";

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

    private FloatBuffer bPos, bCoord;

    private int mProgram;
    private int mPositionHandle;
    private int mCoordinateHandle;
    private int mMatrixHandle;
    private int mTextureHanle;

    private Bitmap mBitmap;

    private float[] mViewMatrix = new float[16];
    private float[] mProjectMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private int mTextureId = OpenGlUtils.NO_TEXTURE;


    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

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

        mProgram = OpenGlUtils.loadProgram(vertexShaderCode, fragmentShaderCode);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        mTextureHanle = GLES20.glGetUniformLocation(mProgram, "vTexture");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        if (mBitmap != null && !mBitmap.isRecycled()) {
            int w = mBitmap.getWidth();
            int h = mBitmap.getHeight();
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
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        GLES20.glUniform1i(mTextureHanle, 0);
        mTextureId = OpenGlUtils.loadTexture(mBitmap, mTextureId);

        //指定vMatrix的值
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);

        //启用三角形顶点的句柄
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        //传入顶点坐标
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, bPos);

        //启用纹理坐标的句柄
        GLES20.glEnableVertexAttribArray(mCoordinateHandle);
        //传入纹理坐标
        GLES20.glVertexAttribPointer(mCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, bCoord);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}

package viewset.com.kkcamera.view.image.opengl.render;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;

public class OvalRender implements GLSurfaceView.Renderer {

    private FloatBuffer vertexBuffer;

    float triangleCoords[];

    int r = 1;

    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "uniform mat4 vMatrix;" +
                    "void main() {" +
                    "  gl_Position = vMatrix*vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private int mProgram;

    private float[] mViewMatrix = new float[16];
    private float[] mProjectMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private int mMatrixHandler;
    private int mPositionHandle;
    private int mColorHandle;

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 每个顶点四个字节

    static final int COORDS_PER_VERTEX = 3;

    //顶点个数
    private int vertexCount;

    float color[] = {1.0f, 1.0f, 1.0f, 1.0f}; //白色

    private float height = 0.0f;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        triangleCoords = new float[360 * 3 + 3 + 3];
        triangleCoords[0] = 0.0f;             //设置圆心坐标
        triangleCoords[1] = 0.0f;
        triangleCoords[2] = height;
        for (int i = 0; i <= 360; i++) {
            float x = (float) (r * Math.cos(i * (Math.PI / 180f)));
            float y = (float) (r * Math.sin(i * (Math.PI / 180f)));
            float z = height;
            triangleCoords[3 * (i + 1)] = x;
            triangleCoords[3 * (i + 1) + 1] = y;
            triangleCoords[3 * (i + 1) + 2] = z;
        }

        vertexCount = triangleCoords.length / COORDS_PER_VERTEX;

        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        //申请底层空间
        ByteBuffer bb = ByteBuffer.allocateDirect(
                triangleCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        //将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(triangleCoords);
        vertexBuffer.position(0);
        
        //创建一个空的OpenGLES程序
        mProgram = OpenGlUtils.loadProgram(vertexShaderCode, fragmentShaderCode);

        //获取相机的vMatrix成员句柄
        mMatrixHandler = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        //获取顶点着色器的vPosition成员句柄
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        //获取片元着色器的vColor成员的句柄
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        //计算宽高比
        float ratio = (float) width / height;
        //设置透视投影
        Matrix.frustumM(mProjectMatrix, 0, -ratio, ratio, -1, 1, 2, 8);
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        // 执行渲染工作
        //将程序加入到OpenGLES2.0环境
        GLES20.glUseProgram(mProgram);

        //指定vMatrix的值
        GLES20.glUniformMatrix4fv(mMatrixHandler, 1, false, mMVPMatrix, 0);

        //启用三角形顶点的句柄
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        //准备三角形的坐标数据
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        //设置绘制三角形的颜色
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        //绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);
        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    public void setMatrix(float[] matrix) {
        this.mMVPMatrix = matrix;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}

package viewset.com.kkcamera.view.image.opengl.render;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ConeRender implements GLSurfaceView.Renderer {

    private FloatBuffer vertexBuffer, colorBuffer;
    private ShortBuffer indexBuffer;

    private float[] mViewMatrix = new float[16];
    private float[] mProjectMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private final String vertexShaderCode =
            "uniform mat4 vMatrix;" +
                    "varying vec4 vColor;" +
                    "attribute vec4 vPosition;" +
                    "void main(){" +
                    "    gl_Position=vMatrix*vPosition;" +
                    "    if(vPosition.z!=0.0){" +
                    "        vColor=vec4(0.0,0.0,0.0,1.0);" +
                    "    }else{" +
                    "        vColor=vec4(0.9,0.9,0.9,1.0);" +
                    "    }" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private int mProgram;

    float[] conePositions;

    float height = 3f;
    float r = 1f;

    static final int COORDS_PER_VERTEX = 3;
    private int vertexCount;

    private int mMatrixHandler;
    private int mPositionHandle;
    private int mColorHandle;

    private OvalRender topOval;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        topOval = new OvalRender();
        topOval.setHeight(0.0f);
        topOval.onSurfaceCreated(gl, config);

        conePositions = new float[360 * 3 + 3 + 3];
        conePositions[0] = 0.0f;             //设置圆心坐标
        conePositions[1] = 0.0f;
        conePositions[2] = height;
        for (int i = 0; i <= 360; i++) {
            float x = (float) (r * Math.cos(i * (Math.PI / 180f)));
            float y = (float) (r * Math.sin(i * (Math.PI / 180f)));
            float z = 0f;
            conePositions[3 * (i + 1)] = x;
            conePositions[3 * (i + 1) + 1] = y;
            conePositions[3 * (i + 1) + 2] = z;
        }

        vertexCount = conePositions.length / COORDS_PER_VERTEX;

        //将背景设置为灰色
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        //申请底层空间
        ByteBuffer bb = ByteBuffer.allocateDirect(
                conePositions.length * 4);
        bb.order(ByteOrder.nativeOrder());
        //将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(conePositions);
        vertexBuffer.position(0);

        //颜色着色器
//        ByteBuffer dd = ByteBuffer.allocateDirect(
//                color.length * 4);
//        dd.order(ByteOrder.nativeOrder());
//        colorBuffer = dd.asFloatBuffer();
//        colorBuffer.put(color);
//        colorBuffer.position(0);

        //顶点着色器
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        //将资源加入到着色器中，并编译
        GLES20.glShaderSource(vertexShader, vertexShaderCode);
        GLES20.glCompileShader(vertexShader);

        //片元着色器
        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        //将资源加入到着色器中
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
        GLES20.glCompileShader(fragmentShader);

        //创建一个空的OpenGLES程序
        mProgram = GLES20.glCreateProgram();
        //将顶点着色器加入到程序
        GLES20.glAttachShader(mProgram, vertexShader);
        //将片元着色器加入到程序中
        GLES20.glAttachShader(mProgram, fragmentShader);
        //连接到着色器程序
        GLES20.glLinkProgram(mProgram);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // 渲染窗口大小发生改变的处理
        GLES20.glViewport(0, 0, width, height);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        //计算宽高比
        float ratio = (float) width / height;
        //设置透视投影
        Matrix.frustumM(mProjectMatrix, 0, -ratio, ratio, -1, 1, 3, 17);
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 1.0f, -10.0f, -4.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);

        topOval.onSurfaceChanged(gl, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        //将程序加入到OpenGLES2.0环境
        GLES20.glUseProgram(mProgram);

        //获取变换矩阵vMatrix成员句柄
        mMatrixHandler = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        //指定vMatrix的值
        GLES20.glUniformMatrix4fv(mMatrixHandler, 1, false, mMVPMatrix, 0);

        //
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        //启用三角形顶点的句柄
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, vertexBuffer);

        //获取片元着色器的vColor成员的句柄
//        mColorHandle = GLES20.glGetAttribLocation(mProgram, "aColor");
//        //设置绘制三角形的颜色
//        GLES20.glEnableVertexAttribArray(mColorHandle);
//        GLES20.glVertexAttribPointer(mColorHandle, 4,
//                GLES20.GL_FLOAT, false,
//                0, colorBuffer);

        //绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);
        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);

        topOval.setMatrix(mMVPMatrix);
        topOval.onDrawFrame(gl);
    }
}

package viewset.com.kkcamera.view.activity.opengl.vary;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import viewset.com.kkcamera.view.activity.opengl.texture.OpenGlUtils;

public class Cube implements Graph {

    private FloatBuffer vertexBuffer, colorBuffer;
    private ShortBuffer indexBuffer;

    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "uniform mat4 vMatrix;" +
                    "varying  vec4 aColor;" +
                    "attribute vec4 vColor;" +
                    "void main() {" +
                    "  gl_Position = vMatrix*vPosition;" +
                    "  aColor=vColor;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 aColor;" +
                    "void main() {" +
                    "  gl_FragColor = aColor;" +
                    "}";

    final float[] cubePositions = {
            -1.0f, 1.0f, 1.0f,    //正面左上0
            -1.0f, -1.0f, 1.0f,   //正面左下1
            1.0f, -1.0f, 1.0f,    //正面右下2
            1.0f, 1.0f, 1.0f,     //正面右上3
            -1.0f, 1.0f, -1.0f,    //反面左上4
            -1.0f, -1.0f, -1.0f,   //反面左下5
            1.0f, -1.0f, -1.0f,    //反面右下6
            1.0f, 1.0f, -1.0f,     //反面右上7
    };

    final short index[] = {
            0, 3, 2, 0, 2, 1,    //正面
            0, 1, 5, 0, 5, 4,    //左面
            0, 7, 3, 0, 4, 7,    //上面
            6, 7, 4, 6, 4, 5,    //后面
            6, 3, 7, 6, 2, 3,    //右面
            6, 5, 1, 6, 1, 2     //下面
    };

    float color[] = {
            0f, 1f, 1f, 1f,
            0f, 1f, 1f, 1f,
            0f, 1f, 1f, 1f,
            0f, 1f, 1f, 1f,
            1f, 0f, 1f, 1f,
            1f, 0f, 1f, 1f,
            1f, 0f, 1f, 1f,
            1f, 0f, 1f, 1f,
    };

    private int mProgram;
    private int glPositionHandler;
    private int glColorHandler;
    private int glMatrixHandler;

    private float[] matrix;

    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 每个顶点四个字节
    static final int COORDS_PER_VERTEX = 3;

    public Cube() {
        //申请底层空间
        ByteBuffer bb = ByteBuffer.allocateDirect(cubePositions.length * 4);
        bb.order(ByteOrder.nativeOrder());
        //将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(cubePositions);
        vertexBuffer.position(0);

        //颜色着色器
        ByteBuffer dd = ByteBuffer.allocateDirect(color.length * 4);
        dd.order(ByteOrder.nativeOrder());
        colorBuffer = dd.asFloatBuffer();
        colorBuffer.put(color);
        colorBuffer.position(0);

        //链接
        ByteBuffer cc = ByteBuffer.allocateDirect(index.length * 2);
        cc.order(ByteOrder.nativeOrder());
        indexBuffer = cc.asShortBuffer();
        indexBuffer.put(index);
        indexBuffer.position(0);
    }

    @Override
    public void create() {
        mProgram = OpenGlUtils.loadProgram(vertexShaderCode, fragmentShaderCode);
        glPositionHandler = GLES20.glGetAttribLocation(mProgram, "vPosition");
        glMatrixHandler = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        glColorHandler = GLES20.glGetAttribLocation(mProgram, "vColor");
    }

    @Override
    public void draw() {
        //将程序加入到OpenGLES2.0环境
        GLES20.glUseProgram(mProgram);

        //启用三角形顶点的句柄
        GLES20.glEnableVertexAttribArray(glPositionHandler);
        GLES20.glVertexAttribPointer(glPositionHandler, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        //设置绘制三角形的颜色
        GLES20.glEnableVertexAttribArray(glColorHandler);
        GLES20.glVertexAttribPointer(glColorHandler, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        //指定vMatrix的值
        if (matrix != null) {
            GLES20.glUniformMatrix4fv(glMatrixHandler, 1, false, matrix, 0);
        }

        //绘制正方形，通过012，023绘制出两个三角形，组合成一个正方形
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, index.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(glPositionHandler);
    }
}

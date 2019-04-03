package viewset.com.kkcamera.view.activity.opengl.texture2d;

public class texture2dRender {
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

    private final float[] sPos={
            -1.0f,1.0f,    //左上角
            -1.0f,-1.0f,   //左下角
            1.0f,1.0f,     //右上角
            1.0f,-1.0f     //右下角
    };

    private final float[] sCoord={
            0.0f,0.0f,
            0.0f,1.0f,
            1.0f,0.0f,
            1.0f,1.0f,
    };
}

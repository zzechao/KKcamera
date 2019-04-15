package viewset.com.kkcamera.view.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import viewset.com.kkcamera.view.camera.filter.ShowFilter;
import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;
import viewset.com.kkcamera.view.image.opengl.util.Gl2Utils;
import viewset.com.kkcamera.view.camera.filter.BaseFilter;

public class KKRenderer implements GLSurfaceView.Renderer {


    private BaseFilter baseFilter;

    protected Context mContext;
    private int mTextureId = OpenGlUtils.NO_TEXTURE;
    private SurfaceTexture mSurfaceTexture;

    private float[] matrix = new float[16];

    private int mPreviewWidth;
    private int mPreviewHeight;
    private int mWidth;
    private int mHeight;

    private int cameraId = 0;


    public KKRenderer(Context context) {
        mContext = context;

        baseFilter = new ShowFilter(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mTextureId = OpenGlUtils.getExternalOESTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        baseFilter.setTextureId(mTextureId);
        baseFilter.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        setViewSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mSurfaceTexture != null) {
            //更新数据，其实也是消耗数据，将上一帧的数据处理或者抛弃掉，要不然SurfaceTexture是接收不到最新数据
            mSurfaceTexture.updateTexImage();

            baseFilter.onDrawFrame();
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public boolean isAvailable() {
        return mSurfaceTexture != null;
    }

    public void releaseSurfaceTexture() {
        if (mSurfaceTexture != null) {
            boolean shouldRelease = true;
            if (shouldRelease) {
                mSurfaceTexture.release();
            }
            mSurfaceTexture = null;
        }
    }

    public void setPreviewSize(int width, int height) {
        Log.e("ttt", width + "---" + height);
        mPreviewWidth = width;
        mPreviewHeight = height;
        calculateMatrix();
    }

    public void setViewSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        calculateMatrix();
    }

    private void calculateMatrix() {
        Log.e("ttt", "calculateMatrix");
        Gl2Utils.getShowMatrix(matrix, mPreviewWidth, mPreviewHeight, mWidth, mHeight);
        if (cameraId == 1) {
            Gl2Utils.flip(matrix, true, false);
            Gl2Utils.rotate(matrix, 90);
        } else {
            Gl2Utils.rotate(matrix, 270);
        }
        baseFilter.setMatrix(matrix);
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
        calculateMatrix();
    }
}

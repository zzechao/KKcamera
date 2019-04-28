package viewset.com.kkcamera.view.camera.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.Matrix;

import viewset.com.kkcamera.view.image.opengl.texture.OpenGlUtils;
import viewset.com.kkcamera.view.image.opengl.util.Gl2Utils;

public class ImgShowFilter extends NoFilter {

    private Bitmap mBitmap;

    public ImgShowFilter(Context context) {
        super(context);
    }

    @Override
    protected void onClear() {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        float[] mMVPMatrix = new float[16];

        if (!mBitmap.isRecycled()) {
            int w = mBitmap.getWidth();
            int h = mBitmap.getHeight();

            Gl2Utils.getShowMatrix(mMVPMatrix, w, h, width, height);
        }
        setMatrix(mMVPMatrix);

        GLES20.glViewport(0, 0, width, height);
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        if (mBitmap != null) {
            int textureId = OpenGlUtils.loadTexture(mBitmap, OpenGlUtils.NO_TEXTURE, false);
            setTextureId(textureId);
        }
    }
}

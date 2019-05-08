package viewset.com.kkcamera.view.camera.record;

import android.graphics.SurfaceTexture;

/**
 * 录像的Media混合器
 */
public class MediaMuxer {

    private byte[] VideoObject = new byte[0];
    private byte[] AudioObject = new byte[0];

    private int mTextureId;

    public void setTextureId(int textureId) {
        mTextureId = textureId;
    }

    public void frameAvailable(SurfaceTexture mSurfaceTexture) {

    }
}

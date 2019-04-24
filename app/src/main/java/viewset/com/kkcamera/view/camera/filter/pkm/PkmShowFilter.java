package viewset.com.kkcamera.view.camera.filter.pkm;

import android.content.Context;

import viewset.com.kkcamera.view.camera.filter.NoFilter;
import viewset.com.kkcamera.view.image.opengl.util.Gl2Utils;

/**
 * 针对https://blog.csdn.net/junzia/article/details/53872303写的Pkm
 */
public class PkmShowFilter extends NoFilter {

    private ZipPkmAnimationFilter mFilter;

    public PkmShowFilter(Context context) {
        super(context);
        mFilter = new ZipPkmAnimationFilter(context);
        mFilter.setAnimation("assets/etczip/cc.zip");

        float[] OM = Gl2Utils.getOriginalMatrix();
        Gl2Utils.flip(OM, false, true);
        setMatrix(OM);
    }

    @Override
    protected void onClear() {

    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();
        mFilter.onSurfaceCreated();
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        mFilter.setPosition(200, 200);
        mFilter.setSize(width, height);
    }

    @Override
    public void onDrawFrame() {
        super.onDrawFrame();
        mFilter.onDrawFrame();
    }
}

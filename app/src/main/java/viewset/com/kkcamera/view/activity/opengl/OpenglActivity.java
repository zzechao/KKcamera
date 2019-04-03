package viewset.com.kkcamera.view.activity.opengl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.activity.opengl.render.TriangleRenderWithCamera;
import viewset.com.kkcamera.view.widget.GLTextureView;

/**
 * https://blog.csdn.net/junzia/article/details/52801772
 * https://blog.csdn.net/code_better/article/details/52093948
 */
public class OpenglActivity extends AppCompatActivity {

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;

    @BindView(R.id.gltexture)
    GLTextureView mGLView;

    TriangleRenderWithCamera renderer;

    private Unbinder unbinder;

    private float mPreviousX;
    private float mPreviousY;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opengl);
        unbinder = ButterKnife.bind(this);

        renderer = new TriangleRenderWithCamera();

        mGLView.setEGLContextClientVersion(2);
        mGLView.setRenderer(renderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                // reverse direction of rotation above the mid-line
                if (y > mGLView.getMeasuredHeight() / 2) {
                    dx = dx * -1;
                }

                // reverse direction of rotation to left of the mid-line
                if (x < mGLView.getMeasuredWidth() / 2) {
                    dy = dy * -1;
                }

                renderer.setAngle(
                        renderer.getAngle() +
                                ((dx + dy) * TOUCH_SCALE_FACTOR));
                mGLView.requestRender();
        }

        mPreviousX = x;
        mPreviousY = y;
        return true;
    }

}

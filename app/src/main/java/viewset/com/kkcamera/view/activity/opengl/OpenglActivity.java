package viewset.com.kkcamera.view.activity.opengl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.activity.opengl.render.BallRender;
import viewset.com.kkcamera.view.widget.GLTextureView;

/**
 * https://blog.csdn.net/junzia/article/details/52801772
 * https://blog.csdn.net/code_better/article/details/52093948
 */
public class OpenglActivity extends AppCompatActivity {


    @BindView(R.id.gltexture)
    GLTextureView mGLView;

    GLSurfaceView.Renderer renderer;

    private Unbinder unbinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opengl);
        unbinder = ButterKnife.bind(this);

        renderer = new BallRender();

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
}

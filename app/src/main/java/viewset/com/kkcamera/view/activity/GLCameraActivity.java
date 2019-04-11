package viewset.com.kkcamera.view.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.widget.KKGLSurfaceView;

public class GLCameraActivity extends AppCompatActivity {

    @BindView(R.id.kkcamera)
    KKGLSurfaceView kkcamera;

    private Unbinder unbinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        unbinder = ButterKnife.bind(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        kkcamera.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        kkcamera.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        kkcamera.onDestroy();
        unbinder.unbind();
    }
}

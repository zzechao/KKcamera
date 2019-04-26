package viewset.com.kkcamera.view.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.camera.KKGLSurfaceView;

public class GLCameraActivity extends AppCompatActivity {

    @BindView(R.id.kkcamera)
    KKGLSurfaceView kkcamera;

    @BindView(R.id.iv_pic)
    ImageView iv_pic;

    private Unbinder unbinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        unbinder = ButterKnife.bind(this);
    }

    @OnClick({R.id.bt_switch, R.id.takephoto})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.bt_switch:
                kkcamera.switchCamera();
                break;
            case R.id.takephoto:
                kkcamera.takePhoto(new KKGLSurfaceView.Callback<Bitmap>() {
                    @Override
                    public void back(final Bitmap bitmap) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                iv_pic.setVisibility(View.VISIBLE);
                                iv_pic.setImageBitmap(bitmap);
                            }
                        });
                    }
                },iv_pic.getMeasuredWidth(),iv_pic.getMeasuredHeight());
                break;
        }
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

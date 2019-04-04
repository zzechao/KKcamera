package viewset.com.kkcamera.view.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.activity.opengl.OpenglActivity;
import viewset.com.kkcamera.view.activity.opengl.EgTexture2dActivity;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.button_camera)
    Button btCamera;

    @BindView(R.id.button_video)
    Button btVideo;

    @BindView(R.id.button_magic)
    Button btMagic;

    Unbinder unbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);

    }

    @OnClick({R.id.button_camera, R.id.button_video, R.id.button_magic, R.id.button_opengl, R.id.button_texture2d})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.button_camera:
                if (PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_DENIED || PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);
                } else {
                    startActivity(new Intent(this, CameraActivity.class));
                }
                break;
            case R.id.button_video:

                break;
            case R.id.button_magic:
                if (PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_DENIED || PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            2);
                } else {
                    startActivity(new Intent(this, MagicActivity.class));
                }
                break;
            case R.id.button_opengl:
                startActivity(new Intent(this, OpenglActivity.class));
                break;
            case R.id.button_texture2d:
                startActivity(new Intent(this, EgTexture2dActivity.class));
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (grantResults.length != 1 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case 1:
                    startActivity(new Intent(this, CameraActivity.class));
                    break;
                case 2:
                    startActivity(new Intent(this, MagicActivity.class));
                    break;
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}

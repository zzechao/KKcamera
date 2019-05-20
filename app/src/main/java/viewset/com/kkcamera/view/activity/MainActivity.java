package viewset.com.kkcamera.view.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.image.opengl.EgTexture2dActivity;
import viewset.com.kkcamera.view.image.opengl.OpenglActivity;
import viewset.com.kkcamera.view.image.opengl.Texture2dFilterActivity;

/**
 * https://blog.csdn.net/oShunz/column/info/androidrealfilter
 * https://blog.csdn.net/qqchenjian318/article/details/77396653
 * https://blog.csdn.net/junzia/column/info/15997
 * https://www.jianshu.com/u/6bd036a3a723
 */
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

    @OnClick({R.id.button_camera, R.id.button_video, R.id.button_magic, R.id.button_magic_camera, R.id.button_opengl, R.id.button_texture2d, R.id.button_texture2dfilter, R.id.button_glcamera})
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
                    startActivity(new Intent(this, MagicImgActivity.class));
                }
                break;
            case R.id.button_magic_camera:
                if (PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_DENIED || PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            2);
                } else {
                    startActivity(new Intent(this, MagicCameraActivity.class));
                }
                break;
            case R.id.button_opengl:
                startActivity(new Intent(this, OpenglActivity.class));
                break;
            case R.id.button_texture2d:
                startActivity(new Intent(this, EgTexture2dActivity.class));
                break;
            case R.id.button_texture2dfilter:
                startActivity(new Intent(this, Texture2dFilterActivity.class));
                break;
            case R.id.button_glcamera:
                if (PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_DENIED || PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED || PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED || PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                            3);
                } else {
                    startActivity(new Intent(this, GLCameraActivity.class));
                }
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
                    startActivity(new Intent(this, MagicImgActivity.class));
                    break;
                case 3:
                    startActivity(new Intent(this, GLCameraActivity.class));
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

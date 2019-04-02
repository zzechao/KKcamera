package viewset.com.kkcamera.view.activity.opengl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.widget.GLTextureView;

public class Texture2dActivity extends AppCompatActivity {

    @BindView(R.id.gltexture2d)
    GLTextureView glTextureView;

    private Unbinder unbinder;

    private Bitmap bitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture2d);
        ButterKnife.bind(this);

        unbinder = ButterKnife.bind(this);

        try {
            bitmap = BitmapFactory.decodeStream(getResources().getAssets().open("texture/fengj.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        glTextureView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glTextureView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}

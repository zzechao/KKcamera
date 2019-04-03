package viewset.com.kkcamera.view.activity.opengl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.activity.opengl.texture.Texture2dRender;
import viewset.com.kkcamera.view.widget.GLTextureView;

public class Texture2dActivity extends AppCompatActivity {

    @BindView(R.id.gltexture2d)
    GLTextureView glTextureView;

    private Unbinder unbinder;

    Texture2dRender mRenderer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture2d);
        ButterKnife.bind(this);

        unbinder = ButterKnife.bind(this);

        try {
            mRenderer = new Texture2dRender();
            glTextureView.setEGLContextClientVersion(2);
            glTextureView.setRenderer(mRenderer);
            glTextureView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            Bitmap bitmap = BitmapFactory.decodeStream(getResources().getAssets().open("texture/timg.jpeg"));
            mRenderer.setBitmap(bitmap);
            glTextureView.requestRender();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}

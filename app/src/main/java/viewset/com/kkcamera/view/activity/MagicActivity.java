package viewset.com.kkcamera.view.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.seu.magicfilter.MagicEngine;
import com.seu.magicfilter.filter.helper.MagicFilterType;
import com.seu.magicfilter.utils.MagicParams;
import com.seu.magicfilter.widget.MagicImageView;
import com.seu.magicfilter.widget.base.MagicBaseView;

import java.io.IOException;

import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.activity.opengl.texture.OpenGlUtils;

public class MagicActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magic);
        MagicImageView magicImageView = findViewById(R.id.miv);
        new MagicEngine.Builder().build(magicImageView);
        magicImageView.setFilter(MagicFilterType.N1977);
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getResources().getAssets().open("texture/timg.jpeg"));
            magicImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

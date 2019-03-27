package viewset.com.kkcamera.view.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.seu.magicfilter.MagicEngine;
import com.seu.magicfilter.widget.base.MagicBaseView;

import viewset.com.kkcamera.R;

public class MagicActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magic);
        new MagicEngine.Builder().build((MagicBaseView) findViewById(R.id.magic));
    }
}

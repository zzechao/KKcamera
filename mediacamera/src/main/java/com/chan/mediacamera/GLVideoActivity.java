package com.chan.mediacamera;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import com.chan.mediacamera.widget.VideoGLSurfaceView;
import com.dmcbig.mediapicker.entity.Media;

public class GLVideoActivity extends AppCompatActivity implements View.OnClickListener {

    VideoGLSurfaceView gl_video;
    ImageButton tijiao;

    public static void startActivity(AppCompatActivity activity, Media media) {
        Intent intent = new Intent(activity, GLVideoActivity.class);
        intent.putExtra("media", media);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glvideo);

        gl_video = findViewById(R.id.gl_video);
        tijiao = findViewById(R.id.tijiao);

        Intent intent = getIntent();
        Media media = intent.getParcelableExtra("media");

        gl_video.setPath(media.path);

        tijiao.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gl_video.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gl_video.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gl_video.onDestroy();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.tijiao) {

        }
    }
}

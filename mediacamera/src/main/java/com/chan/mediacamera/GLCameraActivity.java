package com.chan.mediacamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.chan.mediacamera.widget.CameraGLSurfaceView;
import com.chan.mediacamera.widget.CircularProgressView;
import com.dmcbig.mediapicker.PickerActivity;
import com.dmcbig.mediapicker.PickerConfig;
import com.dmcbig.mediapicker.entity.Media;

import java.util.ArrayList;


public class GLCameraActivity extends AppCompatActivity implements View.OnClickListener {


    CameraGLSurfaceView kkcamera;
    ImageView iv_pic;
    CircularProgressView record;
    RadioGroup bt_mode;


    private boolean isRecording = false;

    private static final int MAXTIME = 20000;//最长录制20s
    private int currentTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glcamera);

        kkcamera = findViewById(R.id.kkcamera);
        iv_pic = findViewById(R.id.iv_pic);
        record = findViewById(R.id.record);
        bt_mode = findViewById(R.id.bt_mode);

        record.setTotal(MAXTIME);

        record.photo();

        initListener();
    }

    private void initListener() {
        findViewById(R.id.bt_switch).setOnClickListener(this);
        findViewById(R.id.takephoto).setOnClickListener(this);
        findViewById(R.id.record).setOnClickListener(this);
        findViewById(R.id.recordok).setOnClickListener(this);
        findViewById(R.id.file).setOnClickListener(this);
        bt_mode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.photo) {
                    record.photo();
                } else if (checkedId == R.id.video) {
                    record.video();
                }
            }
        });
    }


    Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            if (currentTime <= MAXTIME && isRecording) {
                record.setProcess(currentTime);
                currentTime += 50;
                mHandler.sendMessageDelayed(mHandler.obtainMessage(0), 50);
            } else {
                isRecording = false;
                kkcamera.stopRecord();
                Toast.makeText(GLCameraActivity.this, kkcamera.getOutputPath(), Toast.LENGTH_SHORT).show();
            }
        }
    };

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
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.bt_switch) {
            kkcamera.switchCamera();
        } else if (i == R.id.takephoto) {
            kkcamera.takePhoto(new CameraGLSurfaceView.Callback<Bitmap>() {
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
            });
        } else if (i == R.id.recordok) {
            record.setProcess(0);
            record.clear();
            mHandler.removeMessages(0);
            kkcamera.stopRecord();
            isRecording = false;
            currentTime = 0;
            Toast.makeText(this, kkcamera.getOutputPath(), Toast.LENGTH_SHORT).show();
        } else if (i == R.id.record) {
            int id = bt_mode.getCheckedRadioButtonId();
            if (id == R.id.photo) {
                kkcamera.takePhoto(new CameraGLSurfaceView.Callback<Bitmap>() {
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
                });
            } else {
                if (isRecording) {
                    mHandler.removeMessages(0);
                    record.pause();
                    kkcamera.pauseRecord();
                } else {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(0), 50);
                    if (kkcamera.isPause()) {
                        kkcamera.resumeRecord();
                    } else {
                        kkcamera.startRecord();
                    }
                }
                isRecording = !isRecording;
            }
        } else if (i == R.id.file) {
            Intent intent = new Intent(GLCameraActivity.this, PickerActivity.class); //Take a photo with a camera
            intent.putExtra(PickerConfig.MAX_SELECT_COUNT, 1);
            intent.putExtra(PickerConfig.SELECT_MODE, PickerConfig.PICKER_VIDEO);
            GLCameraActivity.this.startActivityForResult(intent, 200);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200) {
            ArrayList<Media> mediaList = data.getParcelableArrayListExtra(PickerConfig.EXTRA_RESULT);
            if(!mediaList.isEmpty()){
                GLVideoActivity.startActivity(this, mediaList.get(0));
            }
        }
    }
}

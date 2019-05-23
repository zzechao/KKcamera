package viewset.com.kkcamera.view.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dmcbig.mediapicker.PickerActivity;
import com.dmcbig.mediapicker.TakePhotoActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.camera.KKGLSurfaceView;
import viewset.com.kkcamera.widget.CircularProgressView;

public class GLCameraActivity extends AppCompatActivity {

    @BindView(R.id.kkcamera)
    KKGLSurfaceView kkcamera;

    @BindView(R.id.iv_pic)
    ImageView iv_pic;

    @BindView(R.id.record)
    CircularProgressView record;

    @BindView(R.id.bt_mode)
    RadioGroup bt_mode;

    private Unbinder unbinder;

    private boolean isRecording = false;

    private static final int MAXTIME = 20000;//最长录制20s
    private int currentTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        unbinder = ButterKnife.bind(this);

        record.setTotal(MAXTIME);

        record.photo();
    }

    @OnCheckedChanged({R.id.photo, R.id.video})
    public void onCheckedChanged(CompoundButton view, boolean ischanged) {
        switch (view.getId()) {
            case R.id.photo:
                if (ischanged) {
                    record.photo();
                }
                break;
            case R.id.video:
                if (ischanged) {
                    record.video();
                }
                break;
        }
    }


    @OnClick({R.id.bt_switch, R.id.takephoto, R.id.record, R.id.recordok, R.id.file})
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
                });
                break;
            case R.id.recordok:
                record.setProcess(0);
                record.clear();
                mHandler.removeMessages(0);
                kkcamera.stopRecord();
                isRecording = false;
                currentTime = 0;
                Toast.makeText(this, kkcamera.getOutputPath(), Toast.LENGTH_SHORT).show();
                break;
            case R.id.record:
                int id = bt_mode.getCheckedRadioButtonId();
                if (id == R.id.photo) {
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
                break;
            case R.id.file:
                Intent intent = new Intent(GLCameraActivity.this, PickerActivity.class); //Take a photo with a camera
                GLCameraActivity.this.startActivityForResult(intent, 200);
                break;
        }
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
        unbinder.unbind();
    }
}

package com.imay.capturefilter.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.hardware.Camera;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.imay.capturefilter.MagicEngine;
import com.imay.capturefilter.R;
import com.imay.capturefilter.adapter.FilterAdapter;
import com.imay.capturefilter.camera.CameraEngine;
import com.imay.capturefilter.filter.helper.MagicFilterType;
import com.imay.capturefilter.helper.SavePictureTask;
import com.imay.capturefilter.utils.ICUtils;
import com.imay.capturefilter.utils.MagicFilterTools;
import com.imay.capturefilter.utils.MagicParams;

import java.io.File;

/**
 * Created by chan on 2017/6/22 0022.
 */
public class IMCameraView extends RelativeLayout implements View.OnClickListener, IMMagicCameraView.MagicListener {

    private MagicEngine mMagicEngine;

    IMMagicCameraView mMagicCameraView;
    ICAutoAdjustRecylerView mRecyclerView;
    FilterAdapter mAdapter;

    private int mFlashMode = 0; //0关，1自动，2开, 3禁用：前置摄像头，

    //闪关灯按钮  // 网格按钮  //焦点图片
    private ImageView mIvFlash, mIvGrid, mIvFocus;
    private ICGridView mGridView;

    private IMCameraViewListener listener;

    public void setListener(IMCameraViewListener listener) {
        this.listener = listener;
    }

    public interface IMCameraViewListener {
        void close(); // 关闭

        void takeStart(); // 拍摄前

        void takeComplete(String path); // 拍摄完成

        void takeError(); // 拍摄失败

        void photopicker(); // 相册
    }

    public IMCameraView(Context context) {
        super(context);
        init(context);
    }

    public IMCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public IMCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_capture, this, true);
        initView();
        initFitter();
        initListener();
        initCamera();
    }

    private void initCamera() {
        MagicEngine.Builder builder = new MagicEngine.Builder();
        mMagicEngine = builder.build(mMagicCameraView);
    }

    private void initView() {
        mRecyclerView = $(R.id.recyclerView); // 滤镜
        mMagicCameraView = $(R.id.glsurfaceview_camera);
        mIvFlash = $(R.id.iv_flash); //闪光灯
        mIvGrid = $(R.id.iv_grid);   //网格线
        mGridView = $(R.id.imay_capture_gridview); // 网络格
        mIvFocus = $(R.id.iv_focus); //聚焦
    }

    private void initFitter() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayout.HORIZONTAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setPxPerMillsec(1.0f);

        MagicFilterTools filterTools = MagicFilterTools.getInstance(getContext());
        mAdapter = new FilterAdapter(getContext(), MagicFilterTools.types, filterTools.getFilterNames(getContext()));
        mAdapter.setOnFilterChangeListener(onFilterChangeListener);
        mRecyclerView.setAdapter(mAdapter);
    }


    private void initListener() {
        mIvFlash.setOnClickListener(this);
        mIvGrid.setOnClickListener(this);
        mMagicCameraView.setListener(this);
        $(R.id.iv_btn_sys_photo).setOnClickListener(this);   //系统相册
        $(R.id.iv_switch_camera).setOnClickListener(this);   //切换相机
        $(R.id.iv_close).setOnClickListener(this);
        $(R.id.iv_btn_capture).setOnClickListener(this); //拍照
    }

    @Override
    public void foucus(int focusX, int focusY) {
        updateFocusUI(focusX, focusY);
    }

    @Override
    public void flingLeft() {
        int pos = mAdapter.getSelected();
        if (pos < mAdapter.getItemCount() - 1) {
            pos = pos + 1;
            updateFilter(pos);
        }
    }

    @Override
    public void flingRight() {
        int pos = mAdapter.getSelected();
        if (pos > 0) {
            pos = pos - 1;
            updateFilter(pos);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.iv_switch_camera) {
            mMagicCameraView.switchCamera();
            setViewFlasMode();
        } else if (i == R.id.iv_flash) {
            setFlashMode();
        } else if (i == R.id.iv_grid) {
            showOrHidGridView();
        } else if (i == R.id.iv_close) {
            if (listener != null) {
                listener.close();
            }
        } else if (i == R.id.iv_btn_capture) {
            mMagicEngine.setFlashMode(mFlashMode);
            Camera camera = CameraEngine.getInstance().getCamera();
            if (camera != null) {
                if (camera.getParameters().getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    takePhoto();
                } else {
                    if (mMagicCameraView.getState() == IMMagicCameraView.State.NONE) { // 触摸对焦后牌照
                        takePhoto();
                    } else { // 首次没有触摸
                        CameraEngine.getInstance().getCamera().autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(final boolean success, final Camera camera) {
                                takePhoto();
                            }
                        });
                    }
                }
            }
        } else if (i == R.id.iv_btn_sys_photo) {
            if (listener != null) {
                listener.photopicker();
            }
        }
    }

    /**
     * 拍照
     */
    private void takePhoto() {
        if (listener == null) {
            return;
        }
        listener.takeStart();
        File imageFile = ICUtils.getOutputMediaFile(getContext());
        if (imageFile != null) {
            mMagicEngine.savePicture(imageFile, new SavePictureTask.OnPictureSaveListener() {
                @Override
                public void onSaved(final String result) {
                    if (!TextUtils.isEmpty(result)) {
                        // 返回图片路径
                        listener.takeComplete(result);
                    } else {
                        listener.takeError();
                    }
                }
            });
        } else {
            listener.takeError();
        }
    }

    /**
     * 恢复摄像头
     */
    public void resumeCamera() {
        try {
            mMagicCameraView.resumeCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭摄像头
     */
    public void pauceCamera() {
        try {
            mMagicCameraView.pauceCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开网络格
     */
    private void showOrHidGridView() {
        if (mGridView.getVisibility() == View.VISIBLE) {
            mGridView.setVisibility(View.GONE);
        } else {
            mGridView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 依次修改闪关灯
     */
    private void setFlashMode() {
        if (mFlashMode > 1) {
            mFlashMode = 0;
        } else {
            mFlashMode = mFlashMode + 1;
        }
        setViewFlasMode();
    }

    /**
     * 更新滤镜
     *
     * @param position
     */
    private void updateFilter(int position) {
        mMagicEngine.setFilter(MagicFilterTools.types[position]);
        mAdapter.setSelected(position);
        mRecyclerView.checkAutoAdjust(position);
        mAdapter.notifyDataSetChanged();
    }

    private FilterAdapter.FilterChangeListener onFilterChangeListener = new FilterAdapter.FilterChangeListener() {
        @Override
        public void onFilterChanged(MagicFilterType filterType, int position) {
            mMagicEngine.setFilter(filterType);
            mRecyclerView.checkAutoAdjust(position);
        }
    };

    private void updateFocusUI(int mFocusX, int mFocusY) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mIvFocus.getLayoutParams();
        params.setMargins(mFocusX - 50, mFocusY - 50, 0, 0);
        mIvFocus.setLayoutParams(params);
        mIvFocus.setVisibility(ImageView.VISIBLE);
        // 属性动画
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator oaX = ObjectAnimator.ofFloat(mIvFocus, "scaleX", 1.0f, 1.1f);
        ObjectAnimator oaY = ObjectAnimator.ofFloat(mIvFocus, "scaleY", 1.0f, 1.1f);
        ObjectAnimator oaX2 = ObjectAnimator.ofFloat(mIvFocus, "scaleX", 1.1f, 0.9f);
        ObjectAnimator oaY2 = ObjectAnimator.ofFloat(mIvFocus, "scaleY", 1.1f, 0.9f);
        ObjectAnimator oaX3 = ObjectAnimator.ofFloat(mIvFocus, "scaleX", 0.9f, 1.0f);
        ObjectAnimator oaY3 = ObjectAnimator.ofFloat(mIvFocus, "scaleY", 0.9f, 1.0f);
        set.play(oaX).with(oaY);
        set.play(oaX).before(oaX2).before(oaY2);
        set.play(oaX2).before(oaX3).before(oaY3);
        set.setDuration(300);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mIvFocus.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        set.start();
    }

    private void setViewFlasMode() {
        if (!CameraEngine.getInstance().getCameraBackStatus()) {//前置摄像头，不可用
            mMagicEngine.setFlashMode(3);
            mIvFlash.setEnabled(false);
            mIvFlash.setImageResource(R.mipmap.btn_03_camera_flash_clos_disable);
        } else {
            mMagicEngine.setFlashMode(mFlashMode);
            mIvFlash.setEnabled(true);
            mIvGrid.setSelected(false);
            mIvGrid.setEnabled(true);
            if (mFlashMode == 1) {
                mIvFlash.setImageResource(R.mipmap.btn_03_camera_flash_auto);
            } else if (mFlashMode == 2) {
                mIvFlash.setImageResource(R.mipmap.btn_03_camera_flash_default);
            } else {
                mIvFlash.setImageResource(R.mipmap.btn_03_camera_flash_clos);
            }
        }
    }

    /**
     * 销毁
     */
    public void release() {
        mMagicEngine = null;
        mMagicCameraView = null;
        mRecyclerView = null;
        mAdapter = null;
        MagicParams.magicBaseView = null;
        MagicParams.IS_ID_CARD_CAMERA = false;
        MagicParams.OPEN_CAMERA_ID = 0;
        CameraEngine.getInstance().onDestroy();
    }

    protected <T extends View> T $(int id) {
        return (T) findViewById(id);
    }
}

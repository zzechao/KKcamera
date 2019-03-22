package com.imay.capturefilter.widget;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.imay.capturefilter.R;

/**
 * Created by Murphy on 2016/12/23.
 */
public class ICLoadingView extends Dialog {

    private ImageView mRotateView;
    private TextView mMsg;
    private RotateAnimation mAnim;

    public ICLoadingView(Context context) {
        super(context, R.style.LoadingDialog);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        init();
    }

    private void init() {
        setContentView(R.layout.capturefilter_dialog_capture_filter);
        mRotateView = (ImageView) findViewById(R.id.iv_loading);
//        mRotateView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mMsg = (TextView) findViewById(R.id.tv_msg);
        initAnim();
    }

    private void initAnim() {
        // mAnim = new RotateAnimation(360, 0, Animation.RESTART, 0.5f,
        // Animation.RESTART, 0.5f);
        mAnim = new RotateAnimation(0, 359, Animation.RESTART, 0.5f, Animation.RESTART, 0.5f);
        mAnim.setDuration(1000);
        mAnim.setRepeatCount(Animation.INFINITE);
        mAnim.setRepeatMode(Animation.RESTART);
        //mAnim.setStartTime(Animation.START_ON_FIRST_FRAME);
    }

    @Override
    public void show() {// 在要用到的地方调用这个方法
        mRotateView.startAnimation(mAnim);
        super.show();
    }

    @Override
    public void dismiss() {
        mAnim.cancel();
        super.dismiss();
    }

    @Override
    public void setTitle(CharSequence title) {
        if (TextUtils.isEmpty(title)) {
            mMsg.setVisibility(View.GONE);
        } else {
            mMsg.setVisibility(View.VISIBLE);
            mMsg.setText(title);
        }
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getContext().getString(titleId));
    }

}

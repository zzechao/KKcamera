package com.chan.mediacamera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.chan.mediacamera.util.DensityUtils;

import java.util.ArrayList;
import java.util.List;


public class CircularProgressView extends AppCompatImageView {

    private int mStroke = 5;
    private int mProcess = 0;
    private int mTotal = 100;
    private int mNormalColor = 0x99FFFFFF;
    private int mSecondColor = 0xFF3396db;
    private int mStartAngle = -90;
    private List<Float> mPauseAngle = new ArrayList<>();
    private RectF mRectF;

    private Paint mPaint;
    private Drawable mDrawable;

    public CircularProgressView(Context context) {
        this(context, null);
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mStroke = DensityUtils.dp2px(getContext(), mStroke);
        mPaint = new Paint();
        mPaint.setColor(mNormalColor);
        mPaint.setStrokeWidth(mStroke);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);//作用于圆环结尾
        mPaint.setAntiAlias(true);
        mDrawable = new Progress();
        setImageDrawable(mDrawable);
    }

    public void setTotal(int total) {
        this.mTotal = total;
        mDrawable.invalidateSelf();
    }

    public void setProcess(int process) {
        this.mProcess = process;
        post(new Runnable() {
            @Override
            public void run() {
                mDrawable.invalidateSelf();
            }
        });
    }

    public int getProcess() {
        return mProcess;
    }

    public void setStroke(float dp) {
        this.mStroke = DensityUtils.dp2px(getContext(), dp);
        mPaint.setStrokeWidth(mStroke);
        mDrawable.invalidateSelf();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        }
    }

    public void pause() {
        mPauseAngle.add(mStartAngle + mProcess * 360 / (float) mTotal);
    }

    public void clear(){
        mPauseAngle.clear();
    }

    public void photo() {
        mNormalColor = 0x00FFFFFF;
        mDrawable.invalidateSelf();
    }

    public void video() {
        mNormalColor = 0x99FFFFFF;
        mDrawable.invalidateSelf();
    }

    private class Progress extends Drawable {
        @Override
        public void draw(Canvas canvas) {
            int width = getWidth();
            int pd = mStroke / 2 + 1;
            if (mRectF == null) {
                mRectF = new RectF(pd, pd, width - pd, width - pd);
            }
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(mNormalColor);
            canvas.drawCircle(width / 2, width / 2, width / 2 - pd, mPaint);
            mPaint.setColor(mSecondColor);
            canvas.drawArc(mRectF, mStartAngle, mProcess * 360 / (float) mTotal, false, mPaint);
            mPaint.setColor(Color.TRANSPARENT);
            for (int i = 0; i < mPauseAngle.size(); i++) {
                canvas.drawArc(mRectF, mPauseAngle.get(i), 3, false, mPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }
    }

}

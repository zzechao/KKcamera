package com.imay.capturefilter.widget;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class ICAutoAdjustRecylerView extends RecyclerView {

    private final String TAG = ICAutoAdjustRecylerView.class.getSimpleName();

    private Scroller mScroller = null;
    private int mLastx = 0;
    private float mPxPerMillsec = 0;    //用于设置自动平移时候的速度
    private int mTargetPos;

    private AutoAdjustItemClickListener mListener;

    public interface AutoAdjustItemClickListener {
        void onItemClick(View view, int postion);
    }

    public ICAutoAdjustRecylerView(Context context) {
        super(context);
        initData(context);
    }

    public ICAutoAdjustRecylerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData(context);
    }

    public ICAutoAdjustRecylerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initData(context);
    }

    private void initData(Context context) {
        mScroller = new Scroller(context, new Interpolator() {
            public float getInterpolation(float t) {
                return t;
            }
        });
    }

    public void setScroller(Scroller scroller) {
        if (mScroller != scroller) {
            mScroller = scroller;
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller != null) {
            //如果mScroller没有调用startScroll，这里将会返回false。
            if (mScroller.computeScrollOffset()) {
                scrollBy(mLastx - mScroller.getCurrX(), 0);
                mLastx = mScroller.getCurrX();
                postInvalidate();//继续让系统重绘
            }
        }
    }

    public AutoAdjustItemClickListener getItemClickListener() {
        return mListener;
    }

    public void setItemClickListener(AutoAdjustItemClickListener listener) {
        this.mListener = listener;
    }

    public float getPxPerMillsec() {
        return mPxPerMillsec;
    }

    public void setPxPerMillsec(float pxPerMillsec) {
        this.mPxPerMillsec = pxPerMillsec;
    }

    /**
     * 滑动距离
     * @param position
     */
    public void checkAutoAdjust(int position) {
        int parentWidth = getWidth();//获取父视图的宽度
        int firstvisiableposition = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
        int count = (getLayoutManager()).getItemCount();//获取item总数
        mTargetPos = Math.max(0, Math.min(count - 1, position));//获取目标item的位置（参考listview中的smoothScrollToPosition方法）
        View targetChild = getChildAt(mTargetPos - firstvisiableposition);//获取目标item在当前可见视图item集合中的位置
        if (targetChild != null) {
            int childLeftPx = targetChild.getLeft();//子view相对于父view的左边距
            int childRightPx = targetChild.getRight();//子view相对于父view的右边距

            int childWidth = targetChild.getWidth();
            int centerLeft = parentWidth / 2 - childWidth / 2;//计算子view居中后相对于父view的左边距
            int centerRight = parentWidth / 2 + childWidth / 2;//计算子view居中后相对于父view的右边距
            /*Log.i(TAG,"childLeftPx"+childLeftPx+"--------centerLeft"+centerLeft);
            Log.i(TAG,"childRightPx"+childRightPx+"--------centerRight"+centerRight);*/
            if (childLeftPx > centerLeft) {//子view左边距比居中view大（说明子view靠父view的右边，此时需要把子view向左平移
                autoAdjustScroll(childLeftPx, centerLeft);
            } else if (childRightPx < centerRight) {
                autoAdjustScroll(childRightPx, centerRight);
            }
        }
    }

    /**
     * @param start 滑动起始位置
     * @param end   滑动结束位置
     */
    private void autoAdjustScroll(int start, int end) {
        int duration = 0;
        if (mPxPerMillsec != 0) {
            duration = (int) ((Math.abs(end - start) / mPxPerMillsec));
        }
        mLastx = start;
        mScroller.startScroll(start, 0, end - start, 0, duration);
        postInvalidate();
    }

}

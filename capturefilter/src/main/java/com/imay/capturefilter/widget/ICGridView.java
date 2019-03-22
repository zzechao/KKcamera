package com.imay.capturefilter.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.imay.capturefilter.utils.ICDensityUtils;


public class ICGridView extends View {

    Paint paint;  //绘图
    private int width;

    public ICGridView(Context context) {
        super(context);
    }

    public ICGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(3);
        width = ICDensityUtils.getScreenW((Activity) context);
    }

    public ICGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 绘制网格线
     */
    @Override
    protected void onDraw(Canvas canvas) {
        int height = width;
        final int xspace = width / 3;   //长宽间隔
        int hspace = height / 3;
        int vertz = 0;
        int hortz = 0;
        for (int i = 0; i < 4; i++) {
            canvas.drawLine(0, hortz, width, hortz, paint);
            canvas.drawLine(vertz, 0, vertz, height, paint);
            vertz += xspace;
            hortz += hspace;
        }
    }
}

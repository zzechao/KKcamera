package com.imay.capturefilter.widget;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.imay.capturefilter.camera.CameraEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chan on 2017/6/22 0022.
 */

public class IMMagicCameraView extends MagicCameraView {

    private int mFocusX;
    private int mFocusY;

    private MagicListener listener;

    public void setListener(MagicListener listener) {
        this.listener = listener;
    }

    public enum State {NONE, DRAG, ZOOM}

    private ScaleGestureDetector mScaleGestureDetector; //手势放大监听
    private GestureDetector mGestureDetetor;
    private State state;

    public IMMagicCameraView(Context context) {
        super(context);
        init(context);
    }

    public IMMagicCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
        mGestureDetetor = new GestureDetector(context, new GestureDetetor());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            mGestureDetetor.onTouchEvent(event);
        } else {
            mScaleGestureDetector.onTouchEvent(event);
        }
        return true;
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    /**
     * 缩放监听
     */
    public class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float scale = 0;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (detector.getScaleFactor() > 1)//放大
                scale = 1;
            else
                scale = -2;
            CameraEngine.getInstance().setZoom(scale);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            setState(State.ZOOM);
            return true;    // 一定要返回true才会进入onScale()这个函数
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            setState(State.NONE);
        }
    }

    /**
     * 监听拖拽滑动
     */
    public class GestureDetetor extends GestureDetector.SimpleOnGestureListener {

        private static final int FLING_MIN_DISTANCE = 100;
        private static final int FLING_MIN_VELOCITY = 0;

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            setState(State.DRAG);
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            if (state == State.DRAG) {
                mFocusX = (int) event.getX();
                mFocusY = (int) event.getY();
                if (listener != null) {
                    listener.foucus(mFocusX, mFocusY);
                }
                handleFocusMetering(event);    //自动对焦
            }
            setState(State.NONE);
            return super.onSingleTapConfirmed(event);
        }

        //猛滑动
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (state == State.DRAG) {
                if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
                        && Math.abs(velocityX) > FLING_MIN_VELOCITY) { //左滑
                    if (listener != null) {
                        listener.flingLeft();
                    }
                } else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
                        && Math.abs(velocityX) > FLING_MIN_VELOCITY) { //右滑
                    if (listener != null) {
                        listener.flingRight();
                    }
                }
            }
            return true;
        }
    }

    /**
     * 聚焦
     *
     * @param event
     */
    private void handleFocusMetering(final MotionEvent event) {
        Camera mCamera = CameraEngine.getInstance().getCamera();
        if (mCamera != null) {
            Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);
            Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

            if (parameters.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
                focusAreas.add(new Camera.Area(focusRect, 1000));
                parameters.setFocusAreas(focusAreas);
            }

            if (parameters.getMaxNumMeteringAreas() > 0) {
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(meteringRect, 1000));
                parameters.setMeteringAreas(meteringAreas);
            }

            try {
                mCamera.setParameters(parameters);
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {

                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Rect calculateTapArea(float x, float y, float coefficient) {
        float focusAreaSize = 100;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

        // 获取屏幕中点
        int centerScreenX = getWidth() / 2;
        int centerScreenY = getHeight() / 2;

        // 计算以屏幕的中点的触点坐标
        int centerScreenTouchX = (int) (x - centerScreenX);
        int centerScreenTouchY = (int) (y - centerScreenY);

        // 以相机对焦为X(-1000,1000),Y(-1000,1000)的二维坐标中按比例计算对焦的坐标
        int centerX = (centerScreenTouchX * 1000) / centerScreenX;
        int centerY = (centerScreenTouchY * 1000) / centerScreenY;

        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int right = clamp(centerX + areaSize / 2, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        int bottom = clamp(centerY + areaSize / 2, -1000, 1000);
        return new Rect(left, top, right, bottom);
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    public interface MagicListener {
        void foucus(int focusX, int focusY);

        void flingLeft();

        void flingRight();
    }
}

package com.imay.capturefilter.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import com.imay.capturefilter.R;

/**
 * 预览图|拍照后编辑的ImageView
 */
public class ClipImageView extends ImageView implements
        ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

    private final Paint mPaint;

    private final int mMaskColor;
    private int mAspectX;
    private int mAspectY;
    private String mTipText;
    private final int mClipPadding;

    private float mScaleMax = 4.0f;
    private float mScaleMin = 2.0f;

    /**
     * 初始化时的缩放比例
     */
    private float mInitScale = 1.0f;

    /**
     * 用于存放矩阵
     */
    private final float[] mMatrixValues = new float[9];

    /**
     * 缩放的手势检查
     */
    private ScaleGestureDetector mScaleGestureDetector = null;
    private final Matrix mScaleMatrix = new Matrix();

    /**
     * 用于双击
     */
    private GestureDetector mGestureDetector;
    private boolean isAutoScale;

    private float mLastX;
    private float mLastY;

    private boolean isCanDrag;
    private int lastPointerCount;

    private Rect mClipBorder = new Rect();
    private int mMaxOutputWidth = 0;

    private boolean mDrawCircleFlag;
    private float mRoundCorner;


    // 图片被旋转的角度
    private int mDegree;
    // 大图被设置之前的缩放比例
    private int mSampleSize;
    private int mSourceWidth;
    private int mSourceHeight;

    private String mInput;  //文件路径
    private int mMaxWidth;

    private boolean isFirstSetImage = true;

    public ClipImageView(Context context) {
        this(context, null);
    }

    public ClipImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setScaleType(ScaleType.MATRIX);
        mGestureDetector = new GestureDetector(context,
                new SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (isAutoScale)
                            return true;

                        float x = e.getX();
                        float y = e.getY();
                        if (getScale() < mScaleMin) {
                            ClipImageView.this.postDelayed(new AutoScaleRunnable(mScaleMin, x, y), 16);
                        } else {
                            ClipImageView.this.postDelayed(new AutoScaleRunnable(mInitScale, x, y), 16);
                        }
                        isAutoScale = true;

                        return true;
                    }
                });
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        this.setOnTouchListener(this);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.WHITE);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ClipImageView);
        mAspectX = ta.getInteger(R.styleable.ClipImageView_civWidth, 1);
        mAspectY = ta.getInteger(R.styleable.ClipImageView_civHeight, 1);
        mClipPadding = ta.getDimensionPixelSize(R.styleable.ClipImageView_civClipPadding, 0);
        mTipText = ta.getString(R.styleable.ClipImageView_civTipText);
        mMaskColor = ta.getColor(R.styleable.ClipImageView_civMaskColor, 0xB2000000);
        mDrawCircleFlag = ta.getBoolean(R.styleable.ClipImageView_civClipCircle, false);
        mRoundCorner = ta.getDimension(R.styleable.ClipImageView_civClipRoundCorner, 0);
        final int textSize = ta.getDimensionPixelSize(R.styleable.ClipImageView_civTipTextSize, 24);
        mPaint.setTextSize(textSize);
        ta.recycle();

        mPaint.setDither(true);
    }

    /**
     * 自动缩放的任务
     */
    private class AutoScaleRunnable implements Runnable {
        static final float BIGGER = 1.07f;
        static final float SMALLER = 0.93f;
        private float mTargetScale;
        private float tmpScale;

        /**
         * 缩放的中心
         */
        private float x;
        private float y;

        /**
         * 传入目标缩放值，根据目标值与当前值，判断应该放大还是缩小
         *
         * @param targetScale
         */
        public AutoScaleRunnable(float targetScale, float x, float y) {
            this.mTargetScale = targetScale;
            this.x = x;
            this.y = y;
            if (getScale() < mTargetScale) {
                tmpScale = BIGGER;
            } else {
                tmpScale = SMALLER;
            }

        }

        @Override
        public void run() {
            // 进行缩放
            mScaleMatrix.postScale(tmpScale, tmpScale, x, y);
            checkBorder();
            setImageMatrix(mScaleMatrix);

            final float currentScale = getScale();
            // 如果值在合法范围内，继续缩放
            if (((tmpScale > 1f) && (currentScale < mTargetScale))
                    || ((tmpScale < 1f) && (mTargetScale < currentScale))) {
                ClipImageView.this.postDelayed(this, 16);
            } else {
                // 设置为目标的缩放比例
                final float deltaScale = mTargetScale / currentScale;
                mScaleMatrix.postScale(deltaScale, deltaScale, x, y);
                checkBorder();
                setImageMatrix(mScaleMatrix);
                isAutoScale = false;
            }

        }
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();

        if (getDrawable() == null)
            return true;

        /**
         * 缩放的范围控制
         */
        if ((scale < mScaleMax && scaleFactor > 1.0f)
                || (scale > mInitScale && scaleFactor < 1.0f)) {
            /**
             * 缩放阙值最小值判断
             */
            if (scaleFactor * scale < mInitScale) {
                scaleFactor = mInitScale / scale;
            }
            if (scaleFactor * scale > mScaleMax) {
                scaleFactor = mScaleMax / scale;
            }
            /**
             * 设置缩放比例
             */
            mScaleMatrix.postScale(scaleFactor, scaleFactor,
                    detector.getFocusX(), detector.getFocusY());
            checkBorder();
            setImageMatrix(mScaleMatrix);
        }
        return true;
    }

    /**
     * 根据当前图片的Matrix获得图片的范围
     *
     * @return
     */
    private RectF getMatrixRectF() {
        Matrix matrix = mScaleMatrix;
        RectF rect = new RectF();
        Drawable d = getDrawable();
        if (null != d) {
            rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(rect);
        }
        return rect;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event))
            return true;
        mScaleGestureDetector.onTouchEvent(event);

        float x = 0, y = 0;
        // 拿到触摸点的个数
        final int pointerCount = event.getPointerCount();

        // 得到多个触摸点的x与y均值
        for (int i = 0; i < pointerCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }
        x /= pointerCount;
        y /= pointerCount;

        /**
         * 每当触摸点发生变化时，重置mLasX , mLastY
         */
        if (pointerCount != lastPointerCount) {
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }

        lastPointerCount = pointerCount;
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - mLastX;
                float dy = y - mLastY;

                if (!isCanDrag) {
                    isCanDrag = isCanDrag(dx, dy);
                }
                if (isCanDrag) {
                    if (getDrawable() != null) {

                        RectF rectF = getMatrixRectF();
                        // 如果宽度小于屏幕宽度，则禁止左右移动
                        if (rectF.width() <= mClipBorder.width()) {
                            dx = 0;
                        }

                        // 如果高度小雨屏幕高度，则禁止上下移动
                        if (rectF.height() <= mClipBorder.height()) {
                            dy = 0;
                        }
                        mScaleMatrix.postTranslate(dx, dy);
                        checkBorder();
                        setImageMatrix(mScaleMatrix);
                    }
                }
                mLastX = x;
                mLastY = y;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                lastPointerCount = 0;
                break;
        }

        return true;
    }

    /**
     * 获得当前的缩放比例
     *
     * @return
     */
    public final float getScale() {
        mScaleMatrix.getValues(mMatrixValues);
        return mMatrixValues[Matrix.MSCALE_X];
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateBorder();
    }

    private void updateBorder() {
        final int width = getWidth();
        final int height = getHeight();
        mClipBorder.left = mClipPadding;
        mClipBorder.right = width - mClipPadding;
        final int borderHeight = mClipBorder.width() * mAspectY / mAspectX;
        if (mDrawCircleFlag == true) { // 如果是圆形,宽高比例是1:1
            final int borderTempHeight = mClipBorder.width() * 1 / 1;
            mClipBorder.top = (height - borderTempHeight) / 2;
            mClipBorder.bottom = mClipBorder.top + borderTempHeight;
        } else { // 如果不是圆形,根据宽高比例
            mClipBorder.top = 0;
            mClipBorder.bottom = mClipBorder.top + getHeight();
        }
    }

    public void setAspect(int aspectX, int aspectY) {
        mAspectX = aspectX;
        mAspectY = aspectY;
    }

    public void setTip(String tip) {
        mTipText = tip;
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (isFirstSetImage)
            postResetImageMatrix();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        if (isFirstSetImage)
            postResetImageMatrix();
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        if (isFirstSetImage)
            postResetImageMatrix();
    }


    /**
     * 这里没有使用post方式,因为图片会有明显的从初始位置移动到需要缩放的位置
     */
    private void postResetImageMatrix() {
        if (getWidth() != 0) {
            resetImageMatrix();
        } else {
            post(new Runnable() {
                @Override
                public void run() {
                    resetImageMatrix();
                }
            });
        }
    }

    /**
     * 垂直方向与View的边矩
     */
    public void resetImageMatrix() {
        final Drawable d = getDrawable();
        if (d == null) {
            return;
        }

        final int dWidth = d.getIntrinsicWidth();
        final int dHeight = d.getIntrinsicHeight();

        final int cWidth = mClipBorder.width();
        final int cHeight = mClipBorder.height();

        final int vWidth = getWidth();
        final int vHeight = getHeight();

        final float scale;
        final float dx;
        final float dy;

        if (dWidth * cHeight > cWidth * dHeight) {
            scale = cHeight / (float) dHeight;
        } else {
            scale = cWidth / (float) dWidth;
        }

        dx = (vWidth - dWidth * scale) * 0.5f;
        dy = (vHeight - dHeight * scale) * 0.5f;

        mScaleMatrix.setScale(scale, scale);
        mScaleMatrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));

        setImageMatrix(mScaleMatrix);

        mInitScale = scale;
        mScaleMin = mInitScale * 2;
        mScaleMax = mInitScale * 4;
        isFirstSetImage = false;
    }

    /**
     * 剪切图片
     *
     * @return 返回剪切后的bitmap对象
     */
    public Bitmap clip() {
        final Drawable drawable = getDrawable();
        final Bitmap originalBitmap = ((BitmapDrawable) drawable).getBitmap();

        final float[] matrixValues = new float[9];
        mScaleMatrix.getValues(matrixValues);
        final float scale = matrixValues[Matrix.MSCALE_X] * drawable.getIntrinsicWidth() / originalBitmap.getWidth();
        final float transX = matrixValues[Matrix.MTRANS_X];
        final float transY = matrixValues[Matrix.MTRANS_Y];

        float cropX = (-transX + 0) / scale;
        float cropY = (-transY + 0) / scale;
        final float cropWidth = getWidth() / scale;
        final float cropHeight = getHeight() / scale;

        Matrix outputMatrix = null;
        if (mMaxOutputWidth > 0 && cropWidth > mMaxOutputWidth) {
            final float outputScale = mMaxOutputWidth / cropWidth;
            outputMatrix = new Matrix();
            outputMatrix.setScale(outputScale, outputScale);
        }

        if (cropY < 0) cropY = 0;
        if (cropX < 0) cropX = 0;
        return Bitmap.createBitmap(originalBitmap,
                (int) cropX, (int) cropY, (int) cropWidth, (int) cropHeight,
                outputMatrix, false);
    }

    /**
     * 边界检查
     */
    private void checkBorder() {
        RectF rect = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        // 如果宽或高大于屏幕，则控制范围
        if (rect.width() >= mClipBorder.width()) {
            if (rect.left > mClipBorder.left) {
                deltaX = -rect.left + mClipBorder.left;
            }

            if (rect.right < mClipBorder.right) {
                deltaX = mClipBorder.right - rect.right;
            }
        }

        if (rect.height() >= mClipBorder.height()) {
            if (rect.top > mClipBorder.top) {
                deltaY = -rect.top + mClipBorder.top;
            }

            if (rect.bottom < mClipBorder.bottom) {
                deltaY = mClipBorder.bottom - rect.bottom;
            }
        }

        mScaleMatrix.postTranslate(deltaX, deltaY);
    }

    /**
     * 是否是拖动行为
     *
     * @param dx
     * @param dy
     * @return
     */
    private boolean isCanDrag(float dx, float dy) {
        return Math.sqrt((dx * dx) + (dy * dy)) >= 0;
    }

    public Rect getClipBorder() {
        return mClipBorder;
    }

    public void setMaxOutputWidth(int maxOutputWidth) {
        mMaxOutputWidth = maxOutputWidth;
    }

    public float[] getClipMatrixValues() {
        final float[] matrixValues = new float[9];
        mScaleMatrix.getValues(matrixValues);
        return matrixValues;
    }


    /**
     * 参考showtipsview的做法
     */
    public void drawRectangleOrCircle(Canvas canvas) {
        Bitmap bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas temp = new Canvas(bitmap);
        Paint transparentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        PorterDuffXfermode porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        transparentPaint.setColor(Color.TRANSPARENT);
        temp.drawRect(0, 0, temp.getWidth(), temp.getHeight(), mPaint);
        transparentPaint.setXfermode(porterDuffXfermode);
        if (mDrawCircleFlag) { // 画圆
            float cx = mClipBorder.left + mClipBorder.width() / 2f;
            float cy = mClipBorder.top + mClipBorder.height() / 2f;
            float radius = mClipBorder.height() / 2f;
            temp.drawCircle(cx, cy, radius, transparentPaint);
        } else { // 画矩形(可以设置矩形的圆角)
            RectF rectF = new RectF(mClipBorder.left, mClipBorder.top, mClipBorder.right, mClipBorder.bottom);
            temp.drawRoundRect(rectF, mRoundCorner, mRoundCorner, transparentPaint);
        }
        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final int width = getWidth();
        final int height = getHeight();

        mPaint.setColor(mMaskColor);
        mPaint.setStyle(Paint.Style.FILL);
//        canvas.drawRect(0, 0, width, mClipBorder.top, mPaint);
//        canvas.drawRect(0, mClipBorder.bottom, width, height, mPaint);
//        canvas.drawRect(0, mClipBorder.top, mClipBorder.left, mClipBorder.bottom, mPaint);
//        canvas.drawRect(mClipBorder.right, mClipBorder.top, width, mClipBorder.bottom, mPaint);

//        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(1);
        drawRectangleOrCircle(canvas);
//        mPaint.setStyle(Paint.Style.STROKE);
//
//        canvas.drawRect(mClipBorder.left, mClipBorder.top, mClipBorder.right, mClipBorder.bottom, mPaint);

        if (mTipText != null) {
            final float textWidth = mPaint.measureText(mTipText);
            final float startX = (width - textWidth) / 2;
            final Paint.FontMetrics fm = mPaint.getFontMetrics();
            final float startY = mClipBorder.bottom + mClipBorder.top / 2 - (fm.descent - fm.ascent) / 2;
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(mTipText, startX, startY, mPaint);
        }
    }


    public Bitmap createClippedBitmap(byte[] data) {
        if (mSampleSize <= 1) {
            return clip();
        }

        // 获取缩放位移后的矩阵值
        final float[] matrixValues = getClipMatrixValues();
        final float scale = matrixValues[Matrix.MSCALE_X];
        final float transX = matrixValues[Matrix.MTRANS_X];
        final float transY = matrixValues[Matrix.MTRANS_Y];

        // 获取在显示的图片中裁剪的位置
        final Rect border = getClipBorder();
        final float cropX = ((-transX + border.left) / scale) * mSampleSize;
        final float cropY = ((-transY + border.top) / scale) * mSampleSize;
        final float cropWidth = (border.width() / scale) * mSampleSize;
        final float cropHeight = (border.height() / scale) * mSampleSize;

        // 获取在旋转之前的裁剪位置
        final RectF srcRect = new RectF(cropX, cropY, cropX + cropWidth, cropY + cropHeight);
        final Rect clipRect = getRealRect(srcRect);

        final BitmapFactory.Options ops = new BitmapFactory.Options();
        final Matrix outputMatrix = new Matrix();

        outputMatrix.setRotate(mDegree);
        // 如果裁剪之后的图片宽高仍然太大,则进行缩小
        if (mMaxWidth > 0 && cropWidth > mMaxWidth) {
            ops.inSampleSize = findBestSample((int) cropWidth, mMaxWidth);

            final float outputScale = mMaxWidth / (cropWidth / ops.inSampleSize);
            outputMatrix.postScale(outputScale, outputScale);
        }

        // 裁剪
        BitmapRegionDecoder decoder = null;
        try {
//            decoder = BitmapRegionDecoder.newInstance(mInput, false);
            decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, false);
            final Bitmap source = decoder.decodeRegion(clipRect, ops);
//            recycleImageViewBitmap();
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), outputMatrix, false);
        } catch (Exception e) {
            return clip();
        } finally {
            if (decoder != null && !decoder.isRecycled()) {
                decoder.recycle();
            }
        }
    }

    private Rect getRealRect(RectF srcRect) {
        switch (mDegree) {
            case 90:
                return new Rect((int) srcRect.top, (int) (mSourceHeight - srcRect.right),
                        (int) srcRect.bottom, (int) (mSourceHeight - srcRect.left));
            case 180:
                return new Rect((int) (mSourceWidth - srcRect.right), (int) (mSourceHeight - srcRect.bottom),
                        (int) (mSourceWidth - srcRect.left), (int) (mSourceHeight - srcRect.top));
            case 270:
                return new Rect((int) (mSourceWidth - srcRect.bottom), (int) srcRect.left,
                        (int) (mSourceWidth - srcRect.top), (int) srcRect.right);
            default:
                return new Rect((int) srcRect.left, (int) srcRect.top, (int) srcRect.right, (int) srcRect.bottom);
        }
    }

    /**
     * 计算最好的采样大小。
     *
     * @param origin 当前宽度
     * @param target 限定宽度
     * @return sampleSize
     */
    public static int findBestSample(int origin, int target) {
        int sample = 1;
        for (int out = origin / 2; out > target; out /= 2) {
            sample *= 2;
        }
        return sample;
    }

    private void recycleImageViewBitmap() {
        post(new Runnable() {
            @Override
            public void run() {
                setImageBitmap(null);
            }
        });
    }

    public String getmInput() {
        return mInput;
    }

    public void setmInput(String mInput) {
        this.mInput = mInput;
    }
}
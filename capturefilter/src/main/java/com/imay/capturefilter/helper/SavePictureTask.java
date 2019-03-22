package com.imay.capturefilter.helper;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;

import com.imay.capturefilter.camera.CameraEngine;
import com.imay.capturefilter.utils.MagicParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SavePictureTask extends AsyncTask<Bitmap, Integer, String> {

    private OnPictureSaveListener onPictureSaveListener;
    private File file;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    public void setSurface(int surfaceWidth, int surfaceHeight) {
        mSurfaceWidth = surfaceWidth;
        mSurfaceHeight = surfaceHeight;
    }

    public interface OnPictureSaveListener {
        void onSaved(String result);
    }

    public SavePictureTask(File file, OnPictureSaveListener listener) {
        this.onPictureSaveListener = listener;
        this.file = file;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(final String result) {
        if (result != null)
            if (onPictureSaveListener != null)
                onPictureSaveListener.onSaved(result);
    }

    @Override
    protected String doInBackground(Bitmap... params) {
        if (file == null)
            return null;
        return saveBitmap(params[0]);
    }

    private String saveBitmap(Bitmap bitmap) {
        if (file.exists()) {
            file.delete();
        }
        /*if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            return fileOutput(bitmap);
        } else {
            return finallySaveBitmap(bitmap);
        }*/
        return finallySaveBitmap(bitmap);
    }

    private String finallySaveBitmap(Bitmap b1) {
        //图片宽高，使用原始比例，在编辑图片界面裁剪图片，再压缩保存
        String result;
        Bitmap bm;
        if (!CameraEngine.getInstance().getCameraBackStatus()) {
            if (b1.getWidth() > b1.getHeight()) {//适配三星手机
                Matrix matrix = new Matrix();
                //如果是前置摄像头身份证认真，则照片需要水平翻转
                if (MagicParams.IS_ID_CARD_CAMERA) {
                    matrix.postRotate(270);
                    matrix.postScale(-1, 1);
                } else {
                    matrix.postRotate(270);
                    matrix.postScale(-1, 1);
                }
                bm = Bitmap.createBitmap(b1, 0, 0, b1.getWidth(), b1.getHeight(), matrix, true);
            } else { //水平翻转
                Matrix matrix = new Matrix();
                //如果是前置摄像头身份证认真，则照片需要水平翻转
                if (MagicParams.IS_ID_CARD_CAMERA) {
                    // beizhu
                    matrix.postScale(-1, 1);
                    matrix.postRotate(180);
                    bm = Bitmap.createBitmap(b1, 0, 0, b1.getWidth(), b1.getHeight(), matrix, true);
                } else {
                    matrix.postScale(1, -1);// 缩放 当sx为-1时向左翻转,当sy为-1时向上翻转,sx、sy都为-1时相当于旋转180°
                    matrix.postTranslate(0, b1.getHeight());// 因为向上翻转了所以y要向下平移一个bitmap的高度
                    bm = Bitmap.createBitmap(b1, 0, 0, b1.getWidth(), b1.getHeight(), matrix, true);
                }
            }
        } else {
            if (b1.getWidth() > b1.getHeight()) { //适配三星手机
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                bm = Bitmap.createBitmap(b1, 0, 0, b1.getWidth(), b1.getHeight(), matrix, true);
            } else {
                bm = Bitmap.createBitmap(b1, 0, 0, b1.getWidth(), b1.getHeight());
            }
        }

        int height = bm.getHeight();
        int width = bm.getWidth();

        // 根据surfaceView 的长宽进行截图处理
        float radio1 = ((float) mSurfaceHeight) / height;
        float radio2 = ((float) mSurfaceWidth) / width;

        float radio = Math.max(radio2, radio1);
        int imageHeight = (int) (mSurfaceHeight / radio);
        int imageWidth = (int) (mSurfaceWidth / radio);
        Bitmap result1 = bm;
        if (radio1 == radio) {
            int c = width - imageWidth;
            c /= 2;
            result1 = Bitmap.createBitmap(bm, c, 0, imageWidth, imageHeight);
        } else {
            int c = height - imageHeight;
            c /= 2;
            result1 = Bitmap.createBitmap(bm, 0, c, imageWidth, imageHeight);
        }
        if (bm.isRecycled()) {
            bm.recycle();
        }


        result = fileOutput(result1);
        recycledBitmap(b1);
        recycledBitmap(bm);
        return result;    //null，保存失败了。
    }

    private String fileOutput(Bitmap bm) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
            return file.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void recycledBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}

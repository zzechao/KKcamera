package com.imay.capturefilter.utils;

import android.app.Activity;
import android.content.Context;
import android.media.ExifInterface;
import android.util.DisplayMetrics;

import java.io.IOException;

public class ICDensityUtils {
	
	private static int screenW = 0;
	private static int screenH = 0;

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 sp
     */
    public static int px2sp(Context context, float pxValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 sp 的单位 转成为 px
     */
    public static int sp2px(Context context, float spValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }


    /**
     * 获取屏幕宽度
     */
    public static int getScreenW(Activity aty) {
    	if (screenW == 0){
            DisplayMetrics dm = aty.getResources().getDisplayMetrics();
            int w = dm.widthPixels;
            screenW = w;
            // int w = aty.getWindowManager().getDefaultDisplay().getWidth();
    	}
    	return screenW;
    }

    /**
     * 获取屏幕高度
     */
    public static int getScreenH(Activity aty) {
    	if (screenH == 0){
            DisplayMetrics dm = aty.getResources().getDisplayMetrics();
            int h = dm.heightPixels;
            screenH = h;
            // int w = aty.getWindowManager().getDefaultDisplay().getWidth();
    	}
        return screenH;
    }
    /**
  	 * 获得状态栏的高度
  	 */
  	public static int getStatusHeight(Context context) {

  		int statusHeight = -1;
  		try {
  			Class<?> clazz = Class.forName("com.android.internal.R$dimen");
  			Object object = clazz.newInstance();
  			int height = Integer.parseInt(clazz.getField("status_bar_height")
  					.get(object).toString());
  			statusHeight = context.getResources().getDimensionPixelSize(height);
  		} catch (Exception e) {
  			e.printStackTrace();
  		}
  		return statusHeight;
  	}

    /**
     * 读取图片属性：旋转的角度
     *
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

}
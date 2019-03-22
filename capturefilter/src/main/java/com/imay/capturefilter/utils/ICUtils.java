package com.imay.capturefilter.utils;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.imay.capturefilter.widget.ICLoadingView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 统一帮助类
 * Created by Murphy on 2016/12/13.
 */
public class ICUtils {

    public static File getOutputMediaFile(Context context) {
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    || !Environment.isExternalStorageRemovable()) {
                File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), ICCons.folderName);
                if (!mediaStorageDir.exists()) {
                    if (!mediaStorageDir.mkdirs()) {
                        return getFileDirImg(context);
                    }
                }
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
                File file = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
                try {
                    file.createNewFile();
                    return file;
                } catch (Exception e) {
                    file.delete();
                    return getFileDirImg(context);
                }
            } else {
                return getFileDirImg(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * SD的file报错之后走的应用内存方法
     * java.io.filenotfoundexception open failed eacces (permission denied)
     *
     * @param context
     * @return
     * @throws IOException
     */
    public static File getFileDirImg(Context context) throws IOException {
        String filesDir = context.getFilesDir().getAbsolutePath();
        File fileDir = new File(filesDir, ICCons.folderAppName);
        if (!fileDir.exists()) {
            if (!fileDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
        File file1 = new File(fileDir.getPath() + File.separator + "IMG_IMAY_" + timeStamp + ".jpg");
        if (file1.exists()) {
            file1.delete();
            file1.createNewFile();
            return file1;
        } else {
            return file1;
        }
    }

    /**
     * 删除应用的内存数据
     *
     * @param context
     */
    public static void deleteFile(Context context) {
        String filesDir = context.getFilesDir().getAbsolutePath();
        File fileDir = new File(filesDir, ICCons.folderAppName);
        if (fileDir.exists()) {
            File[] files = fileDir.listFiles();
            for (File file : files) {
                file.delete();
            }
        }
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), ICCons.folderName);
        if (mediaStorageDir.exists()) {
            File[] files = mediaStorageDir.listFiles();
            for (File file : files) {
                file.delete();
            }
        }
    }

    public static boolean deleteFile(Context context, String path) {
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {

                if (PermissionUtil.selfPermissionGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    try {
                        // 删除数据库
                        ContentResolver resolver = context.getContentResolver();
                        Cursor cursor = MediaStore.Images.Media.query(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=?",
                                new String[]{path}, null);

                        if (cursor.moveToFirst()) {
                            long id = cursor.getLong(0);
                            Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            Uri uri = ContentUris.withAppendedId(contentUri, id);
                            resolver.delete(uri, null, null);
                        }
                        cursor.close();
                    } catch (Exception e) {
                        Log.e(context.getPackageName(), Manifest.permission.READ_EXTERNAL_STORAGE + " Permiss Denial");
                    }
                }
                // 删除图片
                file.delete();
                return true;
            }
        }
        return false;
    }

    /**
     * 通过uri获取真实的图片路径
     *
     * @param activity
     * @param uri
     * @return
     */
    public static String getPathByUri(Activity activity, Uri uri) {
        Cursor cursor = activity
                .managedQuery(uri,
                        new String[]{MediaStore.Images.Media.DATA}, null,
                        null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.Media.DATA));
            }
        } else {
            String path = Uri.decode(uri.toString().substring(
                    "file://".length(), uri.toString().length()));
            if (path.endsWith("jpg") || path.endsWith("png")
                    || path.endsWith("jpeg")) {
                return path;
            } else {
                return "typeerror";
            }
        }
        return "";
    }

    /**
     * 将图片转成数组
     *
     * @param bmp
     * @param needRecycle
     * @return
     */
    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
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

    /**
     * 释放图片资源
     *
     * @param bitmap
     */
    public static void recycledBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    /**
     * 压缩指定byte[]图片，并得到压缩后的图像
     *
     * @param bts
     * @param reqsW
     * @param reqsH
     * @return
     */
    public final static Bitmap compressBitmap(byte[] bts, int reqsW, int reqsH) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bts, 0, bts.length, options);
        options.inSampleSize = caculateInSampleSize(options, reqsW, reqsH);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(bts, 0, bts.length, options);
    }

    public final static int caculateInSampleSize(BitmapFactory.Options options, int rqsW, int rqsH) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (rqsW == 0 || rqsH == 0) return 1;
        if (height > rqsH || width > rqsW) {
            final int heightRatio = Math.round((float) height / (float) rqsH);
            final int widthRatio = Math.round((float) width / (float) rqsW);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    public static Dialog createDialog(Context ctx, String msg) {
        ICLoadingView dialog = new ICLoadingView(ctx);
        dialog.setTitle(msg);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    public static boolean hasKitkat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static void scanPhotos(String filePath, Context context) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(new File(filePath));
        intent.setData(uri);
        context.sendBroadcast(intent);
    }

}

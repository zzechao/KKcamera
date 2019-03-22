package com.imay.capturefilter.utils;

import android.os.Environment;

import com.imay.capturefilter.widget.base.MagicBaseView;

public class MagicParams {
    public static MagicBaseView magicBaseView;

    public static String videoPath = Environment.getExternalStorageDirectory().getPath();
    public static String videoName = "MagicCamera_test.mp4";

    public static int beautyLevel = 5;

    public static boolean IS_ID_CARD_CAMERA = false;
    public static int OPEN_CAMERA_ID = 0;
}

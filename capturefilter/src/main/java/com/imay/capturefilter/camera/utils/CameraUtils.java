package com.imay.capturefilter.camera.utils;

import android.hardware.Camera;

import com.imay.capturefilter.utils.ICCons;

import java.util.ArrayList;
import java.util.List;

public class CameraUtils {

    public static Camera.Size getLargePictureSize(Camera camera) {
        if (camera != null) {
            List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
            /*Camera.Size temp = sizes.get(0);
            for(int i = 1;i < sizes.size();i ++){
                float scale = (float)(sizes.get(i).height) / sizes.get(i).width;
                if(temp.width < sizes.get(i).width && scale < 0.6f && scale > 0.5f)
                    temp = sizes.get(i);
            }
            return temp;*/
            for (int i = 0; i < sizes.size(); i++) {
                Camera.Size temp = sizes.get(i);
                int tempHeight = temp.height;
                if (temp.width > temp.height) {
//                    temp.width = temp.height;
//                    temp.height = tempHeight;
                    tempHeight = temp.width;
                }
                if (ICCons.screenH >= tempHeight) {    // && ICCons.screenW >= temp.width
                    return temp;
                }
            }
            //Log.d("XXOO", "对不起,我已经绕过你,设置了最大像素图片");
            return sizes.get(0);
        }
        return null;
    }

    public static Camera.Size getLargePreviewSize(Camera camera) {
        if (camera != null) {
            List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
            Camera.Size temp = sizes.get(0);
            for (int i = 1; i < sizes.size(); i++) {
                if (temp.width < sizes.get(i).width)
                    temp = sizes.get(i);
            }
            return temp;
        }
        return null;
    }

    /**
     * 获得最接近频幕宽度的尺寸
     * @param sizeList
     * @param n        放大几倍 （>0)
     * @return
     */
    public static Camera.Size getCurrentScreenSize(List<Camera.Size> sizeList, int n,int width,int height) {
        if (sizeList != null && sizeList.size() > 0) {
            int screenWidth = width;
            int[] arry = new int[sizeList.size()];
            int temp = 0;
            for (Camera.Size size : sizeList) {
                arry[temp++] = Math.abs(size.width - screenWidth);
            }
            temp = 0;
            int index = 0;
            for (int i = 0; i < arry.length; i++) {
                if (i == 0) {
                    temp = arry[i];
                    index = 0;
                } else {
                    if (arry[i] < temp) {
                        index = i;
                        temp = arry[i];
                    }
                }
            }
            return sizeList.get(index);
        }
        return null;
    }


    public static Camera.Size currentScreenSize;
    public static Camera.Size currentScreenPicSize;

    public static void release(){
        currentScreenSize = null;
        currentScreenPicSize = null;
    }

    /**
     * 獲取前後攝像頭最接近屏幕相同的尺寸
     * @param n        放大几倍 （>0)
     * @return
     */
    public static Camera.Size getCurrentScreenSize(int n,int width,int height) {
        /**
         * 初始化最小的Size
         */
        List<Camera.Size> sizeList = new ArrayList<>();
        List<Camera.Size> sizePicList = new ArrayList<>();
        try{
            Camera camera = Camera.open(0);
            if(camera != null){
                Camera.Parameters parameters = camera.getParameters();
                sizeList = parameters.getSupportedPreviewSizes();
                sizePicList = parameters.getSupportedPictureSizes();
                camera.release();
            }
        }catch (Exception e){

        }

        List<Camera.Size> sizeList2 = new ArrayList<>();
        List<Camera.Size> sizePicList2 = new ArrayList<>();
        try{
            Camera camera2 = Camera.open(1);
            if(camera2 != null){
                Camera.Parameters parameters2 = camera2.getParameters();
                sizeList2 = parameters2.getSupportedPreviewSizes();
                sizePicList2 = parameters2.getSupportedPictureSizes();
                camera2.release();
            }
        }catch (Exception e){

        }

        // PreviewSizes
        List<Camera.Size> sizeTarget = new ArrayList<>();
        for(int i = 0;i<sizeList.size();i++){
            final Camera.Size sizeI = sizeList.get(i);
            final int widthI = sizeI.width;
            final int heightI = sizeI.height;
            for(int j = 0;j<sizeList2.size();j++){
                final Camera.Size sizeJ = sizeList2.get(j);
                final int widthJ = sizeJ.width;
                final int heightJ = sizeJ.height;
                if(widthI == widthJ && heightI == heightJ){
                    sizeTarget.add(sizeI);
                    break;
                }else{
                    continue;
                }
            }
        }
        currentScreenSize = getCurrentScreenSize(sizeTarget,n,width,height);

        // PIC
        List<Camera.Size> sizePicTarget = new ArrayList<>();
        for(int i = 0;i<sizePicList.size();i++){
            final Camera.Size sizeI = sizePicList.get(i);
            final int widthI = sizeI.width;
            final int heightI = sizeI.height;
            for(int j = 0;j<sizePicList2.size();j++){
                final Camera.Size sizeJ = sizePicList2.get(j);
                final int widthJ = sizeJ.width;
                final int heightJ = sizeJ.height;
                if(widthI == widthJ && heightI == heightJ){
                    sizePicTarget.add(sizeI);
                    break;
                }else{
                    continue;
                }
            }
        }
        currentScreenPicSize = getCurrentScreenSize(sizePicTarget,n,currentScreenSize.width,height);

        return currentScreenSize;
    }

}

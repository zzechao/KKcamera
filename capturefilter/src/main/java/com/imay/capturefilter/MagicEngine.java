package com.imay.capturefilter;

import com.imay.capturefilter.camera.CameraEngine;
import com.imay.capturefilter.filter.helper.MagicFilterType;
import com.imay.capturefilter.helper.SavePictureTask;
import com.imay.capturefilter.utils.MagicParams;
import com.imay.capturefilter.widget.MagicCameraView;
import com.imay.capturefilter.widget.base.MagicBaseView;

import java.io.File;

public class MagicEngine {

    private static MagicEngine mMagicEngine;

    public static MagicEngine getInstance(){
        if(mMagicEngine == null)
            throw new NullPointerException("MagicEngine must be built first");
        else
            return mMagicEngine;
    }

    private MagicEngine(Builder builder){

    }

    public void setFilter(MagicFilterType type){
        MagicParams.magicBaseView.setFilter(type);
    }

    public void savePicture(File file, SavePictureTask.OnPictureSaveListener listener){
        SavePictureTask savePictureTask = new SavePictureTask(file, listener);
        MagicParams.magicBaseView.savePicture(savePictureTask);
    }

    public void startRecord(){
        if(MagicParams.magicBaseView instanceof MagicCameraView)
            ((MagicCameraView)MagicParams.magicBaseView).changeRecordingState(true);
    }

    public void stopRecord(){
        if(MagicParams.magicBaseView instanceof MagicCameraView)
            ((MagicCameraView)MagicParams.magicBaseView).changeRecordingState(false);
    }

    public void setBeautyLevel(int level){
        if(MagicParams.magicBaseView instanceof MagicCameraView && MagicParams.beautyLevel != level) {
            MagicParams.beautyLevel = level;
            ((MagicCameraView) MagicParams.magicBaseView).onBeautyLevelChanged();
        }
    }

    public void setFlashMode(int flashMode){
        CameraEngine.getInstance().setFlashMode(flashMode);
    }

    public static class Builder{

        public MagicEngine build(MagicBaseView magicBaseView) {
            MagicParams.magicBaseView = magicBaseView;
            return new MagicEngine(this);
        }

        public Builder setVideoPath(String path){
            MagicParams.videoPath = path;
            return this;
        }

        public Builder setVideoName(String name){
            MagicParams.videoName = name;
            return this;
        }

    }
}

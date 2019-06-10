package com.chan.mediacamera.clip;

public interface OnClipperListener {

    void onPrepare();

    void onStart();

    void onProcess();

    void onStop();

    void onComplete();
}

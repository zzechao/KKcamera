package com.dmcbig.mediapicker.data;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.dmcbig.mediapicker.R;
import com.dmcbig.mediapicker.entity.Folder;
import com.dmcbig.mediapicker.entity.Media;

import java.util.ArrayList;

/**
 * Created by dmcBig on 2017/7/3.
 */

public class ImageLoader extends LoaderM implements LoaderManager.LoaderCallbacks<Cursor> {

    String[] IMAGE_PROJECTION = {
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media._ID};

    Context mContext;
    DataCallback mLoader;

    public ImageLoader(Context context, DataCallback loader) {
        this.mContext = context;
        this.mLoader = loader;
    }


    @Override
    public void onLoadFinished(Loader loader, Cursor o) {
        try {
            ArrayList<Folder> folders = new ArrayList<>();
            Folder allFolder = new Folder(mContext.getResources().getString(R.string.all_image));
            folders.add(allFolder);
            Cursor cursor = (Cursor) o;
            while (cursor.moveToNext()) {

                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                long dateTime = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED));
                int mediaType = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE));
                long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));

                if (size < 1) continue;
                if(path == null || path.equals("")) continue;
                String dirName = getParent(path);
                Media media = new Media(path, name, dateTime, mediaType, size, id, dirName);
                allFolder.addMedias(media);

                int index = hasDir(folders, dirName);
                if (index != -1) {
                    folders.get(index).addMedias(media);
                } else {
                    Folder folder = new Folder(dirName);
                    folder.addMedias(media);
                    folders.add(folder);
                }
            }
            mLoader.onData(folders);
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        return new CursorLoader(
                mContext,
                queryUri,
                IMAGE_PROJECTION,
                null,
                null, // Selection args (none).
                MediaStore.Images.Media.DATE_ADDED + " DESC" // Sort order.
        );
    }
}
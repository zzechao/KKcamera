package com.imay.capturefilter.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.imay.capturefilter.R;
import com.imay.capturefilter.utils.ICCons;
import com.imay.capturefilter.utils.ICDensityUtils;

import java.util.ArrayList;

/**
 * 编辑照片滤镜Adapter
 * Created by shoucuixiang on 2016/11/7.
 */
public class FilterImageAdapter extends RecyclerView.Adapter<FilterImageAdapter.FilterViewHolder> {

    private Context mContext;
    private ArrayList<String> mNames;
    private ArrayList<Bitmap> mBitmapList = new ArrayList<>();
    private int mSelected = 0, mLastPostion = 0;

    private AutoAdjustItemClickListener mListener = null;

    public interface AutoAdjustItemClickListener {
        void onItemClick(View view, int postion);
    }

    public FilterImageAdapter(Context context) {
        mNames = new ArrayList<>();
        mContext = context;
    }

    public void setNames(ArrayList<String> names) {
        this.mNames = names;
        this.mLastPostion = names.size() - 1;
    }

    @Override
    public FilterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.capturefilter_item_filter_image, parent, false);
        return new FilterViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(FilterViewHolder holder, int position) {
        holder.tvName.setText(mNames.get(position));
        if (position == mSelected) {
            holder.tvName.setSelected(true);
        } else {
            holder.tvName.setSelected(false);
        }
        if (mBitmapList != null && mBitmapList.size() > 0 && mBitmapList.size() > position) {
            holder.ivPhoto.setImageBitmap(mBitmapList.get(position));
        }
//        slideItem(holder, position);
    }

    private void slideItem(FilterViewHolder holder, int position) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.tvName.getLayoutParams();
        if (position == 0) {
            params.setMargins((ICCons.screenW - ICDensityUtils.dip2px(mContext, 37)) / 2, 0, 0, 0);
        } else if (position == mLastPostion) {
            params.setMargins(0, 0, (ICCons.screenW - ICDensityUtils.dip2px(mContext, 37)) / 2, 0);
        } else {
            params.setMargins(0, 0, 0, 0);
        }
        holder.llItemRoot.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return mNames.size();
    }

    public void setSelectedPostion(int selectedPostion) {
        this.mSelected = selectedPostion;
    }

    public void setBitmapList(ArrayList<Bitmap> bitmapList) {
        this.mBitmapList.clear();
        this.mBitmapList.addAll(bitmapList);
    }

    public ArrayList<Bitmap> getBitmapList(){
        return mBitmapList;
    }

    public void setItemOnClickListener(AutoAdjustItemClickListener mListener) {
        this.mListener = mListener;
    }

    class FilterViewHolder extends RecyclerView.ViewHolder {

        public TextView tvName;
        public ImageView ivPhoto;
        public LinearLayout llItemRoot;

        public FilterViewHolder(View view) {
            super(view);
            tvName = (TextView) view.findViewById(R.id.tv_name);
            ivPhoto = (ImageView) view.findViewById(R.id.iv_photo);
            llItemRoot = (LinearLayout) view.findViewById(R.id.ll_item_root);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onItemClick(view, getPosition());
                    }
                }
            });
        }
    }

}

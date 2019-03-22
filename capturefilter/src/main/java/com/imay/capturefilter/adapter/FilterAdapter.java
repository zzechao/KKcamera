package com.imay.capturefilter.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.imay.capturefilter.R;
import com.imay.capturefilter.filter.helper.MagicFilterType;
import com.imay.capturefilter.utils.ICCons;
import com.imay.capturefilter.utils.ICDensityUtils;

import java.util.ArrayList;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterHolder> {

    private MagicFilterType[] mFilters;
    private ArrayList<String> mFilterNames;
    private Context mContext;
    private int mSelected = 0, mLastPostion = 0;

    private FilterChangeListener onFilterChangeListener;

    public interface FilterChangeListener {
        void onFilterChanged(MagicFilterType filterType, int position);
    }

    public FilterAdapter(Context context, MagicFilterType[] filters, ArrayList<String> filterNames) {
        this.mFilters = filters;
        this.mContext = context;
        this.mFilterNames = filterNames;
        this.mLastPostion = getItemCount() - 1;
    }

    @Override
    public FilterHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.capturefilter_item_filter, parent, false);
        return new FilterHolder(view);
    }

    @Override
    public void onBindViewHolder(FilterHolder holder, final int position) {
        holder.tvName.setText(String.valueOf(mFilterNames.get(position)));
        if (position == mSelected) {
            holder.tvName.setSelected(true);
        } else {
            holder.tvName.setSelected(false);
        }
        holder.tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelected == position)
                    return;
                int lastSelected = mSelected;
                mSelected = position;
                notifyItemChanged(lastSelected);
                notifyItemChanged(position);
                onFilterChangeListener.onFilterChanged(mFilters[position], position);
            }
        });
        slideTextView(holder, position);
    }

    private void slideTextView(FilterHolder holder, int position) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.tvName.getLayoutParams();
        if (position == 0) {
            int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            holder.tvName.measure(spec,spec);
            int measuredWidth = holder.tvName.getMeasuredWidth();
            params.setMargins((ICCons.screenW / 2) - measuredWidth / 2, 0, 0, 0);
        } else if (position == mLastPostion) {
            int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            holder.tvName.measure(spec,spec);
            int measuredWidth = holder.tvName.getMeasuredWidth();
            params.setMargins(0, 0, (ICCons.screenW  - measuredWidth)/ 2, 0);
        } else {
            params.setMargins(0, 0, 0, 0);
        }
        holder.tvName.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return mFilters == null ? 0 : mFilters.length;
    }

    class FilterHolder extends RecyclerView.ViewHolder {
        public TextView tvName;

        public FilterHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.tv_name);
        }
    }

    public void setOnFilterChangeListener(FilterChangeListener onFilterChangeListener) {
        this.onFilterChangeListener = onFilterChangeListener;
    }

    public int getSelected() {
        return mSelected;
    }

    public void setSelected(int selected) {
        this.mSelected = selected;
    }
}

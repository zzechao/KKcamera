package com.imay.capturefilter.utils;

import android.content.Context;

import com.imay.capturefilter.R;
import com.imay.capturefilter.filter.base.gpuimage.GPUImageFilter;
import com.imay.capturefilter.filter.helper.MagicFilterType;
import com.imay.capturefilter.filter.imay.IF1977Filter;
import com.imay.capturefilter.filter.imay.IFAmaroFilter;
import com.imay.capturefilter.filter.imay.IFBeautifyFilter;
import com.imay.capturefilter.filter.imay.IFBrannanFilter;
import com.imay.capturefilter.filter.imay.IFEarlybirdFilter;
import com.imay.capturefilter.filter.imay.IFHefeFilter;
import com.imay.capturefilter.filter.imay.IFHudsonFilter;
import com.imay.capturefilter.filter.imay.IFInkwellFilter;
import com.imay.capturefilter.filter.imay.IFLomoFilter;
import com.imay.capturefilter.filter.imay.IFLordKelvinFilter;
import com.imay.capturefilter.filter.imay.IFNashvilleFilter;
import com.imay.capturefilter.filter.imay.IFRiseFilter;
import com.imay.capturefilter.filter.imay.IFSierraFilter;
import com.imay.capturefilter.filter.imay.IFSutroFilter;
import com.imay.capturefilter.filter.imay.IFToasterFilter;
import com.imay.capturefilter.filter.imay.IFValenciaFilter;
import com.imay.capturefilter.filter.imay.IFWaldenFilter;
import com.imay.capturefilter.filter.imay.IFXprollFilter;

import java.util.ArrayList;

/**
 * // 单列模式，防止创建过多的存储区域，效果公共资源的时间，相对来说，比较难销毁，但这个资源复用性太高，单利有他的好处
 * Created by Murphy on 2016/12/14.
 * Update by 周泽超 on 2016/12/29.
 */
public class MagicFilterTools {

    private static MagicFilterTools instance;

    public static MagicFilterTools getInstance(Context context) {
        if(instance == null){
            instance = new MagicFilterTools(context.getApplicationContext());
        }
        return instance;
    }

    public final static MagicFilterType[] types = new MagicFilterType[]{
//            MagicFilterType.NONE,
            MagicFilterType.ORIGINAL,
            MagicFilterType.I_BEAUTIFY,
            MagicFilterType.I_1977,
            MagicFilterType.I_AMARO,
            MagicFilterType.I_BRANNAN,
            MagicFilterType.I_EARLYBIRD,
            MagicFilterType.I_HEFE,
            MagicFilterType.I_HUDSON,

            MagicFilterType.I_INKWELL,
            MagicFilterType.I_LOMO,
            MagicFilterType.I_LORDKELVIN,
            MagicFilterType.I_NASHVILLE,
            MagicFilterType.I_RISE,

            MagicFilterType.I_SIERRA,
            MagicFilterType.I_SUTRO,
            MagicFilterType.I_TOASTER,
            MagicFilterType.I_VALENCIA,

            MagicFilterType.I_WALDEN,
            MagicFilterType.I_XPROII
    };

    private FilterList filters;

    public MagicFilterTools(Context context) {
        initFilterData(context);
    }

    /**
     * 初始化滤镜数据
     */
    private void initFilterData(Context context) {
        filters = new FilterList();
        filters.addFilter(context.getString(R.string.ic_filter_original), MagicFilterType.ORIGINAL);
        filters.addFilter(context.getString(R.string.ic_filter_beautify), MagicFilterType.I_BEAUTIFY);
        filters.addFilter(context.getString(R.string.ic_filter_1977), MagicFilterType.I_1977);
        filters.addFilter(context.getString(R.string.ic_filter_amaro), MagicFilterType.I_AMARO);
        filters.addFilter(context.getString(R.string.ic_filter_brannan), MagicFilterType.I_BRANNAN);
        filters.addFilter(context.getString(R.string.ic_filter_earlybird), MagicFilterType.I_EARLYBIRD);
        filters.addFilter(context.getString(R.string.ic_filter_hefe), MagicFilterType.I_HEFE);
        filters.addFilter(context.getString(R.string.ic_filter_hudson), MagicFilterType.I_HUDSON);

        filters.addFilter(context.getString(R.string.ic_filter_inkwell), MagicFilterType.I_INKWELL);
        filters.addFilter(context.getString(R.string.ic_filter_lomo), MagicFilterType.I_LOMO);
        filters.addFilter(context.getString(R.string.ic_filter_lordkelvin), MagicFilterType.I_LORDKELVIN);
        filters.addFilter(context.getString(R.string.ic_filter_nashville), MagicFilterType.I_NASHVILLE);
        filters.addFilter(context.getString(R.string.ic_filter_rise), MagicFilterType.I_RISE);

        filters.addFilter(context.getString(R.string.ic_filter_sierra), MagicFilterType.I_SIERRA);
        filters.addFilter(context.getString(R.string.ic_filter_sutro), MagicFilterType.I_SUTRO);
        filters.addFilter(context.getString(R.string.ic_filter_toaster), MagicFilterType.I_TOASTER);
        filters.addFilter(context.getString(R.string.ic_filter_valencia), MagicFilterType.I_VALENCIA);

        filters.addFilter(context.getString(R.string.ic_filter_walden), MagicFilterType.I_WALDEN);
        filters.addFilter(context.getString(R.string.ic_filter_xproii), MagicFilterType.I_XPROII);
    }

    public GPUImageFilter getGPUImageFilter(Context context, int position) {
        return createFilterForType(context, filters.filters.get(position));
    }

    private GPUImageFilter createFilterForType(final Context context, final MagicFilterType type) {
        switch (type) {
            case I_1977:
                return new IF1977Filter(context);
            case I_BEAUTIFY:
                return new IFBeautifyFilter(context);   //美颜滤镜
            case I_AMARO:
                return new IFAmaroFilter(context);
            case I_BRANNAN:
                return new IFBrannanFilter(context);
            case I_EARLYBIRD:
                return new IFEarlybirdFilter(context);
            case I_HEFE:
                return new IFHefeFilter(context);
            case I_HUDSON:
                return new IFHudsonFilter(context);
            case I_INKWELL:
                return new IFInkwellFilter(context);
            case I_LOMO:
                return new IFLomoFilter(context);
            case I_LORDKELVIN:
                return new IFLordKelvinFilter(context);
            case I_NASHVILLE:
                return new IFNashvilleFilter(context);
            case I_RISE:
                return new IFRiseFilter(context);
            case I_SIERRA:
                return new IFSierraFilter(context);
            case I_SUTRO:
                return new IFSutroFilter(context);
            case I_TOASTER:
                return new IFToasterFilter(context);
            case I_VALENCIA:
                return new IFValenciaFilter(context);
            case I_WALDEN:
                return new IFWaldenFilter(context);
            case I_XPROII:
                return new IFXprollFilter(context);
            case ORIGINAL:
                return new GPUImageFilter();
//                return new MagicBeautyFilter();
//                return null;

            default:
                throw new IllegalStateException("No filter of that type!");
        }

    }

    public ArrayList<String> getFilterNames(Context context) {
        if (filters == null){
            initFilterData(context.getApplicationContext());
        }
        return filters.names;
    }

    private class FilterList {
        public ArrayList<String> names = new ArrayList<>();
        public ArrayList<MagicFilterType> filters = new ArrayList<>();

        public void addFilter(final String name, final MagicFilterType filter) {
            names.add(name);
            filters.add(filter);
        }
    }

}

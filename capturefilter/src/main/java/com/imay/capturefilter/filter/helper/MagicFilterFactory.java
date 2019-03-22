package com.imay.capturefilter.filter.helper;

import android.content.Context;

import com.imay.capturefilter.filter.base.gpuimage.GPUImageFilter;
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

public class MagicFilterFactory {

    private static MagicFilterType filterType = MagicFilterType.NONE;   //NONE;

    public static GPUImageFilter initFilters(Context context,MagicFilterType type) {
        filterType = type;
        switch (type) {
            //----------分--------隔----------符----------

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
            case ORIGINAL:  //原图
                return new GPUImageFilter();
//                return new MagicBeautyFilter();
            default:
                return null;
        }
    }

    public MagicFilterType getCurrentFilterType() {
        return filterType;
    }
}

package com.hyh.paging3demo.utils;


import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hyh.paging3demo.R;

/**
 * @author Administrator
 * @description
 * @data 2020/4/13
 */
public class BindingAdapterUtil {

    @BindingAdapter({"app:imageUrl"})
    public static void setImageUrl(ImageView imageView, String url) {
        Glide.with(imageView.getContext())
                .load(url)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.img_default)
                .error(R.drawable.img_default)
                .into(imageView);
    }
}
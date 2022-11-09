package com.bitpunchlab.android.pawsgo.dogsDisplay

import android.net.Uri
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

@BindingAdapter("loadImage")
fun fetchImage(view: ImageView, src: String?) {
    var uri : Uri? = null
    src?.let {
        uri = src.toUri()
    }
    uri?.let {
        Glide
            .with(view)
            .asBitmap()
            .load(uri)
            .into(view)
    }
}
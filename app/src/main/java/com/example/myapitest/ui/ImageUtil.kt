package com.example.meuprimeiroapp.ui

import android.widget.ImageView
import com.example.myapitest.R
import com.example.myapitest.ui.CircleTransform
import com.squareup.picasso.Picasso

fun ImageView.loadImage(url: String) {
    Picasso.get()
        .load(url)
        .placeholder(R.drawable.ic_download)
        .error(R.drawable.ic_error)
        .transform(CircleTransform())
        .into(this)
}
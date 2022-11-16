package com.bitpunchlab.android.pawsgo.messages

import android.util.Log
import android.widget.TextView
import androidx.databinding.BindingAdapter
import java.text.SimpleDateFormat

@BindingAdapter("formatDate")
fun parseDate(view: TextView, dateString: String)  {
    val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy")

    try {
        val date = dateFormat.parse(dateString)
        val formatterOut = SimpleDateFormat("dd MMM yyyy  HH:mm:ss")
        view.text = formatterOut.format(date)
    } catch (e: java.lang.Exception) {
        Log.i("parse date", "parsing error")
        view.text = "Now Available"
    }
}
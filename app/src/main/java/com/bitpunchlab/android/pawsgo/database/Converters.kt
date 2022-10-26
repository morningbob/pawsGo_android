package com.bitpunchlab.android.pawsgo.database

import android.util.Log
import androidx.room.TypeConverter
import java.text.SimpleDateFormat
import java.util.*

class Converters {

    @TypeConverter
    fun dateFromString(dateString: String) : Date {
        var date : Date = Calendar.getInstance().time
        val simpleDateFormat : SimpleDateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy")
        try {
            date = simpleDateFormat.parse(dateString)
        } catch (e: Error){
            Log.i("error parsing date", e.toString())
        }
        return date
    }

    @TypeConverter
    fun dateToString(date: Date) : String {
        return date.toString()
    }
}
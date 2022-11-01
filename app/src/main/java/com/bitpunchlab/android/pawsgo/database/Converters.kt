package com.bitpunchlab.android.pawsgo.database

import android.util.Log
import androidx.room.TypeConverter
import com.bitpunchlab.android.pawsgo.modelsRoom.DogList
import com.bitpunchlab.android.pawsgo.modelsRoom.DogRoom
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    @TypeConverter
    fun fromDogRoomToJSON(dogList: List<DogRoom>) : String {
        return Json.encodeToString(dogList)
    }

    @TypeConverter
    fun fromJSONToDogRoom(dogJSON: String) : List<DogRoom> {
        return Json.decodeFromString<List<DogRoom>>(dogJSON)
    }
}
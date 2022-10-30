package com.bitpunchlab.android.pawsgo.modelsRoom

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.*

@Entity(tableName = "dog_table")
@Parcelize
data class DogRoom (
    @PrimaryKey()
    var dogID : String = "",
    var dogName: String? = null,
    var dogBreed: String? = null,
    var dogGender: Boolean? = null,
    var dogAge: Int? = null,
    var placeLastSeen: String = "",
    var dateLastSeen: String = "",
    var hour: Int? = null,
    var minute: Int? = null,
    var ownerID: String,
    var ownerEmail: String,
    var isLost : Boolean? = null,
    var isFound : Boolean? = null
    ) : Parcelable
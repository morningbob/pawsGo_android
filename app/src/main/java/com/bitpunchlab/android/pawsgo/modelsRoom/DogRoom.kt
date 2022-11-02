package com.bitpunchlab.android.pawsgo.modelsRoom

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import java.io.Serializable
import java.util.*

@Entity(tableName = "dog_table")
@Parcelize
@kotlinx.serialization.Serializable
data class DogRoom (
    @PrimaryKey
    //@SerialName("dogID")
    var dogID : String = "",
    //@SerialName("dogName")
    var dogName: String? = null,
    //@SerialName("dogBreed")
    var dogBreed: String? = null,
    //@SerialName("dogGender")
    var dogGender: Boolean? = null,
    //@SerialName("dogAge")
    var dogAge: Int? = null,
    //@SerialName("placeLastSeen")
    var placeLastSeen: String = "",
    //@SerialName("dateLastSeen")
    var dateLastSeen: String = "",
    //@SerialName("hour")
    var hour: Int? = null,
    //@SerialName("minute")
    var minute: Int? = null,
    //@SerialName("ownerID")
    var ownerID: String,
    //@SerialName("ownerEmail")
    var ownerEmail: String,
    //@SerialName("isLost")
    var isLost : Boolean? = null,
    //@SerialName("isFound")
    var isFound : Boolean? = null
    ) : Parcelable
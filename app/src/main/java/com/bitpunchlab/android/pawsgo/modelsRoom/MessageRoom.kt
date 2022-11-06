package com.bitpunchlab.android.pawsgo.modelsRoom

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "message_table")
@Parcelize
data class MessageRoom(
    @PrimaryKey
    val messageID : String,
    val senderEmail : String,
    val targetEmail : String,
    val messageContent : String,
    val date : String
) : Parcelable
package com.bitpunchlab.android.pawsgo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bitpunchlab.android.pawsgo.modelsRoom.UserRoom

object LoginInfo {
    var state = MutableLiveData<AppState>(AppState.LOGGED_OUT)

    lateinit var user : LiveData<UserRoom>

}
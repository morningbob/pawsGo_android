package com.bitpunchlab.android.pawsgo

import androidx.lifecycle.MutableLiveData

object LoginInfo {
    var state = MutableLiveData<AppState>(AppState.LOGGED_OUT)

    var userName: String? = null
    var userEmail: String? = null



}
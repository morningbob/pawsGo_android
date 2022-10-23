package com.bitpunchlab.android.pawsgo

import androidx.lifecycle.MutableLiveData

object LoginState {
    var state = MutableLiveData<LoginStatus>(LoginStatus.LOGGED_OUT)
}
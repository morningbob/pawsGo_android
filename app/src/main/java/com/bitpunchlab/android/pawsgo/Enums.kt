package com.bitpunchlab.android.pawsgo

enum class LoginStatus {
    LOGGED_IN,
    LOGGED_OUT
}

enum class CreateUserAccountStatus {
    NORMAL,
    READY_CREATE_USER_FIREBASE,
    READY_CREATE_USER_AUTH,
    RESET
}

enum class AppState {
    NORMAL,
    LOGGED_IN,
    LOGGED_OUT,
    READY_CREATE_USER_AUTH,
    ERROR_CREATE_USER_AUTH,
    READY_CREATE_USER_FIREBASE,
    SUCCESS_CREATED_USER_ACCOUNT,
    ERROR_CREATE_USER_ACCOUNT,


    RESET
}


package com.bitpunchlab.android.pawsgo.`models-firebase`

import java.util.*

class UserFirebase {

    var userID: String = ""
    var userName: String = ""
    var userEmail: String = ""
    var lostDogs: List<String> = emptyList()
    var dogs: List<String> = emptyList()
    var dateCreated: Date? = Date()

    constructor()

    constructor(id: String, name: String, email: String,
                lostDogs: List<String>, dogs: List<String>) : this() {
                    userID = id
                    userName = name
                    userEmail = email
                }
}
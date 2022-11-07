package com.bitpunchlab.android.pawsgo.modelsFirebase

class DogFirebase {

    var dogID : String = ""
    var dogName : String? = null
    var dogBreed : String? = null
    var dogGender : Boolean? = null
    var dogAge : Int? = null
    var placeLastSeen : String = ""
    var dateLastSeen: String = ""
    var hour : Int? = null
    var minute : Int? = null
    var ownerID : String = ""
    var ownerName : String = ""
    var ownerEmail : String = ""
    var dogImages = HashMap<String, String>()
    var isLost : Boolean? = null
    var isFound : Boolean? = null

    constructor()

    constructor(id: String, name: String?, breed: String?, gender: Boolean?, age: Int?,
        place: String, date: String, hr: Int?, min: Int?, userID: String, userName: String,
                userEmail: String,
        lost: Boolean?, found: Boolean?,
                images: HashMap<String, String> = HashMap<String, String>()) : this() {
            dogID = id
            dogName = name
            dogBreed = breed
            dogGender = gender
            dogAge = age
            placeLastSeen = place
            dateLastSeen = date
            hour = hr
            minute = min
            ownerID = userID
            ownerName = userName
            ownerEmail = userEmail
            isLost = lost
            isFound = found
            dogImages = images
        }
}
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
    var notes : String? = null
    var ownerID : String = ""
    var ownerName : String = ""
    var ownerEmail : String = ""
    var dogImages = HashMap<String, String>()
    var isLost : Boolean? = null
    var isFound : Boolean? = null
    var locationLatLng = HashMap<String, Double>()
    var locationAddress : String? = ""

    constructor()

    constructor(id: String, name: String?, breed: String?, gender: Boolean?, age: Int?,
        place: String, date: String, hr: Int?, min: Int?, note: String?,
                userID: String, userName: String,
                userEmail: String,
                images: HashMap<String, String> = HashMap<String, String>(),
                lost: Boolean?, found: Boolean?,
                latLngPoint: HashMap<String, Double> = HashMap<String, Double>(),
                address: String?) : this() {
            dogID = id
            dogName = name
            dogBreed = breed
            dogGender = gender
            dogAge = age
            placeLastSeen = place
            dateLastSeen = date
            hour = hr
            minute = min
            notes = note
            ownerID = userID
            ownerName = userName
            ownerEmail = userEmail
            isLost = lost
            isFound = found
            dogImages = images
            locationLatLng = latLngPoint
            locationAddress = address
        }
}
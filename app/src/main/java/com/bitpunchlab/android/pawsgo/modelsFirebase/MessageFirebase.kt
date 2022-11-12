package com.bitpunchlab.android.pawsgo.modelsFirebase

class MessageFirebase {
    var messageID : String = ""
    var senderEmail : String = ""
    var senderName : String = ""
    var targetEmail : String = ""
    var messageContent : String = ""
    var date : String = ""

    constructor()

    constructor(id: String, userEmail: String, userName: String, receiverEmail: String, message: String, currentDate: String) {
        messageID = id
        senderEmail = userEmail
        senderName = userName
        targetEmail = receiverEmail
        messageContent = message
        date = currentDate
    }

}
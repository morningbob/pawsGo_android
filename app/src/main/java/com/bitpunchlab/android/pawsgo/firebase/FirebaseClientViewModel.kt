package com.bitpunchlab.android.pawsgo.firebase

import android.app.Activity
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.*
import com.bitpunchlab.android.pawsgo.AppState
import com.bitpunchlab.android.pawsgo.database.PawsGoDatabase
import com.bitpunchlab.android.pawsgo.modelsFirebase.DogFirebase
import com.bitpunchlab.android.pawsgo.modelsFirebase.UserFirebase
import com.bitpunchlab.android.pawsgo.modelsRoom.DogRoom
import com.bitpunchlab.android.pawsgo.modelsRoom.UserRoom
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.*
import java.util.regex.Pattern

@OptIn(InternalCoroutinesApi::class)
class FirebaseClientViewModel(activity: Activity) : ViewModel() {

    var auth : FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore

    private var _userName = MutableLiveData<String>()
    val userName get() = _userName

    private var _userEmail = MutableLiveData<String>()
    val userEmail get() = _userEmail

    private var _userPassword = MutableLiveData<String>()
    val userPassword get() = _userPassword

    private var _userConfirmPassword = MutableLiveData<String>()
    val userConfirmPassword get() = _userConfirmPassword

    private var _nameError = MutableLiveData<String>()
    val nameError get() = _nameError

    private var _emailError = MutableLiveData<String>()
    val emailError get() = _emailError

    private var _passwordError = MutableLiveData<String>()
    val passwordError get() = _passwordError

    private var _confirmPasswordError= MutableLiveData<String>()
    val confirmPasswordError get() = _confirmPasswordError

    private var fieldsValidArray = ArrayList<Int>()

    private var coroutineScope = CoroutineScope(Dispatchers.IO)

    var isCreatingUserAccount = false

    private var localDatabase : PawsGoDatabase

    var _appState = MutableLiveData<AppState>(AppState.NORMAL)
    val appState get() = _appState

    var _currentUserRoom = MediatorLiveData<UserRoom?>()
    val currentUserRoom get() = _currentUserRoom

    // this is the trigger live data that trigger the fetch of a user object
    private var userIDLiveData  = MutableLiveData<String>()
    // whenever the userIDLiveData changed, the currentUserLiveData's transformation
    // will be triggered and retrieve the user from local database
    // now, we can observe this variable to update the UI.
    val currentUserRoomLiveData = Transformations.switchMap(userIDLiveData) { id ->
        Log.i("current user live data", "retrieving user")
        retrieveUserRoom(id)
    }

    var currentUserID : String = ""
    var currentUserEmail : String = ""

    var _currentUserFirebaseLiveData = MutableLiveData<UserFirebase>()
    val currentUserFirebaseLiveData get() = _currentUserFirebaseLiveData

    val storageRef = Firebase.storage.reference
    //val lostDogsRef = storageRef.child()

    private var authStateListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser != null) {
            _appState.postValue(AppState.LOGGED_IN)
            Log.i("auth", "changed state to login")
            // as soon as we got the auth current user, we use its uid to retrieve the
            // user room in local database
            userIDLiveData.postValue(auth.currentUser!!.uid)
        } else {
            _appState.postValue(AppState.LOGGED_OUT)
            Log.i("auth", "changed state to logout")
        }
    }

    private val nameValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userName) { name ->
            if (name.isNullOrEmpty()) {
                nameError.value = "Name must not be empty."
                value = false
            } else {
                value = true
                nameError.value = ""
            }
        }
    }

    private val emailValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userEmail) { email ->
            if (!email.isNullOrEmpty()) {
                if (!isEmailValid(email)) {
                    emailError.value = "Please enter a valid email."
                    value = false
                } else {
                    value = true
                    emailError.value = ""
                }
            } else {
                value = false
            }
            Log.i("email valid? ", value.toString())
        }
    }
/*
    // should be at least 10 number and max 15, I think
    private val phoneValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userPhone) { phone ->
            if (phone.isNullOrEmpty()) {
                phoneError.value = "Phone number must not be empty."
                value = false
            } else if (phone.count() < 10 || phone.count() > 13)
                phoneError.value = "Phone number should be at least \n 10 number and maximum 13 number."
            else if (!isPhoneValid(phone)) {
                phoneError.value = "Phone number is not valid."
            } else {
                value = true
                phoneError.value = ""
            }
        }
    }
*/
    private val passwordValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userPassword) { password ->
            if (!password.isNullOrEmpty()) {
                if (isPasswordContainSpace(password)) {
                    passwordError.value = "Password cannot has space."
                    value = false
                } else if (password.count() < 8) {
                    passwordError.value = "Password should be at least 8 characters."
                    value = false
                } else if (!isPasswordValid(password)) {
                    passwordError.value = "Password can only be composed of letters and numbers."
                    value = false
                } else {
                    passwordError.value = ""
                    value = true
                }
            } else {
                value = false
            }
            Log.i("password valid? ", value.toString())
        }
    }

    private val confirmPasswordValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userConfirmPassword) { confirmPassword ->
            if (!confirmPassword.isNullOrEmpty()) {
                if (!isConfirmPasswordValid(userPassword.value!!, confirmPassword)) {
                    confirmPasswordError.value = "Passwords must be the same."
                    value = false
                } else {
                    confirmPasswordError.value = ""
                    value = true
                }
            } else {
                value = false
            }
            Log.i("confirm valid? ", value.toString())
        }
    }

    private fun isEmailValid(email: String) : Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPhoneValid(phone: String) : Boolean {
        return PhoneNumberUtils.isGlobalPhoneNumber(phone)
    }

    private fun isPasswordValid(password: String) : Boolean {
        val passwordPattern = Pattern.compile("^[A-Za-z0-9]{8,20}$")
        return passwordPattern.matcher(password).matches()
    }

    private fun isPasswordContainSpace(password: String) : Boolean {
        return password.contains(" ")
    }

    private fun isConfirmPasswordValid(password: String, confirmPassword: String) : Boolean {
        //Log.i("confirming password: ", "password: $password, confirm: $confirmPassword")
        return password == confirmPassword
    }

    private fun sumFieldsValue() : Boolean {
        return fieldsValidArray.sum() == 4
    }
    var readyRegisterLiveData = MediatorLiveData<Boolean>()
    var readyLoginLiveData = MediatorLiveData<Boolean>()


    init {
        // this live data stores 5 values,
        // it represents the validity of all the fields of the registration form
        // if the corresponding field is valid, the value in this array, the particular position
        // will be 1, otherwise, it will be 0
        // that makes checking validities of all fields more efficient,
        // by just summing up all 4 fields to see if it is 4, then it is ready
        fieldsValidArray = arrayListOf(0,0,0,0)
        auth.addAuthStateListener(authStateListener)
        localDatabase = PawsGoDatabase.getInstance(activity)

        // we observe the currentUserRoom here, we need to observe it, so we can read the value
        // I use another variable to store this user object.
        currentUserRoom.observe(activity as LifecycleOwner, Observer { user ->
            user?.let {
                currentUserRoom.value = user
            }
        })

        currentUserRoomLiveData.observe(activity as LifecycleOwner, Observer { user ->
            user?.let {
                Log.i("user live data", "got back user")
                Log.i("user live data", "userEmail: ${user.userEmail}")
                currentUserID = user.userID
                currentUserEmail = user.userEmail
            }
        })

        appState.observe(activity as LifecycleOwner, Observer { state ->
            when (state) {
                AppState.READY_CREATE_USER_AUTH -> {
                    coroutineScope.launch {
                        if (createUserOfAuth()) {
                            _appState.postValue(AppState.READY_CREATE_USER_FIREBASE)
                        } else {
                            _appState.postValue(AppState.ERROR_CREATE_USER_AUTH)
                        }
                    }
                }
                AppState.ERROR_CREATE_USER_AUTH -> {
                    resetAllFields()
                }
                AppState.READY_CREATE_USER_FIREBASE -> {
                    Log.i("ready create user firebase", "user id from auth ${auth.currentUser!!.uid}")
                    val user = createUserFirebase(
                        id = auth.currentUser!!.uid,
                        name = userName.value!!,
                        email = userEmail.value!!,
                        lost = HashMap<String, DogFirebase>(),
                        dogs = HashMap<String, DogFirebase>())
                    coroutineScope.launch {
                        if (saveUserFirebase(user)) {
                            // we also create the user room and save it here
                            val userRoom = convertUserFirebaseToUserRoom(user)
                            saveUserRoom(userRoom)
                            // we also update LoginInfo , the current user
                            //LoginInfo.user = userRoom
                            _appState.postValue(AppState.SUCCESS_CREATED_USER_ACCOUNT)
                        } else {
                            _appState.postValue(AppState.ERROR_CREATE_USER_ACCOUNT)
                        }

                    }
                }
                AppState.ERROR_CREATE_USER_ACCOUNT -> {
                    resetAllFields()
                }
                AppState.SUCCESS_CREATED_USER_ACCOUNT -> {
                    resetAllFields()
                }
                AppState.LOGGED_IN -> {
                    //retrieveUserRoom()
                    Log.i("firebaseClient", "login state detected")
                    if (isCreatingUserAccount) {
                        _appState.postValue(AppState.READY_CREATE_USER_FIREBASE)
                    }
                }
                else -> 0
            }
        })


        // this live data observes all the validities of the fields' live data
        // it set the fieldsValidArray's value according to the validity.
        // whenever the sum of the fieldsValidArray is 4, this data returns true
        // else false
        readyRegisterLiveData.addSource(nameValid) { valid ->
            if (valid) {
                fieldsValidArray[0] = 1
                // check other fields validity
                readyRegisterLiveData.value = sumFieldsValue()
            } else {
                fieldsValidArray[0] = 0
                readyRegisterLiveData.value = false
            }
        }
        readyRegisterLiveData.addSource(emailValid) { valid ->
            if (valid) {
                fieldsValidArray[1] = 1
                // check other fields validity
                readyRegisterLiveData.value = sumFieldsValue()
            } else {
                fieldsValidArray[1] = 0
                readyRegisterLiveData.value = false
            }
        }

        readyRegisterLiveData.addSource(passwordValid) { valid ->
            if (valid) {
                fieldsValidArray[2] = 1
                // check other fields validity
                readyRegisterLiveData.value = sumFieldsValue()
            } else {
                fieldsValidArray[2] = 0
                readyRegisterLiveData.value = false
            }
        }
        readyRegisterLiveData.addSource(confirmPasswordValid) { valid ->
            if (valid) {
                fieldsValidArray[3] = 1
                // check other fields validity
                readyRegisterLiveData.value = sumFieldsValue()
            } else {
                fieldsValidArray[3] = 0
                readyRegisterLiveData.value = false
            }
        }
        readyLoginLiveData.addSource(emailValid) { valid ->
            readyLoginLiveData.value = (valid && passwordValid.value != null && passwordValid.value!!)

        }
        readyLoginLiveData.addSource(passwordValid) { valid ->
            readyLoginLiveData.value = (valid && emailValid.value != null && emailValid.value!!)

        }

    }

    private fun createUserFirebase(id: String, name: String, email: String, lost: HashMap<String, DogFirebase>,
                                   dogs: HashMap<String, DogFirebase>) : UserFirebase {
        return UserFirebase(id = id, name = name, email = email,
            lost = HashMap<String, DogFirebase>(), dog = HashMap<String, DogFirebase>())
    }

    private suspend fun saveUserFirebase(user: UserFirebase) =
        suspendCancellableCoroutine<Boolean> { cancellableContinuation ->
            firestore
                .collection("users")
                .document(user.userID)
                .set(user)
                .addOnSuccessListener { docRef ->
                    Log.i("firestore", "save new user, success")
                    cancellableContinuation.resume(true){}
                }
                .addOnFailureListener { e ->
                    Log.i("firestore", "save user failed: ${e.message}")
                    cancellableContinuation.resume(false){}
                }
    }

    private fun convertUserFirebaseToUserRoom(userFirebase: UserFirebase) : UserRoom {
        return UserRoom(userID = userFirebase.userID, userName = userFirebase.userName,
            userEmail = userFirebase.userEmail, dateCreated = userFirebase.dateCreated,
            lostDogs = convertDogMapToDogList(userFirebase.lostDogs),
            dogs = convertDogMapToDogList(userFirebase.dogs))
    }

    private fun saveUserRoom(user: UserRoom) {
        coroutineScope.launch {
            localDatabase.pawsDAO.insertUser(user)
        }
    }

    private suspend fun retrieveUserFirebase() : UserFirebase? =
        suspendCancellableCoroutine<UserFirebase?> { cancellableContinuation ->
            firestore
                .collection("users")
                .document(auth.currentUser!!.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        Log.i("retrieve user", "can't find the user")
                        cancellableContinuation.resume(null) {}
                        // post an error state here
                        //createAccountAppState.postValue(CreateAccountAppState.GET_USER_OBJECT_FAILURE)
                    } else {
                        Log.i("retrieve user", "found the user")
                        val user = document.toObject(UserFirebase::class.java)!!
                        Log.i("retrieve user object", user.userName)
                        cancellableContinuation.resume(user) {}
                    }
                }
        }


    private fun retrieveUserRoom(id: String) : LiveData<UserRoom> {
        return localDatabase.pawsDAO.getUser(id)
    }

    private fun convertDogMapToDogList(dogHashmap: HashMap<String, DogFirebase>) : List<DogRoom> {
        val list : MutableList<DogRoom> = emptyList<DogRoom>() as MutableList<DogRoom>
        for ((key, value) in dogHashmap) {
            list.add(convertDogFirebaseToDogRoom(value))
        }
        return list
    }

    private fun convertDogListToDogMap(dogList: List<DogRoom>) {

    }
    /*
    private fun convertDogRoomToDogFirebase(dogRoom: DogRoom): DogFirebase {
        return DogFirebase(id = dogRoom.dogID, name = dogRoom.dogName, gender = dogRoom.dogGender,
            breed = dogRoom.dogBreed, age = dogRoom.dogAge, date = dogRoom.dateLastSeen,
            hr = dogRoom.hour, min = dogRoom.minute, place = dogRoom.placeLastSeen,
            userID = dogRoom.ownerID, userEmail = dogRoom.ownerEmail, lost = dogRoom.isLost,
            found = dogRoom.isFound)
    }
    */
    private fun convertDogFirebaseToDogRoom(dogFirebase: DogFirebase): DogRoom {
        return DogRoom(dogID = dogFirebase.dogID, dogName = dogFirebase.dogName,
            dogBreed = dogFirebase.dogBreed, dogGender = dogFirebase.dogGender,
            dogAge = dogFirebase.dogAge, ownerID = dogFirebase.ownerID,
            ownerEmail = dogFirebase.ownerEmail, isLost = dogFirebase.isLost,
            isFound = dogFirebase.isFound, dateLastSeen = dogFirebase.dateLastSeen,
            hour = dogFirebase.hour, minute = dogFirebase.minute,
            placeLastSeen = dogFirebase.placeLastSeen)
    }

    private suspend fun createUserOfAuth() : Boolean =
        suspendCancellableCoroutine<Boolean> { cancellableContinuation ->
            auth
                .createUserWithEmailAndPassword(userEmail.value!!, userPassword.value!!)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i("firebase auth", "create user success.")
                        cancellableContinuation.resume(true){}
                    } else {
                        Log.i("firebase auth", "create user failure: ${task.exception?.message}")
                        cancellableContinuation.resume(false){}
                    }
                }
    }

    suspend fun loginUserOfAuth() : Boolean =
        suspendCancellableCoroutine<Boolean> { cancellableContinuation ->
            auth
                .signInWithEmailAndPassword(userEmail.value!!, userPassword.value!!)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i("firebase auth, sign in", "success")
                        cancellableContinuation.resume(true){}
                    } else {
                        Log.i("firebase auth, sign in", "failed ${task.exception?.message}")
                        cancellableContinuation.resume(false){}
                    }
                }

    }

    fun logoutUser() {
        Log.i("logout", "logging out")
        auth.signOut()
    }

    fun resetAllFields() {
        userName.value = ""
        userEmail.value = ""
        userPassword.value = ""
        userConfirmPassword.value = ""
        nameError.value = ""
        emailError.value = ""
        passwordError.value = ""
        confirmPasswordError.value = ""
        isCreatingUserAccount = false
    }

    fun handleNewLostDog(dogRoom: DogRoom) {
        val dogFirebase = convertDogRoomToDogFirebase(dogRoom)
        if (dogFirebase != null) {
            coroutineScope.launch {
                saveDogFirebase(dogFirebase)
            }
        } else {
            Log.i("firebaseClient", "current user id and email is null")
        }
    }

    private fun convertDogRoomToDogFirebase(dogRoom: DogRoom): DogFirebase? {
        if (currentUserID != "" && currentUserEmail != "") {
            return DogFirebase(
                id = dogRoom.dogID, name = dogRoom.dogName, breed = dogRoom.dogBreed,
                gender = dogRoom.dogGender, age = dogRoom.dogAge, date = dogRoom.dateLastSeen,
                hr = dogRoom.hour, min = dogRoom.minute, place = dogRoom.placeLastSeen,
                userID = currentUserID, userEmail = currentUserEmail,
                lost = null, found = null
            )
        } else {
            return null
            }
    }

    private suspend fun saveDogFirebase(dog: DogFirebase) : Boolean =
        suspendCancellableCoroutine<Boolean> { cancellableContinuation ->
            firestore
                .collection("lostDogs")
                .document(dog.dogID.toString())
                .set(dog)
                .addOnSuccessListener { docRef ->
                    Log.i("save dog firebase", "success")
                    cancellableContinuation.resume(true){}
                }
                .addOnFailureListener { e ->
                    Log.i("save dog firesbase", "failure: ${e.message}")
                    cancellableContinuation.resume(false){}
                }
    }

    suspend fun uploadImageFirebase(data: ByteArray, dogID: String) : Task<Uri>? =
        suspendCancellableCoroutine<Task<Uri>?> { cancellableContinuation ->
            // create a ref for the image
            val imageRef = storageRef.child("lostDogs/${dogID}.jpg")
            val uploadTask = imageRef.putBytes(data)
            uploadTask
                .addOnSuccessListener { taskSnapshot ->
                    Log.i("upload image", "success")
                    cancellableContinuation.resume(taskSnapshot.storage.downloadUrl) {}
                    //val b =  taskSnapshot.metadata!!.reference!!.downloadUrl
                }
                .addOnFailureListener { e ->
                    Log.i("upload image", "failure")
                    cancellableContinuation.resume(null) {}
                }
        }

    // here we retrieve the most updated
    // user firebase from Firestore, and immediately add the lost dog
    // info in it and send it to Firestore.
    private suspend fun updateUserLostDogFirebase(user: UserFirebase, dog: DogFirebase) : Boolean {
            //retrieveUserFirebase()!!.lostDogs
            val user = retrieveUserFirebase()
            if (user != null) {
                user.lostDogs.put(dog.dogID, dog)

            } else {
                Log.i("update lost dog in user object", "can't find the user in firebase")
            }
        return false
    }
}

class FirebaseClientViewModelFactory(private val activity: Activity)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FirebaseClientViewModel::class.java)) {
            return FirebaseClientViewModel(activity) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
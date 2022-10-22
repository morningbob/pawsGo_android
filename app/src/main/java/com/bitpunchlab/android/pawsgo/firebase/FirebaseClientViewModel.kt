package com.bitpunchlab.android.pawsgo.firebase

import android.app.Activity
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import java.util.regex.Pattern

class FirebaseClientViewModel(activity: Activity) : ViewModel() {

    var auth : FirebaseAuth = FirebaseAuth.getInstance()

    private var _userName = MutableLiveData<String>()
    private val userName get() = _userName

    private var _userEmail = MutableLiveData<String>()
    private val userEmail get() = _userEmail

    private var _userPassword = MutableLiveData<String>()
    private val userPassword get() = _userPassword

    private var _userConfirmPassword = MutableLiveData<String>()
    private val userConfirmPassword get() = _userConfirmPassword

    private var _nameError = MutableLiveData<String>()
    private val nameError get() = _nameError

    private var _emailError = MutableLiveData<String>()
    private val emailError get() = _emailError

    private var _passwordError = MutableLiveData<String>()
    private val passwordError get() = _passwordError

    private var _confirmPasswordError= MutableLiveData<String>()
    private val confirmPasswordError get() = _confirmPasswordError

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

    val emailValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
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

    var totalValidity = MediatorLiveData<ArrayList<Int>>()
    
    var readyRegisterLiveData = MediatorLiveData<Boolean>()
    var shouldRegisterLiveDate = MediatorLiveData<Boolean>()

    init {

        totalValidity.addSource(userName) { name ->
            if (name.isNullOrEmpty()) {
                nameError.value = "Name must not be empty."
                //value = false
            } else {
                //value = true
                nameError.value = ""
            }
        }
        totalValidity.addSource(userEmail) { email ->
            if (!email.isNullOrEmpty()) {
                if (!isEmailValid(email)) {
                    emailError.value = "Please enter a valid email."
                    //value = false
                } else {
                    //value = true
                    emailError.value = ""
                }
            } else {
                //value = false
            }
            //Log.i("email valid? ", value.toString())

        }

        shouldRegisterLiveDate

        readyRegisterLiveData.addSource(nameValid) { valid ->
            readyRegisterLiveData.value = if (valid) {
                //allValid.value?.set(0, 1)
                // check other fields validity
                //checkIfAllValid()
                true
            } else {
                //allValid.value?.set(0, 0)
                false
            }
        }
        readyRegisterLiveData.addSource(emailValid) { valid ->
            if (valid) {
                allValid.value?.set(1, 1)
                // check other fields validity
                readyRegisterLiveData.value = checkIfAllValid()
            } else {
                allValid.value?.set(1, 0)
                readyRegisterLiveData.value = false
            }
        }

        readyRegisterLiveData.addSource(passwordValid) { valid ->
            if (valid) {
                allValid.value?.set(3, 1)
                // check other fields validity
                readyRegisterLiveData.value = checkIfAllValid()
            } else {
                allValid.value?.set(3, 0)
                readyRegisterLiveData.value = false
            }
        }
        readyRegisterLiveData.addSource(confirmPasswordValid) { valid ->
            if (valid) {
                allValid.value?.set(4, 1)
                // check other fields validity
                readyRegisterLiveData.value = checkIfAllValid()
            } else {
                allValid.value?.set(4, 0)
                readyRegisterLiveData.value = false
            }
        }
    }
    fun createUserOfAuth(email: String, password: String) {
        auth
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i("firebase auth", "create user success.")
                } else {
                    Log.i("firebase auth", "create user failure: ${task.exception?.message}")
                }
            }
    }

    fun loginUserOfAuth(email: String, password: String) {
        auth
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i("firebase auth, sign in", "success")
                } else {
                    Log.i("firebase auth, sign in", "failed ${task.exception?.message}")
                }
            }
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
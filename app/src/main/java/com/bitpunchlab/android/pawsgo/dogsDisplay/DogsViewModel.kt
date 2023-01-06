package com.bitpunchlab.android.pawsgo.dogsDisplay

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.pawsgo.database.PawsGoDatabase
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.modelsRoom.DogRoom
import kotlinx.coroutines.InternalCoroutinesApi

// we will load all lost dogs and found dogs in Firebase client
// and save to local database, then, we retrieve them here as there
// is any change
class DogsViewModel(application: Application) : AndroidViewModel(application) {

    //private val context = getApplication<Application>().applicationContext

    @OptIn(InternalCoroutinesApi::class)
    val localDatabase = PawsGoDatabase.getInstance(application.applicationContext)
    var lostDogs = localDatabase.pawsDAO.getAllLostDogs()

    var foundDogs = localDatabase.pawsDAO.getAllFoundDogs()

    var _dogReports = MutableLiveData<List<DogRoom>>()
    val dogReports get() = _dogReports

    var _chosenDog = MutableLiveData<DogRoom?>()
    val chosenDog get() = _chosenDog

    var _dogMessage = MutableLiveData<DogRoom?>()
    val dogMessage get() = _dogMessage

    fun onDogChosen(dog: DogRoom) {
        _chosenDog.value = dog
    }

    fun finishedDogChosen() {
        _chosenDog.value = null
    }

    // when the dog message variable has a dog in it,
    // we'll start the messaging function
    fun onDogMessageClicked(dog: DogRoom) {
        _dogMessage.value = dog
    }

    fun finishedDogMessage() {
        _dogMessage.value = null
    }

    // retrieve the current user's profile in local database
    // check the lostDogs list to get all dogs

    fun prepareDogReports(userID: String) {
        //dogReports = localDatabase
    }

}

class DogsViewModelFactory(private val application: Application)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DogsViewModel::class.java)) {
            return DogsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
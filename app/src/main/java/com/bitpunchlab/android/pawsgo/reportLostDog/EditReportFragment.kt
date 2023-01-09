package com.bitpunchlab.android.pawsgo.reportLostDog

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.databinding.FragmentEditReportBinding
import com.bitpunchlab.android.pawsgo.dogsDisplay.DogsViewModel
import com.bitpunchlab.android.pawsgo.dogsDisplay.DogsViewModelFactory
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModelFactory
import com.bitpunchlab.android.pawsgo.location.LocationViewModel
import com.bitpunchlab.android.pawsgo.modelsRoom.DogRoom
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import java.util.*


class EditReportFragment : Fragment() {

    private var _binding : FragmentEditReportBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseClient : FirebaseClientViewModel
    private var pet : DogRoom? = null
    private lateinit var locationViewModel : LocationViewModel
    private lateinit var dogsViewModel : DogsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditReportBinding.inflate(inflater, container, false)
        firebaseClient = ViewModelProvider(requireActivity(),
            FirebaseClientViewModelFactory(requireActivity().application))
            .get(FirebaseClientViewModel::class.java)
        locationViewModel = ViewModelProvider(requireActivity()).get(LocationViewModel::class.java)
        dogsViewModel = ViewModelProvider(requireActivity(), DogsViewModelFactory(requireActivity().application))
            .get(DogsViewModel::class.java)
        pet = requireArguments().getParcelable<DogRoom>("pet")
        //prefillPetInfo(pet!!)
        setupPetFormFragment()
        binding.lifecycleOwner = viewLifecycleOwner
        //binding.pet = pet

        locationViewModel.shouldNavigateChooseLocation.observe(viewLifecycleOwner, Observer { should ->
            Log.i("should navigate", should.toString())
            should?.let {
                if (should) {
                    Log.i("edit report", "navigating")
                    findNavController().navigate(R.id.chooseLocationAction)
                    // reset
                    locationViewModel.shouldNavigateChooseLocation.value = false
                }
            }
        })

        dogsViewModel.readyProcessReport.observe(viewLifecycleOwner, Observer { ready ->
            ready?.let {
                if (ready) {
                    processUpdateReport()
                    dogsViewModel.readyProcessReport.value = false
                }
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupPetFormFragment() {
        val petFormFragment = PetFormFragment()
        val bundle = Bundle()
        bundle.putParcelable("pet", pet)
        bundle.putBoolean("lostOrFound", pet!!.isLost!!)
        petFormFragment.arguments = bundle
        val fragmentTransaction = childFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.editFragmentContainer, petFormFragment)
        fragmentTransaction.commit()
    }

    // prefill the live data, so the pet form can show it
    private fun prefillPetInfo(pet: DogRoom) {
        dogsViewModel.petName.value = pet.dogName
        dogsViewModel.petType.value = pet.animalType
        dogsViewModel.petGender.value = pet.dogGender
        dogsViewModel.petBreed.value = pet.dogBreed
        dogsViewModel.petAge.value = pet.dogAge
        dogsViewModel.dateLastSeen.value = pet.dateLastSeen
        dogsViewModel.placeLastSeen.value = pet.placeLastSeen
        dogsViewModel.lostHour.value = pet.hour
        dogsViewModel.lostMinute.value = pet.minute
        dogsViewModel.petNotes.value = pet.notes

    }

    private fun processUpdateReport() {
        // general data is verified in pet form
        // we get all the info that we prefilled and user changed in the liva data variables
        // create a new object with the same dogID, and new info
        var name = ""
        if (dogsViewModel.petName.value != null) {
            name = dogsViewModel.petName.value!!
        }
        var gender = 0
        if (dogsViewModel.petGender.value != null) {
            gender = dogsViewModel.petGender.value!!
        }
        var date = ""
        if (dogsViewModel.dateLastSeen.value != null) {
            date = dogsViewModel.dateLastSeen.value!!
        }
        var place = ""
        if (dogsViewModel.placeLastSeen.value != null) {
            place = dogsViewModel.placeLastSeen.value!!
        }


        val updatePet = createDogRoom(id = pet!!.dogID, name = name, animal = dogsViewModel.petType.value,
        gender = gender, age = dogsViewModel.petAge.value, breed = dogsViewModel.petBreed.value,
        date = date, hour = dogsViewModel.lostHour.value, minute = dogsViewModel.lostMinute.value,
        place = place, note = dogsViewModel.petNotes.value, lost = pet!!.isLost!!, lat = locationViewModel.lostDogLocationLatLng.value?.latitude,
        lng = locationViewModel.lostDogLocationLatLng.value?.longitude, address = place, found = false)

        // reset locationVM address and latlng
        Log.i("edit report", "update pet ${updatePet}")
    }

    private fun createDogRoom(id: String, name: String, animal: String?, breed: String?,
                              gender: Int, age: Int?, date: String,
                              hour: Int?, minute: Int?, note: String?, place: String, lost: Boolean, found: Boolean,
                              lat: Double?, lng: Double?, address: String?): DogRoom {
        return DogRoom(dogID = id, dogName = name,
            animalType = animal, dogBreed = breed,
            dogGender = gender, dogAge = age, isLost = lost, isFound = found,
            dateLastSeen = date, hour = hour, minute = minute, notes = note,
            placeLastSeen = place, ownerID = firebaseClient.currentUserID,
            ownerEmail = firebaseClient.currentUserEmail,
            ownerName = firebaseClient.currentUserFirebaseLiveData.value!!.userName,
            locationLat = lat, locationLng = lng,
            locationAddress = address)
    }

}
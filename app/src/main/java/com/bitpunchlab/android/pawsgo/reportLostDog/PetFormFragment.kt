package com.bitpunchlab.android.pawsgo.reportLostDog

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.databinding.FragmentPetFormBinding
import com.bitpunchlab.android.pawsgo.dogsDisplay.DogsViewModel
import com.bitpunchlab.android.pawsgo.dogsDisplay.DogsViewModelFactory
import com.bitpunchlab.android.pawsgo.location.LocationViewModel
import com.bitpunchlab.android.pawsgo.modelsRoom.DogRoom
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*


class PetFormFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var _binding : FragmentPetFormBinding? = null
    private val binding get() = _binding!!
    private var timePicker : MaterialTimePicker? = null
    private var datePicker : MaterialDatePicker<Long>? = null
    private var lostDate : String? = null
    private var lostHour : Int? = null
    private var lostMinute : Int? = null
    //private var gender : Int = 0
    private var animalType = MutableLiveData<String?>()
    private var petPassed : DogRoom? = null
    val ONE_DAY_IN_MILLIS = 86400000
    private var coroutineScope = CoroutineScope(Dispatchers.IO)
    private var reportOrEdit : Boolean? = null
    private var isPermissionGranted : Boolean? = null
    private lateinit var locationViewModel : LocationViewModel
    private lateinit var dogsViewModel : DogsViewModel
    private var lostOrFound : Boolean? = null
    private var petAgeString = MutableLiveData<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPetFormBinding.inflate(inflater, container, false)
        petPassed = requireArguments().getParcelable<DogRoom>("pet")
        lostOrFound = requireArguments().getBoolean("lostOrFound")

        // check if pet == null
        // if we should use it in layout to display pet data, ie the edit mode
        // if it is null, we're in the report mode
        reportOrEdit = petPassed == null
        locationViewModel = ViewModelProvider(requireActivity()).get(LocationViewModel::class.java)
        dogsViewModel = ViewModelProvider(requireActivity(), DogsViewModelFactory(requireActivity().application))
            .get(DogsViewModel::class.java)
        binding.pet = petPassed
        binding.locationVM = locationViewModel
        binding.dogsVM = dogsViewModel
        if (!checkPermission()) {
            permissionResultLauncher.launch(permissions)
        } else {
            isPermissionGranted = true
        }
        // if petPassed is null, we know that we are in the report mode
        // I create a dummy pet room object and put it in the layout binding.
        // all the info user entered will be passed to the dummy pet
        // we then pass this temp pet to dogsVM.
        if (reportOrEdit == true) {
            dogsViewModel.tempPet.value = DogRoom(
                ownerID = "", ownerName = "", ownerEmail = "", dateLastSeen = "",
                placeLastSeen = "", locationAddress = "", locationLng = null, locationLat = null,
                isLost = lostOrFound
            )
            binding.pet = dogsViewModel.tempPet.value
        }
        // set the title lost or found
        if (lostOrFound!!) {
            binding.textviewReportTitle.text = "Lost Report"
        } else {
            binding.textviewReportTitle.text = "Found Report"
        }

        setupGenderSpinner()
        setupPetSpinner()
        // edit mode
        if (reportOrEdit == false) {
            showGenderAtSpinner()
            showTypeAtSpinner()
            showAge()
            showDate()
            showTime()
        }

        locationViewModel.lostDogLocationAddress.observe(viewLifecycleOwner, androidx.lifecycle.Observer { address ->
            locationViewModel.displayAddress.value = address?.get(0) ?: ""
            // update tempPet
            dogsViewModel.placeLastSeen.value = address?.get(0) ?: ""
        })

        binding.buttonChooseDate.setOnClickListener {
            showDatePicker()
        }

        binding.buttonChooseTime.setOnClickListener {
            showTimePicker()
        }

        binding.buttonShowMap.setOnClickListener {
            if (isPermissionGranted != null && !isPermissionGranted!!) {
                permissionsNeededAlert()
            } else {
                findNavController().navigate(R.id.action_petFormFragment_to_chooseLocationFragment)
            }
        }

        binding.buttonUpload.setOnClickListener {
            selectImageFromGalleryResult.launch("image/*")
        }

        // this variable seems extra, because we already have dogsVM.tempPet to hold the data
        // but I use this variable to detect the change of type user picks at the spinner
        // this live data variable watch the change for me and show type in textview
        dogsViewModel.petType.observe(viewLifecycleOwner, androidx.lifecycle.Observer { type ->
            type?.let {
                if (type != "Other" && type != "Choose" ) {
                    binding.edittextOtherType.hint = type
                } else {
                    binding.edittextOtherType.hint = "Please enter the type here."
                }
            }
        })

        dogsViewModel.petAgeString.observe(viewLifecycleOwner, androidx.lifecycle.Observer { ageString ->
            ageString?.let {
                try {
                    dogsViewModel.petAge.value = ageString.toInt()
                } catch (e: java.lang.NumberFormatException) {
                    Log.i("processing dog age", "error converting to number")
                    // can alert user here, or turn edittext to red
                }
            }
        })

        binding.buttonSend.setOnClickListener {
            processReportInputs()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val choice = parent!!.getItemAtPosition(position)
        when (choice) {
            "Male" -> {
                //gender = 1
                //dogsViewModel.tempPet.value!!.dogGender = 1
                dogsViewModel.petGender.value = 1
                Log.i("spinner", "set gender male")
            }
            "Female" -> {
                //gender = 2
                //dogsViewModel.tempPet.value!!.dogGender = 2
                dogsViewModel.petGender.value = 2
                Log.i("spinner", "set gender female")
            }
            "Dog" -> {
                //animalType.value = "Dog"
                dogsViewModel.petType.value = "Dog"
                //dogsViewModel.tempPet.value!!.animalType = "Dog"
                Log.i("spinner", "set type dog")
            }
            "Cat" -> {
                //animalType.value = "Cat"
                dogsViewModel.petType.value = "Cat"
                //dogsViewModel.tempPet.value!!.animalType = "Cat"
                Log.i("spinner", "set type cat")
            }
            "Bird" -> {
                //animalType.value = "Bird"
                dogsViewModel.petType.value = "Bird"
                //dogsViewModel.tempPet.value!!.animalType = "Bird"
                Log.i("spinner", "set type bird")
            }
            "Other" -> {
                //animalType.value = "Other"
                Log.i("spinner", "set type other")
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    private fun setupGenderSpinner() {
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.gender_array,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.genderSpinner.adapter = adapter
        }

        binding.genderSpinner.onItemSelectedListener= this
    }

    private fun showGenderAtSpinner() {
        //Log.i("show gender", "trying to set gender")
        binding.genderSpinner.setSelection(petPassed!!.dogGender)
    }

    private fun showTypeAtSpinner() {
        if (petPassed!!.animalType != null && petPassed!!.animalType != "") {
            Log.i("show tyoe", "trying to set type")
            when (petPassed!!.animalType) {
                "Dog" -> binding.petSpinner.setSelection(1)
                "Cat" -> binding.petSpinner.setSelection(2)
                "Bird" -> binding.petSpinner.setSelection(3)
                else -> binding.petSpinner.setSelection(4) // Other
            }
        }
    }

    private fun showAge() {
        if (petPassed!!.dogAge != null && petPassed!!.dogAge != 0) {
            binding.edittextDogAge.hint = petPassed!!.dogAge.toString()
        }
    }

    private fun showDate() {
        binding.textviewDateLostData.visibility = View.VISIBLE
        //binding.textviewDateLostData.text = pet!!.dateLastSeen
    }

    private fun showTime() {
        binding.textviewTimeLostData.visibility = View.VISIBLE
        //binding.textviewTimeLostData.text =
    }

    private fun setupPetSpinner() {
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.animalType_array,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.petSpinner.adapter = adapter
        }

        binding.petSpinner.onItemSelectedListener= this
    }

    private fun showDatePicker() {
        val today = MaterialDatePicker.todayInUtcMilliseconds() + ONE_DAY_IN_MILLIS
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH)

        calendar.timeInMillis = today
        calendar[Calendar.YEAR] = 2012
        val startDate = calendar.timeInMillis

        calendar.timeInMillis = today
        //calendar[Calendar.YEAR] = 2022
        val endDate = calendar.timeInMillis

        val constraints: CalendarConstraints = CalendarConstraints.Builder()
            .setOpenAt(endDate)
            .setStart(startDate)
            .setEnd(endDate)
            .build()

        datePicker = MaterialDatePicker
            .Builder
            .datePicker()
            .setCalendarConstraints(constraints)
            .setTitleText(getString(R.string.select_a_date))
            .build()

        datePicker!!.show(childFragmentManager, "DATE_PICKER")

        datePicker!!.addOnPositiveButtonClickListener { data ->
            if (data < today) {
                val simpleDate = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)

                val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                utc.timeInMillis = data
                val dayOfMonth = utc.get(Calendar.DAY_OF_MONTH)
                val month = utc.get(Calendar.MONTH)
                val year = utc.get(Calendar.YEAR)
                // then, we put the day, month and year to calendar
                val calendarChosen = Calendar.getInstance()
                calendarChosen.set(year, month, dayOfMonth)

                lostDate = simpleDate.format(calendarChosen.time)
                //Log.i("got back date", "day: $dayOfMonth, month: $month, year: $year")
                Log.i("got back date", lostDate.toString())

                // I didn't use two-way binding here, because I need to make sure
                // the format of the date is correct.
                // so I need to set the date string in the tempPet manually
                dogsViewModel.dateLastSeen.value = lostDate!!
                binding.textviewDateLostData.text = lostDate
                binding.textviewDateLostData.visibility = View.VISIBLE
            } else {
                coroutineScope.launch(Dispatchers.Main) {
                    invalidDateAlert()
                }
            }
        }

        dogsViewModel.petName.observe(viewLifecycleOwner, androidx.lifecycle.Observer { name ->
            Log.i("from pet form: name", name)
        })
    }

    private fun showTimePicker() {
        timePicker = MaterialTimePicker
            .Builder()
            .setTitleText(getString(R.string.select_a_time))
            .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
            .build()

        timePicker!!.show(childFragmentManager, "TIME_PICKER")

        timePicker!!.addOnPositiveButtonClickListener {
            //lostHour = timePicker!!.hour
            //lostMinute = timePicker!!.minute
            // there is no two-way binding for hour and minute
            dogsViewModel.lostHour.value = timePicker!!.hour
            dogsViewModel.lostMinute.value = timePicker!!.minute
            //Log.i("got time back", "hour: ${lostHour.toString()} minute: ${lostMinute.toString()}")
            binding.textviewTimeLostData.text = "${dogsViewModel.lostHour.value} hour and ${dogsViewModel.lostMinute.value} minutes"
            binding.textviewTimeLostData.visibility = View.VISIBLE
        }
    }

    private fun processReportInputs() {
        if (verifyLostDogData(dogsViewModel.tempPet.value!!)) {
            //clearForm()
            dogsViewModel._readyProcessReport.value = true
        } else {
            invalidDogDataAlert()
        }
        /*
        var processedAge : Int? = null
        if (binding.edittextDogAge.text != null && binding.edittextDogAge.text.toString() != "") {
            try {
                processedAge = binding.edittextDogAge.text.toString().toInt()
            } catch (e: java.lang.NumberFormatException) {
                Log.i("processing dog age", "error converting to number")
            }
        }
        // we know we need to get the type from the edittext

        if (animalType == "Other") {
            val type = binding.edittextOtherType.text.toString()
            if (type != null && type != "") {
                animalType = type
            }
        }
*/
        //if (verifyLostDogData(dogsViewModel.tempPet.value!!)) {
            /*
            val dogRoom = createDogRoom(
                name = binding.edittextDogName.text.toString(),
                animal = animalType,
                breed = binding.edittextDogBreed.text.toString(),
                gender = gender,
                age = processedAge,
                date = lostDate!!,
                hour = lostHour,
                minute = lostMinute,
                note = binding.edittextNotes.text.toString(),
                place = binding.edittextPlaceLost.text.toString(),
                lost = lostOrFound!!,
                found = !lostOrFound!!,
                lat = locationViewModel.lostDogLocationLatLng.value?.latitude,
                lng = locationViewModel.lostDogLocationLatLng.value?.longitude,
                address = locationViewModel.lostDogLocationAddress.value?.get(0))

            // check if imageview is empty
            // if it is not, save the image to cloud storage
            var dataByteArray : ByteArray? = null
            if (binding.previewUpload.drawable != null) {
                Log.i("check image", "image is not null")
                val imageBitmap = getBitmapFromView(binding.previewUpload)
                dataByteArray = convertImageToBytes(imageBitmap)
            }
            coroutineScope.launch {
                if (firebaseClient.handleNewDog(dogRoom, dataByteArray)) {
                    firebaseClient._appState.postValue(AppState.LOST_DOG_REPORT_SENT_SUCCESS)
                } else {
                    firebaseClient._appState.postValue(AppState.LOST_DOG_REPORT_SENT_ERROR) }
            }

            clearForm()
        } else {
            invalidDogDataAlert()
        }
        */
    }

    private fun verifyLostDogData(pet: DogRoom) : Boolean {
        var nameValidity = true
        var dateValidity = true
        var placeValidity = true

        if (dogsViewModel.petName.value == "" && lostOrFound == true) {
            nameValidity = false
            binding.textviewDogName.setTextColor(resources.getColor(R.color.error_red))
        } else {
            binding.textviewDogName.setTextColor(resources.getColor(R.color.black))
        }
        if (dogsViewModel.dateLastSeen.value == "") {
            binding.textviewDateLost.setTextColor(resources.getColor(R.color.error_red))
            dateValidity = false
        } else {
            binding.textviewDateLost.setTextColor(resources.getColor(R.color.black))
        }
        if (dogsViewModel.placeLastSeen.value == "") {
            binding.textviewPlaceLost.setTextColor(resources.getColor(R.color.error_red))
            placeValidity = false
        } else {
            binding.textviewPlaceLost.setTextColor(resources.getColor(R.color.black))
        }

        return nameValidity && dateValidity && placeValidity
    }

    private val selectImageFromGalleryResult =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                // preview
                binding.previewUpload.setImageURI(uri)
                binding.previewUpload.visibility = View.VISIBLE
                dogsViewModel.tempImage = getBitmapFromView(binding.previewUpload)
                dogsViewModel.tempImageByteArray = convertImageToBytes(dogsViewModel.tempImage!!)
            }
        }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            //view.width, view.height, Bitmap.Config.ARGB_8888
            200, 200, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun convertImageToBytes(bitmap: Bitmap) : ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        return baos.toByteArray()
    }

    // we can actually clear all the field by setting a new pet object in layout binding
    private fun clearForm() {
        binding.edittextDogName.text = null
        binding.edittextDogBreed.text = null
        binding.edittextDogAge.text = null
        binding.textviewDateLostData.text = null
        binding.textviewDateLostData.visibility = View.GONE
        binding.textviewTimeLostData.text = null
        binding.textviewTimeLostData.visibility = View.GONE
        binding.edittextPlaceLost.text = null
        binding.previewUpload.visibility = View.GONE
        binding.edittextNotes.text = null
        lostDate = null
        lostHour = null
        lostMinute = null
        //gender = 0
        //animalType.value = null
        setupGenderSpinner()
        setupPetSpinner()
    }

    private fun invalidDogDataAlert() {
        val invalidAlert = AlertDialog.Builder(context)

        with(invalidAlert) {
            setTitle(getString(R.string.missing_data_alert))
            setMessage(getString(R.string.missing_data_alert_desc))
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    dialog.dismiss()
                })
            show()
        }
    }

    private fun invalidDateAlert() {
        val dateAlert = AlertDialog.Builder(context)

        with(dateAlert) {
            setTitle(getString(R.string.lost_dog_report))
            setMessage(getString(R.string.invalid_date_alert_desc))
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    dialog.dismiss()
                })
            show()
        }
    }

    private fun permissionsNeededAlert() {
        val permissionAlert = AlertDialog.Builder(context)

        with(permissionAlert) {
            setTitle(getString(R.string.location_permissions))
            setMessage(getString(R.string.location_permissions_alert_desc))
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    dialog.dismiss()
                })
            show()
        }
    }

    private var permissions =
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

    private val permissionResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var result = true
            permissions.entries.forEach {
                if (!it.value) {
                    Log.e("result launcher", "Permission ${it.key} not granted : ${it.value}")
                    //allPermissionGranted.value = false
                    result = false
                }
            }
            isPermissionGranted = result
        }

    private fun checkPermission() : Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) !=
                PackageManager.PERMISSION_GRANTED) return false
        }
        return true
    }
}
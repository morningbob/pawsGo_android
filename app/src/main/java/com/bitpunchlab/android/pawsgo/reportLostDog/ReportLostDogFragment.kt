package com.bitpunchlab.android.pawsgo.reportLostDog

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.room.InvalidationTracker
import com.bitpunchlab.android.pawsgo.AppState
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.database.PawsGoDatabase
import com.bitpunchlab.android.pawsgo.databinding.FragmentReportLostDogBinding
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModelFactory
import com.bitpunchlab.android.pawsgo.location.LocationViewModel
import com.bitpunchlab.android.pawsgo.modelsRoom.DogRoom
import com.google.android.gms.tasks.Task
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.firebase.database.collection.LLRBNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class ReportLostDogFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var _binding : FragmentReportLostDogBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseClient : FirebaseClientViewModel
    private var timePicker : MaterialTimePicker? = null
    private var datePicker : MaterialDatePicker<Long>? = null
    private var lostDate : String? = null
    private var lostHour : Int? = null
    private var lostMinute : Int? = null
    private var gender : Boolean? = null
    private var allPermissionGranted = MutableLiveData<Boolean>(true)
    private var coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var localDatabase : PawsGoDatabase
    private var lostOrFound : Boolean? = null
    private lateinit var locationViewModel : LocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @OptIn(InternalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReportLostDogBinding.inflate(inflater, container, false)
        firebaseClient = ViewModelProvider(requireActivity(),
            FirebaseClientViewModelFactory(requireActivity().application))
            .get(FirebaseClientViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner
        localDatabase = PawsGoDatabase.getInstance(requireContext())
        locationViewModel = ViewModelProvider(requireActivity())
            .get(LocationViewModel::class.java)

        lostOrFound = requireArguments().getBoolean("lostOrFound")
        if (lostOrFound == null) {
            findNavController().popBackStack()
        }

        binding.locationVM = locationViewModel

        setupLostOrFoundFields()
        setupGenderSpinner()

        binding.buttonChooseDate.setOnClickListener {
            showDatePicker()
        }

        binding.buttonChooseTime.setOnClickListener {
            showTimePicker()
        }

        binding.buttonShowMap!!.setOnClickListener {
            findNavController().navigate(R.id.showMapAction)
        }

        binding.buttonUpload.setOnClickListener {
            selectImageFromGalleryResult.launch("image/*")
        }

        binding.buttonSend.setOnClickListener {
            // display progress bar
            startProgressBar()

            var processedAge : Int? = null
            if (binding.edittextDogAge.text != null && binding.edittextDogAge.text.toString() != "") {
                try {
                    processedAge = binding.edittextDogAge.text.toString().toInt()
                } catch (e: java.lang.NumberFormatException) {
                    Log.i("processing dog age", "error converting to number")
                }
            }

            if (verifyLostDogData(binding.edittextDogName.text.toString(),
                lostDate,
                binding.edittextPlaceLost.text.toString())) {
                val dogRoom = createDogRoom(
                    name = binding.edittextDogName.text.toString(),
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
        }

        firebaseClient.appState.observe(viewLifecycleOwner, appStateObserver)

        if (!checkPermission()) {
            permissionResultLauncher.launch(permissions)
        }

        allPermissionGranted.observe(viewLifecycleOwner, androidx.lifecycle.Observer { value ->
            if (!value) {
        //        permissionsRequiredAlert()
            }
        })

        locationViewModel.lostDogLocationAddress.observe(viewLifecycleOwner, androidx.lifecycle.Observer { address ->
            //binding.edittextPlaceLost.hint = address?.get(0) ?: ""
            locationViewModel.displayAddress.value = address?.get(0) ?: ""
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startProgressBar() {
        binding.progressBarContainer.progressBar.visibility = View.VISIBLE

        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun stopProgressBar() {
        binding.progressBarContainer.progressBar?.visibility = View.GONE
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private val appStateObserver = androidx.lifecycle.Observer<AppState> { appState ->
        when (appState) {
            AppState.LOST_DOG_REPORT_SENT_SUCCESS -> {
                stopProgressBar()
                sentReportSuccessAlert()
                firebaseClient._appState.value = AppState.NORMAL
            }
            AppState.LOST_DOG_REPORT_SENT_ERROR -> {
                stopProgressBar()
                sentReportFailureAlert()
                firebaseClient._appState.value = AppState.NORMAL
            }
            else -> {

            }
        }
    }

    private fun setupLostOrFoundFields() {
        if (lostOrFound == false) {
            binding.artReport.setImageResource(R.drawable.footprint)
            binding.textviewIntro.text = getString(R.string.found_dog_intro)
            binding.textviewDogAge.visibility = View.GONE
            binding.edittextDogAge.visibility = View.GONE
            binding.textviewDateLost.text = "When did you find the dog?"
            binding.textviewPlaceLost.text = "The place the dog is found: "
        }
    }

    private val selectImageFromGalleryResult =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                // preview
                binding.previewUpload.setImageURI(uri)
                binding.previewUpload.visibility = View.VISIBLE
            }
        }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent!!.getItemAtPosition(position) == "Male") {
            gender = true
            Log.i("spinner", "set gender male")
        } else if (parent!!.getItemAtPosition(position) == "Female") {
            gender = false
            Log.i("spinner", "set gender female")
        } else {
            gender = null
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

    private fun showDatePicker() {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
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
            val simpleDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            lostDate = simpleDate.format(data)
            Log.i("got back date", lostDate.toString())
            binding.textviewDateLostData.text = lostDate
            binding.textviewDateLostData.visibility = View.VISIBLE
        }
    }

    private fun showTimePicker() {
        timePicker = MaterialTimePicker
            .Builder()
            .setTitleText(getString(R.string.select_a_time))
            .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
            .build()

        timePicker!!.show(childFragmentManager, "TIME_PICKER")

        timePicker!!.addOnPositiveButtonClickListener {
            lostHour = timePicker!!.hour
            lostMinute = timePicker!!.minute
            Log.i("got time back", "hour: ${lostHour.toString()} minute: ${lostMinute.toString()}")
            binding.textviewTimeLostData.text = "$lostHour hour and $lostMinute minutes"
            binding.textviewTimeLostData.visibility = View.VISIBLE
        }
    }

    private var permissions =
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            )

    private val permissionResultLauncher =
        registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
            //allPermissionGranted = true
            permissions.entries.forEach {
                if (!it.value) {
                    Log.e("result launcher", "Permission ${it.key} not granted : ${it.value}")
                    allPermissionGranted.value = false
                }
            }
        }

    private fun checkPermission() : Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) !=
                PackageManager.PERMISSION_GRANTED) return false
        }
        return true
    }

    private fun verifyLostDogData(name: String?, date: String?, place: String?) : Boolean {
        var nameValidity = true
        var dateValidity = true
        var placeValidity = true
        if (!(name != null && name != "") && lostOrFound == true) {
            nameValidity = false
            binding.textviewDogName.setTextColor(resources.getColor(R.color.error_red))
        } else {
            binding.textviewDogName.setTextColor(resources.getColor(R.color.black))
        }
        if (!(date != null && date != "")) {
            binding.textviewDateLost.setTextColor(resources.getColor(R.color.error_red))
            dateValidity = false
        } else {
            binding.textviewDateLost.setTextColor(resources.getColor(R.color.black))
        }
        if (!(place != null && place != "")) {
            binding.textviewPlaceLost.setTextColor(resources.getColor(R.color.error_red))
            placeValidity = false
        } else {
            binding.textviewPlaceLost.setTextColor(resources.getColor(R.color.black))
        }
        return nameValidity && dateValidity && placeValidity
    }

    private fun createDogRoom(name: String, breed: String?, gender: Boolean?, age: Int?, date: String,
                        hour: Int?, minute: Int?, note: String?, place: String, lost: Boolean, found: Boolean,
                        lat: Double?, lng: Double?, address: String?): DogRoom {
        return DogRoom(dogID = UUID.randomUUID().toString(), dogName = name, dogBreed = breed,
            dogGender = gender, dogAge = age, isLost = lost, isFound = found,
            dateLastSeen = date, hour = hour, minute = minute, notes = note,
            placeLastSeen = place, ownerID = firebaseClient.currentUserID,
            ownerEmail = firebaseClient.currentUserEmail,
            ownerName = firebaseClient.currentUserFirebaseLiveData.value!!.userName,
            locationLat = lat, locationLng = lng,
            locationAddress = address)
    }

    private fun convertLatLngHashmapToDouble() {

    }

    private fun saveDogLocalDatabase(dog: DogRoom) {
        coroutineScope.launch {
            localDatabase.pawsDAO.insertDogs(dog)
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.width, view.height, Bitmap.Config.ARGB_8888
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
        lostDate = null
        lostHour = null
        lostMinute = null
        gender = null
        setupGenderSpinner()
    }

    private fun permissionsRequiredAlert() {
        val permissionAlert = AlertDialog.Builder(context)

        with(permissionAlert) {
            setTitle(getString(R.string.location_permissions_alert))
            setMessage(getString(R.string.location_permissions_alert_desc))
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    dialog.dismiss()
                    //permissionResultLauncher.launch(permissions)

                })
            //setNegativeButton(getString(R.string.cancel),
            //    DialogInterface.OnClickListener { dialog, button ->
            //        dialog.dismiss()
            //    })
            show()
        }
    }

    private fun invalidDogDataAlert() {
        val invalidAlert = AlertDialog.Builder(context)

        with(invalidAlert) {
            setTitle(getString(R.string.missing_data_alert))
            setMessage(getString(R.string.missing_data_alert_desc))
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    dialog.dismiss()
                    //permissionResultLauncher.launch(permissions)

                })
            show()
        }
    }

    private fun sentReportSuccessAlert() {
        val successAlert = AlertDialog.Builder(context)

        with(successAlert) {
            setTitle("Lost Dog Report")
            setMessage("The report was sent to the server successfully.  We'll send an email to you if there is anyone found the dog.")
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    dialog.dismiss()
                })
            show()
        }
    }

    private fun sentReportFailureAlert() {
        val failureAlert = AlertDialog.Builder(context)

        with(failureAlert) {
            setTitle("Lost Dog Report")
            setMessage("There is error in the server.  Please try again later.")
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    dialog.dismiss()
                })
            show()
        }
    }
}
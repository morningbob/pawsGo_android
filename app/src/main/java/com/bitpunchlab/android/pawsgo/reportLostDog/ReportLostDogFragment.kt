package com.bitpunchlab.android.pawsgo.reportLostDog

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.database.PawsGoDatabase
import com.bitpunchlab.android.pawsgo.databinding.FragmentReportLostDogBinding
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModelFactory
import com.bitpunchlab.android.pawsgo.modelsRoom.DogRoom
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.firebase.database.collection.LLRBNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @OptIn(InternalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReportLostDogBinding.inflate(inflater, container, false)
        firebaseClient = ViewModelProvider(requireActivity(), FirebaseClientViewModelFactory(requireActivity()))
            .get(FirebaseClientViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner
        localDatabase = PawsGoDatabase.getInstance(requireContext())

        setupGenderSpinner()

        binding.buttonChooseDate.setOnClickListener {
            showDatePicker()
        }

        binding.buttonChooseTime.setOnClickListener {
            showTimePicker()
        }

        binding.buttonUpload.setOnClickListener {
            selectImageFromGalleryResult.launch("image/*")
        }

        binding.buttonSend.setOnClickListener {
            var processedAge : Int? = null
            if (binding.edittextDogAge.text != null && binding.edittextDogAge.text.toString() == "") {
                try {
                    processedAge = binding.edittextDogAge.text.toString().toInt()
                } catch (e: java.lang.NumberFormatException) {
                    Log.i("processing dog age", "error converting to number")
                }
            }

            if (verifyDogData(binding.edittextDogName.text.toString(),
                lostDate,
                binding.edittextPlaceLost.text.toString())) {
                val dogRoom = createDogRoom(name = binding.edittextDogName.text.toString(),
                breed = binding.edittextDogBreed.text.toString(),
                gender = gender,
                age = processedAge,
                    date = lostDate!!,
                    hour = lostHour,
                    minute = lostMinute,
                    place = binding.edittextPlaceLost.text.toString())
                //saveDogLocalDatabase(dogRoom)
                firebaseClient.handleNewLostDog(dogRoom)
            } else {
                invalidDogDataAlert()
            }
        }

        if (!checkPermission()) {
            permissionResultLauncher.launch(permissions)
        }

        allPermissionGranted.observe(viewLifecycleOwner, androidx.lifecycle.Observer { value ->
            if (!value) {
        //        permissionsRequiredAlert()
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
        calendar[Calendar.YEAR] = 2022
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
            .setTitleText("Select a date")
            .build()
            //.show(childFragmentManager, "DATE_PICKER")

        datePicker!!.show(childFragmentManager, "DATE_PICKER")

        datePicker!!.addOnPositiveButtonClickListener { data ->
            val simpleDate = SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.getDefault())
            lostDate = simpleDate.format(data)
            Log.i("got back date", lostDate.toString())
            binding.textviewDateLostData.text = lostDate
            binding.textviewDateLostData.visibility = View.VISIBLE
        }
    }

    private fun showTimePicker() {
        timePicker = MaterialTimePicker
            .Builder()
            .setTitleText("Select a time")
            .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
            .build()
            //.show(childFragmentManager, "TIME_PICKER")

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

    private fun verifyDogData(name: String?, date: String?, place: String?) : Boolean {
        var nameValidity = true
        var dateValidity = true
        var placeValidity = true
        if (!(name != null && name != "")) {
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
                        hour: Int?, minute: Int?, place: String): DogRoom {
        return DogRoom(dogID = UUID.randomUUID().toString(), dogName = name, dogBreed = breed,
            dogGender = gender, dogAge = age, isLost = true,
            dateLastSeen = date, placeLastSeen = place, ownerID = "", ownerEmail = "")
    }

    private fun saveDogLocalDatabase(dog: DogRoom) {
        coroutineScope.launch {
            localDatabase.pawsDAO.insertDog(dog)
        }
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

}
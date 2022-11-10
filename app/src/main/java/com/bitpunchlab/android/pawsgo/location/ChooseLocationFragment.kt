package com.bitpunchlab.android.pawsgo.location

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bitpunchlab.android.pawsgo.BuildConfig.MAPS_API_KEY
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.databinding.FragmentChooseLocationBinding
import com.bitpunchlab.android.pawsgo.googleMaps.MapFragment
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.util.*


class ChooseLocationFragment : Fragment() {

    private var _binding : FragmentChooseLocationBinding? = null
    private val binding get() = _binding!!
    private lateinit var autoCompleteSupportFragment : AutocompleteSupportFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChooseLocationBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        insertMapFragment()
        setupAutoCompleteFragment()

        binding.buttonSetLocation.setOnClickListener {

        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun insertMapFragment() {
        val mapFragment = MapFragment()
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.map_fragment_container, mapFragment).commit()
    }

    private fun setupAutoCompleteFragment() {
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), MAPS_API_KEY)
        }

        autoCompleteSupportFragment = childFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                as AutocompleteSupportFragment

        autoCompleteSupportFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME,
            Place.Field.LAT_LNG))

        autoCompleteSupportFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(status: Status) {
                Log.i("auto complete fragment setup", "there is error: $status")
            }

            override fun onPlaceSelected(place: Place) {
                Log.i("auto complete fragment setup", "Place: ${place.id} ${place.name}")
            }

        })
    }
}
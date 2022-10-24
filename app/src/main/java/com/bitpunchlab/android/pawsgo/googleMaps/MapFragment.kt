package com.bitpunchlab.android.pawsgo.googleMaps

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bitpunchlab.android.pawsgo.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment


class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var supportMapFragment: SupportMapFragment
    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_map, container, false)

        supportMapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.i("map fragment", "on map ready")
        map = googleMap
    }


}
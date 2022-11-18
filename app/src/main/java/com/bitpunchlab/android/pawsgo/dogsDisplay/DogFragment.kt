package com.bitpunchlab.android.pawsgo.dogsDisplay

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.databinding.FragmentDogBinding
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModelFactory
import com.bitpunchlab.android.pawsgo.modelsRoom.DogRoom


class DogFragment : Fragment() {

    private var _binding : FragmentDogBinding? = null
    private val binding get() = _binding!!
    private var dog : DogRoom? = null
    private lateinit var firebaseClient : FirebaseClientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDogBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        dog = requireArguments().getParcelable("dog")
        if (dog == null) {
            findNavController().popBackStack()
        }
        firebaseClient = ViewModelProvider(requireActivity(),
            FirebaseClientViewModelFactory(requireActivity().application))
            .get(FirebaseClientViewModel::class.java)
        binding.dog = dog

        binding.buttonShowMap.setOnClickListener {
            // firstly, check if there is a point location in dog object
            if (dog!!.locationLat != null && dog!!.locationLng != null) {
                val action = DogFragmentDirections.showLocationAction(dog!!)
                findNavController().navigate(action)
            } else {
                noLocationAlert()
            }
        }

        binding.buttonSendMessage.setOnClickListener {
            val action = DogFragmentDirections.sendMessageAction(dog, null, null)
            findNavController().navigate(action)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun noLocationAlert() {
        val noAlert = AlertDialog.Builder(context)

        with(noAlert) {
            setTitle(getString(R.string.no_location_available_alert))
            setMessage(getString(R.string.no_location_available_alert_desc))
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    // do nothing
                })
            show()
        }
    }
}
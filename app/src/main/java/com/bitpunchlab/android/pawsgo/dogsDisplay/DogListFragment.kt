package com.bitpunchlab.android.pawsgo.dogsDisplay

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.fragment.findNavController

import com.bitpunchlab.android.pawsgo.databinding.FragmentDogListBinding
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModelFactory
import com.bumptech.glide.Glide
import java.net.URI


class DogListFragment : Fragment() {

    private var _binding : FragmentDogListBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseClient : FirebaseClientViewModel
    private lateinit var dogsAdapter : DogsAdapter
    private lateinit var dogsViewModel : DogsViewModel
    private var lostOrFound : Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDogListBinding.inflate(inflater, container, false)
        firebaseClient = ViewModelProvider(requireActivity(),
                FirebaseClientViewModelFactory(requireActivity().application))
            .get(FirebaseClientViewModel::class.java)
        dogsViewModel = ViewModelProvider(requireActivity(),
            DogsViewModelFactory(requireActivity().application))
            .get(DogsViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner
        lostOrFound = requireArguments().getBoolean("lostOrFound")


        dogsAdapter = DogsAdapter( DogOnClickListener { dog ->
            dogsViewModel.onDogChosen(dog)
        },
        MessageClickListener
         { dog ->
            dogsViewModel.onDogMessageClicked(dog)
         })

        binding.dogListRecycler.adapter = dogsAdapter
        if (lostOrFound == null) {
            findNavController().popBackStack()
        } else if (lostOrFound == true) {
            dogsViewModel.lostDogs.observe(viewLifecycleOwner, Observer { dogs ->
                dogs?.let {
                    Log.i("dogsVM lost dogs", dogs.size.toString())
                    dogsAdapter.submitList(dogs)
                    dogsAdapter.notifyDataSetChanged()
                }
            })


        } else if (lostOrFound == false) {
            dogsViewModel.foundDogs.observe(viewLifecycleOwner, Observer { dogs ->
                dogs?.let {
                    dogsAdapter.submitList(dogs)
                    dogsAdapter.notifyDataSetChanged()
                }
            })
        }
        dogsViewModel.chosenDog.observe(viewLifecycleOwner, Observer { dog ->
            dog?.let {
                // navigate to the dog details fragment
                dogsViewModel.finishedDogChosen()
            }
        })

        dogsViewModel.dogMessage.observe(viewLifecycleOwner, Observer { dog ->
            dog?.let {
                // navigate to send message fragment
                val action = DogListFragmentDirections.SendAMessageAction(dog!!)
                findNavController().navigate(action)
                dogsViewModel.finishedDogMessage()
            }
        })
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
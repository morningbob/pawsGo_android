package com.bitpunchlab.android.pawsgo.dogsDisplay

import android.annotation.SuppressLint
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
import com.bitpunchlab.android.pawsgo.R

import com.bitpunchlab.android.pawsgo.databinding.FragmentDogListBinding
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModelFactory
import com.bitpunchlab.android.pawsgo.modelsRoom.DogRoom
import com.bitpunchlab.android.pawsgo.modelsRoom.MessageRoom
import com.bumptech.glide.Glide
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*


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

        setupCorrespondingIconAndTitle()

        dogsViewModel.chosenDog.observe(viewLifecycleOwner, Observer { dog ->
            dog?.let {
                // navigate to the dog details fragment
                val action = DogListFragmentDirections.showDogAction(it)
                findNavController().navigate(action)
                dogsViewModel.finishedDogChosen()
            }
        })

        dogsViewModel.dogMessage.observe(viewLifecycleOwner, Observer { dog ->
            dog?.let {
                // navigate to send message fragment
                val action = DogListFragmentDirections.SendAMessageAction(dog, null, null)
                findNavController().navigate(action)
                dogsViewModel.finishedDogMessage()
            }
        })
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupCorrespondingIconAndTitle() {
        if (lostOrFound == null) {
            findNavController().popBackStack()
        } else if (lostOrFound == true) {
            binding.artDogList.setImageResource(R.drawable.track)
            binding.textviewTitle.text = getString(R.string.lost_dogs)
            binding.textviewIntro.text = getString(R.string.lost_dogs_list_intro)
            dogsViewModel.lostDogs.observe(viewLifecycleOwner, Observer { dogs ->
                dogs?.let {
                    Log.i("dogsVM lost dogs", dogs.size.toString())
                    val orderedDogs = orderByDate(dogs)
                    dogsAdapter.submitList(orderedDogs)
                    dogsAdapter.notifyDataSetChanged()
                }
            })
        } else if (lostOrFound == false) {
            binding.artDogList.setImageResource(R.drawable.laughing)
            binding.textviewTitle.text = getString(R.string.found_dogs)
            binding.textviewIntro.text = getString(R.string.found_dogs_list_intro)
            dogsViewModel.foundDogs.observe(viewLifecycleOwner, Observer { dogs ->
                dogs?.let {
                    val orderedDogs = orderByDate(dogs)
                    dogsAdapter.submitList(orderedDogs)
                    dogsAdapter.notifyDataSetChanged()
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun orderByDate(dogs: List<DogRoom>) : List<DogRoom> {
        return dogs.sortedByDescending { convertToDate(it.dateLastSeen) }
    }

    private fun convertToDate(dateString: String) : Date? {
        val dateFormat = SimpleDateFormat("dd MMM yyyy")

        try {
            //val formatterOut = SimpleDateFormat("dd MMM yyyy  HH:mm:ss")
            return dateFormat.parse(dateString)
        } catch (e: java.lang.Exception) {
            Log.i("orderByDate", "parsing error")
            return null
        }
    }
}
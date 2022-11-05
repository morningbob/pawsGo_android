package com.bitpunchlab.android.pawsgo

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.pawsgo.database.PawsGoDatabase
import com.bitpunchlab.android.pawsgo.databinding.FragmentMainBinding
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch

// in the main fragment, we'll contact firestore for current info,
// including all the lost dogs and found dogs
// so before we actually display these dogs list,
// we already retrieve the data here
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseClient : FirebaseClientViewModel
    private lateinit var localDatabase : PawsGoDatabase
    private var coroutineScope = CoroutineScope(Dispatchers.IO)

    @OptIn(InternalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        firebaseClient = ViewModelProvider(requireActivity(),
            FirebaseClientViewModelFactory(requireActivity()))
            .get(FirebaseClientViewModel::class.java)
        binding.firebaseClient = firebaseClient

        localDatabase = PawsGoDatabase.getInstance(requireContext())



        requireActivity().addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.reportLostDog -> {
                        // navigate to the lost dog form
                        // true represent the Lost Dog form
                        val action = MainFragmentDirections.reportDogAction(true)
                        findNavController().navigate(action)
                        true
                    }
                    R.id.reportFoundDog -> {
                        // navigate to the found dog form
                        // false represent the Found Dog form
                        val action = MainFragmentDirections.reportDogAction(false)
                        findNavController().navigate(action)
                        true
                    }
                    R.id.listLostDogs -> {
                        val action = MainFragmentDirections.viewDogsAction(true)
                        findNavController().navigate(action)
                        true
                    }
                    R.id.logout -> {
                        firebaseClient.logoutUser()
                        true
                    }
                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        firebaseClient.currentUserRoomLiveData.observe(viewLifecycleOwner, Observer { user ->
            binding.user = user
        })

        firebaseClient.appState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                AppState.LOGGED_OUT -> {
                    findNavController().popBackStack()
                }
                else -> {
                    Log.i("login state", "still logged in")
                }
            }
        })

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun retrieveUserRoom() {
        coroutineScope.launch {
            localDatabase.pawsDAO.getUser(firebaseClient.auth.currentUser!!.uid)
        }
    }
}
package com.bitpunchlab.android.pawsgo.userAccount

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.pawsgo.AppState
import com.bitpunchlab.android.pawsgo.LoginInfo
import com.bitpunchlab.android.pawsgo.LoginStatus
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.databinding.FragmentLoginBinding
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseClient : FirebaseClientViewModel
    private var coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        firebaseClient = ViewModelProvider(requireActivity(), FirebaseClientViewModelFactory(requireActivity()))
            .get(FirebaseClientViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.firebaseClient = firebaseClient

        binding.buttonSend.setOnClickListener {
            coroutineScope.launch {
                firebaseClient.loginUserOfAuth()
            }
        }

        binding.buttonCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_createAccountFragment)
        }

        firebaseClient.readyLoginLiveData.observe(viewLifecycleOwner, Observer { value ->
            value?.let {
                if (value) {
                    Log.i("login fragment", "ready login true")
                    binding.buttonSend.visibility = View.VISIBLE
                } else {
                    Log.i("login fragment", "ready login false")
                    binding.buttonSend.visibility = View.INVISIBLE
                }
            }
        })

        firebaseClient.appState.observe(viewLifecycleOwner, Observer { state ->
            if (state == AppState.LOGGED_IN) {
                findNavController().navigate(R.id.action_LoginFragment_to_MainFragment)
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
}
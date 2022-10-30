package com.bitpunchlab.android.pawsgo.userAccount

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.pawsgo.AppState
import com.bitpunchlab.android.pawsgo.LoginInfo
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.databinding.FragmentCreateAccountBinding
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModelFactory


class CreateAccountFragment : Fragment() {

    private var _binding : FragmentCreateAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseClient : FirebaseClientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateAccountBinding.inflate(inflater, container, false)
        firebaseClient = ViewModelProvider(requireActivity(), FirebaseClientViewModelFactory(requireActivity()))
            .get(FirebaseClientViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.firebaseClient = firebaseClient

        binding.buttonSend.setOnClickListener {
            firebaseClient.isCreatingUserAccount = true
            firebaseClient._appState.value = AppState.READY_CREATE_USER_AUTH
        }

        firebaseClient.readyRegisterLiveData.observe(viewLifecycleOwner, Observer { value ->
            value?.let {
                if (value) {
                    // display send button
                    binding.buttonSend.visibility = View.VISIBLE
                } else {
                    binding.buttonSend.visibility = View.INVISIBLE
                }
            }
        })
/*
        LoginInfo.state.observe(viewLifecycleOwner, Observer { appState ->
            when (appState) {
                AppState.ERROR_CREATE_USER_AUTH -> {
                    authErrorAlert()
                }
                AppState.SUCCESS_CREATED_USER_ACCOUNT -> {
                    createSuccessAlert()
                }
                AppState.ERROR_CREATE_USER_ACCOUNT -> {
                    createErrorAlert()
                }
                AppState.LOGGED_IN -> {
                    findNavController().navigate(R.id.action_createAccountFragment_to_MainFragment)
                }
                else -> 0
            }
        })
*/
        firebaseClient.appState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                AppState.LOGGED_IN -> {
                    Log.i("create account", "login state detected")
                    findNavController().navigate(R.id.action_createAccountFragment_to_MainFragment)
                }
                AppState.ERROR_CREATE_USER_AUTH -> {
                    authErrorAlert()
                }
                AppState.SUCCESS_CREATED_USER_ACCOUNT -> {
                    //createSuccessAlert()
                }
                AppState.ERROR_CREATE_USER_ACCOUNT -> {
                    createErrorAlert()
                }
                else -> 0
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun authErrorAlert() {
        val authAlert = AlertDialog.Builder(context)

        with(authAlert) {
            setTitle(getString(R.string.authentication_service_alert))
            setMessage(getString(R.string.authentication_service_alert_desc))
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    // do nothing
                })
            show()
        }
    }

    private fun createErrorAlert() {
        val createAlert = AlertDialog.Builder(context)

        with(createAlert) {
            setTitle(getString(R.string.create_error_alert))
            setMessage(getString(R.string.create_error_alert_desc))
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    // do nothing
                })
            show()
        }
    }

    private fun createSuccessAlert() {
        val successAlert = AlertDialog.Builder(context)

        with(successAlert) {
            setTitle("Create User Account")
            setMessage("Your account was successfully created.")
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    // do nothing
                })
            show()
        }
    }
}
package com.bitpunchlab.android.pawsgo.messages

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.pawsgo.AppState
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.database.PawsGoDatabase
import com.bitpunchlab.android.pawsgo.databinding.FragmentSendMessageBinding
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModelFactory
import com.bitpunchlab.android.pawsgo.modelsRoom.DogRoom
import com.bitpunchlab.android.pawsgo.modelsRoom.MessageRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import java.util.*


class SendMessageFragment : Fragment() {

    private var _binding : FragmentSendMessageBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseClient : FirebaseClientViewModel
    private lateinit var localDatabase: PawsGoDatabase
    private var coroutineScope = CoroutineScope(Dispatchers.IO)
    private var dog : DogRoom? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @OptIn(InternalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSendMessageBinding.inflate(inflater, container, false)
        firebaseClient = ViewModelProvider(requireActivity(),
            FirebaseClientViewModelFactory(requireActivity().application))
            .get(FirebaseClientViewModel::class.java)
        localDatabase = PawsGoDatabase.getInstance(requireContext())
        binding.lifecycleOwner = viewLifecycleOwner
        dog = requireArguments().getParcelable("dog")
        if (dog == null) {
            findNavController().popBackStack()
        }
        binding.dog = dog

        binding.buttonSend.setOnClickListener {
            // save to messages in user room
            // send to messaging collection in firestore
            // create cloud function to send the message
            // cloud function: write to target user's messages field
            // modify firebase client to load and parse messages and update user room
            val message = binding.edittextMessage.text.toString()
            if (message != null && message != "") {
                val messageRoom = createMessageRoom(
                    userEmail = firebaseClient.currentUserFirebaseLiveData.value!!.userEmail,
                    userName = firebaseClient.currentUserFirebaseLiveData.value!!.userName,
                    targetEmail = dog!!.ownerEmail, message = message
                )
                coroutineScope.launch {
                    if (firebaseClient.sendMessageToFirestoreMessaging(messageRoom)) {
                        // if saved to firestore successfully, we save it here
                        saveMessageRoom(messageRoom)
                        // display an alert
                        firebaseClient._appState.postValue(AppState.MESSAGE_SENT_SUCCESS)
                        // clear fields
                        binding.edittextMessage.text = null
                    }
                }
            }
        }

        firebaseClient.appState.observe(viewLifecycleOwner, messageSentObserver)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val messageSentObserver = androidx.lifecycle.Observer<AppState> { appState ->
        when (appState) {
            AppState.MESSAGE_SENT_SUCCESS -> {
                messageSentSuccessAlert()
                firebaseClient._appState.value = AppState.NORMAL
            }
            else -> 0
        }
    }

    private fun createMessageRoom(userEmail: String, userName: String, targetEmail: String, message: String) : MessageRoom {
        return MessageRoom(messageID = UUID.randomUUID().toString(),
            senderEmail = userEmail, senderName = userName, targetEmail = targetEmail,
            messageContent = message,
            date = Date().toString())
    }

    private fun saveMessageRoom(message: MessageRoom) {
        coroutineScope.launch {
            localDatabase.pawsDAO.insertMessages(message)
        }
    }

    private fun messageSentSuccessAlert() {
        val successAlert = AlertDialog.Builder(context)

        with(successAlert) {
            setTitle(getString(R.string.message_sent_alert))
            setMessage(getString(R.string.message_sent_alert_desc))
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    // do nothing
                })
            show()
        }
    }
}
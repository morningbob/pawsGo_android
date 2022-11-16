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
    private var email: String? = null
    private var name: String? = null

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
        if (dog == null && (email == null || name == null)) {
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
                    targetEmail = dog!!.ownerEmail, targetName = dog!!.ownerName,
                    message = message
                )
                coroutineScope.launch {
                    if (firebaseClient.sendMessageToFirestoreMessaging(messageRoom)) {
                        // if saved to firestore successfully, we save it here
                        saveMessageRoom(messageRoom)
                        // display an alert
                        firebaseClient._appState.postValue(AppState.MESSAGE_SENT_SUCCESS)
                        // clear fields
                        binding.edittextMessage.text = null
                    } else {
                        firebaseClient._appState.postValue(AppState.MESSAGE_SENT_ERROR)
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
            AppState.MESSAGE_SENT_ERROR -> {
                messageSentErrorAlert()
                firebaseClient._appState.value = AppState.NORMAL
            }
            else -> 0
        }
    }

    private fun createMessageRoom(userEmail: String, userName: String, targetEmail: String,
                                  targetName: String, message: String) : MessageRoom {
        return MessageRoom(messageID = UUID.randomUUID().toString(),
            senderEmail = userEmail, senderName = userName, targetEmail = targetEmail,
            targetName = targetName, messageContent = message,
            date = Date().toString(),
            userCreatorID = firebaseClient.auth.currentUser!!.uid)
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

    private fun messageSentErrorAlert() {
        val successAlert = AlertDialog.Builder(context)

        with(successAlert) {
            setTitle("Message was not Sent")
            setMessage("There is error in sending the message.  Please make sure you have wifi.  If the error persists, there is probably error in the server.")
            setPositiveButton(getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, button ->
                    // do nothing
                })
            show()
        }
    }
}
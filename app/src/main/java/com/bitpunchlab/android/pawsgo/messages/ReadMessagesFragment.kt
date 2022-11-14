package com.bitpunchlab.android.pawsgo.messages

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.database.PawsGoDatabase
import com.bitpunchlab.android.pawsgo.databinding.FragmentReadMessagesBinding
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModel
import com.bitpunchlab.android.pawsgo.firebase.FirebaseClientViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch

class ReadMessagesFragment : Fragment() {

    private var _binding : FragmentReadMessagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var messagesViewModel : MessagesViewModel
    private lateinit var localDatabase : PawsGoDatabase
    private var coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var firebaseClient : FirebaseClientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @OptIn(InternalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReadMessagesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        messagesViewModel = ViewModelProvider(requireActivity(),
            MessagesViewModelFactory(requireActivity().application))
            .get(MessagesViewModel::class.java)
        localDatabase = PawsGoDatabase.getInstance(requireContext())
        firebaseClient = ViewModelProvider(requireActivity(),
            FirebaseClientViewModelFactory(requireActivity().application))
            .get(FirebaseClientViewModel::class.java)

        // we set the userID in the messagesVM here, to trigger the live data to retrieve the user
        // object, and hence the messages received
        messagesViewModel.userID.value = firebaseClient.currentUserFirebaseLiveData.value!!.userID

        messagesAdapter = MessagesAdapter(MessageOnClickListener { message ->
            messagesViewModel.onMessageClicked(message)
        })
        binding.messagesRecycler.adapter = messagesAdapter
        //coroutineScope.launch {
        //    messagesViewModel.user.value.messagesReceived =
        //        localDatabase.pawsDAO
        //            .getAllMessagesReceived(firebaseClient.currentUserFirebaseLiveData.value!!.userEmail)
        //}

        messagesViewModel.messagesReceived.observe(viewLifecycleOwner, Observer { messages ->
            Log.i("messages view model", "got back user and messages, size: ${messages.size}")
            messages?.let {
                messagesAdapter.submitList(messages)
                messagesAdapter.notifyDataSetChanged()
            }
        })

        messagesViewModel.chosenMessage.observe(viewLifecycleOwner, Observer { message ->
            message?.let {
                // navigate to individual message description
                messagesViewModel.onFinishedMessage()
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.bitpunchlab.android.pawsgo.messages

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.databinding.FragmentReadMessagesBinding

class ReadMessagesFragment : Fragment() {

    var _binding : FragmentReadMessagesBinding? = null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReadMessagesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner



        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //_binding = null
    }
}
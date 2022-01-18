package com.example.capteurenvoiedonnees.ui.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.capteurenvoiedonnees.databinding.FragmentSampleBinding

class SampleFragment: Fragment() {

    private var _binding: FragmentSampleBinding? = null

    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSampleBinding.inflate(inflater, container, false)

        return binding.root
    }
}
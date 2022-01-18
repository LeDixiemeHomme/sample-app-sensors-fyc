package com.example.capteurenvoiedonnees.ui.sample

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.capteurenvoiedonnees.R
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val button: Button = view.findViewById(R.id.button_send)
        button.setOnClickListener {
            Log.d("button", "bouton cliqué !")
        }
    }
}
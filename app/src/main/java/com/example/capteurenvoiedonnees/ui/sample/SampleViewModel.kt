package com.example.capteurenvoiedonnees.ui.sample

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SampleViewModel: ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is sample Fragment"
    }
    val text: LiveData<String> = _text
}
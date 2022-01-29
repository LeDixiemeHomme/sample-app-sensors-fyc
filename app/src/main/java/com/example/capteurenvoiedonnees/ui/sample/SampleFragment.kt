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
import com.example.capteurenvoiedonnees.model.Sample
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.LocalDateTime

class SampleFragment: Fragment() {

    private var _binding: FragmentSampleBinding? = null

    private val binding get() = _binding!!

    private val externalScope: CoroutineScope = GlobalScope
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val httpClient : OkHttpClient = OkHttpClient()
    
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
        val list = mutableListOf<Sample>()
        list.add(
            Sample(localDateTime = LocalDateTime.now().toString(), measurementName = "donnee",
                unit = "m.s", value = 5)
        )
        button.setOnClickListener {
            Log.d("button", "bouton cliqué !")
            sendSamples(list)
        }
    }

    private fun sendSamples(list: List<Sample>) {
        externalScope.launch(defaultDispatcher) {
            val jsonString: String = Gson().toJson(list)
            val request = Request.Builder()
                .header("Content-Type", "application/json")
                .url("https://sleepy-refuge-95334.herokuapp.com/api/v1/elastic/send/samples")
                .post(jsonString.toRequestBody()).build()

            httpClient.newCall(request = request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
            }
        }
    }
}

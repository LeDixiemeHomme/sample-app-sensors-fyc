package com.example.capteurenvoiedonnees.ui.sample

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
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
import kotlin.math.pow

class SampleFragment: Fragment(), SensorEventListener {

    private var _binding: FragmentSampleBinding? = null

    private val binding get() = _binding!!

    private val externalScope: CoroutineScope = GlobalScope
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val httpClient : OkHttpClient = OkHttpClient()

    private var sensorManager: SensorManager? = null

    private val listOfSensorType: List<Int> = listOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_GYROSCOPE
    )

    private var isSendingData: Boolean = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSampleBinding.inflate(inflater, container, false)

        sensorManager = this.activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        for (sensorType in listOfSensorType){
            registerSensorType(sensorType = sensorType)
        }

        return binding.root
    }

    fun registerSensorType(sensorType: Int) {
        sensorManager?.let { sensorManager ->
            val sensor: Sensor? = sensorManager.getDefaultSensor(sensorType)
            if (sensor != null) {
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                Toast.makeText(context, "NO SENSOR FOR $sensorType FOR SENSOR MANAGER", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val button: Button = view.findViewById(R.id.button_send)
        button.text = "Start sending data"
        button.setOnClickListener {
            if (isSendingData) {
                isSendingData = false
                button.text = "Start sending data"
            } else {
                isSendingData = true
                button.text = "Stop sending data"
            }
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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && isSendingData) {
            var prefix = ""
            var unit = ""
            var okToSend = false
            val list = mutableListOf<Sample>()
            if (event.sensor.stringType.equals("android.sensor.accelerometer") ||
                event.sensor.stringType.equals("android.sensor.linear_acceleration")) {
                prefix = event.sensor.stringType
                unit = "m/s²"
                okToSend = true
            }
            if (event.sensor.stringType.equals("android.sensor.gyroscope")) {
                prefix = event.sensor.stringType
                unit = "rad/s"
                okToSend = true
            }
            if (okToSend) {
                val stringLocalDateTime: String = LocalDateTime.now().minusHours(1).toString()
                val xValue = event.values[0]
                val yValue = event.values[1]
                val zValue = event.values[2]
                val amplitude = getAmplitude(xValue = xValue, yValue = yValue, zValue = zValue)
                list.add(Sample(localDateTime = stringLocalDateTime, measurementName = "$prefix-x", unit = unit, value = xValue))
                list.add(Sample(localDateTime = stringLocalDateTime, measurementName = "$prefix-y", unit = unit, value = yValue))
                list.add(Sample(localDateTime = stringLocalDateTime, measurementName = "$prefix-z", unit = unit, value = zValue))
                list.add(Sample(localDateTime = stringLocalDateTime, measurementName = "$prefix-amplitude", unit = unit, value = amplitude))
                sendSamples(list)
            }
        }
    }

    private fun getAmplitude(xValue: Number, yValue: Number, zValue: Number): Double {
        return Math.sqrt(xValue.toDouble().pow(2) + yValue.toDouble().pow(2) + zValue.toDouble().pow(2))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor != null) {
            Log.d("SENSOR ACC", "${sensor.stringType} : accuracy = $accuracy")
        }
    }
}
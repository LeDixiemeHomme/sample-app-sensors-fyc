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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.capteurenvoiedonnees.R
import com.example.capteurenvoiedonnees.databinding.FragmentSampleBinding

class SampleFragment: Fragment(), SensorEventListener {
    private var _binding: FragmentSampleBinding? = null
    private val binding get() = _binding!!

    private var sensorManager: SensorManager? = null

    // liste des capteurs que l'on va utiliser
    private val listOfSensorType: List<Int> = listOf(
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_GYROSCOPE
    )

    private var currentX: TextView? = null
    private var currentY:TextView? = null
    private var currentZ:TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSampleBinding.inflate(inflater, container, false)

        sensorManager = this.activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // enregistre dans le SensorManager les capteurs de notre liste
        for (sensorType in listOfSensorType){
            registerSensorType(sensorType = sensorType)
        }

        return binding.root
    }

    private fun registerSensorType(sensorType: Int) {
        sensorManager?.let { sensorManager ->
            val sensor: Sensor? = sensorManager.getDefaultSensor(sensorType)
            if (sensor != null) {
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                Toast.makeText(context, "NO SENSOR FOUND FOR $sensorType IN SENSOR MANAGER", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentX = _binding?.root?.findViewById<View>(R.id.currentX) as TextView
        currentY = _binding?.root?.findViewById<View>(R.id.currentY) as TextView
        currentZ = _binding?.root?.findViewById<View>(R.id.currentZ) as TextView
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val xValue = event.values[0]
            val yValue = event.values[1]
            val zValue = event.values[2]
            Log.d("SENSOR", "${event.sensor.stringType} : x=$xValue, y=$yValue, z=$zValue")
            if (event.sensor.stringType.equals("android.sensor.linear_acceleration")) {
                currentX?.text = "$xValue"
                currentY?.text = "$yValue"
                currentZ?.text = "$zValue"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor != null) {
            Log.d("SENSOR ACC", "${sensor.stringType} : accuracy = $accuracy")
        }
    }

    override fun onResume() {
        super.onResume()
        for (sensorType in listOfSensorType){
            registerSensorType(sensorType = sensorType)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
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
    private var accelerometer: Sensor? = null
    private var linearAcceleration: Sensor? = null

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

        sensorManager?.let { sensorManager ->
            if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null && sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
                sensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                Toast.makeText(context, "LINEAR ACCELERATION OR ACCELEROMETER SENSORS NOT AVAILABLE", Toast.LENGTH_LONG).show()
            }
        }
        return binding.root
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
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor != null) {
            Log.d("SENSOR ACC", "${sensor.stringType} : accuracy = $accuracy")
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
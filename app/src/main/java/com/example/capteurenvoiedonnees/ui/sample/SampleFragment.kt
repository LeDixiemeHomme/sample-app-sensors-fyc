package com.example.capteurenvoiedonnees.ui.sample

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.capteurenvoiedonnees.R
import com.example.capteurenvoiedonnees.databinding.FragmentSampleBinding
import kotlin.math.pow
import kotlin.math.sqrt


class SampleFragment: Fragment(), SensorEventListener {
    private var _binding: FragmentSampleBinding? = null
    private val binding get() = _binding!!

    private var sensorManager: SensorManager? = null

    // liste des capteurs que l'on va utiliser
    private val listOfSensorType: List<Int> = listOf(
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_GYROSCOPE
    )

    private var lineaX: TextView? = null
    private var lineaY:TextView? = null
    private var lineaZ:TextView? = null

    private var gyroX: TextView? = null
    private var gyroY:TextView? = null
    private var gyroZ:TextView? = null

    private var numberOfSit:TextView? = null
    private var currentMove:TextView? = null

    private var numberOfSitLayout:LinearLayout? = null
    private var numberOfSitUpperLayout:LinearLayout? = null
    private var currentMoveLayout:LinearLayout? = null

    private val neutral: Double = 0.0
    private val rangeNeutral: Double = 0.30

    // les valeurs affichable des mouvements possible
    private var possibleMove: Map<Int, String> = mapOf()

    // les valeurs des seuils minimum et maximum déterminés arbitrairement par observation pour les données générées
    private val thresholds: MutableMap<String, Double> =
        mutableMapOf(
            "minLineaY" to -0.85,
            "maxLineaY" to 1.15,
            "minLineaAmplitude" to 0.0,
            "maxLineaAmplitude" to 0.0,
            "minGyroX" to 0.0,
            "maxGyroX" to 0.0
        )

    // les valeurs des ensembles de valeurs acceptables pour considerer qu'une valeur est egale a un seuil
    private val ranges: MutableMap<String, Double> =
        mutableMapOf(
            "minLineaY" to 0.3,
            "maxLineaY" to 0.25,
            "minLineaAmplitude" to 0.0,
            "maxLineaAmplitude" to 0.0,
            "minGyroX" to 0.0,
            "maxGyroX" to 0.0
        )

    // temps quand le seuil minimum à été atteint, donc le mouvement de s'asseoir à peut etre commencé
    private var timeStartDetectMoveInMillis: Long? = null

    // durée moyenne en millisecondes pendant laquelle le seuil maximal doit être atteint pour considérer que le mouvement est de s'asseoir
    private var durationMinMaxInMillis: Long = 500

    // durée moyenne en millisecondes totale pendant laquelle le seuil neutre doit être atteint pour considérer que le mouvement de s'asseoir est terminé
    private var durationTotalOfSitMove: Long = 1500

    // temps quand le seuil maximum devrait être atteint pour considérer que le mouvement en cours est de s'assoier
    // (il sera égale à timeStartDetectMoveInMillis + durationMinMaxInMillis)
    private var timeToMaxMinInMillis: Long? = null

    // temps quand le seuil neutre devrait être atteint pour considérer que le mouvement de s'assoier est terminé
    // (il sera égale à timeStartDetectMoveInMillis + durationTotalOfSitMove)
    private var timeToEndMoveInMillis: Long? = null

    private var detectMove: Boolean = false
    private var currMoveIsSit: Boolean = false
    private var moveEnd: Boolean = false

    // sauvegarde de la dernière valeur du capteur pour pouvoir avoir une idée de la direction de la courbe.
    private var lastLineaYValue: Double? = null
    private var maxLineaYValue: Double = - 1.0
    private var minLineaYValue: Double = 1.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSampleBinding.inflate(inflater, container, false)

        possibleMove = mapOf(
            1 to getString(R.string.move_top),
            2 to getString(R.string.move_down),
            3 to getString(R.string.move_bottom),
            4 to getString(R.string.move_up),
            5 to getString(R.string.unknown_move),
            6 to getString(R.string.move_right),
            7 to getString(R.string.move_right)
        )

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
        Log.d("DATETIME", "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        lineaX = _binding?.root?.findViewById<View>(R.id.linea_x) as TextView
        lineaY = _binding?.root?.findViewById<View>(R.id.linea_y) as TextView
        lineaZ = _binding?.root?.findViewById<View>(R.id.linea_z) as TextView

        gyroX = _binding?.root?.findViewById<View>(R.id.gyroX) as TextView
        gyroY = _binding?.root?.findViewById<View>(R.id.gyroY) as TextView
        gyroZ = _binding?.root?.findViewById<View>(R.id.gyroZ) as TextView

        numberOfSit = _binding?.root?.findViewById<View>(R.id.number_of_sit) as TextView
        currentMove = _binding?.root?.findViewById<View>(R.id.current_move) as TextView
        numberOfSitLayout = _binding?.root?.findViewById<View>(R.id.number_of_sit_layout) as LinearLayout
        numberOfSitUpperLayout = _binding?.root?.findViewById<View>(R.id.number_of_sit_upper_layout) as LinearLayout
        currentMoveLayout = _binding?.root?.findViewById<View>(R.id.current_move_layout) as LinearLayout

        numberOfSit?.text = "0"
        currentMove?.text = getString(R.string.unknown_move)
        incrementNumberOfSit()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val xValue = event.values[0]
            val yValue = event.values[1]
            val zValue = event.values[2]
            // traitement seulement si les valeurs proviennent de l'acceleromètre
            if (event.sensor.stringType.equals("android.sensor.linear_acceleration")) {
                // assigne les valeurs du capteur aux textView respective
                lineaX?.text = "$xValue"
                lineaY?.text = "$yValue"
                lineaZ?.text = "$zValue"
                val currentTimeInMillis = System.currentTimeMillis()

                if (moveEnd) {
                    changeCurrentMoveValue(3)
                    incrementNumberOfSit()
                    moveEnd = false
                    detectMove = false
                    currMoveIsSit = false
                }

                if (currMoveIsSit && currentTimeInMillis < timeToEndMoveInMillis!! && currentTimeInMillis > timeToMaxMinInMillis!!) {
                    changeCurrentMoveValue(2)
                    moveEnd = yValue.toDouble() in (neutral - rangeNeutral) .. (neutral + rangeNeutral)

                } else if (detectMove && !moveEnd && timeToMaxMinInMillis != null && timeToEndMoveInMillis != null) {
                    if (currentTimeInMillis < timeToEndMoveInMillis!!) {
                        currMoveIsSit = isValueInThresholdRange(valueToTest = yValue.toDouble(), nameOfTheValue = "maxLineaY")
                        if (!currMoveIsSit){
                            changeCurrentMoveValue(4)
                        }
                    } else {
                        changeCurrentMoveValue(1)
                        detectMove = false
                    }
                } else if (!detectMove) {
                    currMoveIsSit = false
                    moveEnd = false
                    detectMove = isValueInThresholdRange(valueToTest = yValue.toDouble(), nameOfTheValue = "minLineaY")
                    if (detectMove) {
                        timeToMaxMinInMillis = currentTimeInMillis + durationMinMaxInMillis
                        timeToEndMoveInMillis = currentTimeInMillis + durationTotalOfSitMove
                    }
                }
            }
            // traitement seulement si les valeurs proviennent du gyroscope
            if (event.sensor.stringType.equals("android.sensor.gyroscope")) {
                // assigne les valeurs du capteur aux textView respective
                gyroX?.text = "$xValue"
                gyroY?.text = "$yValue"
                gyroZ?.text = "$zValue"
            }
        }
    }

    private fun isValueInThresholdRange(valueToTest: Double, nameOfTheValue: String): Boolean {
        val thresholdForValueToTest: Double? = thresholds[nameOfTheValue]
        val rangeForValueToTest: Double? = ranges[nameOfTheValue]
        if (thresholdForValueToTest == null) {
            Toast.makeText(context, "No values in thresholds map for the name: $nameOfTheValue", Toast.LENGTH_LONG).show()
            return false
        }
        if (rangeForValueToTest == null) {
            Toast.makeText(context, "No values in ranges map for the name: $nameOfTheValue", Toast.LENGTH_LONG).show()
            return false
        }
        return valueToTest in (thresholdForValueToTest - rangeForValueToTest) .. (thresholdForValueToTest + rangeForValueToTest)
    }

    // change la valeur du mouvement du layout en fonction du Int passé en parametre
    private fun changeCurrentMoveValue(newCurrentMove: Int) {
        currentMoveLayout?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.white, null))
        currentMove?.text = possibleMove[newCurrentMove]
        Handler(Looper.getMainLooper()).postDelayed({
            currentMoveLayout?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.custom_blue, null))
        }, 500)
    }

    // augmente de 1 la valeur du nombre de position assise du layout
    private fun incrementNumberOfSit() {
        numberOfSitLayout?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.white, null))
        numberOfSitUpperLayout?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.white, null))
        numberOfSit?.text = (numberOfSit?.text.toString().toInt() + 1).toString()
        Handler(Looper.getMainLooper()).postDelayed({
            numberOfSitLayout?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.custom_blue, null))
            numberOfSitUpperLayout?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.custom_blue, null))
        }, 500)
    }

    private fun getAmplitude(xValue: Number, yValue: Number, zValue: Number): Double {
        return sqrt(xValue.toDouble().pow(2) + yValue.toDouble().pow(2) + zValue.toDouble().pow(2))
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
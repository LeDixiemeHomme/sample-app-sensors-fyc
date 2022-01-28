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

    // les valeurs affichable des mouvements possible
    private var possibleMove: Map<Int, String> = mapOf()

    // la valeur neutre qui est censé représenter l'état de non mouvement
    private val neutral: Double = 0.0

    // la valeur des ensembles de valeurs acceptables pour considerer qu'une valeur est neutre
    private val rangeNeutral: Double = 0.31

    // les valeurs des seuils minimum et maximum déterminés arbitrairement par observation pour les données générées
    private val thresholds: MutableMap<String, Double> =
        mutableMapOf(
            "minLineaY" to -0.85,
            "maxLineaY" to 1.0,
            "minGyroX" to -1.10,
            "maxGyroX" to 1.20
        )

    // les valeurs des ensembles de valeurs acceptables pour considerer qu'une valeur est egale a un seuil
    private val ranges: MutableMap<String, Double> =
        mutableMapOf(
            "minLineaY" to 0.3,
            "maxLineaY" to 0.3,
            "minGyroX" to 0.25,
            "maxGyroX" to 0.25
        )

    // durée moyenne en millisecondes pendant laquelle le seuil maximal doit être atteint pour considérer que le mouvement est de s'asseoir
    private var durationMinMaxInMillis: Long = 500

    // durée moyenne en millisecondes totale pendant laquelle le seuil neutre doit être atteint pour considérer que le mouvement de s'asseoir est terminé
    private var durationTotalOfSitMove: Long = 1500

    // temps quand le seuil maximum devrait être atteint pour considérer que le mouvement en cours est de s'asseoir
    // (il sera égale à timeStartDetectMoveInMillis + durationMinMaxInMillis)
    private var timeToMaxMinInMillis: Long? = null

    // temps quand le seuil neutre devrait être atteint pour considérer que le mouvement de s'asseoir est terminé
    // (il sera égale à timeStartDetectMoveInMillis + durationTotalOfSitMove)
    private var timeToEndMoveInMillis: Long? = null

    // représente la détection ou non d'un début de mouvement le plus souvent par le fait que le seuil minLineaY est atteint
    private var detectBeginMove: Boolean = false
    // représente si le mouvement détécté est de s'asseoir le plus souvent par le fait que le seuil maxLineaY est atteint
    private var currMoveIsSit: Boolean = false
    // représente si le mouvement de s'asseoir est fini le plus souvent par le fait que la valeur neutre est atteinte
    private var moveSitEnd: Boolean = false

    // représente la confirmation du mouvement de s'asseoir par le gyroscope
    private var gyroXMinThreshold: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSampleBinding.inflate(inflater, container, false)

        // dictionnaire de tous les mouvements affichable possible
        possibleMove = mapOf(
            1 to getString(R.string.move_top),
            2 to getString(R.string.move_down),
            3 to getString(R.string.move_bottom),
            4 to getString(R.string.move_up),
            5 to getString(R.string.unknown_move)
        )

        sensorManager = this.activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // enregistre dans le SensorManager les capteurs de notre liste
        for (sensorType in listOfSensorType){
            registerSensorType(sensorType = sensorType)
        }

        return binding.root
    }

    // fonction qui enregistre un type de capteur passé en paramètre au sensor manager
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
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val xValue = event.values[0]
            val yValue = event.values[1]
            val zValue = event.values[2]
            // traitement seulement si les valeurs proviennent de l'acceleromètre
            if (event.sensor.stringType.equals("android.sensor.linear_acceleration")) {
                // assigne les valeurs du capteur aux textView respectifs
                lineaX?.text = "$xValue"
                lineaY?.text = "$yValue"
                lineaZ?.text = "$zValue"

                // sauvegarde dans la variable le temps actuel en millisecondes
                val currentTimeInMillis = System.currentTimeMillis()

                // si le mouvement considéré comme s'asseoir est fini et que le seuil minimal du gyroscope à été atteint alors on incremente le textView
                if (moveSitEnd && gyroXMinThreshold) {
                    Log.d("STEPS","Etape 6 position assise détéctée !")
                    changeCurrentMoveValue(3)
                    incrementNumberOfSit()
                    moveSitEnd = false
                    detectBeginMove = false
                    currMoveIsSit = false
                    gyroXMinThreshold = false
                    timeToMaxMinInMillis = null
                    timeToEndMoveInMillis = null
                }

                if (currMoveIsSit && timeToEndMoveInMillis != null && timeToMaxMinInMillis != null
                                && currentTimeInMillis < timeToEndMoveInMillis!! && currentTimeInMillis > timeToMaxMinInMillis!!) {
                    // si le mouvement en cours est confirmé comme étant s'asseoir alors on va regarder s'il se termine avant la fin du temps moyen de ce mouvement
                    changeCurrentMoveValue(2)
                    Log.d("STEPS","Etape 5 test fin de mouvement si lineaY est proche de la valeur de neutral.")
                    // test si le mouvement est proche de la valeur neutre et donc que le mouvement est fini
                    moveSitEnd = yValue.toDouble() in (neutral - rangeNeutral) .. (neutral + rangeNeutral)

                } else if (detectBeginMove && !moveSitEnd && timeToMaxMinInMillis != null && timeToEndMoveInMillis != null) {
                    Log.d("STEPS","Etape 3 acceleromètre test si le temps est écoulé.")
                    if (currentTimeInMillis < timeToEndMoveInMillis!!) {
                        Log.d("STEPS","Etape 4 test si le seuil maximal lineaY est atteint.")
                        // test si le seuil max de lineaX est atteint et donc confirme que le mouvement est s'asseoir
                        currMoveIsSit = isValueInThresholdRange(valueToTest = yValue.toDouble(), nameOfTheValue = "maxLineaY")
                        // si ce n'est pas le cas alors le mouvement est se levé
                        if (!currMoveIsSit){
                            changeCurrentMoveValue(4)
                        }
                    } else {
                        // si le temps moyen du mouvement est passé le mouvement n'est plus considéré comme s'asseoir
                        Log.d("STEPS","Etape restart acceleromètre le temps est écoulé.")
                        changeCurrentMoveValue(1)
                        detectBeginMove = false
                        currMoveIsSit = false
                        timeToMaxMinInMillis = null
                        timeToEndMoveInMillis = null
                    }
                } else if (!detectBeginMove) {
                    currMoveIsSit = false
                    moveSitEnd = false
                    // si un mouvement n'a pas été détécté alors on test la valeur lineaY en cours pour savoir si elle égale au seuil minimal de lineaY
                    Log.d("STEPS","Etape 1 test de la valeur de lineaY pour voir si le seuil minimal à été atteint")
                    detectBeginMove = isValueInThresholdRange(valueToTest = yValue.toDouble(), nameOfTheValue = "minLineaY")
                    if (detectBeginMove) {
                        // si un mouvement est détécté on affecte les valeurs de temps
                        Log.d("STEPS","Etape 2 le seuil minimal de lineaY à été atteint, définition du temps début mouvement.")
                        timeToMaxMinInMillis = currentTimeInMillis + durationMinMaxInMillis
                        timeToEndMoveInMillis = currentTimeInMillis + durationTotalOfSitMove
                    }
                } else if (detectBeginMove && currMoveIsSit && moveSitEnd && currentTimeInMillis > timeToEndMoveInMillis!!) {
                    // si le gyroscope ne valide pas le mouvement et que le temps est dépassé alors on recommence a chercher un mouvement
                    Log.d("STEPS","Etape 6 restart quand moveSitEnd est à true mais que le seuil max de gyroY n'a pas été atteint.")
                    moveSitEnd = false
                    detectBeginMove = false
                    currMoveIsSit = false
                    gyroXMinThreshold = false
                    timeToMaxMinInMillis = null
                    timeToEndMoveInMillis = null
                }
            }
            // traitement seulement si les valeurs proviennent du gyroscope
            if (event.sensor.stringType.equals("android.sensor.gyroscope")) {
                // assigne les valeurs du capteur aux textView respective
                gyroX?.text = "$xValue"
                gyroY?.text = "$yValue"
                gyroZ?.text = "$zValue"

                // sauvegarde dans la variable le temps actuel en millisecondes
                val currentTimeInMillis = System.currentTimeMillis()

                if (detectBeginMove && !gyroXMinThreshold && currentTimeInMillis < timeToEndMoveInMillis!!) {
                    // si on a détécté un mouvement
                    Log.d("STEPS","Etape 3 gyroscope test si le seuil minimal de gyroX à été atteint.")
                    gyroXMinThreshold = isValueInThresholdRange(valueToTest = xValue.toDouble(), nameOfTheValue = "minGyroX")
                    Log.d("STEPS","$gyroXMinThreshold")
                } else if (!detectBeginMove){
                    Log.d("STEPS","Etape restart gyroscope le seuil minimal pour lineaX n'a pas été atteint")
                    gyroXMinThreshold = false
                }
            }
        }
    }

    // test si une valeur passée en paramètre est considérable comme le seuil du nom de la valeur passé en paramètre
    private fun isValueInThresholdRange(valueToTest: Double, nameOfTheValue: String): Boolean {
        // on récupère le seuil du nom de valeur passé en paramètre
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
package io.github.qobiljon.stress.sensors

import android.hardware.SensorEventListener
import android.app.NotificationChannel
import android.app.NotificationManager
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.app.PendingIntent
import android.app.Notification
import android.content.Context
import android.hardware.Sensor
import android.graphics.Color
import android.content.Intent
import android.app.Service
import android.os.IBinder
import android.os.Binder
import android.util.Log

import kotlinx.android.synthetic.main.activity_main.*

import java.util.*
import java.io.File

import io.github.qobiljon.etagent.R
import io.github.qobiljon.stress.MainActivity


class MotionHRService : Service(), SensorEventListener {
    // region vars
    private lateinit var sensorManager: SensorManager
    private val dataFiles: MutableMap<Sensor, File> = mutableMapOf()
    private val samplingRates = mapOf("com.samsung.sensor.hr_raw" to SensorManager.SENSOR_DELAY_FASTEST, Sensor.STRING_TYPE_ACCELEROMETER to SensorManager.SENSOR_DELAY_FASTEST)
    // endregion

    // region binder
    private val mBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        @Suppress("unused")
        val getService: MotionHRService
            get() = this@MotionHRService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }
    // endregion

    override fun onCreate() {
        Log.e(MainActivity.TAG, "DataCollectorService.onCreate()")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // prepare sensor data files
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)

        listOf<Any>("com.samsung.sensor.hr_raw", Sensor.TYPE_ACCELEROMETER).forEach {
            when (it) {
                is String -> allSensors.find { s -> s.stringType.equals(it) }
                else -> sensorManager.getDefaultSensor(it as Int)
            }?.let { sensor ->
                val file = File(filesDir, "${sensor.stringType}.csv")
                if (!file.exists()) {
                    file.createNewFile()
                    file.writeText("timestamp\tvalue\n")
                }
                dataFiles[sensor] = file
            }
        }

        // foreground svc
        val notificationId = 98764
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val notificationChannelId = javaClass.name
        val notificationChannelName = "Motion and HR service"
        val notificationChannel = NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationChannel.lightColor = Color.BLUE
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
        val notification = Notification.Builder(this, notificationChannelId).setContentTitle(getString(R.string.app_name)).setContentText("Collecting motion and HR data...").setSmallIcon(R.mipmap.ic_launcher).setContentIntent(pendingIntent).build()
        startForeground(notificationId, notification)

        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e(MainActivity.TAG, "DataCollectorService.onStartCommand()")

        dataFiles.forEach { sensor -> samplingRates[sensor.key.stringType]?.let { samplingRate -> sensorManager.registerListener(this, sensor.key, samplingRate) } }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.e(MainActivity.TAG, "DataCollectorService.onDestroy()")
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val timestamp: Long = Calendar.getInstance().timeInMillis
        val values: String = event.values.joinToString(",")
        dataFiles[event.sensor]?.appendText("$timestamp\t$values\n")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // TODO("Not yet implemented")
    }
}

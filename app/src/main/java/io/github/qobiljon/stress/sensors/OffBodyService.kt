package io.github.qobiljon.stress.sensors

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import java.io.File

import io.github.qobiljon.etagent.R
import io.github.qobiljon.stress.MainActivity
import java.util.*

class OffBodyService : Service(), SensorEventListener {
    companion object {
        private const val TYPE_OFF_BODY_SENSOR = "com.samsung.sensor.low_power_offbody_detector"
        private const val SAMPLING_RATE = SensorManager.SENSOR_DELAY_NORMAL
    }

    // region vars
    private lateinit var sensorManager: SensorManager
    private lateinit var dataFile: File
    private var sensor: Sensor? = null
    // endregion

    // region binder
    private val mBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        @Suppress("unused")
        val getService: OffBodyService
            get() = this@OffBodyService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }
    // endregion

    override fun onCreate() {
        Log.e(MainActivity.TAG, "OffBodyService.onCreate()")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // prepare sensor data files
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        sensor = allSensors.find { s -> s.stringType.equals(TYPE_OFF_BODY_SENSOR) }
        sensor?.let {
            dataFile = File(filesDir, "${it.stringType}.csv")
            if (!dataFile.exists()) {
                dataFile.createNewFile()
                dataFile.writeText("timestamp\tisOffBody\tvalue\n")
            }
        }

        // foreground svc
        val notificationId = 98765
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val notificationChannelId = javaClass.name
        val notificationChannelName = "On/off-body detection"
        val notificationChannel = NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationChannel.lightColor = Color.BLUE
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
        val notification = Notification.Builder(this, notificationChannelId).setContentTitle(getString(R.string.app_name)).setContentText("Monitoring smartwatch on/off-body events").setSmallIcon(R.mipmap.ic_launcher).setContentIntent(pendingIntent).build()
        startForeground(notificationId, notification)

        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(MainActivity.TAG, "OffBodyService.onStartCommand()")

        if (sensor != null) sensorManager.registerListener(this, sensor, SAMPLING_RATE)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.values.isNotEmpty()) {
            val intent = Intent("off-body-event")
            val isOffBody = event.values[0] != 1.0f

            val timestamp: Long = Calendar.getInstance().timeInMillis
            val values: String = event.values.joinToString(",")
            dataFile.appendText("$timestamp\t$isOffBody\t$values\n")

            if (isOffBody) stopService(Intent(applicationContext, MotionHRService::class.java))
            else startForegroundService(Intent(applicationContext, MotionHRService::class.java))

            intent.putExtra("isOffBody", isOffBody)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // TODO("Not yet implemented")
    }
}

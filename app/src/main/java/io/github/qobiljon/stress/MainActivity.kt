package io.github.qobiljon.stress

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import io.github.qobiljon.etagent.R
import io.github.qobiljon.etagent.databinding.ActivityMainBinding
import io.github.qobiljon.stress.sensors.MotionHRService
import io.github.qobiljon.stress.sensors.OffBodyService
import io.github.qobiljon.stress.sync.DataSubmissionService


class MainActivity : Activity() {
    companion object {
        const val TAG = "EasyTrackAgent"
    }

    // region vars
    private lateinit var binding: ActivityMainBinding
    // endregion

    // region off-body receiver
    private val offBodyEventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val isOffBody = intent.getBooleanExtra("isOffBody", true)

            if (isOffBody) {
                tvOffBody.text = getString(R.string.off_body)
                binding.root.background = getDrawable(R.drawable.orange_circle)
            } else {
                tvOffBody.text = getString(R.string.on_body)
                binding.root.background = getDrawable(R.drawable.green_circle)
            }
        }
    }
    // endregion

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GlobalScope.launch {
            while (true) {
                runOnUiThread {
                    val dateTime = DateTimeFormatter.ofPattern("EE MM.dd, KK:mm a").format(LocalDateTime.now()).split(", ")
                    tvDate.text = dateTime[0]
                    tvTime.text = dateTime[1]
                }
                delay(1000)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // off-body service
        startForegroundService(Intent(applicationContext, OffBodyService::class.java))
        startForegroundService(Intent(applicationContext, DataSubmissionService::class.java))
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(offBodyEventReceiver, IntentFilter("off-body-event"))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(offBodyEventReceiver)
    }
}

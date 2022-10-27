package io.github.qobiljon.stress.sync

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.github.qobiljon.stress.utils.Utils
import kotlin.concurrent.thread

class DataSubmissionService : Service() {
    // region vars
    private lateinit var _thread: Thread
    private var isRunning = false
    // endregion

    // region binder
    private val mBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        @Suppress("unused")
        val getService: DataSubmissionService
            get() = this@DataSubmissionService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }
    // endregion

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        _thread = thread {
            while (isRunning) {
                if (Utils.isOnline(applicationContext)) {

                }

                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        _thread.start()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isRunning = false
        _thread.interrupt()
        _thread.join()
        super.onDestroy()
    }
}

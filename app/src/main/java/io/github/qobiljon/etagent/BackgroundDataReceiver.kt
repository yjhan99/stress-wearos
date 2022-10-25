package io.github.qobiljon.etagent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.health.services.client.data.PassiveMonitoringUpdate

class BackgroundDataReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PassiveMonitoringUpdate.ACTION_DATA) return
        val update = PassiveMonitoringUpdate.fromIntent(intent) ?: return

        val activityInfo = update.userActivityInfoUpdates
        val dataPoints = update.dataPoints

        if (activityInfo.isNotEmpty())
            for (info in activityInfo) {
                Log.e("UserActivity", "userActivityState = ${info.userActivityState.name}")
                Log.e("UserActivity", "stateChangeTime = ${info.stateChangeTime}")
                Log.e("UserActivity", "exerciseType = ${info.exerciseInfo?.exerciseType}")
                Log.e("UserActivity", "exerciseTrackedStatus = ${info.exerciseInfo?.exerciseTrackedStatus?.name}")
            }
        if (dataPoints.isNotEmpty())
            for (data in dataPoints) {
                data.metadata
                Log.e("DataPoint", "value = ${data.value}")
                Log.e("DataPoint", "accuracy = ${data.accuracy}")
            }
    }
}
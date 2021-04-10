package com.chenyue404.activitylog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LogReceiver(val block: (String) -> Unit) : BroadcastReceiver() {
    companion object {
        const val action = BuildConfig.APPLICATION_ID + "LOG_INTENT"
        const val extraKey = "extra_key"
    }

    override fun onReceive(context: Context, intent: Intent) {
        intent.getStringExtra(extraKey)?.let { block(it) }
    }
}
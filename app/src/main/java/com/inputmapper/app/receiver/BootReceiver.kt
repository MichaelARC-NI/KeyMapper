package com.inputmapper.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.inputmapper.app.service.InputMapperService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startService(Intent(context, InputMapperService::class.java))
        }
    }
}

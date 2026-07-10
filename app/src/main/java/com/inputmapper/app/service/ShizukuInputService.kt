package com.inputmapper.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.inputmapper.app.util.ShellExecutor

class ShizukuInputService : Service() {

    override fun onBind(intent: Intent?): IBinder {
        return object : IShizukuInputService.Stub() {
            override fun executeCommand(cmd: String?): String {
                return if (cmd != null) {
                    ShellExecutor.executeRoot(cmd)
                } else {
                    ""
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}

interface IShizukuInputService {
    fun executeCommand(cmd: String?): String
}

package com.inputmapper.app

import android.app.Application
import android.app.NotificationManager
import android.os.Build

class InputMapperApp : Application() {

    companion object {
        const val CHANNEL_ID = "input_mapper_service"
        lateinit var instance: InputMapperApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}

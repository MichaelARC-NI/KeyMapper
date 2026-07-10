package com.inputmapper.app

import android.app.Application

class InputMapperApp : Application() {

    companion object {
        const val CHANNEL_SERVICE = "keymapper_service"
        const val CHANNEL_ALERT = "keymapper_alert"
        lateinit var instance: InputMapperApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}

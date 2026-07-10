package com.inputmapper.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import com.inputmapper.app.InputMapperApp
import com.inputmapper.app.MainActivity
import com.inputmapper.app.R
import com.inputmapper.app.injection.InputInjector
import com.inputmapper.app.injection.RootInjector
import com.inputmapper.app.injection.ShizukuInjector
import com.inputmapper.app.injection.AdbInjector
import com.inputmapper.app.model.InputMode
import com.inputmapper.app.model.KeyMapping
import com.inputmapper.app.model.MouseConfig
import com.inputmapper.app.model.DeviceInfo
import com.inputmapper.app.util.DeviceDiscovery
import java.util.concurrent.CopyOnWriteArrayList

class InputMapperService : Service() {

    companion object {
        private const val TAG = "InputMapperService"
        private const val NOTIFICATION_ID = 1001

        @JvmStatic var isRunning = false
            private set
        @JvmStatic var currentMode: InputMode = InputMode.AUTO
        @JvmStatic var mouseConfig = MouseConfig()
        @JvmStatic var keyMappings = getDefaultKeyMappings()
        @JvmStatic var onLogMessage: ((String) -> Unit)? = null
        @JvmStatic var onDevicesChanged: ((List<DeviceInfo>) -> Unit)? = null
        @JvmStatic var adbHost = ""
        @JvmStatic var adbPort = 0

        private fun getDefaultKeyMappings(): MutableList<KeyMapping> {
            return mutableListOf(
                KeyMapping(KeyEvent.KEYCODE_HOME, "Home", KeyEvent.KEYCODE_HOME, "Home"),
                KeyMapping(KeyEvent.KEYCODE_DEL, "Backspace", KeyEvent.KEYCODE_BACK, "Back"),
                KeyMapping(KeyEvent.KEYCODE_VOLUME_UP, "Vol+", KeyEvent.KEYCODE_VOLUME_UP, "Vol+"),
                KeyMapping(KeyEvent.KEYCODE_VOLUME_DOWN, "Vol-", KeyEvent.KEYCODE_VOLUME_DOWN, "Vol-"),
                KeyMapping(KeyEvent.KEYCODE_ENTER, "Enter", KeyEvent.KEYCODE_ENTER, "Enter"),
                KeyMapping(KeyEvent.KEYCODE_ESCAPE, "Escape", KeyEvent.KEYCODE_BACK, "Back"),
                KeyMapping(KeyEvent.KEYCODE_TAB, "Tab", KeyEvent.KEYCODE_TAB, "Tab"),
                KeyMapping(KeyEvent.KEYCODE_F1, "F1", KeyEvent.KEYCODE_HOME, "Home"),
                KeyMapping(KeyEvent.KEYCODE_F2, "F2", KeyEvent.KEYCODE_BACK, "Back"),
                KeyMapping(KeyEvent.KEYCODE_F3, "F3", KeyEvent.KEYCODE_APP_SWITCH, "Recent")
            )
        }
    }

    private var injector: InputInjector? = null
    private val devices = CopyOnWriteArrayList<DeviceInfo>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        log("Servicio creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        log("Modo: " + currentMode.displayName)
        initializeInjector()
        discoverDevices()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        log("Servicio detenido")
        super.onDestroy()
    }

    private fun initializeInjector() {
        when (currentMode) {
            InputMode.AUTO -> injector = detectBestInjector()
            InputMode.ROOT -> injector = RootInjector()
            InputMode.SHIZUKU -> injector = ShizukuInjector()
            InputMode.ADB -> {
                val host = if (adbHost.isNotEmpty()) adbHost else "127.0.0.1"
                val port = if (adbPort > 0) adbPort else 5555
                injector = AdbInjector(host, port)
            }
            else -> injector = RootInjector()
        }
        if (injector?.isAvailable != true) {
            log("ADVERTENCIA: " + currentMode.displayName + " no disponible")
        } else {
            log("Inyector OK: " + (injector?.mode?.displayName ?: ""))
        }
    }

    private fun detectBestInjector(): InputInjector {
        val root = RootInjector()
        if (root.isAvailable) { log("Root detectado"); return root }
        val shizuku = ShizukuInjector()
        if (shizuku.isAvailable) { log("Shizuku detectado"); return shizuku }
        val adb = AdbInjector()
        if (adb.isAvailable) { log("ADB detectado"); return adb }
        log("Sin metodo de inyeccion disponible")
        return root
    }

    private fun discoverDevices() {
        val found = DeviceDiscovery.discoverDevices()
        devices.clear()
        devices.addAll(found)
        log("Dispositivos: " + devices.size)
        onDevicesChanged?.invoke(devices.toList())
    }

    @Suppress("DEPRECATION")
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this)
            .setContentTitle("KeyMapper activo")
            .setContentText("Mapeando teclado y raton")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        onLogMessage?.invoke(message)
    }
}

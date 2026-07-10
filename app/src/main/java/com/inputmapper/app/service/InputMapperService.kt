package com.inputmapper.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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
        private const val PREFS_NAME = "keymapper_prefs"
        private const val KEY_TARGET_APP = "target_app"
        private const val KEY_TARGET_APP_NAME = "target_app_name"

        @JvmStatic var isRunning = false
            private set
        @JvmStatic var currentMode: InputMode = InputMode.AUTO
        @JvmStatic var mouseConfig = MouseConfig()
        @JvmStatic var keyMappings = getDefaultKeyMappings()
        @JvmStatic var onLogMessage: ((String) -> Unit)? = null
        @JvmStatic var onDevicesChanged: ((List<DeviceInfo>) -> Unit)? = null
        @JvmStatic var adbHost = ""
        @JvmStatic var adbPort = 0
        @JvmStatic var targetAppPackage = ""
        @JvmStatic var targetAppName = ""

        fun saveTargetApp(context: Context, packageName: String, appName: String) {
            targetAppPackage = packageName
            targetAppName = appName
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_TARGET_APP, packageName).putString(KEY_TARGET_APP_NAME, appName).apply()
        }

        fun loadTargetApp(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            targetAppPackage = prefs.getString(KEY_TARGET_APP, "") ?: ""
            targetAppName = prefs.getString(KEY_TARGET_APP_NAME, "") ?: ""
        }

        fun clearTargetApp(context: Context) {
            targetAppPackage = ""
            targetAppName = ""
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_TARGET_APP).remove(KEY_TARGET_APP_NAME).apply()
        }

        fun isTargetAppActive(): Boolean {
            if (targetAppPackage.isEmpty()) return true
            val current = getCurrentForegroundApp()
            return current == targetAppPackage
        }

        private fun getCurrentForegroundApp(): String {
            return try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "dumpsys window | grep mCurrentFocus"))
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                val match = Regex("u0\\s+(\\S+/\\S+)").find(output)
                if (match != null) match.groupValues[1].split("/")[0] else ""
            } catch (e: Exception) { "" }
        }

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
    private var lastForegroundApp = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        loadTargetApp(this)
        log("Servicio creado")
        if (targetAppPackage.isNotEmpty()) log("App objetivo: $targetAppName") else log("Modo: Todas las apps")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        log("Modo: " + currentMode.displayName)
        initializeInjector()
        discoverDevices()
        startForegroundChecker()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        log("Servicio detenido")
        super.onDestroy()
    }

    private fun startForegroundChecker() {
        Thread {
            while (isRunning) {
                try {
                    Thread.sleep(2000)
                    if (targetAppPackage.isNotEmpty()) {
                        val current = getCurrentForegroundApp()
                        if (current != lastForegroundApp) {
                            lastForegroundApp = current
                            if (current == targetAppPackage) log(">> App activa: $targetAppName")
                            else if (current.isNotEmpty()) log(">> En primer plano: $current")
                        }
                    }
                } catch (e: InterruptedException) { break }
                catch (e: Exception) { }
            }
        }.start()
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
        if (injector?.isAvailable != true) log("ADVERTENCIA: " + currentMode.displayName + " no disponible")
        else log("Inyector OK: " + (injector?.mode?.displayName ?: ""))
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
        val appInfo = if (targetAppPackage.isNotEmpty()) " | $targetAppName" else ""
        return Notification.Builder(this)
            .setContentTitle("KeyMapper activo" + appInfo)
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

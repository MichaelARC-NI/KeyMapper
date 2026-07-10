package com.inputmapper.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Point
import android.hardware.input.InputDevice
import android.hardware.input.InputManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.core.app.NotificationCompat
import com.inputmapper.app.InputMapperApp
import com.inputmapper.app.MainActivity
import com.inputmapper.app.R
import com.inputmapper.app.injection.*
import com.inputmapper.app.model.*
import com.inputmapper.app.util.DeviceDiscovery
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

class InputMapperService : Service() {

    companion object {
        private const val TAG = "InputMapperService"
        private const val NOTIFICATION_ID = 1001

        var isRunning = false
            private set
        var currentMode: InputMode = InputMode.AUTO
        var mouseConfig = MouseConfig()
        var keyMappings = getDefaultKeyMappings()
        var onLogMessage: ((String) -> Unit)? = null
        var onDevicesChanged: ((List<DeviceInfo>) -> Unit)? = null

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
                KeyMapping(KeyEvent.KEYCODE_F3, "F3", KeyEvent.KEYCODE_APP_SWITCH, "Recent"),
            )
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var injector: InputInjector? = null
    private val devices = CopyOnWriteArrayList<DeviceInfo>()
    private var screenWidth = 1080
    private var screenHeight = 1920
    private var cursorX = 0f
    private var cursorY = 0f
    private var isCursorVisible = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        log("Servicio creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        log("Servicio iniciado en modo: ${currentMode.displayName}")

        scope.launch {
            initializeInjector()
            discoverDevices()
            startInputLoop()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        log("Servicio detenido")
        super.onDestroy()
    }

    private fun initializeInjector() {
        injector = when (currentMode) {
            InputMode.AUTO -> detectBestInjector()
            InputMode.ROOT -> RootInjector()
            InputMode.SHIZUKU -> ShizukuInjector()
            InputMode.ADB -> AdbInjector()
        }

        if (injector?.isAvailable != true) {
            log("ADVERTENCIA: Inyector ${currentMode.displayName} no disponible")
        } else {
            log("Inyector ${injector?.mode?.displayName} inicializado")
            val (w, h) = injector!!.getScreenSize()
            screenWidth = w
            screenHeight = h
            cursorX = screenWidth / 2f
            cursorY = screenHeight / 2f
            log("Pantalla: ${screenWidth}x${screenHeight}")
        }
    }

    private fun detectBestInjector(): InputInjector {
        // Try Root first
        val root = RootInjector()
        if (root.isAvailable) {
            log("Root detectado - usando modo Root")
            return root
        }

        // Try Shizuku
        val shizuku = ShizukuInjector()
        if (shizuku.isAvailable) {
            log("Shizuku detectado - usando modo Shizuku")
            return shizuku
        }

        // Try ADB
        val adb = AdbInjector()
        if (adb.isAvailable) {
            log("ADB detectado - usando modo ADB inalámbrico")
            return adb
        }

        log("ERROR: Ningún método de inyección disponible")
        log("Instala Shizuku o habilita depuración inalámbrica")
        return root // fallback
    }

    private fun discoverDevices() {
        val found = DeviceDiscovery.discoverDevices()
        devices.clear()
        devices.addAll(found)

        if (devices.isEmpty()) {
            log("No se encontraron dispositivos de entrada")
        } else {
            log("Dispositivos encontrados: ${devices.size}")
            devices.forEach { device ->
                log("  - ${device.name} [${device.type}]")
            }
        }
        onDevicesChanged?.invoke(devices.toList())
    }

    private fun startInputLoop() {
        // Listen for input events from connected USB/BT devices
        scope.launch {
            val inputManager = getSystemService(INPUT_SERVICE) as InputManager
            val deviceIds = inputManager.inputDeviceIds

            log("Escaneando ${deviceIds.length} dispositivos de Android")

            for (id in deviceIds) {
                val device = inputManager.getInputDevice(id) ?: continue
                val sources = device.sources

                if (sources and InputDevice.SOURCE_KEYBOARD != 0 ||
                    sources and InputDevice.SOURCE_MOUSE != 0 ||
                    sources and InputDevice.SOURCE_CLASS_POINTER != 0) {
                    log("Monitorizando: ${device.name} (id=$id)")
                }
            }
        }

        // Periodic device discovery
        scope.launch {
            while (isActive) {
                delay(5000)
                discoverDevices()
            }
        }
    }

    fun handleKeyEvent(keyCode: Int, action: Int) {
        if (action != KeyEvent.ACTION_DOWN) return

        val mapping = keyMappings.find { it.sourceKeyCode == keyCode && it.enabled }
        if (mapping != null) {
            log("Tecla: ${mapping.sourceKeyName} -> ${mapping.targetKeyName}")
            injector?.keyEvent(mapping.targetKeyCode)
        } else {
            log("Tecla sin mapeo: code=$keyCode")
            injector?.keyEvent(keyCode)
        }
    }

    fun handleMouseEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isCursorVisible = true
                cursorX = event.x
                cursorY = event.y
                log("Mouse click en (${"%.0f".format(cursorX)}, ${"%.0f".format(cursorY)})")
                if (mouseConfig.leftClickAsTap) {
                    injector?.tap(cursorX.toInt(), cursorY.toInt())
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - cursorX
                val dy = event.y - cursorY
                cursorX = (cursorX + dx * mouseConfig.sensitivity).coerceIn(0f, screenWidth.toFloat())
                val yMovement = if (mouseConfig.invertY) -dy else dy
                cursorY = (cursorY + yMovement * mouseConfig.sensitivity).coerceIn(0f, screenHeight.toFloat())
            }

            MotionEvent.ACTION_UP -> {
                isCursorVisible = false
            }
        }
    }

    fun handleScroll(deltaY: Int) {
        if (mouseConfig.scrollAsSwipe) {
            val scrollAmount = if (deltaY > 0) 1 else -1
            injector?.scroll(cursorX.toInt(), cursorY.toInt(), scrollAmount, 1)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, InputMapperApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
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

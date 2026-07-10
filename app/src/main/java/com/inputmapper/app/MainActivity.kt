package com.inputmapper.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.input.InputDevice
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.inputmapper.app.model.*
import com.inputmapper.app.service.InputMapperService
import com.inputmapper.app.util.DeviceDiscovery
import com.inputmapper.app.util.ShellExecutor

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvDevices: TextView
    private lateinit var tvLog: TextView
    private lateinit var tvSensitivityValue: TextView
    private lateinit var tvKeyMapStatus: TextView
    private lateinit var svLog: ScrollView
    private lateinit var rgMode: RadioGroup
    private lateinit var seekbarSensitivity: SeekBar
    private lateinit var switchInvertY: SwitchMaterial
    private lateinit var switchLeftClick: SwitchMaterial
    private lateinit var switchRightClick: SwitchMaterial
    private lateinit var switchScroll: SwitchMaterial
    private lateinit var switchKeyMapEnabled: SwitchMaterial
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var btnConfigureKeys: MaterialButton

    private var selectedMode = InputMode.AUTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        requestPermissions()
        setupListeners()
        updateUI()
        checkShizuku()

        // Register log listener
        InputMapperService.onLogMessage = { message ->
            runOnUiThread { appendLog(message) }
        }
        InputMapperService.onDevicesChanged = { devices ->
            runOnUiThread { updateDeviceList(devices) }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        scanInputDevices()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvDevices = findViewById(R.id.tvDevices)
        tvLog = findViewById(R.id.tvLog)
        tvSensitivityValue = findViewById(R.id.tvSensitivityValue)
        tvKeyMapStatus = findViewById(R.id.tvKeyMapStatus)
        svLog = findViewById(R.id.svLog)
        rgMode = findViewById(R.id.rgMode)
        seekbarSensitivity = findViewById(R.id.seekbarSensitivity)
        switchInvertY = findViewById(R.id.switchInvertY)
        switchLeftClick = findViewById(R.id.switchLeftClick)
        switchRightClick = findViewById(R.id.switchRightClick)
        switchScroll = findViewById(R.id.switchScroll)
        switchKeyMapEnabled = findViewById(R.id.switchKeyMapEnabled)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnConfigureKeys = findViewById(R.id.btnConfigureKeys)
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
        }
    }

    private fun setupListeners() {
        // Mode selection
        rgMode.setOnCheckedChangeListener { _, checkedId ->
            selectedMode = when (checkedId) {
                R.id.rbRoot -> InputMode.ROOT
                R.id.rbShizuku -> InputMode.SHIZUKU
                R.id.rbAdb -> InputMode.ADB
                else -> InputMode.AUTO
            }
            InputMapperService.currentMode = selectedMode
            appendLog("Modo seleccionado: ${selectedMode.displayName}")
        }

        // Sensitivity
        seekbarSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sensitivity = progress / 50f // 0.0 to 2.0
                tvSensitivityValue.text = "Sensibilidad: ${progress}%"
                InputMapperService.mouseConfig = InputMapperService.mouseConfig.copy(
                    sensitivity = sensitivity
                )
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Mouse config switches
        switchInvertY.setOnCheckedChangeListener { _, checked ->
            InputMapperService.mouseConfig = InputMapperService.mouseConfig.copy(invertY = checked)
        }
        switchLeftClick.setOnCheckedChangeListener { _, checked ->
            InputMapperService.mouseConfig = InputMapperService.mouseConfig.copy(leftClickAsTap = checked)
        }
        switchRightClick.setOnCheckedChangeListener { _, checked ->
            InputMapperService.mouseConfig = InputMapperService.mouseConfig.copy(rightClickAsBack = checked)
        }
        switchScroll.setOnCheckedChangeListener { _, checked ->
            InputMapperService.mouseConfig = InputMapperService.mouseConfig.copy(scrollAsSwipe = checked)
        }

        // Start/Stop buttons
        btnStart.setOnClickListener {
            InputMapperService.currentMode = selectedMode
            val intent = Intent(this, InputMapperService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            appendLog("Servicio iniciado")
            updateUI()
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, InputMapperService::class.java))
            appendLog("Servicio detenido")
            updateUI()
        }

        // Key mapping config
        btnConfigureKeys.setOnClickListener {
            showKeyMappingDialog()
        }
    }

    private fun showKeyMappingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_key_mapping, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Mapeo de Teclas")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val mappings = mutableListOf<KeyMapping>()

                if ((dialogView.findViewById<CheckBox>(R.id.cbHome)).isChecked)
                    mappings.add(KeyMapping(KeyEvent.KEYCODE_F5, "F5", KeyEvent.KEYCODE_HOME, "Home"))
                if ((dialogView.findViewById<CheckBox>(R.id.cbBack)).isChecked)
                    mappings.add(KeyMapping(KeyEvent.KEYCODE_DEL, "Backspace", KeyEvent.KEYCODE_BACK, "Back"))
                if ((dialogView.findViewById<CheckBox>(R.id.cbVolUp)).isChecked)
                    mappings.add(KeyMapping(KeyEvent.KEYCODE_PAGE_UP, "PageUp", KeyEvent.KEYCODE_VOLUME_UP, "Vol+"))
                if ((dialogView.findViewById<CheckBox>(R.id.cbVolDown)).isChecked)
                    mappings.add(KeyMapping(KeyEvent.KEYCODE_PAGE_DOWN, "PageDown", KeyEvent.KEYCODE_VOLUME_DOWN, "Vol-"))
                if ((dialogView.findViewById<CheckBox>(R.id.cbTab)).isChecked)
                    mappings.add(KeyMapping(KeyEvent.KEYCODE_TAB, "Tab", KeyEvent.KEYCODE_TAB, "Tab"))
                if ((dialogView.findViewById<CheckBox>(R.id.cbEnter)).isChecked)
                    mappings.add(KeyMapping(KeyEvent.KEYCODE_ENTER, "Enter", KeyEvent.KEYCODE_ENTER, "Enter"))
                if ((dialogView.findViewById<CheckBox>(R.id.cbEscape)).isChecked)
                    mappings.add(KeyMapping(KeyEvent.KEYCODE_ESCAPE, "Escape", KeyEvent.KEYCODE_BACK, "Back"))
                if ((dialogView.findViewById<CheckBox>(R.id.cbF1)).isChecked)
                    mappings.add(KeyMapping(KeyEvent.KEYCODE_F1, "F1", KeyEvent.KEYCODE_HOME, "Home"))
                if ((dialogView.findViewById<CheckBox>(R.id.cbF2)).isChecked)
                    mappings.add(KeyMapping(KeyEvent.KEYCODE_F2, "F2", KeyEvent.KEYCODE_BACK, "Back"))
                if ((dialogView.findViewById<CheckBox>(R.id.cbF3)).isChecked)
                    mappings.add(KeyMapping(KeyEvent.KEYCODE_F3, "F3", KeyEvent.KEYCODE_APP_SWITCH, "Recent"))

                InputMapperService.keyMappings.clear()
                InputMapperService.keyMappings.addAll(mappings)
                updateKeyMapStatus()
                appendLog("Mapeo guardado: ${mappings.size} teclas")
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.show()
    }

    private fun updateKeyMapStatus() {
        val active = InputMapperService.keyMappings.filter { it.enabled }
        val names = active.joinToString(", ") { "${it.sourceKeyName}->${it.targetKeyName}" }
        tvKeyMapStatus.text = "Teclas mapeadas: $names"
    }

    private fun scanInputDevices() {
        val inputManager = getSystemService(INPUT_SERVICE) as InputManager
        val deviceIds = inputManager.inputDeviceIds
        val deviceInfo = mutableListOf<String>()

        for (id in deviceIds) {
            val device = inputManager.getInputDevice(id) ?: continue
            if (id == InputDevice.KEYBOARD_BUILTIN) continue

            val sources = device.sources
            val type = when {
                sources and InputDevice.SOURCE_KEYBOARD != 0 -> "Teclado"
                sources and InputDevice.SOURCE_MOUSE != 0 -> "Ratón"
                sources and InputDevice.SOURCE_CLASS_POINTER != 0 -> "Pointer"
                else -> "Otro"
            }

            deviceInfo.add("${device.name} [$type]")
        }

        if (deviceInfo.isEmpty()) {
            tvDevices.text = "No hay dispositivos externos conectados\n" +
                    "Conecta un teclado o ratón USB/Bluetooth"
        } else {
            tvDevices.text = deviceInfo.joinToString("\n") { "• $it" }
        }
    }

    private fun updateDeviceList(devices: List<DeviceInfo>) {
        if (devices.isEmpty()) {
            tvDevices.text = "No hay dispositivos detectados en /dev/input/"
        } else {
            val sb = StringBuilder()
            devices.forEach { device ->
                sb.appendLine("${device.name} [${device.type}] → ${device.path}")
            }
            tvDevices.text = sb.toString()
        }
    }

    private fun updateUI() {
        val running = InputMapperService.isRunning
        tvStatus.text = if (running) getString(R.string.status_running) else getString(R.string.status_stopped)
        tvStatus.setTextColor(ContextCompat.getColor(this,
            if (running) R.color.status_running else R.color.status_stopped))
        btnStart.isEnabled = !running
        btnStop.isEnabled = running
        updateKeyMapStatus()
    }

    private fun appendLog(message: String) {
        tvLog.append("$message\n")
        svLog.post { svLog.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (InputMapperService.isRunning) {
            InputMapperService.instance?.handleKeyEvent(event.keyCode, event.action)
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (InputMapperService.isRunning && event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
            InputMapperService.instance?.handleMouseEvent(event)
            return true
        }
        if (InputMapperService.isRunning && event.actionMasked == MotionEvent.ACTION_SCROLL) {
            val scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            InputMapperService.instance?.handleScroll(scrollY.toInt())
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun checkShizuku() {
        try {
            rikka.shizuku.Shizuku.addBinderReceivedListener(binderReceivedListener)
            rikka.shizuku.Shizuku.addBinderDeadListener(binderDeadListener)
            rikka.shizuku.Shizuku.addRequestPermissionResultListener(permissionResultListener)

            if (rikka.shizuku.Shizuku.pingBinder()) {
                appendLog("Shizuku binder disponible")
                if (rikka.shizuku.Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    rikka.shizuku.Shizuku.requestPermission(101)
                }
            }
        } catch (e: Exception) {
            appendLog("Shizuku no disponible: ${e.message}")
        }
    }

    private val binderReceivedListener = rikka.shizuku.Shizuku.OnBinderReceivedListener {
        runOnUiThread { appendLog("Shizuku conectado") }
    }

    private val binderDeadListener = rikka.shizuku.Shizuku.OnBinderDeadListener {
        runOnUiThread { appendLog("Shizuku desconectado") }
    }

    private val permissionResultListener =
        rikka.shizuku.Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            runOnUiThread {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    appendLog("Permiso Shizuku concedido")
                } else {
                    appendLog("Permiso Shizuku denegado")
                }
            }
        }

    override fun onDestroy() {
        try {
            rikka.shizuku.Shizuku.removeBinderReceivedListener(binderReceivedListener)
            rikka.shizuku.Shizuku.removeBinderDeadListener(binderDeadListener)
            rikka.shizuku.Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) { }
        super.onDestroy()
    }
}

// Extension to access service instance from Activity
private val InputMapperService.Companion.instance: InputMapperService?
    get() {
        return try {
            val field = InputMapperService::class.java.getDeclaredField("isRunning")
            if (InputMapperService.isRunning) {
                // The service is running, we need a reference
                null // Will use the static methods instead
            } else null
        } catch (e: Exception) { null }
    }

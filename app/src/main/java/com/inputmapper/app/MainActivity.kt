package com.inputmapper.app

import android.Manifest
import android.content.Intent
import com.inputmapper.app.model.InputMode
import com.inputmapper.app.model.KeyMapping
import com.inputmapper.app.model.DeviceInfo
import com.inputmapper.app.service.InputMapperService
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.app.AlertDialog
import android.app.Activity
import android.widget.*

class MainActivity : Activity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvDevices: TextView
    private lateinit var tvLog: TextView
    private lateinit var tvSensitivityValue: TextView
    private lateinit var tvKeyMapStatus: TextView
    private lateinit var svLog: ScrollView
    private lateinit var rgMode: RadioGroup
    private lateinit var seekbarSensitivity: SeekBar
    private lateinit var switchInvertY: CheckBox
    private lateinit var switchLeftClick: CheckBox
    private lateinit var switchRightClick: CheckBox
    private lateinit var switchScroll: CheckBox
    private lateinit var switchKeyMapEnabled: CheckBox
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnConfigureKeys: Button

    private var selectedMode = InputMode.AUTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        updateUI()

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
        tvStatus = findViewById(R.id.tvStatus) as TextView
        tvDevices = findViewById(R.id.tvDevices) as TextView
        tvLog = findViewById(R.id.tvLog) as TextView
        tvSensitivityValue = findViewById(R.id.tvSensitivityValue) as TextView
        tvKeyMapStatus = findViewById(R.id.tvKeyMapStatus) as TextView
        svLog = findViewById(R.id.svLog) as ScrollView
        rgMode = findViewById(R.id.rgMode) as RadioGroup
        seekbarSensitivity = findViewById(R.id.seekbarSensitivity) as SeekBar
        switchInvertY = findViewById(R.id.switchInvertY) as CheckBox
        switchLeftClick = findViewById(R.id.switchLeftClick) as CheckBox
        switchRightClick = findViewById(R.id.switchRightClick) as CheckBox
        switchScroll = findViewById(R.id.switchScroll) as CheckBox
        switchKeyMapEnabled = findViewById(R.id.switchKeyMapEnabled) as CheckBox
        btnStart = findViewById(R.id.btnStart) as Button
        btnStop = findViewById(R.id.btnStop) as Button
        btnConfigureKeys = findViewById(R.id.btnConfigureKeys) as Button
    }

    private fun setupListeners() {
        rgMode.setOnCheckedChangeListener { _, checkedId ->
            selectedMode = when (checkedId) {
                R.id.rbRoot -> InputMode.ROOT
                R.id.rbShizuku -> InputMode.SHIZUKU
                R.id.rbAdb -> InputMode.ADB
                else -> InputMode.AUTO
            }
            InputMapperService.currentMode = selectedMode
            appendLog("Modo: " + selectedMode.displayName)
        }

        seekbarSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sensitivity = progress / 50f
                tvSensitivityValue.text = "Sensibilidad: " + progress + "%"
                InputMapperService.mouseConfig = InputMapperService.mouseConfig.copy(sensitivity = sensitivity)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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

        btnStart.setOnClickListener {
            InputMapperService.currentMode = selectedMode
            val intent = Intent(this, InputMapperService::class.java)
            startService(intent)
            appendLog("Servicio iniciado")
            updateUI()
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, InputMapperService::class.java))
            appendLog("Servicio detenido")
            updateUI()
        }

        btnConfigureKeys.setOnClickListener { showKeyMappingDialog() }
    }

    private fun showKeyMappingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_key_mapping, null)
        AlertDialog.Builder(this)
            .setTitle("Mapeo de Teclas")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val mappings = mutableListOf<KeyMapping>()

                val cbHome = dialogView.findViewById(R.id.cbHome) as CheckBox
                val cbBack = dialogView.findViewById(R.id.cbBack) as CheckBox
                val cbVolUp = dialogView.findViewById(R.id.cbVolUp) as CheckBox
                val cbVolDown = dialogView.findViewById(R.id.cbVolDown) as CheckBox
                val cbTab = dialogView.findViewById(R.id.cbTab) as CheckBox
                val cbEnter = dialogView.findViewById(R.id.cbEnter) as CheckBox
                val cbEscape = dialogView.findViewById(R.id.cbEscape) as CheckBox
                val cbSpace = dialogView.findViewById(R.id.cbSpace) as CheckBox
                val cbF1 = dialogView.findViewById(R.id.cbF1) as CheckBox
                val cbF2 = dialogView.findViewById(R.id.cbF2) as CheckBox
                val cbF3 = dialogView.findViewById(R.id.cbF3) as CheckBox

                if (cbHome.isChecked) mappings.add(KeyMapping(0, "F5", KeyEvent.KEYCODE_HOME, "Home"))
                if (cbBack.isChecked) mappings.add(KeyMapping(KeyEvent.KEYCODE_DEL, "Backspace", KeyEvent.KEYCODE_BACK, "Back"))
                if (cbVolUp.isChecked) mappings.add(KeyMapping(KeyEvent.KEYCODE_PAGE_UP, "PageUp", KeyEvent.KEYCODE_VOLUME_UP, "Vol+"))
                if (cbVolDown.isChecked) mappings.add(KeyMapping(KeyEvent.KEYCODE_PAGE_DOWN, "PageDown", KeyEvent.KEYCODE_VOLUME_DOWN, "Vol-"))
                if (cbTab.isChecked) mappings.add(KeyMapping(KeyEvent.KEYCODE_TAB, "Tab", KeyEvent.KEYCODE_TAB, "Tab"))
                if (cbEnter.isChecked) mappings.add(KeyMapping(KeyEvent.KEYCODE_ENTER, "Enter", KeyEvent.KEYCODE_ENTER, "Enter"))
                if (cbEscape.isChecked) mappings.add(KeyMapping(KeyEvent.KEYCODE_ESCAPE, "Escape", KeyEvent.KEYCODE_BACK, "Back"))
                if (cbF1.isChecked) mappings.add(KeyMapping(KeyEvent.KEYCODE_F1, "F1", KeyEvent.KEYCODE_HOME, "Home"))
                if (cbF2.isChecked) mappings.add(KeyMapping(KeyEvent.KEYCODE_F2, "F2", KeyEvent.KEYCODE_BACK, "Back"))
                if (cbF3.isChecked) mappings.add(KeyMapping(KeyEvent.KEYCODE_F3, "F3", KeyEvent.KEYCODE_APP_SWITCH, "Recent"))

                InputMapperService.keyMappings.clear()
                InputMapperService.keyMappings.addAll(mappings)
                updateKeyMapStatus()
                appendLog("Mapeo: " + mappings.size + " teclas")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateKeyMapStatus() {
        val active = InputMapperService.keyMappings.filter { it.enabled }
        val names = active.joinToString(", ") { it.sourceKeyName + "->" + it.targetKeyName }
        tvKeyMapStatus.text = "Teclas: $names"
    }

    private fun scanInputDevices() {
        tvDevices.text = "Buscando dispositivos via /proc/bus/input/devices..."
        Thread {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "cat /proc/bus/input/devices"))
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                val devices = mutableListOf<String>()
                val blocks = output.split("\n\n")
                for (block in blocks) {
                    val nameMatch = Regex("""Name="(.+?)"""").find(block)
                    val handlersMatch = Regex("Handlers=(.+)").find(block)
                    if (nameMatch != null && handlersMatch != null && handlersMatch.value.contains("event")) {
                        devices.add(nameMatch.groupValues[1])
                    }
                }
                runOnUiThread {
                    tvDevices.text = if (devices.isEmpty()) "No hay dispositivos externos" 
                    else devices.joinToString("\n") { "  * " + it }
                }
            } catch (e: Exception) {
                runOnUiThread { tvDevices.text = "Error descubriendo dispositivos" }
            }
        }.start()
    }

    private fun updateDeviceList(devices: List<DeviceInfo>) {
        tvDevices.text = if (devices.isEmpty()) "No hay dispositivos detectados" else devices.joinToString("\n") { "  * " + it.name + " [" + it.type + "]" }
    }

    private fun updateUI() {
        val running = InputMapperService.isRunning
        tvStatus.text = if (running) "Servicio ACTIVO" else "Servicio DETENIDO"
        tvStatus.setTextColor(if (running) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
        btnStart.isEnabled = !running
        btnStop.isEnabled = running
        updateKeyMapStatus()
    }

    private fun appendLog(message: String) {
        tvLog.append(message + "\n")
        svLog.post { svLog.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (InputMapperService.isRunning && event.action == KeyEvent.ACTION_DOWN) {
            val mapping = InputMapperService.keyMappings.find { it.sourceKeyCode == event.keyCode && it.enabled }
            if (mapping != null) {
                appendLog("Key: " + mapping.sourceKeyName + " -> " + mapping.targetKeyName)
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (InputMapperService.isRunning && event.actionMasked == MotionEvent.ACTION_MOVE) {
            appendLog("Mouse move")
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
}

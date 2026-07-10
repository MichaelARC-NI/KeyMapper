package com.inputmapper.app

import android.content.Intent
import android.content.pm.ResolveInfo
import com.inputmapper.app.model.InputMode
import com.inputmapper.app.model.KeyMapping
import com.inputmapper.app.model.DeviceInfo
import com.inputmapper.app.service.InputMapperService
import android.app.NotificationManager
import android.app.PendingIntent
import android.net.Uri
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
    private lateinit var btnConfigureAdb: Button
    private lateinit var tvAdbStatus: TextView
    private lateinit var tvSelectedApp: TextView
    private lateinit var btnSelectApp: Button
    private lateinit var btnClearApp: Button

    private var selectedMode = InputMode.AUTO
    private var adbHost = ""
    private var adbPort = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        InputMapperService.loadTargetApp(this)
        initViews()
        setupListeners()
        setupSocialButtons()
        updateUI()
        updateAppSelectorUI()
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
        btnConfigureAdb = findViewById(R.id.btnConfigureAdb) as Button
        tvAdbStatus = findViewById(R.id.tvAdbStatus) as TextView
        tvSelectedApp = findViewById(R.id.tvSelectedApp) as TextView
        btnSelectApp = findViewById(R.id.btnSelectApp) as Button
        btnClearApp = findViewById(R.id.btnClearApp) as Button
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
            btnConfigureAdb.visibility = if (selectedMode == InputMode.ADB) View.VISIBLE else View.GONE
            tvAdbStatus.visibility = if (selectedMode == InputMode.ADB) View.VISIBLE else View.GONE
            updateAdbStatusText()
            appendLog("Modo: " + selectedMode.displayName)
        }

        btnConfigureAdb.setOnClickListener { showAdbConnectDialog() }

        btnSelectApp.setOnClickListener { showAppSelectorDialog() }

        btnClearApp.setOnClickListener {
            InputMapperService.clearTargetApp(this)
            updateAppSelectorUI()
            appendLog("App objetivo: Todas las apps")
        }

        seekbarSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sensitivity = progress / 50f
                tvSensitivityValue.text = progress.toString() + "%"
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
            if (selectedMode == InputMode.ADB && adbHost.isNotEmpty() && adbPort > 0) {
                InputMapperService.adbHost = adbHost
                InputMapperService.adbPort = adbPort
            }
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

    private fun setupSocialButtons() {
        findViewById(R.id.btnFacebook).setOnClickListener {
            openUrl("https://www.facebook.com/share/1EhxmtiyQN/")
        }
        findViewById(R.id.btnWhatsapp).setOnClickListener {
            openUrl("https://wa.me/50583341349")
        }
        findViewById(R.id.btnTelegram).setOnClickListener {
            openUrl("https://t.me/Michael_Antonio_Rodriguez")
        }
        findViewById(R.id.btnYoutube).setOnClickListener {
            openUrl("https://youtube.com/@androidmovil")
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            appendLog("Error abriendo URL")
        }
    }

    private fun showHeadsUpNotification(title: String, text: String, bigText: String) {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            @Suppress("DEPRECATION")
            val builder = android.app.Notification.Builder(this)

            val notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(android.app.Notification.BigTextStyle().bigText(bigText))
                .setPriority(android.app.Notification.PRIORITY_HIGH)
                .setDefaults(android.app.Notification.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setFullScreenIntent(pendingIntent, true)
                .build()

            nm.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            appendLog("Error mostrando notificacion")
        }
    }

    // ==================== App Selector ====================

    private fun showAppSelectorDialog() {
        tvSelectedApp.text = "Cargando apps..."
        btnSelectApp.isEnabled = false

        Thread {
            val apps = getInstalledLaunchableApps()
            runOnUiThread {
                btnSelectApp.isEnabled = true
                if (apps.isEmpty()) {
                    tvSelectedApp.text = "No se encontraron apps"
                    return@runOnUiThread
                }

                val appNames = apps.map { it.label }.toTypedArray()
                val currentTarget = InputMapperService.targetAppPackage

                val checkedIndex = if (currentTarget.isNotEmpty()) {
                    apps.indexOfFirst { it.packageName == currentTarget }
                } else {
                    -1
                }

                AlertDialog.Builder(this)
                    .setTitle("Seleccionar App Objetivo")
                    .setSingleChoiceItems(appNames, checkedIndex) { dialog, which ->
                        val selected = apps[which]
                        InputMapperService.saveTargetApp(this, selected.packageName, selected.label)
                        updateAppSelectorUI()
                        appendLog("App objetivo: " + selected.label)
                        dialog.dismiss()
                    }
                    .setNeutralButton("Todas las apps") { _, _ ->
                        InputMapperService.clearTargetApp(this)
                        updateAppSelectorUI()
                        appendLog("App objetivo: Todas las apps")
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }.start()
    }

    private fun getInstalledLaunchableApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)

        val apps = mutableListOf<AppInfo>()
        for (ri in resolveInfos) {
            val label = ri.loadLabel(pm).toString()
            val pkg = ri.activityInfo.packageName
            // Skip our own app
            if (pkg == packageName) continue
            apps.add(AppInfo(label, pkg, ri))
        }
        apps.sortBy { it.label.toLowerCase() }
        return apps
    }

    data class AppInfo(val label: String, val packageName: String, val resolveInfo: ResolveInfo)

    private fun updateAppSelectorUI() {
        val pkg = InputMapperService.targetAppPackage
        val name = InputMapperService.targetAppName
        if (pkg.isNotEmpty() && name.isNotEmpty()) {
            tvSelectedApp.text = name
            tvSelectedApp.setTextColor(0xFF1565C0.toInt())
            btnClearApp.visibility = View.VISIBLE
        } else {
            tvSelectedApp.text = "Todas las apps"
            tvSelectedApp.setTextColor(0xFF757575.toInt())
            btnClearApp.visibility = View.GONE
        }
    }

    // ==================== ADB Connection Dialog ====================

    private fun showAdbConnectDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_adb_connect, null)
        val etPairIp = dialogView.findViewById(R.id.etPairIp) as EditText
        val etPairPort = dialogView.findViewById(R.id.etPairPort) as EditText
        val etPairCode = dialogView.findViewById(R.id.etPairCode) as EditText
        val btnPair = dialogView.findViewById(R.id.btnPair) as Button
        val tvPairStatus = dialogView.findViewById(R.id.tvPairStatus) as TextView
        val etConnectIp = dialogView.findViewById(R.id.etConnectIp) as EditText
        val etConnectPort = dialogView.findViewById(R.id.etConnectPort) as EditText
        val btnConnect = dialogView.findViewById(R.id.btnConnect) as Button
        val tvResult = dialogView.findViewById(R.id.tvResult) as TextView

        if (adbHost.isNotEmpty()) {
            etPairIp.setText(adbHost)
            etConnectIp.setText(adbHost)
        }
        if (adbPort > 0) {
            etPairPort.setText(adbPort.toString())
            etConnectPort.setText(adbPort.toString())
        }

        btnPair.setOnClickListener {
            val ip = etPairIp.text.toString().trim()
            val port = etPairPort.text.toString().trim()
            val code = etPairCode.text.toString().trim()
            if (ip.isEmpty() || port.isEmpty() || code.isEmpty()) {
                tvResult.setTextColor(0xFFF44336.toInt())
                tvResult.text = "Completa IP, puerto y codigo"
                return@setOnClickListener
            }
            tvResult.setTextColor(0xFF757575.toInt())
            tvResult.text = "Emparejando $ip:$port..."
            btnPair.isEnabled = false
            showHeadsUpNotification(
                "Emparejando ADB",
                "IP: $ip  Puerto: $port  Codigo: $code",
                "Enviando comando: adb pair $ip:$port\nCodigo: $code\n\nNo cierres esta pantalla hasta ver el resultado."
            )
            Thread {
                try {
                    val cmd = "adb pair $ip:$port $code"
                    val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                    val output = process.inputStream.bufferedReader().readText()
                    val errOutput = process.errorStream.bufferedReader().readText()
                    process.waitFor()
                    runOnUiThread {
                        btnPair.isEnabled = true
                        if (output.contains("Successfully") || output.contains("ok") || output.length < 5) {
                            tvResult.setTextColor(0xFF4CAF50.toInt())
                            tvResult.text = "Emparejado OK!"
                            etConnectIp.setText(ip)
                            etConnectPort.setText(port)
                            adbHost = ip
                            adbPort = port.toIntOrNull() ?: 0
                            tvPairStatus.text = "Emparejado con $ip:$port"
                            showHeadsUpNotification(
                                "ADB Emparejado!",
                                "Puerto: $port",
                                "Conexion ADB emparejada con $ip:$port\nAhora toca CONECTAR para iniciar la sesion."
                            )
                        } else {
                            tvResult.setTextColor(0xFFF44336.toInt())
                            tvResult.text = "Error: " + (if (errOutput.isNotEmpty()) errOutput else output).trim().take(200)
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        btnPair.isEnabled = true
                        tvResult.setTextColor(0xFFF44336.toInt())
                        tvResult.text = "Error: " + e.message
                    }
                }
            }.start()
        }

        btnConnect.setOnClickListener {
            val ip = etConnectIp.text.toString().trim()
            val port = etConnectPort.text.toString().trim()
            if (ip.isEmpty() || port.isEmpty()) {
                tvResult.setTextColor(0xFFF44336.toInt())
                tvResult.text = "Completa IP y puerto"
                return@setOnClickListener
            }
            tvResult.setTextColor(0xFF757575.toInt())
            tvResult.text = "Conectando a $ip:$port..."
            btnConnect.isEnabled = false
            Thread {
                try {
                    val cmd = "adb connect $ip:$port"
                    val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                    val output = process.inputStream.bufferedReader().readText()
                    process.waitFor()
                    runOnUiThread {
                        btnConnect.isEnabled = true
                        if (output.contains("connected") || output.contains("already")) {
                            tvResult.setTextColor(0xFF4CAF50.toInt())
                            tvResult.text = "Conectado a $ip:$port"
                            adbHost = ip
                            adbPort = port.toIntOrNull() ?: 5555
                            InputMapperService.adbHost = adbHost
                            InputMapperService.adbPort = adbPort
                            updateAdbStatusText()
                            appendLog("ADB conectado: $ip:$port")
                            showHeadsUpNotification(
                                "ADB Conectado!",
                                "Sesion activa: $ip:$port",
                                "Depuracion inalambrica conectada exitosamente.\nAhora puedes usar teclado y raton."
                            )
                        } else {
                            tvResult.setTextColor(0xFFF44336.toInt())
                            tvResult.text = "Error: " + output.trim().take(200)
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        btnConnect.isEnabled = true
                        tvResult.setTextColor(0xFFF44336.toInt())
                        tvResult.text = "Error: " + e.message
                    }
                }
            }.start()
        }

        AlertDialog.Builder(this)
            .setTitle("Conexion ADB Inalambrica")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun updateAdbStatusText() {
        if (adbHost.isNotEmpty() && adbPort > 0) {
            tvAdbStatus.text = "Conectado: $adbHost:$adbPort"
            tvAdbStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            tvAdbStatus.text = "Sin conexion ADB"
            tvAdbStatus.setTextColor(0xFFF44336.toInt())
        }
    }

    // ==================== Key Mapping Dialog ====================

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
        tvKeyMapStatus.text = names
    }

    private fun scanInputDevices() {
        tvDevices.text = "Buscando dispositivos..."
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
        tvDevices.text = if (devices.isEmpty()) "No hay dispositivos detectados"
        else devices.joinToString("\n") { "  * " + it.name + " [" + it.type + "]" }
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
            if (!InputMapperService.isTargetAppActive()) {
                return super.dispatchKeyEvent(event)
            }
            val mapping = InputMapperService.keyMappings.find { it.sourceKeyCode == event.keyCode && it.enabled }
            if (mapping != null) {
                appendLog("Key: " + mapping.sourceKeyName + " -> " + mapping.targetKeyName)
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (InputMapperService.isRunning && event.actionMasked == MotionEvent.ACTION_MOVE) {
            if (!InputMapperService.isTargetAppActive()) {
                return super.dispatchGenericMotionEvent(event)
            }
            appendLog("Mouse move")
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
}

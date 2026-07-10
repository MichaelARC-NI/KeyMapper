package com.inputmapper.app.util

import com.inputmapper.app.model.DeviceInfo
import com.inputmapper.app.model.DeviceType

object DeviceDiscovery {

    private val keyboardKeywords = listOf("keyboard", "kbd", "key", "input", "hid")
    private val mouseKeywords = listOf("mouse", "trackball", "pointing", "gaming mouse", "logitech")

    fun discoverDevices(): List<DeviceInfo> {
        val devices = mutableListOf<DeviceInfo>()

        // Scan /dev/input/ for event devices
        val inputDevices = ShellExecutor.executeShell("ls /dev/input/").trim()
        if (inputDevices.isNotEmpty()) {
            for (device in inputDevices.lines()) {
                if (!device.startsWith("event")) continue
                val path = "/dev/input/$device"

                // Get device name from /sys
                val name = getDeviceName(path)
                val type = classifyDevice(name)

                devices.add(DeviceInfo(
                    name = name,
                    type = type,
                    path = path
                ))
            }
        }

        // Also try /proc/bus/input/devices for more info
        val procDevices = discoverFromProc()
        for (procDev in procDevices) {
            if (devices.none { it.path == procDev.path }) {
                devices.add(procDev)
            }
        }

        return devices
    }

    private fun getDeviceName(devPath: String): String {
        val eventNum = devPath.substringAfterLast("/")
        return try {
            val name = ShellExecutor.executeShell(
                "cat /sys/class/input/$eventNum/device/name"
            ).trim()
            if (name.isNotEmpty()) name else "Dispositivo desconocido ($eventNum)"
        } catch (e: Exception) {
            "Dispositivo desconocido ($eventNum)"
        }
    }

    private fun classifyDevice(name: String): DeviceType {
        val lowerName = name.lowercase()
        return when {
            mouseKeywords.any { lowerName.contains(it) } -> DeviceType.MOUSE
            keyboardKeywords.any { lowerName.contains(it) } -> DeviceType.KEYBOARD
            lowerName.contains("composite") || lowerName.contains("multi") -> DeviceType.COMPOSITE
            else -> DeviceType.UNKNOWN
        }
    }

    private fun discoverFromProc(): List<DeviceInfo> {
        return try {
            val output = ShellExecutor.executeShell("cat /proc/bus/input/devices")
            val devices = mutableListOf<DeviceInfo>()
            val blocks = output.split("\n\n")

            for (block in blocks) {
                val nameMatch = Regex("Name=\"(.+?)\"").find(block)
                val handlersMatch = Regex("Handlers=(.+)").find(block)
                val physMatch = Regex("Phys=(.+)").find(block)

                if (nameMatch != null && handlersMatch != null) {
                    val name = nameMatch.groupValues[1]
                    val handlers = handlersMatch.groupValues[1].trim()
                    val type = classifyDevice(name)

                    if (handlers.contains("event")) {
                        val eventNum = Regex("event(\\d+)").find(handlers)?.groupValues?.get(1) ?: "0"
                        devices.add(DeviceInfo(
                            name = name,
                            type = type,
                            path = "/dev/input/event$eventNum"
                        ))
                    }
                }
            }
            devices
        } catch (e: Exception) {
            emptyList()
        }
    }
}

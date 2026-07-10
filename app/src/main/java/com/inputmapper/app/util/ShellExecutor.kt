package com.inputmapper.app.util

import java.io.BufferedReader
import java.io.InputStreamReader

object ShellExecutor {

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    fun executeRoot(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            output.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun executeShell(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            output.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "rish -c id"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.isNotEmpty() && !output.contains("not found")
        } catch (e: Exception) {
            false
        }
    }

    fun discoverInputDevices(): List<String> {
        return try {
            val output = executeShell("cat /proc/bus/input/devices")
            val devices = mutableListOf<String>()
            val blocks = output.split("\n\n")
            for (block in blocks) {
                val nameMatch = Regex("Name=\"(.+?)\"").find(block)
                val handlersMatch = Regex("Handlers=(.+)").find(block)
                if (nameMatch != null && handlersMatch != null) {
                    val name = nameMatch.groupValues[1]
                    val handlers = handlersMatch.groupValues[1].trim()
                    if (handlers.contains("event")) {
                        devices.add("$name ($handlers)")
                    }
                }
            }
            devices
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAdbDevices(): List<Pair<String, Int>> {
        return try {
            val output = executeShell("adb devices -l")
            val devices = mutableListOf<Pair<String, Int>>()
            val lines = output.lines().drop(1) // Skip header
            for (line in lines) {
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 2 && parts[1] == "device") {
                    val hostPort = parts[0]
                    if (hostPort.contains(":")) {
                        val (host, port) = hostPort.split(":")
                        devices.add(Pair(host, port.toIntOrNull() ?: 5555))
                    }
                }
            }
            devices
        } catch (e: Exception) {
            emptyList()
        }
    }
}

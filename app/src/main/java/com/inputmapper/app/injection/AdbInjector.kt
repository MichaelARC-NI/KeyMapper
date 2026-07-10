package com.inputmapper.app.injection

import com.inputmapper.app.model.InputMode
import com.inputmapper.app.util.ShellExecutor

class AdbInjector(private var adbHost: String = "127.0.0.1", private var adbPort: Int = 5555) : InputInjector {

    override val mode = InputMode.ADB

    override val isAvailable: Boolean
        get() = try {
            val output = ShellExecutor.executeShell("adb -s $adbHost:$adbPort shell echo ok")
            output.trim() == "ok"
        } catch (e: Exception) {
            false
        }

    fun setConnection(host: String, port: Int) {
        this.adbHost = host
        this.adbPort = port
    }

    private fun execAdb(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("adb", "-s", "$adbHost:$adbPort", "shell", cmd))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            ""
        }
    }

    override fun tap(x: Int, y: Int) { execAdb("input tap $x $y") }
    override fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long) {
        execAdb("input swipe $x1 $y1 $x2 $y2 $durationMs")
    }
    override fun scroll(x: Int, y: Int, direction: Int, amount: Int) {
        val deltaY = if (direction > 0) -500 else 500
        for (i in 0 until amount) {
            execAdb("input swipe $x $y $x ${y + deltaY} 200")
        }
    }
    override fun keyEvent(keyCode: Int) { execAdb("input keyevent $keyCode") }
    override fun text(text: String) {
        val escaped = text.replace(" ", "%s")
        execAdb("input text $escaped")
    }
    override fun back() = keyEvent(4)
    override fun home() = keyEvent(3)
    override fun recentApps() = keyEvent(187)
    override fun volumeUp() = keyEvent(24)
    override fun volumeDown() = keyEvent(25)
    override fun getScreenSize(): Pair<Int, Int> {
        val output = execAdb("wm size")
        val match = Regex("(\\d+)x(\\d+)").find(output)
        return if (match != null) {
            Pair(match.groupValues[1].toInt(), match.groupValues[2].toInt())
        } else {
            Pair(1080, 1920)
        }
    }
}

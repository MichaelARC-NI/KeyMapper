package com.inputmapper.app.injection

import com.inputmapper.app.model.InputMode
import com.inputmapper.app.util.ShellExecutor

class ShizukuInjector : InputInjector {

    override val mode = InputMode.SHIZUKU

    override val isAvailable: Boolean
        get() = ShellExecutor.isShizukuAvailable()

    private fun execShizuku(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            ""
        }
    }

    override fun tap(x: Int, y: Int) { execShizuku("input tap $x $y") }
    override fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long) {
        execShizuku("input swipe $x1 $y1 $x2 $y2 $durationMs")
    }
    override fun scroll(x: Int, y: Int, direction: Int, amount: Int) {
        val deltaY = if (direction > 0) -500 else 500
        for (i in 0 until amount) {
            execShizuku("input swipe $x $y $x ${y + deltaY} 200")
        }
    }
    override fun keyEvent(keyCode: Int) { execShizuku("input keyevent $keyCode") }
    override fun text(text: String) {
        val escaped = text.replace(" ", "%s")
        execShizuku("input text $escaped")
    }
    override fun back() = keyEvent(4)
    override fun home() = keyEvent(3)
    override fun recentApps() = keyEvent(187)
    override fun volumeUp() = keyEvent(24)
    override fun volumeDown() = keyEvent(25)
    override fun getScreenSize(): Pair<Int, Int> {
        val output = execShizuku("wm size")
        val match = Regex("(\\d+)x(\\d+)").find(output)
        return if (match != null) {
            Pair(match.groupValues[1].toInt(), match.groupValues[2].toInt())
        } else {
            Pair(1080, 1920)
        }
    }
}

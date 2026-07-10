package com.inputmapper.app.injection

import com.inputmapper.app.model.InputMode
import com.inputmapper.app.util.ShellExecutor

class RootInjector : InputInjector {

    override val mode = InputMode.ROOT

    override val isAvailable: Boolean
        get() = ShellExecutor.isRootAvailable()

    override fun tap(x: Int, y: Int) {
        ShellExecutor.executeRoot("input tap $x $y")
    }

    override fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long) {
        ShellExecutor.executeRoot("input swipe $x1 $y1 $x2 $y2 $durationMs")
    }

    override fun scroll(x: Int, y: Int, direction: Int, amount: Int) {
        val deltaY = if (direction > 0) -500 else 500
        for (i in 0 until amount) {
            ShellExecutor.executeRoot("input swipe $x $y $x ${y + deltaY} 200")
        }
    }

    override fun keyEvent(keyCode: Int) {
        ShellExecutor.executeRoot("input keyevent $keyCode")
    }

    override fun text(text: String) {
        val escaped = text.replace(" ", "%s")
        ShellExecutor.executeRoot("input text $escaped")
    }

    override fun back() = keyEvent(4)
    override fun home() = keyEvent(3)
    override fun recentApps() = keyEvent(187)
    override fun volumeUp() = keyEvent(24)
    override fun volumeDown() = keyEvent(25)

    override fun getScreenSize(): Pair<Int, Int> {
        val output = ShellExecutor.executeRoot("wm size")
        val match = Regex("(\\d+)x(\\d+)").find(output)
        return if (match != null) {
            Pair(match.groupValues[1].toInt(), match.groupValues[2].toInt())
        } else {
            Pair(1080, 1920)
        }
    }
}

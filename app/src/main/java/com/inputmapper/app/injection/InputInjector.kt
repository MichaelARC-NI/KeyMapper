package com.inputmapper.app.injection

import com.inputmapper.app.model.InputMode

interface InputInjector {
    val mode: InputMode
    val isAvailable: Boolean
    fun tap(x: Int, y: Int)
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = 300)
    fun scroll(x: Int, y: Int, direction: Int, amount: Int = 1)
    fun keyEvent(keyCode: Int)
    fun text(text: String)
    fun back()
    fun home()
    fun recentApps()
    fun volumeUp()
    fun volumeDown()
    fun getScreenSize(): Pair<Int, Int>
}

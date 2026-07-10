package com.inputmapper.app.model

data class KeyMapping(
    val sourceKeyCode: Int,
    val sourceKeyName: String,
    val targetKeyCode: Int,
    val targetKeyName: String,
    val enabled: Boolean = true
)

data class MouseConfig(
    val sensitivity: Float = 1.0f,
    val invertY: Boolean = false,
    val leftClickAsTap: Boolean = true,
    val rightClickAsBack: Boolean = true,
    val scrollAsSwipe: Boolean = true
)

data class DeviceInfo(
    val name: String,
    val type: DeviceType,
    val path: String,
    val vendorId: Int = 0,
    val productId: Int = 0
)

enum class DeviceType {
    KEYBOARD, MOUSE, COMPOSITE, UNKNOWN
}

package com.arena.hidcompatibilitytester.ui.screen.trackpad

data class TrackpadSettings(
    val pointerSpeed        : Float         = 1.2f,
    val scrollSpeed         : Float         = 1.0f,
    val invertScroll        : Boolean       = false,
    val tapToClick          : Boolean       = true,
    val twoFingerRightClick : Boolean       = true,
    val accelerationEnabled : Boolean       = true,
    val dragLockMode        : Boolean       = true,
    val clickPressure       : ClickPressure = ClickPressure.MEDIUM
)

enum class ClickPressure { LIGHT, MEDIUM, FIRM }
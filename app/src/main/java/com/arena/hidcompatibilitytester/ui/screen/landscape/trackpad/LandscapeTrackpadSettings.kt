package com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad

data class LandscapeTrackpadSettings(
    val pointerSpeed: Float = 1.0f,
    val scrollSpeed: Float = 0.5f,
    val invertScroll: Boolean = false,
    val tapToClick: Boolean = true,
    val twoFingerRightClick: Boolean = true,
    val accelerationEnabled: Boolean = true,
    val dragLockMode: Boolean = false,
    val clickPressure: LandscapeClickPressure = LandscapeClickPressure.MEDIUM,
    val scrollPosition: LandscapeSidePosition = LandscapeSidePosition.RIGHT,
    val arrowPosition: LandscapeSidePosition = LandscapeSidePosition.LEFT,
    val showArrowKeys: Boolean = true,
    val showScrollStrip: Boolean = true,
    val showSystemKeyboard: Boolean = true,
    val showInAppKeyboard: Boolean = true,
)

enum class LandscapeClickPressure { LIGHT, MEDIUM, FIRM }
enum class LandscapeSidePosition { LEFT, RIGHT }
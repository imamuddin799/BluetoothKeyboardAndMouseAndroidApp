package com.arena.hidcompatibilitytester.bluetooth

sealed class BleHidState {
    object IDLE          : BleHidState()
    object STARTING      : BleHidState()
    object ADVERTISING   : BleHidState()
    object CONNECTED     : BleHidState()
    object PAIR_REQUIRED : BleHidState()
    data class ERROR(val message: String) : BleHidState()
}
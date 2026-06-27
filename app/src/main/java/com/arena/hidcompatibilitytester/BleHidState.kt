// BleHidState.kt
package com.arena.hidcompatibilitytester

sealed class BleHidState {
    object IDLE        : BleHidState()
    object STARTING    : BleHidState()
    object ADVERTISING : BleHidState()
    object CONNECTED   : BleHidState()
    data class ERROR(val message: String) : BleHidState()
    object PAIR_REQUIRED : BleHidState()
}
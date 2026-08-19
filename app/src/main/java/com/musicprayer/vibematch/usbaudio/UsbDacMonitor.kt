package com.musicprayer.vibematch.usbaudio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UsbDac(
    val deviceId: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val hasPermission: Boolean,
)

class UsbDacMonitor(private val context: Context) : BroadcastReceiver() {
    private val manager = context.getSystemService(UsbManager::class.java)
    private val _devices = MutableStateFlow<List<UsbDac>>(emptyList())
    val devices = _devices.asStateFlow()

    fun start() {
        ContextCompat.registerReceiver(
            context, this,
            IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }, ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        refresh()
    }

    fun stop() = runCatching { context.unregisterReceiver(this) }.getOrNull().let { Unit }
    override fun onReceive(context: Context?, intent: Intent?) = refresh()

    private fun refresh() {
        _devices.value = manager.deviceList.values.filter(UsbDevice::isAudioDevice).map {
            UsbDac(it.deviceId, it.productName ?: it.deviceName, it.vendorId, it.productId, manager.hasPermission(it))
        }
    }
}

private fun UsbDevice.isAudioDevice(): Boolean =
    (0 until interfaceCount).any { getInterface(it).interfaceClass == UsbConstants.USB_CLASS_AUDIO }

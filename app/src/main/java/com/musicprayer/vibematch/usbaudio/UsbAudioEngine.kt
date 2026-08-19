package com.musicprayer.vibematch.usbaudio

/**
 * Boundary for the future native UAC1/UAC2 engine. The MVP deliberately does not claim
 * bit-perfect output: Media3 remains the active route until a tested native engine exists.
 */
interface UsbAudioEngine {
    val isBitPerfect: Boolean
    suspend fun open(dac: UsbDac): Result<Unit>
    suspend fun close()
}

class UnsupportedUsbAudioEngine : UsbAudioEngine {
    override val isBitPerfect = false
    override suspend fun open(dac: UsbDac) = Result.failure<Unit>(
        UnsupportedOperationException("Native USB exclusive engine is not implemented yet")
    )
    override suspend fun close() = Unit
}

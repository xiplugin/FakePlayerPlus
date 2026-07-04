package com.coderxi.plugin.fakeplayer.api.nms

interface NMSServerGamePacketListener {

    fun latency(): Int

    fun latency(value: Int, flush: Boolean = false)

}
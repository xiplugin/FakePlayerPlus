package com.coderxi.plugin.fakeplayer.utils.common

import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger

object IPGenerator {

    private val next = AtomicInteger(1)
    fun next(): InetAddress {
        val ip = next.getAndIncrement()
        if (ip == 0xfffffe) {
            next.set(0)
        }
        val p2 = (ip shr 16) and 0xff
        val p3 = (ip shr 8) and 0xff
        val p4 = ip and 0xff
        return InetAddress.getByAddress(byteArrayOf(127, p2.toByte(), p3.toByte(), p4.toByte()))
    }

}
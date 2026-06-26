package com.coderxi.plugin.fakeplayer.nms.v26_1_1

import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import java.net.InetAddress

open class NMSBridgeImpl : com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSBridgeImpl(), NMSBridge {

    override fun supportVersion() = "26.1.1"

    override fun createNetwork(address: InetAddress) = NMSNetworkImpl(address)

}
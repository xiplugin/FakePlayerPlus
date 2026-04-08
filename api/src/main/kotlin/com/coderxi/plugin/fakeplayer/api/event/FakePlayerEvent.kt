package com.coderxi.plugin.fakeplayer.api.event

import net.kyori.adventure.text.Component
import org.bukkit.Location

sealed class FakePlayerEvent {

    //假人刚被初始化 (尚未建立网络连接)
    object PreSpawn: FakePlayerEvent()
    //假人已建立网络连接并注册到了假人列表 (尚未进入世界)
    object PostSpawn: FakePlayerEvent()
    //假人已进入世界
    object AfterSpawn: FakePlayerEvent()
    //假人触发了退出事件
    data class Quit(val reason: Component?) : FakePlayerEvent()
    //假人退出事件完成之后
    object PostQuit : FakePlayerEvent()

    object Respawn: FakePlayerEvent()
    data class Death(val location: Location?) : FakePlayerEvent()

}
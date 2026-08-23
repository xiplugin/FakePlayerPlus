package com.coderxi.plugin.fakeplayer.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor

object HelpLineUtil {

    fun paginationComponent(page: Int, pageTotal: Int, command: String = "/fp help"): Component {
        val builder = Component.text()
        val prev = if (page <= 1) Component.text("◀").color(NamedTextColor.GRAY) else
            Component.text("◀").color(NamedTextColor.WHITE)
              .clickEvent(ClickEvent.runCommand("$command ${page - 1}"))
              .hoverEvent(HoverEvent.showText(Component.text("上一页")))
        builder.append(prev,Component.text(" "))

        for (p in 1..pageTotal) {
            val page = if (p == page) Component.text(p).color(NamedTextColor.AQUA) else
                Component.text(p).color(NamedTextColor.WHITE)
                    .clickEvent(ClickEvent.runCommand("$command $p"))
                    .hoverEvent(HoverEvent.showText(Component.text("查看第 $p 页")))
            builder.append(page, Component.text(" "))
        }

        val next = if (page >= pageTotal) Component.text("▶").color(NamedTextColor.GRAY) else
            Component.text("▶").color(NamedTextColor.WHITE)
                .clickEvent(ClickEvent.runCommand("$command ${page + 1}"))
                .hoverEvent(HoverEvent.showText(Component.text("下一页")))
        builder.append(next)
        return builder.build()
    }

}
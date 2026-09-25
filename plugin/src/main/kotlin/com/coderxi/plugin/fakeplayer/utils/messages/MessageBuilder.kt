package com.coderxi.plugin.fakeplayer.utils.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import java.util.Locale

object MessageBuilder {

    fun pagination(locale: Locale, page: Int, pageTotal: Int, command: String): Component {
        val builder = Component.text()
        val prev = if (page <= 1) Component.text("◀").color(NamedTextColor.GRAY) else
            Component.text("◀").color(NamedTextColor.WHITE)
              .clickEvent(ClickEvent.runCommand("$command ${page - 1}"))
              .hoverEvent(HoverEvent.showText(tl(locale, "fakeplayer.help.pagination.prev")))
        builder.append(prev, Component.text(" "))

        for (p in 1..pageTotal) {
            val page = if (p == page) Component.text(p).color(NamedTextColor.AQUA) else
                Component.text(p).color(NamedTextColor.WHITE)
                    .clickEvent(ClickEvent.runCommand("$command $p"))
                    .hoverEvent(HoverEvent.showText(tl(locale, "fakeplayer.help.pagination.page", p)))
            builder.append(page, Component.text(" "))
        }

        val next = if (page >= pageTotal) Component.text("▶").color(NamedTextColor.GRAY) else
            Component.text("▶").color(NamedTextColor.WHITE)
                .clickEvent(ClickEvent.runCommand("$command ${page + 1}"))
                .hoverEvent(HoverEvent.showText(tl(locale, "fakeplayer.help.pagination.next")))
        builder.append(next)
        return builder.build()
    }

}
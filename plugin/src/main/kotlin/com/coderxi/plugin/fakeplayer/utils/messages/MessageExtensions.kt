package com.coderxi.plugin.fakeplayer.utils.messages

import com.coderxi.plugin.fakeplayer.plugin
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.annotations.PropertyKey
import java.util.Locale

fun tl(locale: Locale, @PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = plugin.messages.translate(locale, key, *args)
fun tlp(locale: Locale, @PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = plugin.messages.translateWithPrefix(locale, key, *args)
fun tls(locale: Locale, @PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = plugin.messages.translateString(locale, key, *args)

fun tl(player: Player, @PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = tl(player.locale(), key, *args)
fun tlp(player: Player, @PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = tlp(player.locale(), key, *args)
fun tls(player: Player, @PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = tls(player.locale(), key, *args)

fun CommandSender.sendLocalizedMessage(@PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) {
    sendMessage(tlp(localOrDefault(),key,*args))
}

fun CommandSender.localOrDefault() : Locale {
    return if (this is Player) { locale() } else Locale.getDefault()
}
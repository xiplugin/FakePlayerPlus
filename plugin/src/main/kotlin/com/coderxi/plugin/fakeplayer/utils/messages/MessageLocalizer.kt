package com.coderxi.plugin.fakeplayer.utils.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.plugin.java.JavaPlugin
import java.text.MessageFormat
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle
import java.util.concurrent.ConcurrentHashMap

class MessageLocalizer(private val plugin: JavaPlugin, val prefixKey: String) {

    companion object {
        const val BUNDLE_PREFIX = "messages"
    }

    private val miniMessage = MiniMessage.miniMessage()

    private val bundles = ConcurrentHashMap<Locale, ResourceBundle>()
    private val formats = ConcurrentHashMap<String, MessageFormat>()
    private val prefixes = ConcurrentHashMap<Locale, Component>()

    fun reload() {
        bundles.clear()
        formats.clear()
        prefixes.clear()
    }

    private fun loadBundle(locale: Locale): ResourceBundle {
        when (locale.toLanguageTag()) {
            "zh-HK" -> return loadBundle(Locale.forLanguageTag("zh-TW"))
        }
        val bundleFileName = "${BUNDLE_PREFIX}_${locale.toLanguageTag()}.properties"
        println("Loading $bundleFileName")
        val customBundleFile = plugin.dataFolder
            .resolve(BUNDLE_PREFIX)
            .resolve(bundleFileName)
        val stream = if (customBundleFile.exists()) {
            customBundleFile.inputStream()
        } else {
            plugin.javaClass.classLoader.getResourceAsStream("$BUNDLE_PREFIX/$bundleFileName")
        }
        val bundle = stream?.let { PropertyResourceBundle(it.reader(Charsets.UTF_8)) }
        if (bundle != null) return bundle
        return bundle ?: ResourceBundle.getBundle("$BUNDLE_PREFIX.$BUNDLE_PREFIX", locale)
    }

    private fun getFormat(locale: Locale, key: String): String {
        val bundle = bundles.computeIfAbsent(locale) { loadBundle(it) }
        return runCatching { bundle.getString(key) }.getOrDefault(key)
    }

    fun translateString(locale: Locale, key: String, vararg args: Any): String {
        val text = getFormat(locale, key)
        if (args.isEmpty()) return text
        val format = formats.computeIfAbsent(text) { MessageFormat(it) }
        return format.format(args)
    }

    fun translate(locale: Locale, key: String, vararg args: Any): Component {
        return miniMessage.deserialize(translateString(locale, key, *args))
    }

    fun translateWithPrefix(locale: Locale, key: String, vararg args: Any): Component {
        val prefix = prefixes.computeIfAbsent(locale) { translate(locale, prefixKey) }
        return prefix.append(translate(locale, key, *args))
    }

}
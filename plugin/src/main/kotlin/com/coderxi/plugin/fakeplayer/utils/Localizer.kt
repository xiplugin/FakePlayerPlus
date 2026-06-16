package com.coderxi.plugin.fakeplayer.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.io.File
import java.text.MessageFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Localizer: PluginComponent {
    private val bundleName = "messages"
    private var currentLocale = Locale.getDefault()
    
    private val bundleCache = ConcurrentHashMap<Locale, ResourceBundle>()
    private val formatCache = ConcurrentHashMap<String, MessageFormat>()
    private lateinit var prefixComponent: Component

    fun translate(key: String, vararg args: Any) = MiniMessage.miniMessage().deserialize(translateStringWithArgs(key, *args))
    fun translateWithPrefix(key: String, vararg args: Any) = prefixComponent.append(translate(key, *args))
    fun translateStringWithArgs(key: String, vararg args: Any): String {
        val text = translateString(key)
        if (args.isEmpty()) return text
        val format = formatCache.computeIfAbsent(text) { MessageFormat(it) }
        return format.format(args)
    }
    private fun translateString(key: String): String {
        val bundle = bundleCache.computeIfAbsent(currentLocale) { loadBundle(it) }
        return try { bundle.getString(key) } catch (_: MissingResourceException) { key }
    }

    private fun loadBundle(locale: Locale): ResourceBundle {
        val folder = File(plugin.dataFolder, bundleName)
        val fileName = "${bundleName}_${locale.language}_${locale.country}.properties"
        val file = File(folder, fileName)

        return if (file.exists()) {
            file.inputStream().reader(Charsets.UTF_8).use { PropertyResourceBundle(it) }
        } else {
            ResourceBundle.getBundle("$bundleName.$bundleName", locale, object : ResourceBundle.Control() {
                override fun getFallbackLocale(baseName: String?, locale: Locale?) = null
            })
        }
    }

    fun locale(langTag: String) {
        currentLocale = Locale.forLanguageTag(langTag.replace("[_.]".toRegex(), "-"))
        prefixComponent = translate("fakeplayer.prefix")
        bundleCache.clear()
        formatCache.clear()
    }
}
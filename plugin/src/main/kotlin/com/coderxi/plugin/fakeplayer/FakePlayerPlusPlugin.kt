package com.coderxi.plugin.fakeplayer

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.command.FakePlayerCommand
import com.coderxi.plugin.fakeplayer.command.parameter.FakePlayerParameterType
import com.coderxi.plugin.fakeplayer.command.annotaion.SelectFlag
import com.coderxi.plugin.fakeplayer.command.annotaion.SelectFlagReplacer
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandExceptionHandler
import com.coderxi.plugin.fakeplayer.config.PluginConfig
import com.coderxi.plugin.fakeplayer.event.FakePlayerEventListener
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.manager.FakePlayerTicker
import com.coderxi.plugin.fakeplayer.manager.FakePlayerManagerImpl
import com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSBridgeImpl
import com.coderxi.plugin.fakeplayer.utils.ListenerExtensions.registerMyEvents
import com.coderxi.plugin.fakeplayer.utils.Localizer
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import eu.okaeri.configs.ConfigManager
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import org.sql2o.Sql2o
import revxrsal.commands.Lamp
import revxrsal.commands.bukkit.BukkitLamp
import java.io.File

class FakePlayerPlusPlugin: FakePlayerPlusPluginApi, JavaPlugin() {

    override lateinit var nms: NMSBridge private set
    override lateinit var nmsServer: NMSServer private set

    lateinit var config : PluginConfig private set
    lateinit var messages : Localizer private set

    lateinit var sql2o: Sql2o private set
    lateinit var lamp: Lamp<*> private set

    override lateinit var fakePlayerManager: FakePlayerManager;

    override fun onEnable() {
        nms = NMSBridgeImpl()
        nmsServer = nms.fromServer(server)
        config = ConfigManager.create(PluginConfig::class.java).apply {
            configure { opt ->
                opt.configurer(YamlBukkitConfigurer())
                opt.bindFile(File(dataFolder, "config.yml"))
                opt.removeOrphans(true)
            }
            saveDefaults().load(true)
        }
        messages = Localizer().apply {
            locale(config.language)
        }
        sql2o = Sql2o("jdbc:sqlite:${File(dataFolder, "$name.db").absolutePath}", null, null).also { sql2o ->
            val sqlStatements = classLoader.getResourceAsStream("database/init.sql")!!
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
                .split(";")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            @Suppress("SqlSourceToSinkFlow")
            sql2o.open().use { conn -> sqlStatements.forEach { sql -> conn.createQuery(sql).executeUpdate() } }
        }
        fakePlayerManager = FakePlayerManagerImpl().also { fpm ->
            FakePlayerTicker(fpm).start()
            FakePlayerEventListener(fpm).registerMyEvents()
            fpm.registerMyEvents()
        }
        lamp = BukkitLamp.builder(this)
            .annotationReplacer(SelectFlag::class.java, SelectFlagReplacer())
            .dependency(FakePlayerManager::class.java,fakePlayerManager)
            .parameterTypes { parameters -> parameters.addParameterType(FakePlayer::class.java, FakePlayerParameterType()) }
            .exceptionHandler(FakePlayerCommandExceptionHandler())
            .build()
            .apply { register(FakePlayerCommand()) }
    }

    fun onReload() {
        config.load()
        messages.locale(config.language)
        PluginComponent.executeReload()
    }

    override fun onDisable() {
        HandlerList.unregisterAll(this)
        PluginComponent.executeDisable()
    }
}
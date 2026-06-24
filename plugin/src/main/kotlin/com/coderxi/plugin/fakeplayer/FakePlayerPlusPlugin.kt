package com.coderxi.plugin.fakeplayer

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.command.FakePlayerCommand
import com.coderxi.plugin.fakeplayer.command.parameter.FakePlayerParameterType
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandExceptionHandler
import com.coderxi.plugin.fakeplayer.config.FakePlayerPlusPluginConfig
import com.coderxi.plugin.fakeplayer.event.*
import com.coderxi.plugin.fakeplayer.component.*
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.command.FakePlayerActionCommand
import com.coderxi.plugin.fakeplayer.command.annotaion.PluginCommandPermissionFactory
import com.coderxi.plugin.fakeplayer.command.annotaion.Select
import com.coderxi.plugin.fakeplayer.command.annotaion.SelectReplacer
import com.coderxi.plugin.fakeplayer.manager.FakePlayerManagerImpl
import com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSBridgeImpl
import com.coderxi.plugin.fakeplayer.utils.registerEvents
import com.coderxi.plugin.fakeplayer.utils.Localizer
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import com.coderxi.plugin.fakeplayer.utils.RegexTransformer
import eu.okaeri.configs.ConfigManager
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import org.sql2o.Sql2o
import revxrsal.commands.Lamp
import revxrsal.commands.bukkit.BukkitLamp
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.io.File

class FakePlayerPlusPlugin: FakePlayerPlusPluginApi, JavaPlugin() {

    override lateinit var nms: NMSBridge private set
    override lateinit var nmsServer: NMSServer private set

    lateinit var config : FakePlayerPlusPluginConfig private set
    lateinit var messages : Localizer private set

    lateinit var sql2o: Sql2o private set
    lateinit var lamp: Lamp<BukkitCommandActor> private set

    override lateinit var fakePlayerManager: FakePlayerManager

    override fun onEnable() {
        nms = NMSBridgeImpl()
        nmsServer = nms.fromServer(server)
        config = ConfigManager.create(FakePlayerPlusPluginConfig::class.java).apply {
            configure { opt ->
                opt.configurer(YamlBukkitConfigurer().apply {
                    register(RegexTransformer())
                })
                opt.bindFile(File(dataFolder, "config.yml"))
                opt.removeOrphans(true)
            }
            saveDefaults().load(true)
        }
        messages = Localizer(this).apply {
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
        var fakePlayerLimiter : FakePlayerLimiter
        fakePlayerManager = FakePlayerManagerImpl().also { fpm ->
            FakePlayerTicker(fpm)
            FakePlayerEventDispatcher(fpm).registerEvents()
            FakePlayerBehaviorImplementListener(fpm).registerEvents()
            FakePlayerLifecycleCommandListener().registerEvents()
            fakePlayerLimiter = FakePlayerLimiter(fpm).apply { registerEvents() }
            FakePlayerPingUpdater(fpm).registerEvents()
            FakePlayerSelector.registerEvents()
            FakePlayerReplenishListener(fpm).registerEvents()
            fpm.registerEvents()
        }
        lamp = BukkitLamp.builder(this)
            .permissionFactory(PluginCommandPermissionFactory())
            .annotationReplacer(Select::class.java, SelectReplacer())
            .dependency(FakePlayerManager::class.java,fakePlayerManager)
            .dependency(FakePlayerLimiter::class.java,fakePlayerLimiter)
            .parameterTypes { parameters -> parameters.addParameterType(FakePlayer::class.java, FakePlayerParameterType()) }
            .exceptionHandler(FakePlayerCommandExceptionHandler())
            .build()
            .apply {
                register(FakePlayerCommand())
                register(FakePlayerActionCommand())
            }
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
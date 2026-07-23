package com.coderxi.plugin.fakeplayer

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi
import com.coderxi.plugin.fakeplayer.api.action.ActionMode
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
import com.coderxi.plugin.fakeplayer.command.annotaion.PluginCommandPermissionFactory
import com.coderxi.plugin.fakeplayer.command.annotaion.Select
import com.coderxi.plugin.fakeplayer.command.annotaion.SelectReplacer
import com.coderxi.plugin.fakeplayer.command.annotaion.SuggestCommands
import com.coderxi.plugin.fakeplayer.command.annotaion.SuggestCommandsProvider
import com.coderxi.plugin.fakeplayer.command.parameter.ActionModeParameterType
import com.coderxi.plugin.fakeplayer.config.StaticFakePlayersConfig
import com.coderxi.plugin.fakeplayer.expansion.FakePlayerPlaceholderExpansion
import com.coderxi.plugin.fakeplayer.manager.FakePlayerManagerImpl
import com.coderxi.plugin.fakeplayer.utils.Localizer
import com.coderxi.plugin.fakeplayer.utils.NMSBridgeLoader
import com.coderxi.plugin.fakeplayer.utils.RegexTransformer
import com.coderxi.plugin.fakeplayer.utils.executeDisable
import com.coderxi.plugin.fakeplayer.utils.executeReload
import com.coderxi.plugin.fakeplayer.utils.globalCoroutineScope
import eu.okaeri.configs.ConfigManager
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer
import kotlinx.coroutines.cancel
import org.bukkit.Bukkit
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
        nms = NMSBridgeLoader.load(server.minecraftVersion)
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
        fakePlayerManager = FakePlayerManagerImpl().also { fpm -> listOf(
            FakePlayerTicker(fpm),
            FakePlayerEventDispatcher(fpm),
            FakePlayerBehaviorImplementListener(fpm),
            FakePlayerLifecycleCommandListener(),
            FakePlayerLimiter(fpm).also { fakePlayerLimiter = it },
            FakePlayerPingUpdater(fpm),
            FakePlayerSelector,
            FakePlayerReplenishListener(fpm),
            FakePlayerDummyVarsNotifyListener(fpm),
            FakePlayerAutoFishListener(fpm),
            StaticFakePlayerManager(fpm, ConfigManager.create(StaticFakePlayersConfig::class.java).apply {
                configure { opt ->
                    opt.configurer(YamlBukkitConfigurer().apply {
                        null
                    })
                    opt.bindFile(File(dataFolder, "static-fakeplayers.yml"))
                    opt.removeOrphans(true)
                }
                saveDefaults().load(true)
            }),
            fpm
        ).forEach { component ->
            server.pluginManager.registerEvents(component, this) }
        }.also { fpm ->
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                FakePlayerPlaceholderExpansion(fpm).register()
            }
        }
        lamp = BukkitLamp.builder(this)
            .permissionFactory(PluginCommandPermissionFactory())
            .annotationReplacer(Select::class.java, SelectReplacer())
            .dependency(FakePlayerManager::class.java,fakePlayerManager)
            .dependency(FakePlayerLimiter::class.java,fakePlayerLimiter)
            .parameterTypes { parameters ->
                parameters.addParameterType(FakePlayer::class.java, FakePlayerParameterType(fakePlayerManager))
                parameters.addParameterType(ActionMode::class.java, ActionModeParameterType())
            }
            .suggestionProviders { providers -> providers.addProviderForAnnotation(SuggestCommands::class.java, SuggestCommandsProvider()) }
            .exceptionHandler(FakePlayerCommandExceptionHandler())
            .build()
            .apply { register(FakePlayerCommand()) }
    }

    fun onReload() {
        config.load()
        messages.locale(config.language)
        executeReload()
    }

    override fun onDisable() {
        globalCoroutineScope.cancel()
        HandlerList.unregisterAll(this)
        executeDisable()
    }

}
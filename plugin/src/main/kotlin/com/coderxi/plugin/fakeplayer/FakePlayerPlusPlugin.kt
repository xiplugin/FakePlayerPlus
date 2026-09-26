package com.coderxi.plugin.fakeplayer

import com.coderxi.plugin.fakeplayer.utils.messages.MessageLocalizer
import com.coderxi.plugin.fakeplayer.action.ActionRegistryImpl
import com.coderxi.plugin.fakeplayer.action.base.CommonActionMode
import com.coderxi.plugin.fakeplayer.action.handler.*
import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi
import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginComponent as PluginComponent0
import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionRegistry
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.command.FakePlayerCommand
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandExceptionHandler
import com.coderxi.plugin.fakeplayer.config.FakePlayerPlusPluginConfig
import com.coderxi.plugin.fakeplayer.event.*
import com.coderxi.plugin.fakeplayer.component.*
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.command.annotaion.*
import com.coderxi.plugin.fakeplayer.command.parameter.*
import com.coderxi.plugin.fakeplayer.command.permission.Permission
import com.coderxi.plugin.fakeplayer.expansion.FakePlayerPlaceholderExpansion
import com.coderxi.plugin.fakeplayer.manager.FakePlayerManagerImpl
import com.coderxi.plugin.fakeplayer.provider.invsee.InvseeProvider
import com.coderxi.plugin.fakeplayer.utils.plugin.NMSBridgeLoader
import com.coderxi.plugin.fakeplayer.utils.common.RegexTransformer
import com.coderxi.plugin.fakeplayer.utils.coroutine.globalCoroutineScope
import eu.okaeri.configs.ConfigManager
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer
import kotlinx.coroutines.cancel
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.sql2o.Sql2o
import revxrsal.commands.Lamp
import revxrsal.commands.bukkit.BukkitLamp
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

lateinit var plugin: FakePlayerPlusPlugin

class FakePlayerPlusPlugin: FakePlayerPlusPluginApi, JavaPlugin() {

    override lateinit var nms: NMSBridge private set
    override lateinit var nmsServer: NMSServer private set

    lateinit var config : FakePlayerPlusPluginConfig private set
    lateinit var messages : MessageLocalizer private set

    lateinit var sql2o: Sql2o private set
    lateinit var lamp: Lamp<BukkitCommandActor> private set

    private val components = CopyOnWriteArrayList<PluginComponent0>()

    override lateinit var fakePlayerManager: FakePlayerManager

    override lateinit var globalActionRegistry: ActionRegistry

    override fun onEnable() {
        plugin = this
        nms = NMSBridgeLoader.load(server.minecraftVersion)
        nmsServer = nms.fromServer(server)
        config = loadConfig("config.yml", FakePlayerPlusPluginConfig::class).apply { saveDefaults().load(true) }
        messages = MessageLocalizer(this, "fakeplayer.prefix")
        sql2o = loadDatabase()
        server.pluginManager.addPermission(Permission.BASIC)
        registerComponents(
            FakePlayerManagerImpl().also { fakePlayerManager = it },
            FakePlayerEventDispatcher(),
            FakePlayerTicker(),
            FakePlayerSelector,
            //config
            FakePlayerLimiter,
            FakePlayerPingUpdater(),
            FakePlayerLifecycleCommandListener(),
            InvseeProvider.Companion,
            FakePlayerAutoAuthListener(),
            //settings
            FakePlayerDummyVarsNotifyListener(),
            FakePlayerAutoReplenishListener(),
            FakePlayerAutoFishListener(),
            FakePlayerAutoEquipToolListener(),
            FakePlayerInteractedListener(),
            FakePlayerDeathListener(),
            FakePlayerFollowQuittingListener(),
            //other
            StaticFakePlayerManager()
        )
        if (server.pluginManager.isPluginEnabled("PlaceholderAPI")) {
            registerComponents(FakePlayerPlaceholderExpansion().apply(PlaceholderExpansion::register))
        }
        globalActionRegistry = ActionRegistryImpl().apply {
            registerCommonHandlers(
                AttackHandler,
                MineHandler,
                UseItemHandler,
                DropItemHandler,
                JumpHandler,
                SneakHandler,
                LookAtEntityHandler
            )
            CommonActionMode.entries.forEach { mode ->
                setModeSuggestParameters(mode.key, mode.suggestParameters)
            }
        }
        lamp = BukkitLamp.builder(this)
            .permissionFactory(PluginCommandPermissionFactory())
            .annotationReplacer(Select::class.java, SelectReplacer())
            .dependency(FakePlayerManager::class.java,fakePlayerManager)
            .parameterTypes { parameters ->
                parameters.addParameterType(FakePlayer::class.java, FakePlayerParameterType(fakePlayerManager))
                parameters.addParameterType(Action::class.java, ActionParameterType())
                parameters.addParameterType(ActionModeAndParameters::class.java, ActionModeAndParametersParameterType())
            }
            .suggestionProviders { providers -> providers.addProviderForAnnotation(SuggestCommands::class.java, SuggestCommandsProvider()) }
            .exceptionHandler(FakePlayerCommandExceptionHandler())
            .build()
            .apply { register(FakePlayerCommand()) }
    }

    fun <T : OkaeriConfig> loadConfig(name: String, clazz: KClass<T>): T = ConfigManager.create(clazz.java).apply {
        configure { opt ->
            opt.configurer(YamlBukkitConfigurer().apply {
                register(RegexTransformer())
            })
            opt.bindFile(File(plugin.dataFolder, name))
            opt.removeOrphans(true)
        }
    }

    fun loadDatabase(): Sql2o {
        val sql2o = Sql2o("jdbc:sqlite:${File(dataFolder, "$name.db").absolutePath}", null, null)
        val initSql = classLoader.getResourceAsStream("database/init.sql")!!.readAllBytes().toString(Charsets.UTF_8)
        val sqlList = initSql.split(";").map { it.trim() }.filter { it.isNotBlank() }
        @Suppress("SqlSourceToSinkFlow")
        sqlList.forEach { sql2o.open().createQuery(it).executeUpdate() }
        return sql2o
    }

    override fun registerComponent(component: PluginComponent0) {
        registerComponents(component)
    }

    fun registerComponents(vararg components: PluginComponent0) {
        components.forEach { component ->
            this.components.add(component)
            (component as? Listener)?.let { server.pluginManager.registerEvents(it, this) }
        }
        this.components.sortByDescending { it.priority }
    }

    fun onReload() {
        config.load()
        messages.reload()
        components.forEach(PluginComponent0::onReload)
    }

    override fun onDisable() {
        globalCoroutineScope.cancel()
        HandlerList.unregisterAll(this)
        components.forEach(PluginComponent0::onDisable)
    }

}
package com.coderxi.plugin.fakeplayer.scope

import com.coderxi.plugin.fakeplayer.context.PluginContext
import com.coderxi.plugin.fakeplayer.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerEvent.*
import com.coderxi.plugin.fakeplayer.utils.InetAddressUtil
import org.bukkit.Location
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractFakePlayerScope(override val uniqueId: UUID): FakePlayerScope, PluginContext {

    companion object {
        val ipGenerator = InetAddressUtil.Generator()
    }

    init {
        registry.registerScope(this)
    }

    protected val fakeplayers = ConcurrentHashMap<UUID, FakePlayer>()

    override fun fakeplayers() = fakeplayers.values

    protected fun uuid(name: String): UUID = UUID.nameUUIDFromBytes("FakePlayer:$uniqueId:$name".toByteArray())

    abstract fun checkSpawnLimit(): Boolean

    protected abstract fun getFakePlayerSpawnLocation(): Location

    protected fun create(uuid: UUID, name: String): CompletableFuture<FakePlayer?> {
        return CompletableFuture.supplyAsync {
            val nmsPlayer = nmsServer.newPlayer(uuid, name).apply {
                setPlayBefore()
                disableAdvancements(plugin)
            }
            val nmsNetwork = bridge.createNetwork(ipGenerator.next(), plugin)
            val spawnLocation = getFakePlayerSpawnLocation()
            Triple(nmsPlayer, nmsNetwork, spawnLocation)
        }.thenComposeAsync({ (nmsPlayer, nmsNetwork, spawnLocation) ->
            val fakePlayer = FakePlayer(nmsPlayer)
            fakePlayer.emit(PreSpawn)
            fakePlayer.connection = nmsNetwork.placeNewPlayer(fakePlayer.player)
            onFakePlayerSpawn(fakePlayer)
            fakeplayers[uuid] = fakePlayer
            registry.registerFakePlayer(fakePlayer)
            fakePlayer.on<PostQuit> {
                fakeplayers.remove(uuid)
                registry.unregisterFakePlayer(uuid)
            }
            fakePlayer.emit(PostSpawn)
            fakePlayer.teleportAsync(spawnLocation).thenApply { success ->
                if (success == true) {
                    startTicker()
                    notify(tl("fakeplayer.spawn.success",fakePlayer.name,fakePlayer.player.world.name,"%.2f, %.2f, %.2f".format(spawnLocation.x, spawnLocation.y, spawnLocation.z)))
                    fakePlayer.emit(AfterSpawn)
                    fakePlayer
                } else {
                    fakePlayer.quit("Spawn failed")
                    fakeplayers.remove(uuid)
                    registry.unregisterFakePlayer(uuid)
                    null
                }
            }
        }, scheduler.getMainThreadExecutor(plugin))
    }

    private val spawnAsyncNull = CompletableFuture.completedFuture<FakePlayer?>(null)

    final override fun spawnAsync(name: String): CompletableFuture<FakePlayer?> {
        if (!checkSpawnLimit()) return spawnAsyncNull
        val uuid = uuid(name)
        if (fakeplayers.containsKey(uuid)) {
            notify(tl("fakeplayer.spawn.exists",name))
            return spawnAsyncNull
        }
        return create(uuid,name)
    }

    protected abstract fun onFakePlayerSpawn(fakePlayer: FakePlayer)

    private var ticker: BukkitTask? = null
    protected fun startTicker() {
        if (ticker != null) return
        ticker = scheduler.runTaskTimer(plugin, Runnable {
            if (fakeplayers.isEmpty()) {
                stopTicker()
                return@Runnable
            }
            tick()
        }, 0L, 1L)
    }

    protected fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    override fun tick() {
        onTick()
        fakeplayers.values.forEach { it.doTick() }
    }

    protected open fun onTick() {}

    override fun remove(uuid: UUID) {
        fakeplayers.remove(uuid)
        registry.getFakePlayer(uuid)?.quit()
    }

    override fun destroy() {
        onDestroy()
        stopTicker()
        fakeplayers.clear()
        registry.unregisterScope(this.uniqueId)
    }

    protected open fun onDestroy() {}
}
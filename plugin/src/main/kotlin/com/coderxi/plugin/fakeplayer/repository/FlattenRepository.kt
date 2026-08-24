package com.coderxi.plugin.fakeplayer.repository

import com.coderxi.plugin.fakeplayer.api.action.ActionMode
import com.coderxi.plugin.fakeplayer.api.action.FlattenAction
import com.coderxi.plugin.fakeplayer.component.FlattenSelection
import com.coderxi.plugin.fakeplayer.utils.plugin
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.sql2o.Connection
import java.util.UUID

data class BlockLocDTO(val world: String, val x: Int, val y: Int, val z: Int, val role: String = "OUTPUT")

class FlattenRepository {

    private val gson = Gson()
    private val blockLocListType = object : TypeToken<List<BlockLocDTO>>() {}.type
    private val stringListType = object : TypeToken<List<String>>() {}.type

    init {
        runCatching {
            open().use { conn ->
                conn.createQuery("ALTER TABLE player_flatten_selection ADD COLUMN selected_workers TEXT").executeUpdate()
            }
        }
    }

    private fun open(): Connection = plugin.sql2o.open()

    fun loadSelection(playerUuid: UUID): FlattenSelection? {
        val sql = "SELECT * FROM player_flatten_selection WHERE player_uuid = :uuid LIMIT 1"
        return open().use { conn ->
            val row = conn.createQuery(sql)
                .addParameter("uuid", playerUuid.toString())
                .executeAndFetchTable()
                .rows()
                .firstOrNull() ?: return null

            val pos1WorldName = row.getString("pos1_world")
            val pos1World = if (pos1WorldName != null) Bukkit.getWorld(pos1WorldName) else null
            val pos1 = if (pos1World != null && row.getInteger("pos1_x") != null) {
                pos1World.getBlockAt(row.getInteger("pos1_x"), row.getInteger("pos1_y"), row.getInteger("pos1_z"))
            } else null

            val pos2WorldName = row.getString("pos2_world")
            val pos2World = if (pos2WorldName != null) Bukkit.getWorld(pos2WorldName) else null
            val pos2 = if (pos2World != null && row.getInteger("pos2_x") != null) {
                pos2World.getBlockAt(row.getInteger("pos2_x"), row.getInteger("pos2_y"), row.getInteger("pos2_z"))
            } else null

            val chestBlocksJson = row.getString("chest_blocks")
            val outputChests = mutableListOf<Block>()
            val toolChests = mutableListOf<Block>()
            if (!chestBlocksJson.isNullOrBlank()) {
                val dtos = runCatching { gson.fromJson<List<BlockLocDTO>>(chestBlocksJson, blockLocListType) }.getOrNull() ?: emptyList()
                for (dto in dtos) {
                    val w = Bukkit.getWorld(dto.world) ?: continue
                    val block = w.getBlockAt(dto.x, dto.y, dto.z)
                    if (dto.role.equals("TOOL", ignoreCase = true)) {
                        toolChests.add(block)
                    } else {
                        outputChests.add(block)
                    }
                }
            }

            val workersJson = row.getString("selected_workers")
            val selectedWorkers = mutableSetOf<UUID>()
            if (!workersJson.isNullOrBlank()) {
                val workerUuids = runCatching { gson.fromJson<List<String>>(workersJson, stringListType) }.getOrNull() ?: emptyList()
                for (uStr in workerUuids) {
                    runCatching { UUID.fromString(uStr) }.getOrNull()?.let { selectedWorkers.add(it) }
                }
            }

            val preserveOres = (row.getInteger("preserve_ores") ?: 0) == 1
            val pickupItems = (row.getInteger("pickup_items") ?: 1) == 1
            val autoDeposit = (row.getInteger("auto_deposit") ?: 1) == 1

            FlattenSelection(
                pos1 = pos1,
                pos2 = pos2,
                outputChests = outputChests,
                toolChests = toolChests,
                selectedWorkers = selectedWorkers,
                preserveOres = preserveOres,
                pickupItems = pickupItems,
                autoDeposit = autoDeposit
            )
        }
    }

    fun saveSelection(playerUuid: UUID, selection: FlattenSelection) {
        val sql = """
            INSERT INTO player_flatten_selection (
                player_uuid, pos1_world, pos1_x, pos1_y, pos1_z,
                pos2_world, pos2_x, pos2_y, pos2_z,
                chest_blocks, selected_workers, preserve_ores, pickup_items, auto_deposit
            ) VALUES (
                :playerUuid, :pos1World, :pos1X, :pos1Y, :pos1Z,
                :pos2World, :pos2X, :pos2Y, :pos2Z,
                :chestBlocks, :selectedWorkers, :preserveOres, :pickupItems, :autoDeposit
            ) ON CONFLICT(player_uuid) DO UPDATE SET
                pos1_world = excluded.pos1_world, pos1_x = excluded.pos1_x, pos1_y = excluded.pos1_y, pos1_z = excluded.pos1_z,
                pos2_world = excluded.pos2_world, pos2_x = excluded.pos2_x, pos2_y = excluded.pos2_y, pos2_z = excluded.pos2_z,
                chest_blocks = excluded.chest_blocks, selected_workers = excluded.selected_workers,
                preserve_ores = excluded.preserve_ores, pickup_items = excluded.pickup_items, auto_deposit = excluded.auto_deposit
        """.trimIndent()

        val p1 = selection.pos1
        val p2 = selection.pos2
        val chestDtos = mutableListOf<BlockLocDTO>()
        selection.outputChests.forEach { chestDtos.add(BlockLocDTO(it.world.name, it.x, it.y, it.z, "OUTPUT")) }
        selection.toolChests.forEach { chestDtos.add(BlockLocDTO(it.world.name, it.x, it.y, it.z, "TOOL")) }
        val chestJson = gson.toJson(chestDtos)
        val workersJson = gson.toJson(selection.selectedWorkers.map { it.toString() })

        open().use { conn ->
            conn.createQuery(sql)
                .addParameter("playerUuid", playerUuid.toString())
                .addParameter("pos1World", p1?.world?.name)
                .addParameter("pos1X", p1?.x)
                .addParameter("pos1Y", p1?.y)
                .addParameter("pos1Z", p1?.z)
                .addParameter("pos2World", p2?.world?.name)
                .addParameter("pos2X", p2?.x)
                .addParameter("pos2Y", p2?.y)
                .addParameter("pos2Z", p2?.z)
                .addParameter("chestBlocks", chestJson)
                .addParameter("selectedWorkers", workersJson)
                .addParameter("preserveOres", if (selection.preserveOres) 1 else 0)
                .addParameter("pickupItems", if (selection.pickupItems) 1 else 0)
                .addParameter("autoDeposit", if (selection.autoDeposit) 1 else 0)
                .executeUpdate()
        }
    }

    fun deleteSelection(playerUuid: UUID) {
        val sql = "DELETE FROM player_flatten_selection WHERE player_uuid = :uuid"
        open().use { conn ->
            conn.createQuery(sql).addParameter("uuid", playerUuid.toString()).executeUpdate()
        }
    }

    fun saveTask(fakePlayerUuid: UUID, action: FlattenAction) {
        val sql = """
            INSERT INTO fakeplayer_flatten_task (
                fakeplayer_uuid, world, min_x, max_x, min_y, max_y, min_z, max_z,
                preserve_ores, pickup_items, auto_deposit, chest_locations,
                total_blocks, cleared_blocks, created_at
            ) VALUES (
                :uuid, :world, :minX, :maxX, :minY, :maxY, :minZ, :maxZ,
                :preserveOres, :pickupItems, :autoDeposit, :chestLocations,
                :totalBlocks, :clearedBlocks, :createdAt
            ) ON CONFLICT(fakeplayer_uuid) DO UPDATE SET
                world = excluded.world, min_x = excluded.min_x, max_x = excluded.max_x,
                min_y = excluded.min_y, max_y = excluded.max_y, min_z = excluded.min_z, max_z = excluded.max_z,
                preserve_ores = excluded.preserve_ores, pickup_items = excluded.pickup_items, auto_deposit = excluded.auto_deposit,
                chest_locations = excluded.chest_locations,
                total_blocks = excluded.total_blocks, cleared_blocks = excluded.cleared_blocks
        """.trimIndent()

        val worldName = action.world?.name ?: action.chestWorld?.name ?: return
        val chestDtos = mutableListOf<BlockLocDTO>()
        action.outputChestLocations.forEach { chestDtos.add(BlockLocDTO(it.world.name, it.blockX, it.blockY, it.blockZ, "OUTPUT")) }
        action.toolChestLocations.forEach { chestDtos.add(BlockLocDTO(it.world.name, it.blockX, it.blockY, it.blockZ, "TOOL")) }
        if (chestDtos.isEmpty()) {
            action.chestLocations.forEach { chestDtos.add(BlockLocDTO(it.world.name, it.blockX, it.blockY, it.blockZ, "OUTPUT")) }
        }
        val chestJson = gson.toJson(chestDtos)

        open().use { conn ->
            conn.createQuery(sql)
                .addParameter("uuid", fakePlayerUuid.toString())
                .addParameter("world", worldName)
                .addParameter("minX", action.minX)
                .addParameter("maxX", action.maxX)
                .addParameter("minY", action.minY)
                .addParameter("maxY", action.maxY)
                .addParameter("minZ", action.minZ)
                .addParameter("maxZ", action.maxZ)
                .addParameter("preserveOres", if (action.preserveOres) 1 else 0)
                .addParameter("pickupItems", if (action.pickupItems) 1 else 0)
                .addParameter("autoDeposit", if (action.autoDeposit) 1 else 0)
                .addParameter("chestLocations", chestJson)
                .addParameter("totalBlocks", action.totalBlocks)
                .addParameter("clearedBlocks", action.clearedBlocks)
                .addParameter("createdAt", System.currentTimeMillis())
                .executeUpdate()
        }
    }

    fun updateTaskProgress(fakePlayerUuid: UUID, totalBlocks: Int, clearedBlocks: Int) {
        val sql = "UPDATE fakeplayer_flatten_task SET total_blocks = :total, cleared_blocks = :cleared WHERE fakeplayer_uuid = :uuid"
        open().use { conn ->
            conn.createQuery(sql)
                .addParameter("uuid", fakePlayerUuid.toString())
                .addParameter("total", totalBlocks)
                .addParameter("cleared", clearedBlocks)
                .executeUpdate()
        }
    }

    fun deleteTask(fakePlayerUuid: UUID) {
        val sql = "DELETE FROM fakeplayer_flatten_task WHERE fakeplayer_uuid = :uuid"
        open().use { conn ->
            conn.createQuery(sql).addParameter("uuid", fakePlayerUuid.toString()).executeUpdate()
        }
    }

    fun loadTask(fakePlayerUuid: UUID): FlattenAction? {
        val sql = "SELECT * FROM fakeplayer_flatten_task WHERE fakeplayer_uuid = :uuid LIMIT 1"
        return open().use { conn ->
            val row = conn.createQuery(sql)
                .addParameter("uuid", fakePlayerUuid.toString())
                .executeAndFetchTable()
                .rows()
                .firstOrNull() ?: return null

            val worldName = row.getString("world")
            val world = Bukkit.getWorld(worldName) ?: return null

            val action = FlattenAction(ActionMode.Continuous).apply {
                this.world = world
                this.minX = row.getInteger("min_x") ?: 0
                this.maxX = row.getInteger("max_x") ?: 0
                this.minY = row.getInteger("min_y") ?: 0
                this.maxY = row.getInteger("max_y") ?: 0
                this.minZ = row.getInteger("min_z") ?: 0
                this.maxZ = row.getInteger("max_z") ?: 0
                this.preserveOres = (row.getInteger("preserve_ores") ?: 0) == 1
                this.pickupItems = (row.getInteger("pickup_items") ?: 1) == 1
                this.autoDeposit = (row.getInteger("auto_deposit") ?: 1) == 1
                this.totalBlocks = row.getInteger("total_blocks") ?: 0
                this.clearedBlocks = row.getInteger("cleared_blocks") ?: 0

                val chestJson = row.getString("chest_locations")
                if (!chestJson.isNullOrBlank()) {
                    val dtos = runCatching { gson.fromJson<List<BlockLocDTO>>(chestJson, blockLocListType) }.getOrNull() ?: emptyList()
                    for (dto in dtos) {
                        val cw = Bukkit.getWorld(dto.world) ?: continue
                        val loc = Location(cw, dto.x.toDouble(), dto.y.toDouble(), dto.z.toDouble())
                        this.chestLocations.add(loc)
                        if (dto.role.equals("TOOL", ignoreCase = true)) {
                            this.toolChestLocations.add(loc)
                        } else {
                            this.outputChestLocations.add(loc)
                        }
                    }
                    if (this.chestLocations.isNotEmpty()) {
                        val first = this.chestLocations.first()
                        this.chestWorld = first.world
                        this.chestX = first.blockX
                        this.chestY = first.blockY
                        this.chestZ = first.blockZ
                    }
                }
            }
            action
        }
    }

    fun findAllActiveTasks(): Map<UUID, FlattenAction> {
        val sql = "SELECT * FROM fakeplayer_flatten_task"
        return open().use { conn ->
            val rows = conn.createQuery(sql).executeAndFetchTable().rows()
            val result = mutableMapOf<UUID, FlattenAction>()
            for (row in rows) {
                val uuidStr = row.getString("fakeplayer_uuid") ?: continue
                val uuid = runCatching { UUID.fromString(uuidStr) }.getOrNull() ?: continue
                val worldName = row.getString("world") ?: continue
                val world = Bukkit.getWorld(worldName) ?: continue

                val action = FlattenAction(ActionMode.Continuous).apply {
                    this.world = world
                    this.minX = row.getInteger("min_x") ?: 0
                    this.maxX = row.getInteger("max_x") ?: 0
                    this.minY = row.getInteger("min_y") ?: 0
                    this.maxY = row.getInteger("max_y") ?: 0
                    this.minZ = row.getInteger("min_z") ?: 0
                    this.maxZ = row.getInteger("max_z") ?: 0
                    this.preserveOres = (row.getInteger("preserve_ores") ?: 0) == 1
                    this.pickupItems = (row.getInteger("pickup_items") ?: 1) == 1
                    this.autoDeposit = (row.getInteger("auto_deposit") ?: 1) == 1
                    this.totalBlocks = row.getInteger("total_blocks") ?: 0
                    this.clearedBlocks = row.getInteger("cleared_blocks") ?: 0

                    val chestJson = row.getString("chest_locations")
                    if (!chestJson.isNullOrBlank()) {
                        val dtos = runCatching { gson.fromJson<List<BlockLocDTO>>(chestJson, blockLocListType) }.getOrNull() ?: emptyList()
                        for (dto in dtos) {
                            val cw = Bukkit.getWorld(dto.world) ?: continue
                            val loc = Location(cw, dto.x.toDouble(), dto.y.toDouble(), dto.z.toDouble())
                            this.chestLocations.add(loc)
                            if (dto.role.equals("TOOL", ignoreCase = true)) {
                                this.toolChestLocations.add(loc)
                            } else {
                                this.outputChestLocations.add(loc)
                            }
                        }
                        if (this.chestLocations.isNotEmpty()) {
                            val first = this.chestLocations.first()
                            this.chestWorld = first.world
                            this.chestX = first.blockX
                            this.chestY = first.blockY
                            this.chestZ = first.blockZ
                        }
                    }
                }
                result[uuid] = action
            }
            result
        }
    }
}

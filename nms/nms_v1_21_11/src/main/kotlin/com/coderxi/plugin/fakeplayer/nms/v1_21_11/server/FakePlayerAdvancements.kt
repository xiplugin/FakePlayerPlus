package com.coderxi.plugin.fakeplayer.nms.v1_21_11.server

import com.mojang.datafixers.DataFixer
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.server.PlayerAdvancements
import net.minecraft.server.ServerAdvancementManager
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.PlayerList
import java.nio.file.Path

class FakePlayerAdvancements(
    dataFixer: DataFixer,
    playerManager: PlayerList,
    advancementLoader: ServerAdvancementManager,
    filePath: Path,
    owner: ServerPlayer
) : PlayerAdvancements(dataFixer, playerManager, advancementLoader, filePath, owner) {
    init { runCatching { super.stopListening() } }
    override fun setPlayer(owner: ServerPlayer) {}
    override fun stopListening() {}
    override fun reload(advancementLoader: ServerAdvancementManager) {}
    override fun save() {}
    override fun award(advancement: AdvancementHolder, criterionName: String) = false
    override fun revoke(advancement: AdvancementHolder, criterionName: String) = false
    override fun flushDirty(player: ServerPlayer, showAdvancements: Boolean) {}
    override fun setSelectedTab(advancement: AdvancementHolder?) {}
    override fun getOrStartProgress(advancement: AdvancementHolder) = AdvancementProgress()
}
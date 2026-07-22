package com.coderxi.plugin.fakeplayer.api.action

import com.coderxi.plugin.fakeplayer.api.action.ActionTrack.*

enum class ActionType(val track: ActionTrack) {
    ATTACK(INTERACTION),
    MINE(INTERACTION),
    USE_ITEM(INTERACTION),
    JUMP(POSTURE),
    SNEAK(POSTURE);
}
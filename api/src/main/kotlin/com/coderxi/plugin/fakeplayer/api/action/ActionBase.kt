package com.coderxi.plugin.fakeplayer.api.action

sealed interface Action {
    val type: ActionType
    val track: ActionTrack
}

interface AttackAction : Action {
    companion object { val track = ActionTrack.INTERACTION }
    override val type get() = ActionType.ATTACK
    override val track get() = Companion.track
}

interface MineAction : Action {
    companion object { val track = ActionTrack.INTERACTION }
    override val type get() = ActionType.MINE
    override val track get() = Companion.track
    var freezeTick : Int
}

interface UseItemAction : Action {
    companion object { val track = ActionTrack.INTERACTION }
    override val type get() = ActionType.USE_ITEM
    override val track get() = Companion.track
    var freezeTick : Int
}

interface JumpAction : Action {
    companion object { val track = ActionTrack.POSTURE }
    override val type get() = ActionType.JUMP
    override val track get() = Companion.track
}

interface SneakAction : Action {
    companion object { val track = ActionTrack.POSTURE }
    override val type get() = ActionType.SNEAK
    override val track get() = Companion.track
}

interface TurnAroundAction : Action {
    companion object { val track = ActionTrack.GLOBAL }
    override val type get() = ActionType.TURN_AROUND
    override val track get() = Companion.track
}

interface LookAtEntityAction : Action {
    companion object { val track = ActionTrack.GLOBAL }
    override val type get() = ActionType.LOOK_AT
    override val track get() = Companion.track
}
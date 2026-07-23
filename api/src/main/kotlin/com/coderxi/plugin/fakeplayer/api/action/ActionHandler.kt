package com.coderxi.plugin.fakeplayer.api.action

interface ActionHandler {
    fun dispatch(action: Action)
    fun doTick()
    fun stop(track: ActionTrack)
    fun stop(actionType: ActionType) = stop(actionType.track)
    fun stop(action: Action)
    fun stopAll()
    fun getActiveActions(): Map<ActionTrack, Action>
}
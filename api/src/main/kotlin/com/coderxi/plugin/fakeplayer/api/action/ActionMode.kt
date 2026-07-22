package com.coderxi.plugin.fakeplayer.api.action

import com.coderxi.plugin.fakeplayer.api.utils.Order
import com.coderxi.plugin.fakeplayer.api.utils.ParamName

sealed interface ActionMode {

    @Order(1)
    object Once : ActionMode

    @Order(2)
    data class Interval(@ParamName("intervalTicks")val intervalTicks: Int) : ActionMode

    @Order(3)
    object Continuous : ActionMode

}
package com.coderxi.plugin.fakeplayer.api.command

interface Permission {

    val node: String

    data class Of(override val node: String) : Permission

    fun params(vararg params: Pair<String, Any>): Permission = Of(params.fold(node) { str, (k, v) -> str.replace("{$k}", v.toString()) })

}
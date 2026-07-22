package com.coderxi.plugin.fakeplayer.api.utils

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ParamName(val value: String)
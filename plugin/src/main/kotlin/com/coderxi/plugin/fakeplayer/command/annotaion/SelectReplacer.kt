package com.coderxi.plugin.fakeplayer.command.annotaion

import revxrsal.commands.annotation.Default
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.dynamic.AnnotationReplacer
import revxrsal.commands.annotation.dynamic.Annotations
import java.lang.reflect.AnnotatedElement

class SelectReplacer: AnnotationReplacer<Select> {
    override fun replaceAnnotation(element: AnnotatedElement, annotation: Select): Collection<Annotation?> = listOf(
        Annotations.create(Default::class.java, "value",""),
        Annotations.create(Named::class.java, "value","name")
    )
}
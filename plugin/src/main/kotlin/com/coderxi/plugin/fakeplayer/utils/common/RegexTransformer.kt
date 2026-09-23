package com.coderxi.plugin.fakeplayer.utils.common

import eu.okaeri.configs.schema.GenericsDeclaration
import eu.okaeri.configs.schema.GenericsPair
import eu.okaeri.configs.serdes.BidirectionalTransformer
import eu.okaeri.configs.serdes.SerdesContext

class RegexTransformer : BidirectionalTransformer<String, Regex>() {
    override fun getPair(): GenericsPair<String?, Regex?> =
        GenericsPair(GenericsDeclaration.of(String::class.java), GenericsDeclaration.of(Regex::class.java))
    override fun leftToRight(data: String, serdesContext: SerdesContext) = Regex(data)
    override fun rightToLeft(data: Regex, serdesContext: SerdesContext) = data.pattern
}
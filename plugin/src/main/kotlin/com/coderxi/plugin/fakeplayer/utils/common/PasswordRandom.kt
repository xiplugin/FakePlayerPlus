package com.coderxi.plugin.fakeplayer.utils.common

import java.security.SecureRandom

object PasswordRandom {

    private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private const val CHARS_SPECIAL = "!@#$%^&*()-_=+[]{};:',.<>|~"
    private val secureRandom = SecureRandom()

    fun randomPassword(length: Int = 16, includeSpecialChars: Boolean = false): String {
        val chars = if (includeSpecialChars) {
            CHARS + CHARS_SPECIAL
        } else {
            CHARS
        }
        return CharArray(length) {
            chars[secureRandom.nextInt(chars.length)]
        }.concatToString()
    }

}
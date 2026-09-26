package com.coderxi.plugin.fakeplayer.provider.login

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.utils.bukkit.PluginApiProxy
import com.coderxi.plugin.fakeplayer.utils.common.PasswordRandom.randomPassword
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class AuthMeAuthProvider : AuthProvider.ByPlugin("AuthMe") {

    private val authMeApiProxy by lazy { hookedPlugin?.let { AuthMeApiProxy(it) } }

    override fun forceAuth(fakePlayer: FakePlayer) {
        val authMeApi = authMeApiProxy ?: return
        if (!authMeApi.isRegistered(fakePlayer.name)) {
            authMeApi.forceRegister(fakePlayer.player, randomPassword(includeSpecialChars = true))
        } else {
            authMeApi.forceLogin(fakePlayer.player)
        }
    }

    //https://github.com/AuthMe/AuthMeReloaded/blob/master/authme-core/src/main/java/fr/xephi/authme/api/v3/AuthMeApi.java
    class AuthMeApiProxy(plugin: Plugin): PluginApiProxy(plugin) {
        private val apiClass by lazy { getClass("fr.xephi.authme.api.v3.AuthMeApi")!! }
        private val apiInstance = apiClass.getMethod("getInstance").invoke(null)
        private val isRegisteredMethod = apiClass.getMethod("isRegistered", String::class.java)
        fun isRegistered(playerName: String) : Boolean  = isRegisteredMethod.invoke(apiInstance, playerName) as Boolean
        private val forceRegisterMethod = apiClass.getMethod("forceRegister", Player::class.java, String::class.java)
        fun forceRegister(player: Player, password: String) { forceRegisterMethod.invoke(apiInstance, player, password) }
        private val forceLoginMethod = apiClass.getMethod("forceLogin", Player::class.java)
        fun forceLogin(player: Player) { forceLoginMethod.invoke(apiInstance, player) }
    }

}
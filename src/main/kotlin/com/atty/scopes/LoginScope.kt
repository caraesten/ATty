package com.atty.scopes

import bsky4j.ATProtocolException
import com.atty.DisconnectReason
import com.atty.libs.BlueskyClient
import com.atty.models.StartupOptions
import java.net.Socket

const val WELCOME_TEXT = "WELCOME TO BSKY.TEL,\r\nTHE TELNET BLUESKY CLIENT\r\n"

class LoginScope (socket: Socket, threadProvider: () -> Thread, disconnectHandler: (DisconnectReason) -> Unit) : BaseScope(socket, threadProvider, disconnectHandler) {
    fun performLogin(block: MenuScope.() -> Unit) {
        try {
            var isLoggedIn = false
            var blueskyClient: BlueskyClient? = null
            var isCommodore = false
            var fullImages = false
            while (!isLoggedIn) {
                socket.getOutputStream().write(WELCOME_TEXT.toByteArray())
                waitForReturnKey()
                socket.getOutputStream().write("\r\nCOMMODORE 64/128 (Y/N)?: ".toByteArray())
                isCommodore = waitForStringInput().uppercase() == "Y"

                writePrompt("Username", isCommodore)
                val usernameInput = waitForStringInput()
                writePrompt("Password", isCommodore)
                val passwordInput = waitForStringInput(isCommodore)

                try {
                    blueskyClient = BlueskyClient(usernameInput, passwordInput)
                    isLoggedIn = true
                } catch (e: ATProtocolException) {
                    socket.getOutputStream().write("\r\nINVALID CREDENTIALS\r\n".toByteArray())
                }
            }
            clearScreen()
            MenuScope(blueskyClient!!, socket, StartupOptions(isCommodore, fullImages), threadProvider, disconnectHandler).apply(block)
        } catch (ex: Throwable) {
            ex.printStackTrace()
            disconnectHandler(DisconnectReason.EXCEPTION)
        }
    }
}
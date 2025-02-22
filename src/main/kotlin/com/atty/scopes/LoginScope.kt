package com.atty.scopes

import com.atty.DisconnectReason
import com.atty.libs.BlueskyClient
import com.atty.models.ImageMode
import com.atty.models.StartupOptions
import work.socialhub.kbsky.ATProtocolException
import java.net.Socket
import java.net.SocketException

const val WELCOME_TEXT = "WELCOME TO BSKY.TEL,\r\nTHE TELNET BLUESKY CLIENT\r\n"

class LoginScope (socket: Socket, threadProvider: () -> Thread, disconnectHandler: (DisconnectReason) -> Unit) : BaseScope(socket, threadProvider, disconnectHandler) {
    fun performLogin(block: MenuScope.() -> Unit) {
        try {
            var isLoggedIn = false
            var blueskyClient: BlueskyClient? = null
            var isCommodore = false
            var imageMode = ImageMode.NoImages
            while (!isLoggedIn) {
                socket.getOutputStream().write(WELCOME_TEXT.toByteArray())
                waitForReturnKey()
                socket.getOutputStream().write("\r\nCOMMODORE 64/128 (Y/N)?: ".toByteArray())
                isCommodore = waitForStringInput().uppercase() == "Y"

                if (!isCommodore) {
                    socket.getOutputStream().write("\r\nIMAGES: Alt [T]ext (default), [A]SCII, [S]ixels".toByteArray())
                    socket.getOutputStream().write("\r\n(T,A,S)?: ".toByteArray())
                    imageMode = ImageMode.fromStringInput(waitForStringInput().uppercase())
                }

                writePrompt("Username", isCommodore)
                val usernameInput = waitForStringInput()
                writePrompt("Password", isCommodore)
                val passwordInput = waitForStringInput(isCommodore)

                try {
                    blueskyClient = BlueskyClient(usernameInput, passwordInput, imageMode)
                    isLoggedIn = true
                } catch (e: ATProtocolException) {
                    socket.getOutputStream().write("\r\nINVALID CREDENTIALS\r\n".toByteArray())
                }
            }
            clearScreen()
            MenuScope(blueskyClient!!, socket, StartupOptions(isCommodore, imageMode), threadProvider, disconnectHandler).apply(block)
        } catch (ex: Throwable) {
            // Normal disconnects shouldn't pollute logs
            if (ex !is SocketException) {
                ex.printStackTrace()
            }
            disconnectHandler(DisconnectReason.EXCEPTION)
        }
    }
}
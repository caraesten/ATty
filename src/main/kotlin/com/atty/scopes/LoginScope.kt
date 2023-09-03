package com.atty.scopes

import bsky4j.ATProtocolException
import com.atty.DisconnectReason
import com.atty.libs.BlueskyClient
import io.ktor.network.sockets.*
import io.ktor.utils.io.*

const val WELCOME_TEXT = "WELCOME TO BSKY.TEL,\r\nTHE TELNET BLUESKY CLIENT\r\n"

class LoginScope (connection: Connection, disconnectHandler: suspend (DisconnectReason) -> Unit) : BaseScope(connection, disconnectHandler) {
    suspend fun performLogin(block: suspend MenuScope.() -> Unit) {
        try {
            var isLoggedIn = false
            var blueskyClient: BlueskyClient? = null
            var isCommodore = false
            while (!isLoggedIn) {
                connection.output.writeStringUtf8(WELCOME_TEXT)
                waitForReturnKey()
                connection.output.writeStringUtf8("\r\nCOMMODORE 64/128 (Y/N)?: ")
                isCommodore = waitForStringInput().uppercase() == "Y"

                writePrompt("Username", isCommodore)
                val usernameInput = waitForStringInput()
                writePrompt("Password", isCommodore)
                val passwordInput = waitForStringInput(isCommodore)

                try {
                    blueskyClient = BlueskyClient(usernameInput, passwordInput)
                    isLoggedIn = true
                } catch (e: ATProtocolException) {
                    connection.output.writeStringUtf8("\r\nINVALID CREDENTIALS\r\n")
                }
            }
            clearScreen()
            MenuScope(blueskyClient!!, connection, isCommodore, disconnectHandler).apply { block() }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            disconnectHandler(DisconnectReason.EXCEPTION)
        }
    }
}
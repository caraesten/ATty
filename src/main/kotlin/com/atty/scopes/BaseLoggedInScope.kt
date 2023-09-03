package com.atty.scopes

import com.atty.DisconnectHandler
import com.atty.libs.BlueskyReadClient
import io.ktor.network.sockets.*

abstract class BaseLoggedInScope(
    val blueskyClient: BlueskyReadClient,
    connection: Connection,
    val isCommodore: Boolean,
    disconnectHandler: DisconnectHandler,
) : BaseScope(connection, disconnectHandler) {

    suspend fun waitForStringInput(): String {
        return waitForStringInput(isCommodore)
    }

    suspend fun writeUi(text: String) {
        writeUi(text, isCommodore)
    }

    suspend fun writeAppText(text: String) {
        writeAppText(text, isCommodore)
    }

    suspend fun writePrompt(text: String = "") {
        writePrompt(text, isCommodore)
    }
}
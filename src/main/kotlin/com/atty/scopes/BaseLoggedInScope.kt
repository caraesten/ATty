package com.atty.scopes

import com.atty.DisconnectReason
import com.atty.libs.BlueskyReadClient
import java.net.Socket

abstract class BaseLoggedInScope(
    val blueskyClient: BlueskyReadClient,
    clientSocket: Socket,
    val isCommodore: Boolean,
    threadProvider: () -> Thread,
    disconnectHandler: (DisconnectReason) -> Unit,
) : BaseScope(clientSocket, threadProvider, disconnectHandler) {

    fun waitForStringInput(): String {
        return waitForStringInput(isCommodore)
    }

    fun writeUi(text: String) {
        writeUi(text, isCommodore)
    }

    fun writeAppText(text: String) {
        writeAppText(text, isCommodore)
    }

    fun writePrompt(text: String = "") {
        writePrompt(text, isCommodore)
    }
}
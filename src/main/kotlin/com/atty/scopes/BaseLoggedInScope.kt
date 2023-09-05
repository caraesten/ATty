package com.atty.scopes

import com.atty.DisconnectReason
import com.atty.libs.BlueskyReadClient
import com.atty.models.StartupOptions
import java.net.Socket

abstract class BaseLoggedInScope(
    val blueskyClient: BlueskyReadClient,
    clientSocket: Socket,
    val startupOptions: StartupOptions,
    threadProvider: () -> Thread,
    disconnectHandler: (DisconnectReason) -> Unit,
) : BaseScope(clientSocket, threadProvider, disconnectHandler) {

    fun waitForStringInput(): String {
        return waitForStringInput(startupOptions.isCommodore)
    }

    fun writeUi(text: String) {
        writeUi(text, startupOptions.isCommodore)
    }

    fun writeAppText(text: String) {
        writeAppText(text, startupOptions.isCommodore)
    }

    fun writePrompt(text: String = "") {
        writePrompt(text, startupOptions.isCommodore)
    }
}
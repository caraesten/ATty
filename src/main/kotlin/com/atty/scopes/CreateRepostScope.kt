package com.atty.scopes

import com.atty.DisconnectReason
import com.atty.libs.BlueskyReadClient
import com.atty.models.GenericPostAttributes
import com.atty.models.StartupOptions
import java.net.Socket

class CreateRepostScope(
    val genericPostAttributes: GenericPostAttributes,
    blueskyClient: BlueskyReadClient,
    clientSocket: Socket,
    disconnectHandler: (DisconnectReason) -> Unit,
    startupOptions: StartupOptions,
    threadProvider: () -> Thread) : BaseLoggedInScope(blueskyClient, clientSocket, startupOptions, threadProvider, disconnectHandler) {
        fun showReposted() {
            writeUi("Reposted")
        }
    }
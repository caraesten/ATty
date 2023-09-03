package com.atty.scopes

import com.atty.DisconnectHandler
import com.atty.libs.BlueskyReadClient
import com.atty.models.GenericPostAttributes
import io.ktor.network.sockets.*

class CreateRepostScope(
    val genericPostAttributes: GenericPostAttributes,
    blueskyClient: BlueskyReadClient,
    connection: Connection,
    disconnectHandler: DisconnectHandler,
    isCommodore: Boolean) : BaseLoggedInScope(blueskyClient, connection, isCommodore, disconnectHandler) {
        suspend fun showReposted() {
            writeUi("Reposted")
        }
    }
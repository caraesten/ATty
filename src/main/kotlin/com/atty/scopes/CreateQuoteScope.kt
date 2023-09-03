package com.atty.scopes

import com.atty.DisconnectHandler
import com.atty.libs.BlueskyReadClient
import com.atty.models.GenericPostAttributes
import com.atty.models.PendingPost
import io.ktor.network.sockets.*
import io.ktor.utils.io.*

class CreateQuoteScope(
    val genericPostAttributes: GenericPostAttributes,
    blueskyClient: BlueskyReadClient,
    connection: Connection,
    disconnectHandler: DisconnectHandler,
    isCommodore: Boolean) : BaseLoggedInScope(blueskyClient, connection, isCommodore, disconnectHandler) {

    suspend fun getPost(): PendingPost {
        connection.output.writeStringUtf8(
            "\n>"
        )

        var inputString = waitForStringInput()
        while (inputString.length > 300) {
            writeUi(
                "Post too long (must be 300 characters or less)"
            )
            writePrompt()
            inputString = waitForStringInput()
        }
        return PendingPost(inputString, null, genericPostAttributes)
    }

    suspend fun showQuoted() {
        writeUi("Quoted")
    }
}

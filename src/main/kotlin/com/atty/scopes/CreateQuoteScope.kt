package com.atty.scopes

import com.atty.DisconnectReason
import com.atty.libs.BlueskyReadClient
import com.atty.models.GenericPostAttributes
import com.atty.models.PendingPost
import java.net.Socket

class CreateQuoteScope(
    val genericPostAttributes: GenericPostAttributes,
    blueskyClient: BlueskyReadClient,
    clientSocket: Socket,
    disconnectHandler: (DisconnectReason) -> Unit,
    isCommodore: Boolean,
    threadProvider: () -> Thread) : BaseLoggedInScope(blueskyClient, clientSocket, isCommodore, threadProvider, disconnectHandler) {

    fun getPost(): PendingPost {
        socket.getOutputStream().write(
            "\n>".toByteArray()
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

    fun showQuoted() {
        writeUi("Quoted")
    }
}

package com.atty.libs

import com.atty.models.GenericPostAttributes
import com.atty.waitForStringInput
import java.net.Socket

data class PendingPost(
    val text: String,
    val inReplyTo: GenericPostAttributes?
)

class PostSender(val socket: Socket) {
    fun getPendingPost(inReplyTo: GenericPostAttributes? = null): PendingPost {
        socket.getOutputStream().write(
            "\n>".toByteArray()
        )

        var inputString = socket.waitForStringInput()
        while (inputString.length > 300) {
            socket.getOutputStream().write(
                "\nPost too long (must be 300 characters or less)\n".toByteArray()
            )
            socket.getOutputStream().write(
                "\n>".toByteArray()
            )
            inputString = socket.waitForStringInput()
        }
        return PendingPost(inputString, inReplyTo)
    }
}
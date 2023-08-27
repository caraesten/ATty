package com.atty.libs

import bsky4j.model.bsky.feed.FeedPost
import com.atty.models.AuthorAttributes
import com.atty.models.GenericPostAttributes
import com.atty.waitForReturnKey
import com.atty.waitForStringInput
import java.net.Socket

enum class PostContext {
    AsPost,
    AsNotification
}

class PostReader(val author: AuthorAttributes, val feedPost: FeedPost, val blueskyClient: BlueskyClient, val socket: Socket, val genericPostAttributes: GenericPostAttributes? = null) {
    fun readPost(context: PostContext = PostContext.AsPost) {
        socket.getOutputStream().write(
            "\n ${author.displayName} (${author.handle}) \n ${feedPost.text}\n".toByteArray()
        )
        if (context == PostContext.AsPost) {
            readPostActions()
        } else {
            socket.waitForReturnKey()
        }
    }

    private fun readPostActions() {
        val options = "\n [R]eply Re[P]ost [Q]uote [L]ike \n".toByteArray()
        socket.getOutputStream().write(
            options
        )
        var stringIn = socket.waitForStringInput()
        if (stringIn.isNotEmpty()) {
            while (stringIn.length != 1 || !validActions.contains(stringIn.uppercase().first())) {
                socket.getOutputStream().write(
                    options
                )
                stringIn = socket.waitForStringInput()
            }
            when (stringIn.uppercase().first()) {
                'R' -> {
                    val pendingPost = PostSender(socket).getPendingPost(genericPostAttributes)
                    blueskyClient.sendPost(pendingPost)
                    socket.getOutputStream().write(
                        "\n Sent post! \n".toByteArray()
                    )
                }
                'P' -> {
                    blueskyClient.repost(genericPostAttributes!!)
                    socket.getOutputStream().write(
                        "\n Reskeeted! \n".toByteArray()
                    )
                }
                'Q' -> {

                }
                'L' -> {
                    blueskyClient.like(genericPostAttributes!!)
                    socket.getOutputStream().write(
                        "\n Liked! \n".toByteArray()
                    )
                }
            }
        }
    }

    companion object {
        val validActions = setOf('R', 'P', 'Q', 'L')
    }
}
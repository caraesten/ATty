package com.atty.scopes

import bsky4j.model.bsky.feed.FeedPost
import com.atty.DisconnectReason
import com.atty.libs.BlueskyReadClient
import com.atty.models.AuthorAttributes
import com.atty.models.GenericPostAttributes
import java.net.Socket

enum class PostContext {
    AsPost, AsNotification
}

class PostScope (
    val author: AuthorAttributes,
    val feedPost: FeedPost,
    val genericPostAttributes: GenericPostAttributes? = null,
    blueskyClient: BlueskyReadClient,
    socket: Socket,
    isCommodore: Boolean,
    threadProvider: () -> Thread,
    disconnectHandler: (DisconnectReason) -> Unit) :
    BaseLoggedInScope(blueskyClient, socket, isCommodore, threadProvider, disconnectHandler) {

    fun readPost(context: PostContext = PostContext.AsPost, onReply: CreatePostScope.() -> Unit, onRepost: CreateRepostScope.() -> Unit, onQuote: CreateQuoteScope.() -> Unit, onLike: CreateLikeScope.() -> Unit) {
        writeAppText(
            "${author.displayName} (${author.handle}) \r\n ${feedPost.text}"
        )
        if (context == PostContext.AsPost) {
            readPostActions(onReply, onRepost, onQuote, onLike)
        } else {
            waitForReturnKey()
        }
    }

    private fun readPostActions(onReply: CreatePostScope.() -> Unit, onRepost: CreateRepostScope.() -> Unit, onQuote: CreateQuoteScope.() -> Unit, onLike: CreateLikeScope.() -> Unit) {
        val options = "[R]eply Re[P]ost [Q]uote [L]ike"
        writeUi(options)
        var stringIn = waitForStringInput()
        if (stringIn.isNotEmpty()) {
            while (stringIn.isNotEmpty() && (stringIn.length != 1 || !validActions.contains(stringIn.uppercase().first()))) {
                writeUi("Invalid option. Enter valid option or enter to continue.")
                writeUi(options)
                stringIn = waitForStringInput()
            }
            if (stringIn.isEmpty()) {
                return
            }
            when (stringIn.uppercase().first()) {
                'R' -> {
                    CreatePostScope(genericPostAttributes, blueskyClient, socket, disconnectHandler, isCommodore, threadProvider).apply(onReply)
                }
                'P' -> {
                    CreateRepostScope(genericPostAttributes!!, blueskyClient, socket, disconnectHandler, isCommodore, threadProvider).apply(onRepost)
                }
                'Q' -> {
                    CreateQuoteScope(genericPostAttributes!!, blueskyClient, socket, disconnectHandler, isCommodore, threadProvider).apply(onQuote)
                }
                'L' -> {
                    CreateLikeScope(genericPostAttributes!!, blueskyClient, socket, disconnectHandler, isCommodore, threadProvider).apply(onLike)
                }
            }
        }
    }

    companion object {
        val validActions = setOf('R', 'P', 'Q', 'L')
    }
}
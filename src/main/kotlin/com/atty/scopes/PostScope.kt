package com.atty.scopes

import bsky4j.model.bsky.feed.FeedPost
import com.atty.DisconnectReason
import com.atty.libs.BlueskyReadClient
import com.atty.libs.isReply
import com.atty.models.AuthorAttributes
import com.atty.models.GenericPostAttributes
import java.net.Socket

enum class PostContext {
    AsPost, AsNotification, AsReply
}

class PostScope (
    val author: AuthorAttributes,
    val feedPost: FeedPost,
    val genericPostAttributes: GenericPostAttributes,
    blueskyClient: BlueskyReadClient,
    socket: Socket,
    isCommodore: Boolean,
    threadProvider: () -> Thread,
    disconnectHandler: (DisconnectReason) -> Unit) :
    BaseLoggedInScope(blueskyClient, socket, isCommodore, threadProvider, disconnectHandler) {

    fun readPost(context: PostContext = PostContext.AsPost, onContext: ReplyContextScope.() -> Unit, onReply: CreatePostScope.() -> Unit, onRepost: CreateRepostScope.() -> Unit, onQuote: CreateQuoteScope.() -> Unit, onLike: CreateLikeScope.() -> Unit) {
        writeAppText(
            "${if (context == PostContext.AsReply) ">>> " else ""}${author.displayName} (${author.handle}) \r\n ${if (feedPost.isReply()) "Reply: " else ""}${feedPost.text}"
        )
        if (context == PostContext.AsPost) {
            readPostActions(context, onContext, onReply, onRepost, onQuote, onLike)
        } else {
            waitForReturnKey()
        }
    }

    private fun readPostActions(context: PostContext, onContext: ReplyContextScope.() -> Unit, onReply: CreatePostScope.() -> Unit, onRepost: CreateRepostScope.() -> Unit, onQuote: CreateQuoteScope.() -> Unit, onLike: CreateLikeScope.() -> Unit) {
        val contextOption = "[C]ontext "
        val showContext = feedPost.isReply() && context != PostContext.AsReply
        val options = "${if (showContext) contextOption else ""}[R]eply Re[P]ost [Q]uote [L]ike"
        writeUi(options)
        var stringIn = waitForStringInput()
        val validActions = if (showContext) validForReplyActions else validForPostActions
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
                'C' -> {
                    val replies = blueskyClient.fetchThread(genericPostAttributes.uri, 3)
                    ReplyContextScope(replies, blueskyClient, socket, isCommodore, threadProvider, disconnectHandler).apply(onContext)
                }
                'R' -> {
                    CreatePostScope(genericPostAttributes, blueskyClient, socket, disconnectHandler, isCommodore, threadProvider).apply(onReply)
                }
                'P' -> {
                    CreateRepostScope(genericPostAttributes, blueskyClient, socket, disconnectHandler, isCommodore, threadProvider).apply(onRepost)
                }
                'Q' -> {
                    CreateQuoteScope(genericPostAttributes, blueskyClient, socket, disconnectHandler, isCommodore, threadProvider).apply(onQuote)
                }
                'L' -> {
                    CreateLikeScope(genericPostAttributes, blueskyClient, socket, disconnectHandler, isCommodore, threadProvider).apply(onLike)
                }
            }
        }
    }

    companion object {
        const val DEFAULT_THREAD_DEPTH = 3
        val validForPostActions = setOf('R', 'P', 'Q', 'L')
        val validForReplyActions = setOf('R', 'P', 'C', 'Q', 'L')
    }
}
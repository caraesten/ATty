package com.atty.scopes

import bsky4j.model.bsky.feed.FeedPost
import com.atty.DisconnectHandler
import com.atty.libs.BlueskyReadClient
import com.atty.libs.isReply
import com.atty.models.AuthorAttributes
import com.atty.models.GenericPostAttributes
import io.ktor.network.sockets.*

enum class PostContext {
    AsPost, AsNotification, AsReply
}

class PostScope (
    val author: AuthorAttributes,
    val feedPost: FeedPost,
    val genericPostAttributes: GenericPostAttributes,
    blueskyClient: BlueskyReadClient,
    connection: Connection,
    isCommodore: Boolean,
    disconnectHandler: DisconnectHandler) :
    BaseLoggedInScope(blueskyClient, connection, isCommodore, disconnectHandler) {

    suspend fun readPost(context: PostContext = PostContext.AsPost, onContext: suspend ReplyContextScope.() -> Unit, onReply: suspend CreatePostScope.() -> Unit, onRepost: suspend CreateRepostScope.() -> Unit, onQuote: suspend CreateQuoteScope.() -> Unit, onLike: suspend CreateLikeScope.() -> Unit) {
        writeAppText(
            "${if (context == PostContext.AsReply) ">>> " else ""}${author.displayName} (${author.handle}) \r\n ${if (feedPost.isReply()) "Reply: " else ""}${feedPost.text}"
        )
        if (context == PostContext.AsPost) {
            readPostActions(context, onContext, onReply, onRepost, onQuote, onLike)
        } else {
            waitForReturnKey()
        }
    }

    private suspend fun readPostActions(context: PostContext, onContext: suspend ReplyContextScope.() -> Unit, onReply: suspend CreatePostScope.() -> Unit, onRepost: suspend CreateRepostScope.() -> Unit, onQuote: suspend CreateQuoteScope.() -> Unit, onLike: suspend CreateLikeScope.() -> Unit) {
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
                    ReplyContextScope(replies, blueskyClient, connection, isCommodore, disconnectHandler).apply { onContext() }
                }
                'R' -> {
                    CreatePostScope(genericPostAttributes, blueskyClient, connection, disconnectHandler, isCommodore).apply { onReply() }
                }
                'P' -> {
                    CreateRepostScope(genericPostAttributes, blueskyClient, connection, disconnectHandler, isCommodore).apply { onRepost() }
                }
                'Q' -> {
                    CreateQuoteScope(genericPostAttributes, blueskyClient, connection, disconnectHandler, isCommodore).apply { onQuote() }
                }
                'L' -> {
                    CreateLikeScope(genericPostAttributes, blueskyClient, connection, disconnectHandler, isCommodore).apply { onLike() }
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
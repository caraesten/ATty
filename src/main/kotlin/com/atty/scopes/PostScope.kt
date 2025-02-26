package com.atty.scopes

import com.atty.DisconnectReason
import com.atty.libs.BlueskyReadClient
import com.atty.libs.isReply
import com.atty.models.AuthorAttributes
import com.atty.models.GenericPostAttributes
import com.atty.models.ImageMode
import com.atty.models.StartupOptions
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImages
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImagesView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
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
    startupOptions: StartupOptions,
    threadProvider: () -> Thread,
    disconnectHandler: (DisconnectReason) -> Unit) :
    BaseLoggedInScope(blueskyClient, socket, startupOptions, threadProvider, disconnectHandler) {

    fun readPost(context: PostContext = PostContext.AsPost, onContext: ReplyContextScope.() -> Unit, onReply: CreatePostScope.() -> Unit, onRepost: CreateRepostScope.() -> Unit, onQuote: CreateQuoteScope.() -> Unit, onLike: CreateLikeScope.() -> Unit) {
        writeAppText(
            "${if (context == PostContext.AsReply) ">>> " else ""}${author.displayName} (${author.handle}) \r\n ${if (feedPost.isReply()) "Reply: " else ""}${feedPost.text}"
        )
        readImages()
        if (context == PostContext.AsPost) {
            readPostActions(context, onContext, onReply, onRepost, onQuote, onLike)
        } else {
            waitForReturnKey()
        }
    }

    private fun readImages() {
        val embed = feedPost.embed
        val embedView = genericPostAttributes.embedView
        val images = embedView as? EmbedImagesView
        if (startupOptions.imageMode != ImageMode.NoImages && images != null) {
            (images.images ?: emptyList()).forEach { image ->
                val outputBytes = blueskyClient.readImage(image.thumb)
                writeBytes(outputBytes)
                waitForReturnKey()
                val altText = (image.alt ?: "").ifEmpty { "[alt text not provided]" }
                writeAppText(altText)
                waitForReturnKey()
            }
        } else if (embed is EmbedImages) {
            (embed.images ?: emptyList()).forEachIndexed { i, image ->
                val altText = "[IMAGE $i]\r\n" + (image.alt ?: "").ifEmpty { "[alt text not provided]" }
                writeAppText(altText)
                waitForReturnKey()
            }
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
                    ReplyContextScope(replies, blueskyClient, socket, startupOptions, threadProvider, disconnectHandler).apply(onContext)
                }
                'R' -> {
                    CreatePostScope(genericPostAttributes, blueskyClient, socket, disconnectHandler, startupOptions, threadProvider).apply(onReply)
                }
                'P' -> {
                    CreateRepostScope(genericPostAttributes, blueskyClient, socket, disconnectHandler, startupOptions, threadProvider).apply(onRepost)
                }
                'Q' -> {
                    CreateQuoteScope(genericPostAttributes, blueskyClient, socket, disconnectHandler, startupOptions, threadProvider).apply(onQuote)
                }
                'L' -> {
                    CreateLikeScope(genericPostAttributes, blueskyClient, socket, disconnectHandler, startupOptions, threadProvider).apply(onLike)
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
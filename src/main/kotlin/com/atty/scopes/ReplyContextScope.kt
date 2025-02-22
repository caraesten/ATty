package com.atty.scopes

import com.atty.DisconnectReason
import com.atty.libs.BlueskyReadClient
import com.atty.models.AuthorAttributes
import com.atty.models.GenericPostAttributes
import com.atty.models.StartupOptions
import com.atty.models.getAuthorAttributes
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import java.net.Socket

class ReplyContextScope(
    val replies: List<FeedDefsPostView>,
    blueskyClient: BlueskyReadClient,
    socket: Socket,
    startupOptions: StartupOptions,
    threadProvider: () -> Thread,
    disconnectHandler: (DisconnectReason) -> Unit) :
    BaseLoggedInScope(blueskyClient, socket, startupOptions, threadProvider, disconnectHandler) {
    fun forEachPost(block: PostScope.() -> Unit) {
        replies.forEach { threadItem ->
            val record = threadItem.record
            if (record is FeedPost) { // this should always be true
                PostScope(
                    threadItem.author?.getAuthorAttributes() ?: AuthorAttributes("", ""),
                    record,
                    GenericPostAttributes(record, threadItem.uri ?: "", threadItem.cid ?: "", threadItem.embed),
                    blueskyClient,
                    socket,
                    startupOptions, threadProvider, disconnectHandler
                ).apply(block)
            }
        }
    }
}

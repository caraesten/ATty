package com.atty.scopes

import bsky4j.model.bsky.feed.FeedDefsPostView
import bsky4j.model.bsky.feed.FeedPost
import com.atty.DisconnectHandler
import com.atty.libs.BlueskyReadClient
import com.atty.models.GenericPostAttributes
import com.atty.models.getAuthorAttributes
import io.ktor.network.sockets.*

class ReplyContextScope(
    val replies: List<FeedDefsPostView>,
    blueskyClient: BlueskyReadClient,
    connection: Connection,
    isCommodore: Boolean,
    disconnectHandler: DisconnectHandler) :
    BaseLoggedInScope(blueskyClient, connection, isCommodore, disconnectHandler) {
    suspend fun forEachPost(block: suspend PostScope.() -> Unit) {
        replies.forEach { threadItem ->
            val record = threadItem.record
            if (record is FeedPost) { // this should always be true
                PostScope(
                    threadItem.author.getAuthorAttributes(),
                    record,
                    GenericPostAttributes(record, threadItem.uri, threadItem.cid),
                    blueskyClient,
                    connection,
                    isCommodore, disconnectHandler
                ).apply { block() }
            }
        }
    }
}

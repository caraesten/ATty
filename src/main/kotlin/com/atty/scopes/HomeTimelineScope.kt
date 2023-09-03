package com.atty.scopes

import bsky4j.model.bsky.feed.FeedDefsFeedViewPost
import bsky4j.model.bsky.feed.FeedPost
import com.atty.DisconnectHandler
import com.atty.libs.BlueskyReadClient
import com.atty.models.GenericPostAttributes
import com.atty.models.getAuthorAttributes
import io.ktor.network.sockets.*

class HomeTimelineScope (
    val feed: List<FeedDefsFeedViewPost>,
    blueskyClient: BlueskyReadClient,
    connection: Connection,
    isCommodore: Boolean,
    disconnectHandler: DisconnectHandler) : BaseLoggedInScope(blueskyClient, connection, isCommodore, disconnectHandler) {
    suspend fun forEachPost(block: suspend PostScope.() -> Unit) {
        feed.forEach {
            if (it.post.record is FeedPost) {
                val feedPost = it.post.record as FeedPost
                PostScope(
                    it.post.author.getAuthorAttributes(),
                    feedPost,
                    GenericPostAttributes(feedPost, it.post.uri, it.post.cid),
                    blueskyClient,
                    connection,
                    isCommodore, disconnectHandler
                ).apply { block() }
            }
        }
    }
}
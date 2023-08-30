package com.atty.scopes

import bsky4j.model.bsky.feed.FeedDefsFeedViewPost
import bsky4j.model.bsky.feed.FeedPost
import com.atty.DisconnectReason
import com.atty.libs.BlueskyReadClient
import com.atty.models.GenericPostAttributes
import com.atty.models.getAuthorAttributes
import java.net.Socket

class HomeTimelineScope (
    val feed: List<FeedDefsFeedViewPost>,
    blueskyClient: BlueskyReadClient,
    socket: Socket,
    isCommodore: Boolean,
    threadProvider: () -> Thread,
    disconnectHandler: (DisconnectReason) -> Unit) : BaseLoggedInScope(blueskyClient, socket, isCommodore, threadProvider, disconnectHandler) {
    fun forEachPost(block: PostScope.() -> Unit) {
        feed.forEach {
            if (it.post.record is FeedPost) {
                val feedPost = it.post.record as FeedPost
                PostScope(
                    it.post.author.getAuthorAttributes(),
                    feedPost,
                    GenericPostAttributes(feedPost, it.post.uri, it.post.cid),
                    blueskyClient,
                    socket,
                    isCommodore, threadProvider, disconnectHandler
                ).apply(block)
            }
        }
    }
}
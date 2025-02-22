package com.atty.scopes

import com.atty.DisconnectReason
import com.atty.libs.BlueskyReadClient
import com.atty.models.AuthorAttributes
import com.atty.models.GenericPostAttributes
import com.atty.models.StartupOptions
import com.atty.models.getAuthorAttributes
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import java.net.Socket

class HomeTimelineScope (
    val feed: List<FeedDefsFeedViewPost>,
    blueskyClient: BlueskyReadClient,
    socket: Socket,
    startupOptions: StartupOptions,
    threadProvider: () -> Thread,
    disconnectHandler: (DisconnectReason) -> Unit) : BaseLoggedInScope(blueskyClient, socket, startupOptions, threadProvider, disconnectHandler) {
    fun forEachPost(block: PostScope.() -> Unit) {
        feed.forEach {
            if (it.post.record is FeedPost) {
                val feedPost = it.post.record as FeedPost
                PostScope(
                    it.post.author?.getAuthorAttributes() ?: AuthorAttributes("", ""),
                    feedPost,
                    GenericPostAttributes(feedPost, it.post.uri ?: "", it.post.cid ?: "", it.post.embed),
                    blueskyClient,
                    socket,
                    startupOptions, threadProvider, disconnectHandler
                ).apply(block)
            }
        }
    }
}
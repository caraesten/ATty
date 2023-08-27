package com.atty.libs

import bsky4j.model.bsky.feed.FeedDefsFeedViewPost
import bsky4j.model.bsky.feed.FeedPost
import com.atty.models.GenericPostAttributes
import com.atty.models.getAuthorAttributes
import java.net.Socket

class FeedReader (val feed: List<FeedDefsFeedViewPost>, val blueskyClient: BlueskyClient, val socket: Socket) {

    fun readPosts() {
        feed.forEach {
            if (it.post.record is FeedPost) {
                val feedPost = it.post.record as FeedPost
                PostReader(
                    it.post.author.getAuthorAttributes(),
                    feedPost,
                    blueskyClient,
                    socket,
                    genericPostAttributes = GenericPostAttributes(feedPost, it.post.uri, it.post.cid)
                ).readPost()
            }
        }
    }
}
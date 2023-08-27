package com.atty.libs

import bsky4j.model.bsky.feed.FeedDefsFeedViewPost
import bsky4j.model.bsky.feed.FeedPost
import com.atty.models.getAuthorAttributes
import java.net.Socket

class FeedReader (val feed: List<FeedDefsFeedViewPost>, val socket: Socket) {

    fun readPosts() {
        feed.forEach {
            if (it.post.record is FeedPost) {
                PostReader(it.post.author.getAuthorAttributes(), it.post.record as FeedPost, socket).readPost()
            }
        }
    }
}
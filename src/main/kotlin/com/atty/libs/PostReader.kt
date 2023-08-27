package com.atty.libs

import bsky4j.model.bsky.feed.FeedPost
import com.atty.models.AuthorAttributes
import com.atty.waitForReturnKey
import java.net.Socket

enum class PostContext {
    AsPost,
    AsNotification
}

class PostReader(val author: AuthorAttributes, val feedPost: FeedPost, val socket: Socket) {
    fun readPost(context: PostContext = PostContext.AsPost) {
        socket.getOutputStream().write(
            "\n ${author.displayName} (${author.handle}) \n ${feedPost.text}\n".toByteArray()
        )
        socket.waitForReturnKey()
    }
}
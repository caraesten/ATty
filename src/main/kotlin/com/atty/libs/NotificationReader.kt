package com.atty.libs

import bsky4j.model.bsky.feed.FeedDefsFeedViewPost
import bsky4j.model.bsky.feed.FeedLike
import bsky4j.model.bsky.feed.FeedPost
import bsky4j.model.bsky.feed.FeedRepost
import bsky4j.model.bsky.graph.GraphFollow
import bsky4j.model.bsky.notification.NotificationListNotificationsNotification
import com.atty.models.getAuthorAttributes
import java.net.Socket

class NotificationReader (
    val notifs: List<NotificationListNotificationsNotification>,
    val bsky: BlueskyClient,
    val socket: Socket
) {
    fun readNotifications() {
        val prefetchPosts = notifs
            .filter { it.record is FeedRepost || it.record is FeedLike }
            .map {
                when(it.record) {
                    is FeedRepost -> (it.record as FeedRepost).subject.uri
                    is FeedLike -> (it.record as FeedLike).subject.uri
                    else -> error("Impossible bc of cast above")
                }
            }
        val posts = bsky.fetchPosts(prefetchPosts)
        notifs.forEach { notif ->
            val record = notif.record
            when (record) {
                is FeedPost -> {
                    PostReader(notif.author.getAuthorAttributes(), record, socket).readPost()
                }
                is FeedRepost -> {
                    socket.getOutputStream().write(
                        "\n ${notif.author.displayName} (${notif.author.handle}) Reposted:\n".toByteArray()
                    )
                    val post = posts.find { it.uri == record.subject.uri }!!
                    PostReader(notif.author.getAuthorAttributes(), post.record as FeedPost, socket).readPost(PostContext.AsNotification)
                }
                is FeedLike -> {
                    socket.getOutputStream().write(
                        "\n ${notif.author.displayName} (${notif.author.handle}) Liked:\n".toByteArray()
                    )
                    val post = posts.find { it.uri == record.subject.uri }!!
                    PostReader(notif.author.getAuthorAttributes(), post.record as FeedPost, socket).readPost(PostContext.AsNotification)
                }
                is GraphFollow -> {
                    socket.getOutputStream().write(
                        "\n ${notif.author.displayName} (${notif.author.handle}) Followed You\n".toByteArray()
                    )
                }
            }
        }
    }
}
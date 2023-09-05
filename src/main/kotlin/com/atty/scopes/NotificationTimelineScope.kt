package com.atty.scopes

import bsky4j.model.bsky.feed.FeedLike
import bsky4j.model.bsky.feed.FeedPost
import bsky4j.model.bsky.feed.FeedRepost
import bsky4j.model.bsky.graph.GraphFollow
import bsky4j.model.bsky.notification.NotificationListNotificationsNotification
import com.atty.DisconnectReason
import com.atty.libs.BlueskyReadClient
import com.atty.models.GenericPostAttributes
import com.atty.models.StartupOptions
import com.atty.models.getAuthorAttributes
import java.net.Socket

class NotificationTimelineScope (val notifs: List<NotificationListNotificationsNotification>,
                                 blueskyClient: BlueskyReadClient,
                                 socket: Socket,
                                 startupOptions: StartupOptions,
                                 threadProvider: () -> Thread,
                                 disconnectHandler: (DisconnectReason) -> Unit) :
    BaseLoggedInScope(blueskyClient, socket, startupOptions, threadProvider, disconnectHandler) {

    fun forEachPost(block: PostScope.() -> Unit) {
            val prefetchPosts = notifs
                .filter { it.record is FeedRepost || it.record is FeedLike }
                .map {
                    when(it.record) {
                        is FeedRepost -> (it.record as FeedRepost).subject.uri
                        is FeedLike -> (it.record as FeedLike).subject.uri
                        else -> error("Impossible bc of cast above")
                    }
                }
            val posts = blueskyClient.fetchPosts(prefetchPosts)
            notifs.forEach { notif ->
                val record = notif.record
                when (record) {
                    is FeedPost -> {
                        PostScope(
                            notif.author.getAuthorAttributes(),
                            record,
                            GenericPostAttributes(
                                record,
                                notif.uri,
                                notif.cid,
                                null
                            ),
                            blueskyClient, socket, startupOptions, threadProvider, disconnectHandler
                        ).apply(block)
                    }
                    is FeedRepost -> {
                        writeAppText(
                            "${notif.author.displayName} (${notif.author.handle}) Reposted:"
                        )
                        val post = posts.find { it.uri == record.subject.uri }!!
                        val feedPost = post.record as FeedPost
                        PostScope(
                            notif.author.getAuthorAttributes(),
                            feedPost,
                            GenericPostAttributes(
                                feedPost,
                                notif.uri,
                                notif.cid,
                                null
                            ),
                            blueskyClient, socket, startupOptions, threadProvider, disconnectHandler
                        ).apply(block)
                    }
                    is FeedLike -> {
                        writeAppText(
                            "${notif.author.displayName} (${notif.author.handle}) Liked:"
                        )
                        val post = posts.find { it.uri == record.subject.uri }!!
                        val feedPost = post.record as FeedPost
                        PostScope(
                            notif.author.getAuthorAttributes(),
                            feedPost,
                            GenericPostAttributes(
                                feedPost,
                                notif.uri,
                                notif.cid,
                                null
                            ),
                            blueskyClient, socket, startupOptions, threadProvider, disconnectHandler
                        ).apply(block)
                    }
                    is GraphFollow -> {
                        writeAppText(
                            "${notif.author.displayName} (${notif.author.handle}) Followed You"
                        )
                    }
                }
            }
        }
 }
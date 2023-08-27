package com.atty.libs

import bsky4j.BlueskyFactory
import bsky4j.api.entity.atproto.server.ServerCreateSessionRequest
import bsky4j.api.entity.bsky.feed.FeedGetPostsRequest
import bsky4j.api.entity.bsky.feed.FeedGetTimelineRequest
import bsky4j.api.entity.bsky.notification.NotificationListNotificationsRequest
import bsky4j.domain.Service
import bsky4j.model.bsky.feed.FeedDefsFeedViewPost
import bsky4j.model.bsky.feed.FeedDefsPostView
import bsky4j.model.bsky.notification.NotificationListNotificationsNotification
import com.atty.AtReaderThread

class BlueskyClient (
    username: String,
    password: String
) {
    private val bskyFactory = BlueskyFactory.getInstance(Service.BSKY_SOCIAL.uri)
    private val accessJwt: String

    init {
        val response =
            bskyFactory.server().createSession(
                ServerCreateSessionRequest.builder()
                    .identifier(username)
                    .password(password)
                    .build()
            )

        accessJwt = response.get().accessJwt
    }

    fun getHomeTimeline(): List<FeedDefsFeedViewPost> {
        val response = bskyFactory.feed().getTimeline(
            FeedGetTimelineRequest.builder().accessJwt(accessJwt).limit(POST_LIMIT).build()
        )
        return response.get().feed
    }

    fun getNotificationsTimeline(): List<NotificationListNotificationsNotification> {
        val response = bskyFactory.notification().listNotifications(
            NotificationListNotificationsRequest.builder().accessJwt(accessJwt).limit(POST_LIMIT).build()
        )
        return response.get().notifications
    }

    fun fetchPosts(uris: List<String>): List<FeedDefsPostView> {
        val response = bskyFactory.feed().getPosts(
            FeedGetPostsRequest
                .builder()
                .accessJwt(accessJwt)
                .uris(uris)
                .build()
        )
        return response.get().posts
    }

    companion object {
        const val POST_LIMIT = 10
    }
}
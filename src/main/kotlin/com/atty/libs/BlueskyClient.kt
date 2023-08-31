package com.atty.libs

import bsky4j.BlueskyFactory
import bsky4j.api.entity.atproto.server.ServerCreateSessionRequest
import bsky4j.api.entity.bsky.feed.*
import bsky4j.api.entity.bsky.notification.NotificationListNotificationsRequest
import bsky4j.domain.Service
import bsky4j.model.atproto.repo.RepoStrongRef
import bsky4j.model.bsky.embed.EmbedRecord
import bsky4j.model.bsky.feed.FeedDefsFeedViewPost
import bsky4j.model.bsky.feed.FeedDefsPostView
import bsky4j.model.bsky.feed.FeedPostReplyRef
import bsky4j.model.bsky.notification.NotificationListNotificationsNotification
import com.atty.models.GenericPostAttributes
import com.atty.models.PendingPost

interface BlueskyReadClient {
    fun getHomeTimeline(): List<FeedDefsFeedViewPost>
    fun getNotificationsTimeline(): List<NotificationListNotificationsNotification>
    fun fetchPosts(uris: List<String>): List<FeedDefsPostView>
}

interface BlueskyWriteClient {
    fun sendPost(post: PendingPost)
    fun repost(genericPostAttributes: GenericPostAttributes)
    fun like(genericPostAttributes: GenericPostAttributes)
}

class BlueskyClient (
    username: String,
    password: String
) : BlueskyReadClient, BlueskyWriteClient {
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

    override fun getHomeTimeline(): List<FeedDefsFeedViewPost> {
        val response = bskyFactory.feed().getTimeline(
            FeedGetTimelineRequest.builder().accessJwt(accessJwt).limit(POST_LIMIT).build()
        )
        return response.get().feed
    }

    override fun getNotificationsTimeline(): List<NotificationListNotificationsNotification> {
        val response = bskyFactory.notification().listNotifications(
            NotificationListNotificationsRequest.builder().accessJwt(accessJwt).limit(POST_LIMIT).build()
        )
        return response.get().notifications
    }

    override fun fetchPosts(uris: List<String>): List<FeedDefsPostView> {
        val response = bskyFactory.feed().getPosts(
            FeedGetPostsRequest
                .builder()
                .accessJwt(accessJwt)
                .uris(uris)
                .build()
        )
        return response.get().posts
    }

    override fun sendPost(post: PendingPost) {
        val builder = FeedPostRequest.builder()
            .accessJwt(accessJwt)
        if (post.inReplyTo != null) {
            val record = post.inReplyTo.feedPost
            val refToPost = RepoStrongRef(post.inReplyTo.uri, post.inReplyTo.cid)
            val replyRoot = if (record.reply != null) record.reply.root else refToPost
            builder.reply(
                FeedPostReplyRef().apply {
                    root = replyRoot
                    parent = refToPost
                }
            )
        }
        if (post.embed != null) {
            val embedRecord = EmbedRecord()
            embedRecord.record = RepoStrongRef(post.embed.uri, post.embed.cid)
            builder.embed(embedRecord)
        }
        builder.text(post.text)
        val response = bskyFactory.feed().post(
            builder.build()
        )
    }

    override fun repost(genericPostAttributes: GenericPostAttributes) {
        bskyFactory.feed().repost(FeedRepostRequest.builder()
            .accessJwt(accessJwt)
            .subject(RepoStrongRef(genericPostAttributes.uri, genericPostAttributes.cid))
            .build())
    }

    override fun like(genericPostAttributes: GenericPostAttributes) {
        bskyFactory.feed().like(FeedLikeRequest.builder()
            .accessJwt(accessJwt)
            .subject(RepoStrongRef(genericPostAttributes.uri, genericPostAttributes.cid))
            .build()
        )
    }

    companion object {
        const val POST_LIMIT = 10
    }
}
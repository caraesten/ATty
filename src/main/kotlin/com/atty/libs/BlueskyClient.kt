package com.atty.libs

import com.atty.models.GenericPostAttributes
import com.atty.models.ImageMode
import com.atty.models.PendingPost
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.*
import work.socialhub.kbsky.api.entity.app.bsky.notification.NotificationListNotificationsRequest
import work.socialhub.kbsky.api.entity.com.atproto.identity.IdentityResolveHandleRequest
import work.socialhub.kbsky.api.entity.com.atproto.server.ServerCreateSessionRequest
import work.socialhub.kbsky.auth.BearerTokenAuthProvider
import work.socialhub.kbsky.domain.Service
import work.socialhub.kbsky.model.app.bsky.embed.EmbedRecord
import work.socialhub.kbsky.model.app.bsky.feed.*
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification
import work.socialhub.kbsky.model.app.bsky.richtext.RichtextFacet
import work.socialhub.kbsky.model.app.bsky.richtext.RichtextFacetByteSlice
import work.socialhub.kbsky.model.app.bsky.richtext.RichtextFacetLink
import work.socialhub.kbsky.model.app.bsky.richtext.RichtextFacetMention
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

interface BlueskyReadClient {
    fun getHomeTimeline(): List<FeedDefsFeedViewPost>
    fun getNotificationsTimeline(): List<NotificationListNotificationsNotification>
    fun fetchPosts(uris: List<String>): List<FeedDefsPostView>
    fun resolveHandle(handle: String): String
    fun fetchThread(uri: String, depth: Int): List<FeedDefsPostView>
    fun readImage(url: String?): ByteArray
}

interface BlueskyWriteClient {
    fun sendPost(post: PendingPost)
    fun repost(genericPostAttributes: GenericPostAttributes)
    fun like(genericPostAttributes: GenericPostAttributes)
}

class BlueskyClient (
    username: String,
    password: String,
    imageMode: ImageMode
) : BlueskyReadClient, BlueskyWriteClient {
    private val bskyFactory = BlueskyFactory
        .instance(Service.BSKY_SOCIAL.uri)

    private val accessJwt: String
    private val imageReader: ImageReader = ImageReaderImpl(imageMode)

    init {
        val response =
            BlueskyFactory
                .instance(Service.BSKY_SOCIAL.uri)
                .server()
                .createSession(
                    ServerCreateSessionRequest().also {
                        it.identifier = username
                        it.password = password
                    }
                )

        accessJwt = response.data.accessJwt
    }

    override fun getHomeTimeline(): List<FeedDefsFeedViewPost> {
        val response = bskyFactory.feed().getTimeline(
            FeedGetTimelineRequest(getAuth()).also {
                it.limit = POST_LIMIT
            }
        )
        return response.data.feed
    }

    override fun getNotificationsTimeline(): List<NotificationListNotificationsNotification> {

        val response = bskyFactory.notification().listNotifications(
            NotificationListNotificationsRequest(getAuth()).apply {
                limit = POST_LIMIT
            }
        )
        return response.data.notifications
    }

    override fun fetchThread(uri: String, depth: Int): List<FeedDefsPostView> {
        val response = bskyFactory.feed().getPostThread(
            FeedGetPostThreadRequest(getAuth()).apply {
                this.uri = uri
                this.depth = depth
            }
        )
        return when (val thread = response.data.thread) {
            is FeedDefsNotFoundPost -> {
                return emptyList()
            }
            is FeedDefsThreadViewPost -> {
                val posts = mutableListOf<FeedDefsPostView>()
                var postPointer: FeedDefsThreadUnion? = thread
                while (postPointer is FeedDefsThreadViewPost) {
                    posts.add(postPointer.post)
                    postPointer = postPointer.parent
                }
                return posts
            }
            else -> {
                emptyList() // probably blocked idk
            }
        }
    }

    override fun readImage(url: String?): ByteArray {
        return if (url == null) ByteArray(0) else imageReader.readImage(url)
    }

    override fun fetchPosts(uris: List<String>): List<FeedDefsPostView> {
        val response = bskyFactory.feed().getPosts(
            FeedGetPostsRequest(getAuth()).apply {
                this.uris = uris
            }
        )
        return response.data.posts
    }

    override fun resolveHandle(handle: String): String {
        return bskyFactory.identity().resolveHandle(
            IdentityResolveHandleRequest().apply {
                this.handle = handle
            }
        ).data.did
    }

    override fun sendPost(post: PendingPost) {
        val request = FeedPostRequest(getAuth())
        if (post.inReplyTo != null) {
            val record = post.inReplyTo.feedPost
            val refToPost = RepoStrongRef(post.inReplyTo.uri, post.inReplyTo.cid)
            val reply = record.reply
            val replyRoot = if (reply != null) reply.root else refToPost
            request.reply = FeedPostReplyRef().apply {
                root = replyRoot
                parent = refToPost
            }
        }
        if (post.embed != null) {
            val embedRecord = EmbedRecord()
            embedRecord.record = RepoStrongRef(post.embed.uri, post.embed.cid)
            request.embed = embedRecord
        }
        val mentions = post.text.getMentions()
        val links = post.text.getLinks().filter { link ->
            // Filter out any links that are actually just handles. naive, but should work?
            !mentions.any { (it.startIndex .. it.endIndex).contains(link.startIndex) }
        }
        val facets = mentions.map { mention ->
            val resolvedDid = resolveHandle(mention.username.trimStart('@'))
            val slice = RichtextFacetByteSlice().apply {
                byteStart = mention.startIndex
                byteEnd = mention.endIndex
            }
            RichtextFacet().apply {
                index = slice
                val facetMention = RichtextFacetMention().apply { did = resolvedDid }
                features = mutableListOf(facetMention)
            }
        } + links.map { link ->
            val slice = RichtextFacetByteSlice().apply {
                byteStart = link.startIndex
                byteEnd = link.endIndex
            }
            RichtextFacet().apply {
                index = slice
                val facetLink = RichtextFacetLink().apply { uri = link.address }
                features = mutableListOf(facetLink)
            }
        }
        request.facets = facets
        request.text = post.text
        val response = bskyFactory.feed().post(
            request
        )
    }

    override fun repost(genericPostAttributes: GenericPostAttributes) {
        bskyFactory.feed().repost(
            FeedRepostRequest(getAuth()).apply {
                subject = RepoStrongRef(genericPostAttributes.uri, genericPostAttributes.cid)
            }
        )
    }

    override fun like(genericPostAttributes: GenericPostAttributes) {
        bskyFactory.feed().like(FeedLikeRequest(getAuth()).apply {
            this.subject = RepoStrongRef(genericPostAttributes.uri, genericPostAttributes.cid)
        })
    }

    private fun getAuth() = BearerTokenAuthProvider(accessJwt)

    companion object {
        const val POST_LIMIT = 10
    }
}
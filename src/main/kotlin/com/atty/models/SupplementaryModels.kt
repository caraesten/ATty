package com.atty.models

import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewBasic
import work.socialhub.kbsky.model.app.bsky.embed.EmbedViewUnion
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost

enum class ImageMode {
    NoImages, AsciiImages, SixelImages;

    companion object {
        fun fromStringInput(input: String): ImageMode {
            return when (input) {
                "A" -> AsciiImages
                "S" -> SixelImages
                else -> NoImages
            }
        }
    }
}

data class AuthorAttributes(
    val displayName: String,
    val handle: String
)

fun ActorDefsProfileView.getAuthorAttributes(): AuthorAttributes = AuthorAttributes(
    displayName ?: "", handle
)

fun ActorDefsProfileViewBasic.getAuthorAttributes(): AuthorAttributes = AuthorAttributes(
    displayName ?: "", handle
)

data class StartupOptions(
    val isCommodore: Boolean,
    val imageMode: ImageMode
)

data class GenericPostAttributes(
    val feedPost: FeedPost,
    val uri: String,
    val cid: String,
    val embedView: EmbedViewUnion?
)

data class PendingPost(
    val text: String,
    val inReplyTo: GenericPostAttributes?,
    val embed: GenericPostAttributes? = null
)

interface Facet {
    val startIndex: Int
    val endIndex: Int
}

data class Mention(
    val username: String,
    override val startIndex: Int,
    override val endIndex: Int
) : Facet

data class Link(
    val address: String,
    override val startIndex: Int,
    override val endIndex: Int
) : Facet

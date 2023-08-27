package com.atty.models

import bsky4j.model.bsky.actor.ActorDefsProfileView
import bsky4j.model.bsky.actor.ActorDefsProfileViewBasic
import bsky4j.model.bsky.feed.FeedDefsPostView
import bsky4j.model.bsky.notification.NotificationListNotificationsNotification

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
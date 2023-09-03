package com.atty

import com.atty.libs.BlueskyClient
import com.atty.scopes.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.cancel
import java.nio.charset.Charset
import kotlin.coroutines.coroutineContext


enum class BskyOptions(val choice: Int) {
    HOME_TIMELINE(1),
    NOTIFICATIONS_TIMELINE(2),
    CREATE_POST(3)
}

data class OptionItem(val optionString: String, val option: BskyOptions)

interface AtConnection {
    val startTime: Long
    suspend fun timeoutConnection()
}

class AtReaderThread(private val connection: Connection,
                     private val onDisconnect: (AtReaderThread, DisconnectReason) -> Unit,
                     private val charset: Charset = Charsets.UTF_8) : AtConnection {

    lateinit var blueskyClient: BlueskyClient

    override val startTime = System.currentTimeMillis()

    override suspend fun timeoutConnection() {
        performDisconnect(DisconnectReason.TIMEOUT)
    }

    suspend fun run() {
        LoginScope(connection, ::performDisconnect).performLogin {
            val performReply: suspend CreatePostScope.() -> Unit = {
                val pending = getPost()
                writeClient().sendPost(pending)
                showPosted()
            }
            val performRepost: suspend CreateRepostScope.() -> Unit = {
                writeClient().repost(genericPostAttributes)
                showReposted()
            }
            val performQuote: suspend CreateQuoteScope.() -> Unit = {
                val pending = getPost()
                writeClient().sendPost(pending)
                showQuoted()
            }
            val performLike: suspend CreateLikeScope.() -> Unit = {
                writeClient().like(genericPostAttributes)
                showLiked()
            }
            val showContext: suspend ReplyContextScope.() -> Unit = {
                forEachPost {
                    readPost(PostContext.AsReply, {}, performReply, performRepost, performQuote, performLike)
                }
            }

            val readPostAction: (PostContext) -> suspend (PostScope.() -> Unit) = { context -> {
                    readPost(context, showContext, performReply, performRepost, performQuote, performLike)
                }
            }

            readMenu(
                onHomeSelected = {
                    forEachPost(readPostAction(PostContext.AsPost))
                },
                onNotificationsSelected = {
                    forEachPost(readPostAction(PostContext.AsNotification))
                },
                onPostSkeetSelected = {
                    val pendingPost = getPost()
                    writeClient().sendPost(pendingPost)
                    showPosted()
                },
            )
        }
    }

    private suspend fun performDisconnect(reason: DisconnectReason) {
        connection.socket.close()
        onDisconnect(this, reason)
        coroutineContext.cancel()
    }
}

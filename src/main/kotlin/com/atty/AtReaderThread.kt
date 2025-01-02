package com.atty

import com.atty.libs.BlueskyClient
import com.atty.scopes.*
import java.net.Socket
import java.net.SocketException
import java.nio.charset.Charset


enum class BskyOptions(val choice: Int) {
    HOME_TIMELINE(1),
    NOTIFICATIONS_TIMELINE(2),
    CREATE_POST(3)
}

data class OptionItem(val optionString: String, val option: BskyOptions)

interface AtConnection {
    val startTime: Long
    fun timeoutConnection()
}

class AtReaderThread(private val clientSocket: Socket,
                   private val onDisconnect: (AtReaderThread, DisconnectReason) -> Unit,
                   private val charset: Charset = Charsets.UTF_8) : Thread(), AtConnection {

    lateinit var blueskyClient: BlueskyClient

    override val startTime = System.currentTimeMillis()

    override fun timeoutConnection() {
        performDisconnect(DisconnectReason.TIMEOUT)
    }

    override fun run() {
        try {
            LoginScope(clientSocket, { this@AtReaderThread }, ::performDisconnect).performLogin {
                val performReply: CreatePostScope.() -> Unit = {
                    val pending = getPost()
                    writeClient().sendPost(pending)
                    showPosted()
                }
                val performRepost: CreateRepostScope.() -> Unit = {
                    writeClient().repost(genericPostAttributes)
                    showReposted()
                }
                val performQuote: CreateQuoteScope.() -> Unit = {
                    val pending = getPost()
                    writeClient().sendPost(pending)
                    showQuoted()
                }
                val performLike: CreateLikeScope.() -> Unit = {
                    writeClient().like(genericPostAttributes)
                    showLiked()
                }
                val showContext: ReplyContextScope.() -> Unit = {
                    forEachPost {
                        readPost(PostContext.AsReply, {}, performReply, performRepost, performQuote, performLike)
                    }
                }

                val readPostAction: (PostContext) -> (PostScope.() -> Unit) = { context ->
                    {
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
                    onCreatePostSelected = {
                        val pendingPost = getPost()
                        writeClient().sendPost(pendingPost)
                        showPosted()
                    },
                )
            }
        } catch (e: SocketException) {
            // tidy up
            performDisconnect(DisconnectReason.EXCEPTION)
        }
    }

    private fun performDisconnect(reason: DisconnectReason) {
        clientSocket.close()
        onDisconnect(this, reason)
        interrupt()
    }
}

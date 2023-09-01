package com.atty

import com.atty.libs.BlueskyClient
import com.atty.scopes.*
import java.net.Socket
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
        LoginScope(clientSocket, { currentThread() }, ::performDisconnect).performLogin {
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

            readMenu(
                onHomeSelected = {
                    forEachPost {
                        readPost(PostContext.AsPost, performReply, performRepost, performQuote, performLike)
                    }
                },
                onNotificationsSelected = {
                    forEachPost {
                        readPost(PostContext.AsNotification, performReply, performRepost, performQuote, performLike)
                    }
                },
                onPostSkeetSelected = {
                    val pendingPost = getPost()
                    writeClient().sendPost(pendingPost)
                    showPosted()
                },
            )
        }
    }

    private fun performDisconnect(reason: DisconnectReason) {
        clientSocket.close()
        onDisconnect(this, reason)
        interrupt()
    }
}

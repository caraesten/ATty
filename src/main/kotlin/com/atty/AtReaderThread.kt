package com.atty

import com.atty.libs.BlueskyClient
import com.atty.libs.FeedReader
import com.atty.libs.NotificationReader
import com.atty.libs.PostSender
import com.atty.models.AtConfig
import java.net.Socket
import java.nio.charset.Charset


enum class BskyOptions(val choice: Int) {
    HOME_TIMELINE(1),
    NOTIFICATIONS_TIMELINE(2),
    POST_SKEET(3)
}

data class OptionItem(val optionString: String, val option: BskyOptions)

interface AtConnection {
    val startTime: Long
    fun timeoutConnection()
}

class AtReaderThread(private val clientSocket: Socket,
                   private val atConfig: AtConfig,
                   private val onDisconnect: (AtReaderThread, DisconnectReason) -> Unit,
                   private val charset: Charset = Charsets.UTF_8) : Thread(), AtConnection {

    lateinit var blueskyClient: BlueskyClient

    override val startTime = System.currentTimeMillis()

    override fun timeoutConnection() {
        performDisconnect(DisconnectReason.TIMEOUT)
    }

    override fun run() {
        try {
            clientSocket.clearScreen()
            clientSocket.getOutputStream().write(atConfig.welcomeText.toByteArray())
            clientSocket.waitForReturnKey()
            clientSocket.getOutputStream().write("Username: ".toByteArray())
            val usernameInput = clientSocket.waitForStringInput()
            clientSocket.getOutputStream().write("Password: ".toByteArray())
            val passwordInput = clientSocket.waitForStringInput()

            blueskyClient = BlueskyClient(usernameInput, passwordInput)

            clientSocket.clearScreen()
        } catch (ex: Throwable) {
            ex.printStackTrace()
            performDisconnect(DisconnectReason.EXCEPTION)
        }
        while (!currentThread().isInterrupted) {
            try {
                clientSocket.getOutputStream().write(bskyOptions.toMenuString().toByteArray())
                val selectedMenuItem = clientSocket.waitForSelectionChoice(bskyOptions.size)
                if (selectedMenuItem == -1) {
                    performDisconnect(DisconnectReason.GRACEFUL)
                    return
                }
                when (selectedMenuItem) {
                    BskyOptions.HOME_TIMELINE.choice -> {
                        val feed = blueskyClient.getHomeTimeline()
                        FeedReader(feed, blueskyClient, clientSocket).readPosts()
                    }
                    BskyOptions.NOTIFICATIONS_TIMELINE.choice -> {
                        val notifications = blueskyClient.getNotificationsTimeline()
                        NotificationReader(notifications, blueskyClient, clientSocket).readNotifications()
                    }
                    BskyOptions.POST_SKEET.choice -> {
                        val post = PostSender(clientSocket).getPendingPost()
                        blueskyClient.sendPost(post)
                        clientSocket.getOutputStream().write(
                            "\n Sent Post! \n".toByteArray()
                        )
                    }
                }
            } catch (ex: Throwable) { // TODO: be more specific
                ex.printStackTrace()
                performDisconnect(DisconnectReason.EXCEPTION)
            }
        }
    }

    private fun performDisconnect(reason: DisconnectReason) {
        clientSocket.close()
        onDisconnect(this, reason)
        interrupt()
    }

    private companion object {
        val bskyOptions = listOf(
            OptionItem("Home Timeline", BskyOptions.HOME_TIMELINE),
            OptionItem("Notifications Timeline", BskyOptions.NOTIFICATIONS_TIMELINE),
            OptionItem("Post Skeet", BskyOptions.POST_SKEET)
        )
    }
}

fun List<OptionItem>.toMenuString(): String = """
    |Choose an option (or X to quit):
    |${this.map { "${it.option.choice}: ${it.optionString}" }.joinToString("\n")}
    |${'\n'}>
""".trimMargin()

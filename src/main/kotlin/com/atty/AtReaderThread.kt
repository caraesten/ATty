package com.atty

import bsky4j.BlueskyFactory
import bsky4j.api.entity.atproto.server.ServerCreateSessionRequest
import bsky4j.api.entity.bsky.feed.FeedGetFeedGeneratorsRequest
import bsky4j.api.entity.bsky.feed.FeedGetTimelineRequest
import bsky4j.api.entity.bsky.feed.FeedGetTimelineRequest.FeedGetTimelineRequestBuilder
import bsky4j.domain.Service
import bsky4j.model.bsky.feed.FeedPost
import com.atty.models.AtConfig
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket
import java.nio.charset.Charset


enum class BskyOptions(val choice: Int) {
    HOME_TIMELINE(1),
    NOTIFICATIONS_TIMELINE(2)
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

    lateinit var accessJwt: String

    override val startTime = System.currentTimeMillis()

    override fun timeoutConnection() {
        performDisconnect(DisconnectReason.TIMEOUT)
    }

    override fun run() {
        try {
            clearScreen()
            clientSocket.getOutputStream().write(atConfig.welcomeText.toByteArray())
            waitForReturnKey()
            clientSocket.getOutputStream().write("Username: ".toByteArray())
            val usernameInput = waitForStringInput()
            clientSocket.getOutputStream().write("Password: ".toByteArray())
            val passwordInput = waitForStringInput()

            val response = BlueskyFactory
                .getInstance(Service.BSKY_SOCIAL.uri)
                .server().createSession(
                    ServerCreateSessionRequest.builder()
                        .identifier(usernameInput)
                        .password(passwordInput)
                        .build()
                )

            accessJwt = response.get().accessJwt

            clearScreen()
        } catch (ex: Throwable) {
            ex.printStackTrace()
            performDisconnect(DisconnectReason.EXCEPTION)
        }
        while (!currentThread().isInterrupted) {
            try {
                clientSocket.getOutputStream().write(bskyOptions.toMenuString().toByteArray())
                val selectedMenuItem = waitForSelectionChoice(bskyOptions.size)
                if (selectedMenuItem == -1) {
                    performDisconnect(DisconnectReason.GRACEFUL)
                    return
                }
                when (selectedMenuItem) {
                    BskyOptions.HOME_TIMELINE.choice -> {
                        val response = BlueskyFactory.getInstance(Service.BSKY_SOCIAL.uri).feed().getTimeline(
                            FeedGetTimelineRequest.builder().accessJwt(accessJwt).limit(POST_LIMIT).build()
                        )
                        response.get().feed.forEach {
                            if (it.post.record is FeedPost) {
                                val feedPost = it.post.record as FeedPost
                                clientSocket.getOutputStream().write(
                                    "\n ${it.post.author.displayName} (${it.post.author.handle}) \n ${feedPost.text}\n".toByteArray()
                                )
                                waitForReturnKey()
                            }
                        }
                    }
                }
            } catch (ex: Throwable) { // TODO: be more specific
                ex.printStackTrace()
                performDisconnect(DisconnectReason.EXCEPTION)
            }
        }
    }

    private fun waitForReturnKey() {
        while (clientSocket.getInputStream().read() != ASCII_LF) {
            // do nothing
        }
    }

    private fun clearScreen() {
        if (charset != Charsets.UTF_8) {
            println("Unsafe clear screen!")
        }
        val bytesToClearScreen = "\u001b[2J\u001b[H".toByteArray(Charsets.UTF_8)
        clientSocket.getOutputStream().write(bytesToClearScreen)
    }

    private fun waitForSelectionChoice(numberOfOptions: Int): Int {
        val inputStream = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
        val selectionString: String? = try {
            inputStream.readLine()
        } catch (ex: IllegalArgumentException) {
            ""
        } catch (ex: IOException) {
            ""
        }

        return if (selectionString.equals("x", ignoreCase = true)) {
            -1
        } else {
            val selectionNumber = selectionString?.toIntOrNull() ?: -1
            if (selectionNumber in 1..numberOfOptions) {
                selectionNumber
            } else {
                clientSocket.getOutputStream().write(ERROR_INVALID_SELECTION.toByteArray(charset))
                waitForSelectionChoice(numberOfOptions)
            }
        }
    }

    private fun waitForStringInput(): String {
        val inputStream = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
        val selectionString: String = try {
            inputStream.readLine()
        } catch (ex: IllegalArgumentException) {
            ""
        } catch (ex: IOException) {
            ""
        }

        return selectionString
    }


    private fun performDisconnect(reason: DisconnectReason) {
        clientSocket.close()
        onDisconnect(this, reason)
        interrupt()
    }

    private companion object {
        const val ASCII_LF = 10
        const val POST_LIMIT = 10
        const val ERROR_INVALID_SELECTION = "\nPick an option, or X to quit.\n"

        val bskyOptions = listOf(
            OptionItem("Home Timeline", BskyOptions.HOME_TIMELINE),
            OptionItem("Notifications Timeline", BskyOptions.NOTIFICATIONS_TIMELINE)
        )
    }
}

fun List<OptionItem>.toMenuString(): String = """
    |Choose an option (or X to quit):
    |${this.map { "${it.option.choice}: ${it.optionString}" }.joinToString("\n")}
    |${'\n'}>
""".trimMargin()

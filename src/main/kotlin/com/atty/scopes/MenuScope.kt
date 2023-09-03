package com.atty.scopes

import bsky4j.ATProtocolException
import com.atty.*
import com.atty.libs.BlueskyClient
import com.atty.libs.BlueskyWriteClient
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class MenuScope(private val fullBlueskyClient: BlueskyClient, connection: Connection, isCommodore: Boolean, disconnectHandler: DisconnectHandler) : BaseLoggedInScope(fullBlueskyClient, connection, isCommodore, disconnectHandler) {

    // Don't use this from in here; it's meant to be accessible to functions from this scope
    fun writeClient(): BlueskyWriteClient = fullBlueskyClient

    suspend fun readMenu(
        onHomeSelected: suspend HomeTimelineScope.() -> Unit,
        onNotificationsSelected: suspend NotificationTimelineScope.() -> Unit,
        onPostSkeetSelected: suspend CreatePostScope.() -> Unit,
    ) {
        while (coroutineContext.isActive) {
            try {
                connection.output.writeStringUtf8(bskyOptions.toMenuString())
                val selectedMenuItem = waitForSelectionChoice(bskyOptions.size)
                if (selectedMenuItem.isQuit) {
                    disconnectHandler(DisconnectReason.GRACEFUL)
                    return
                }
                try {
                    when (selectedMenuItem.integer) {
                        BskyOptions.HOME_TIMELINE.choice -> {
                            val feed = blueskyClient.getHomeTimeline()
                            HomeTimelineScope(
                                feed,
                                blueskyClient,
                                connection,
                                isCommodore,
                                disconnectHandler
                            ).apply { onHomeSelected() }
                        }
                        BskyOptions.NOTIFICATIONS_TIMELINE.choice -> {
                            val notifications = blueskyClient.getNotificationsTimeline()
                            NotificationTimelineScope(
                                notifications,
                                blueskyClient,
                                connection,
                                isCommodore,
                                disconnectHandler
                            ).apply { onNotificationsSelected() }
                        }
                        BskyOptions.CREATE_POST.choice -> {
                            CreatePostScope(
                                null,
                                blueskyClient,
                                connection,
                                disconnectHandler,
                                isCommodore
                            ).apply { onPostSkeetSelected() }
                        }
                    }
                } catch (e: ATProtocolException) {
                    writeUi(Constants.ERROR_BLUESKY_CONNECTION)
                }
            } catch (ex: Throwable) { // TODO: be more specific
                ex.printStackTrace()
                disconnectHandler(DisconnectReason.EXCEPTION)
            }
        }
    }

    private companion object {
        val bskyOptions = listOf(
            OptionItem("Home Timeline", BskyOptions.HOME_TIMELINE),
            OptionItem("Notifications Timeline", BskyOptions.NOTIFICATIONS_TIMELINE),
            OptionItem("Create Post", BskyOptions.CREATE_POST)
        )
    }

    private fun List<OptionItem>.toMenuString(): String {
        val menuChoice = "Choose an option (or X to quit):".run { if (isCommodore) reverseCase() else this }
        return "" +
                "\r\n$menuChoice" + this.joinToString {
                    val itemString = it.optionString.run { if (isCommodore) reverseCase() else this }
                    "\r\n${it.option.choice}: $itemString"
                } + "\r\n>"
    }
}

package com.atty.scopes

import bsky4j.ATProtocolException
import com.atty.BskyOptions
import com.atty.DisconnectReason
import com.atty.OptionItem
import com.atty.libs.BlueskyClient
import com.atty.libs.BlueskyWriteClient
import com.atty.reverseCase
import java.net.Socket

class MenuScope(private val fullBlueskyClient: BlueskyClient, socket: Socket, isCommodore: Boolean, threadProvider: () -> Thread, disconnectHandler: (DisconnectReason) -> Unit) : BaseLoggedInScope(fullBlueskyClient, socket, isCommodore, threadProvider, disconnectHandler) {

    // Don't use this from in here; it's meant to be accessible to functions from this scope
    fun writeClient(): BlueskyWriteClient = fullBlueskyClient

    fun readMenu(
        onHomeSelected: HomeTimelineScope.() -> Unit,
        onNotificationsSelected: NotificationTimelineScope.() -> Unit,
        onPostSkeetSelected: CreatePostScope.() -> Unit,
    ) {
        while (!threadProvider().isInterrupted) {
            try {
                socket.getOutputStream().write(bskyOptions.toMenuString().toByteArray())
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
                                socket,
                                isCommodore,
                                threadProvider,
                                disconnectHandler
                            ).apply(onHomeSelected)
                        }
                        BskyOptions.NOTIFICATIONS_TIMELINE.choice -> {
                            val notifications = blueskyClient.getNotificationsTimeline()
                            NotificationTimelineScope(
                                notifications,
                                blueskyClient,
                                socket,
                                isCommodore,
                                threadProvider,
                                disconnectHandler
                            ).apply(onNotificationsSelected)
                        }
                        BskyOptions.CREATE_POST.choice -> {
                            CreatePostScope(
                                null,
                                blueskyClient,
                                socket,
                                disconnectHandler,
                                isCommodore,
                                threadProvider
                            ).apply(onPostSkeetSelected)
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

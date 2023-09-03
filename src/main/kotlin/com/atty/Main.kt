package com.atty

import io.ktor.network.sockets.*
import kotlinx.coroutines.runBlocking
import net.socialhub.http.HttpClientImpl
import net.socialhub.logger.Logger
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


fun main(args: Array<String>) {
    val port = System.getProperty("port").toInt()
    val logFile = System.getProperty("logFile")
    val timeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    // Don't log anyone's token
    // TODO: write a pr to bsky4j to configure this via the factory
    Logger.getLogger(HttpClientImpl::class.java).logLevel = Logger.LogLevel.ERROR

    val server = TelnetServerImpl(port, logFile, object : ConnectionListener {
        override fun onConnect(inetAddress: SocketAddress) {
            println("Received connection at: ${ZonedDateTime.now().format(timeFormatter)}")
        }

        override fun onDisconnect(inetAddress: SocketAddress, reason: DisconnectReason) {
            println("Lost connection at: ${ZonedDateTime.now().format(timeFormatter)} due to: $reason")
        }
    })

    runBlocking {
        try {
            server.start()
            server.join()
        } finally {
            server.stop()
        }
    }
}

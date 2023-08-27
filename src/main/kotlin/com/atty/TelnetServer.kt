package com.atty

import com.atty.models.AtConfig
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

interface TelnetServer {
    fun start()

    /**
     * Stops the server; returns true if the server was stopped
     */
    fun stop(): Boolean

    fun isRunning(): Boolean
}

enum class DisconnectReason {
    TIMEOUT,
    EXCEPTION,
    GRACEFUL
}

interface ConnectionListener {
    fun onConnect(inetAddress: InetAddress)
    fun onDisconnect(inetAddress: InetAddress, reason: DisconnectReason)
}

class TelnetServerImpl(port: Int,
                       private val logFilePath: String,
                       private val connectionListener: ConnectionListener) : TelnetServer {
    private val activeConnectionDeque = java.util.ArrayDeque<AtConnection>()
    private val logWriterThread = Executors.newSingleThreadExecutor()
    private val cleanupThread = Executors.newSingleThreadExecutor()
    private val server = ServerSocket(port).apply {
        soTimeout = 0 // block on .accept till a connection comes in; never time out!
    }
    private var coordinatorFuture: Future<*>? = null
    private var isRunning = false

    override fun start() {
        coordinatorFuture = Executors.newSingleThreadExecutor().submit {
            while (true) {
                // Supports 50 connections in the queue, is this too many?
                val socket = server.accept()
                socket.soTimeout = SOCKET_TIMEOUT_MILLIS.toInt()
                activeConnectionDeque.add(AtReaderThread(socket, AtConfig("Welcome to Bluesky"), { connection, reason ->
                    connectionListener.onDisconnect(socket.inetAddress, reason)
                    cleanupThread.submit {
                        activeConnectionDeque.remove(connection)
                        tidyUpConnections()
                    }
                }).apply { start() })

                connectionListener.onConnect(socket.inetAddress)

                cleanupThread.submit {
                    tidyUpConnections()
                }
            }
        }
        isRunning = true
    }

    override fun stop(): Boolean {
        isRunning = false
        return coordinatorFuture?.cancel(true) ?: false
    }

    override fun isRunning() = isRunning

    private fun tidyUpConnections() {
        val deadConnections = activeConnectionDeque.filter {
            System.currentTimeMillis() - it.startTime > OVERALL_TIMEOUT_MILLIS
        }.toSet()
        activeConnectionDeque.removeAll(deadConnections)
        deadConnections.forEach {
            it.timeoutConnection()
        }
        writeConnectionLog()
    }

    private fun writeConnectionLog() {
        logWriterThread.execute {
            val json = buildJsonObject {
                put("activeConnections", JsonPrimitive(activeConnectionDeque.size))
            }
            try {
                File(logFilePath).writeText(json.toString())
            } catch (throwable: Throwable) {
                println("Cannot write to log file")
                println("Connections: ${activeConnectionDeque.size}")
            }
        }
    }

    private companion object {
        private const val SOCKET_TIMEOUT_MINUTES = 5L
        private val SOCKET_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(SOCKET_TIMEOUT_MINUTES)
        // Any session over this length gets kicked regardless of whether it's active
        private const val OVERALL_TIMEOUT_MINUTES = 10L
        private val OVERALL_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(OVERALL_TIMEOUT_MINUTES)
    }
}

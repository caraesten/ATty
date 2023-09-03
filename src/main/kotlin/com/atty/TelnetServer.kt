package com.atty

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.util.concurrent.TimeUnit

interface TelnetServer {
    fun start()

    suspend fun stop()

    suspend fun join()
}

enum class DisconnectReason {
    TIMEOUT,
    EXCEPTION,
    GRACEFUL
}

typealias DisconnectHandler = suspend (DisconnectReason) -> Unit

interface ConnectionListener {
    fun onConnect(inetAddress: SocketAddress)
    fun onDisconnect(inetAddress: SocketAddress, reason: DisconnectReason)
}

class TelnetServerImpl(port: Int,
                       private val logFilePath: String,
                       private val connectionListener: ConnectionListener) : TelnetServer {
    private val activeConnectionDeque = java.util.ArrayDeque<AtConnection>()
    private val logWriterThread = CoroutineScope(Dispatchers.IO)
    private val cleanupThread = CoroutineScope(Dispatchers.IO)
    private val serverCoroutineScope = CoroutineScope(Dispatchers.IO)
    private val selectorManager = SelectorManager(serverCoroutineScope.coroutineContext)
    private val server: ServerSocket = aSocket(selectorManager).tcp().bind(port = port)

    private var coordinatorJob: Job? = null

    override fun start() {
        coordinatorJob = serverCoroutineScope.launch {
            while (isActive) {
                val socket = server.accept()
                launch {
                    try {
                        val conn = Connection(socket, socket.openReadChannel(), socket.openWriteChannel(autoFlush = true))
                        val socketAddress = socket.remoteAddress

                        connectionListener.onConnect(socketAddress)

                        val atReaderThread = AtReaderThread(conn, { thread, reason ->
                            cleanupThread.launch {
                                connectionListener.onDisconnect(socketAddress, reason)
                                activeConnectionDeque.remove(thread)
                                writeConnectionLog()
                            }
                        })

                        cleanupThread.launch {
                            delay(OVERALL_TIMEOUT_MILLIS)
                            atReaderThread.timeoutConnection()
                        }

                        activeConnectionDeque.add(atReaderThread)
                        writeConnectionLog()

                        atReaderThread.run()
                    } catch (e: Throwable) {
                        socket.close()
                    }
                }
            }
        }
    }

    override suspend fun stop() {
        coordinatorJob?.cancelAndJoin()
        coordinatorJob = null
    }

    override suspend fun join() {
        coordinatorJob?.join()
    }

    private fun writeConnectionLog() {
        logWriterThread.launch {
            val json = buildJsonObject {
                put("activeConnections", JsonPrimitive(activeConnectionDeque.size))
            }
            try {
                File(logFilePath).bufferedWriter().use {
                    it.appendLine(json.toString())
                    it.flush()
                }
            } catch (throwable: Throwable) {
                println("Cannot write to log file")
                println("Connections: ${activeConnectionDeque.size}")
            }
        }
    }

    private companion object {
        // Any session over this length gets kicked regardless of whether it's active
        private const val OVERALL_TIMEOUT_MINUTES = 10L
        private val OVERALL_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(OVERALL_TIMEOUT_MINUTES)
    }
}

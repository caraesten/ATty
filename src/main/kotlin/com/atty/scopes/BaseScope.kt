package com.atty.scopes

import com.atty.DisconnectHandler
import com.atty.reverseCase
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.isActive
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.coroutines.coroutineContext

data class IntSelection(
    val isQuit: Boolean = false,
    val isSkip: Boolean = false,
    val integer: Int
)

object Constants {
    const val ERROR_INVALID_SELECTION_QUIT = "Pick an option, or X to quit."
    const val ERROR_INVALID_SELECTION = "Pick an option."
    const val ERROR_STRING_TOO_LONG = "Input is too long."
    const val ERROR_BLUESKY_CONNECTION = "Bluesky not responding!"
}

abstract class BaseScope(
    val connection: Connection,
    val disconnectHandler: DisconnectHandler
) {
    suspend fun waitForReturnKey() {
        val carriage = '\r'.code.toByte()
        val newline = '\n'.code.toByte()

        while (coroutineContext.isActive) {
            assertCanRead()
            when (connection.input.readByte()) {
                carriage -> if (connection.input.availableForRead > 0 && connection.input.readByte() == newline) break
                newline -> break
                else -> Unit
            }
        }
    }

    suspend fun clearScreen(charset: Charset = Charsets.UTF_8) {
        if (charset != Charsets.UTF_8) {
            println("Unsafe clear screen!")
        }
        val bytesToClearScreen = "\u001b[2J\u001b[H"
        connection.output.writeStringUtf8(bytesToClearScreen)
    }

    suspend fun waitForStringInput(isCommodore: Boolean = false, maxLength: Int = 300): String {
        assertCanRead()
        val selectionString: String = connection.input.readUTF8Line().orEmpty()

        return if (selectionString.length > maxLength) {
            writeUi(Constants.ERROR_STRING_TOO_LONG)
            waitForStringInput(isCommodore, maxLength)
        } else {
            selectionString.run { if (isCommodore) this.reverseCase() else this }
        }
    }

    suspend fun waitForSelectionChoice(
        numberOfOptions: Int,
        canQuit: Boolean = true,
        canSkip: Boolean = false,
        charset: Charset = Charsets.UTF_8
    ): IntSelection {
        assertCanRead()
        val selectionString: String? = connection.input.readUTF8Line()

        return if (selectionString.equals("x", ignoreCase = true)) {
            IntSelection(isQuit = true, isSkip = false, integer = -1)
        } else if (canSkip && selectionString.equals("")) {
            IntSelection(isQuit = false, isSkip = true, integer = -2)
        } else {
            val selectionNumber = selectionString?.toIntOrNull() ?: -1
            if (selectionNumber in 1..numberOfOptions) {
                IntSelection(isQuit = false, isSkip = false, integer = selectionNumber)
            } else {
                writeUi(
                    (if (canQuit) Constants.ERROR_INVALID_SELECTION_QUIT else Constants.ERROR_INVALID_SELECTION)
                )
                waitForSelectionChoice(numberOfOptions)
            }
        }
    }

    suspend fun writeUi(text: String, isCommodore: Boolean = false) {
        val string = "\r\n [$text] \r\n".run {
            if (isCommodore) this.reverseCase() else this
        }
        connection.output.writeStringUtf8(
            string
        )
    }

    suspend fun writeAppText(text: String, isCommodore: Boolean = false) {
        val string = "\r\n $text \r\n".run {
            if (isCommodore) this.reverseCase() else this
        }
        connection.output.writeStringUtf8(
            string
        )
    }

    suspend fun writePrompt(text: String = "", isCommodore: Boolean = false) {
        val string = "\r\n$text".run {
            if (isCommodore) this.reverseCase() else this
        }
        connection.output.writeStringUtf8(
            "$string > "
        )
    }

    private fun assertCanRead() {
        require(!connection.input.isClosedForRead) { "Input connection is closed" }
    }
}
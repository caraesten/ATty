package com.atty.scopes

import com.atty.DisconnectReason
import com.atty.reverseCase
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket
import java.nio.charset.Charset

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
    val socket: Socket,
    val threadProvider: () -> Thread,
    val disconnectHandler: (DisconnectReason) -> Unit
) {
    fun waitForReturnKey() {
        val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
        inputStream.readLine()
    }

    fun clearScreen(charset: Charset = Charsets.UTF_8) {
        if (charset != Charsets.UTF_8) {
            println("Unsafe clear screen!")
        }
        val bytesToClearScreen = "\u001b[2J\u001b[H".toByteArray(Charsets.UTF_8)
        socket.getOutputStream().write(bytesToClearScreen)
    }

    fun waitForStringInput(isCommodore: Boolean = false, maxLength: Int = 300): String {
        val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
        val selectionString: String = try {
            inputStream.readLine()
        } catch (ex: IllegalArgumentException) {
            ""
        } catch (ex: IOException) {
            ""
        }

        return if (selectionString.length > maxLength) {
            writeUi(Constants.ERROR_STRING_TOO_LONG)
            waitForStringInput(isCommodore, maxLength)
        } else {
            selectionString.run { if (isCommodore) this.reverseCase() else this }
        }
    }

    fun waitForSelectionChoice(
        numberOfOptions: Int,
        canQuit: Boolean = true,
        canSkip: Boolean = false,
        charset: Charset = Charsets.UTF_8
    ): IntSelection {
        val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
        val selectionString: String? = try {
            inputStream.readLine()
        } catch (ex: IllegalArgumentException) {
            ""
        } catch (ex: IOException) {
            ""
        }

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

    fun writeUi(text: String, isCommodore: Boolean = false) {
        val string = "\r\n[$text] \r\n".run {
            if (isCommodore) this.reverseCase() else this
        }
        socket.getOutputStream().write(
            string.toByteArray()
        )
    }

    fun writeAppText(text: String, isCommodore: Boolean = false) {
        val string = "\r\n$text \r\n".run {
            if (isCommodore) this.reverseCase() else this
        }
        writeBytes(string.toByteArray())
    }

    fun writeBytes(bytes: ByteArray) {
        socket.getOutputStream().write(
            bytes
        )
    }

    fun writePrompt(text: String = "", isCommodore: Boolean = false) {
        val string = "\r\n$text".run {
            if (isCommodore) this.reverseCase() else this
        }
        socket.getOutputStream().write(
            "$string > ".toByteArray()
        )
    }
}
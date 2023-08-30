package com.atty.scopes

import com.atty.Constants
import com.atty.DisconnectReason
import com.atty.reverseCase
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket
import java.nio.charset.Charset

abstract class BaseScope(
    val socket: Socket,
    val threadProvider: () -> Thread,
    val disconnectHandler: (DisconnectReason) -> Unit
) {
    fun waitForReturnKey() {
        while (socket.getInputStream().read() != Constants.ASCII_LF) {
            // do nothing
        }
    }

    fun clearScreen(charset: Charset = Charsets.UTF_8) {
        if (charset != Charsets.UTF_8) {
            println("Unsafe clear screen!")
        }
        val bytesToClearScreen = "\u001b[2J\u001b[H".toByteArray(Charsets.UTF_8)
        socket.getOutputStream().write(bytesToClearScreen)
    }

    fun waitForStringInput(isCommodore: Boolean = false): String {
        val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
        val selectionString: String = try {
            inputStream.readLine()
        } catch (ex: IllegalArgumentException) {
            ""
        } catch (ex: IOException) {
            ""
        }

        return selectionString.run { if (isCommodore) this.reverseCase() else this }
    }

    fun waitForSelectionChoice(
        numberOfOptions: Int,
        canQuit: Boolean = true,
        canSkip: Boolean = false,
        charset: Charset = Charsets.UTF_8
    ): Int {
        val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
        val selectionString: String? = try {
            inputStream.readLine()
        } catch (ex: IllegalArgumentException) {
            ""
        } catch (ex: IOException) {
            ""
        }

        return if (selectionString.equals("x", ignoreCase = true)) {
            -1
        } else if (canSkip && selectionString.equals("")) {
            // TODO (cara): Enums plz this is silly
            -2
        } else {
            val selectionNumber = selectionString?.toIntOrNull() ?: -1
            if (selectionNumber in 1..numberOfOptions) {
                selectionNumber
            } else {
                socket.getOutputStream().write(
                    (if (canQuit) Constants.ERROR_INVALID_SELECTION_QUIT else Constants.ERROR_INVALID_SELECTION).toByteArray(charset)
                )
                waitForSelectionChoice(numberOfOptions)
            }
        }
    }

    fun writeUi(text: String, isCommodore: Boolean = false) {
        val string = "\r\n [$text] \r\n".run {
            if (isCommodore) this.reverseCase() else this
        }
        socket.getOutputStream().write(
            string.toByteArray()
        )
    }

    fun writeAppText(text: String, isCommodore: Boolean = false) {
        val string = "\r\n $text \r\n".run {
            if (isCommodore) this.reverseCase() else this
        }
        socket.getOutputStream().write(
            string.toByteArray()
        )
    }

    fun writePrompt(text: String = "", isCommodore: Boolean = false) {
        val string = "\r\n $text".run {
            if (isCommodore) this.reverseCase() else this
        }
        socket.getOutputStream().write(
            "$string>".toByteArray()
        )
    }
}
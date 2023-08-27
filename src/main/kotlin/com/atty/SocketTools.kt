package com.atty

import com.atty.Constants.ASCII_LF
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket
import java.nio.charset.Charset

fun Socket.waitForReturnKey() {
    while (getInputStream().read() != ASCII_LF) {
        // do nothing
    }
}

fun Socket.clearScreen(charset: Charset = Charsets.UTF_8) {
    if (charset != Charsets.UTF_8) {
        println("Unsafe clear screen!")
    }
    val bytesToClearScreen = "\u001b[2J\u001b[H".toByteArray(Charsets.UTF_8)
    getOutputStream().write(bytesToClearScreen)
}

fun Socket.waitForStringInput(): String {
    val inputStream = BufferedReader(InputStreamReader(getInputStream()))
    val selectionString: String = try {
        inputStream.readLine()
    } catch (ex: IllegalArgumentException) {
        ""
    } catch (ex: IOException) {
        ""
    }

    return selectionString
}

fun Socket.waitForSelectionChoice(numberOfOptions: Int, canQuit: Boolean = true, charset: Charset = Charsets.UTF_8): Int {
    val inputStream = BufferedReader(InputStreamReader(getInputStream()))
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
            getOutputStream().write(
                (if (canQuit) Constants.ERROR_INVALID_SELECTION_QUIT else Constants.ERROR_INVALID_SELECTION).toByteArray(charset)
            )
            waitForSelectionChoice(numberOfOptions)
        }
    }
}


object Constants {
    const val ASCII_LF = 10
    const val ERROR_INVALID_SELECTION_QUIT = "\nPick an option, or X to quit.\n"
    const val ERROR_INVALID_SELECTION = "\nPick an option.\n"
}
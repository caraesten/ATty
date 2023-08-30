package com.atty

fun String.reverseCase(): String = map { if (it.isUpperCase()) it.lowercase() else it.uppercase() }.joinToString("")

object Constants {
    const val ASCII_LF = 10
    const val ERROR_INVALID_SELECTION_QUIT = "\r\nPick an option, or X to quit.\r\n"
    const val ERROR_INVALID_SELECTION = "\r\nPick an option.\r\n"
}
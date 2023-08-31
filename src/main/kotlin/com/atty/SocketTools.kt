package com.atty

fun String.reverseCase(): String = map { if (it.isUpperCase()) it.lowercase() else it.uppercase() }.joinToString("")

package com.atty.libs

import com.atty.models.Link
import com.atty.models.Mention

val mentionRegex = """@([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?""".toRegex()
val urlRegex = """[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)""".toRegex()

fun String.getMentions(): List<Mention> {
    return mentionRegex.findAll(this).map {
        Mention(it.value, it.range.first, it.range.last + 1)
    }.toList()
}

fun String.getLinks(): List<Link> {
    return urlRegex.findAll(this).map {
        val url = if (it.value.startsWith("https://") || it.value.startsWith("http://")) {
            it.value
        } else {
            "https://${it.value}"
        }
        Link(url, it.range.first, it.range.last + 1)
    }.toList()
}

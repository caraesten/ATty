package com.atty.libs

import java.net.URL
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

/**
 * Used for fetching resources not provided by bsky4j, ie, image blobs
 */

interface GenericHttpReadClient {
    fun getImage(urlString: String): ImageInputStream
}
class GenericHttpReadClientImpl : GenericHttpReadClient {
    override fun getImage(urlString: String): ImageInputStream {
        val url = URL(urlString)
        return ImageIO.createImageInputStream(url.openStream())
    }
}

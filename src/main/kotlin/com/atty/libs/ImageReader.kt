package com.atty.libs

import com.atty.models.ImageMode
import com.sshtools.jsixel.awt.AWTImageBitmap
import com.sshtools.jsixel.lib.bitmap.Bitmap2Sixel
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO

interface ImageReader {
    fun readImage(thumbUrl: String): ByteArray
}
class ImageReaderImpl(private val imageMode: ImageMode) : ImageReader {
    private val cache = LruCache<String, BufferedImage>(CACHE_SIZE)
    override fun readImage(thumbUrl: String): ByteArray {
        val url = URL(thumbUrl)
        val img = cache.get(thumbUrl) {
            ImageIO.read(ImageIO.createImageInputStream(url.openStream()))
        }
        return when (imageMode) {
            ImageMode.AsciiImages -> {
                img.convertToAscii().toByteArray()
            }

            ImageMode.SixelImages -> {
                Bitmap2Sixel.Bitmap2SixelBuilder().fromBitmap(
                    AWTImageBitmap.BufferedImageBitmapBuilder().fromImage(img.scaleToSixelSize()).build()
                ).build().toByteArray()
            }
            else -> "".toByteArray()
        }
    }

    private companion object {
        const val CACHE_SIZE = 500
    }
}
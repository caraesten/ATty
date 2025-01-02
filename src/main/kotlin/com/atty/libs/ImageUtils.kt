package com.atty.libs

import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import java.awt.image.BufferedImage

fun BufferedImage.convertToAscii(maxWidth: Int = 78, maxHeight: Int = 24): String {
    val scaled = Thumbnails.of(this).crop(Positions.CENTER).size(maxWidth, maxHeight).asBufferedImage();
    return ASCII(true).convert(scaled)
}

fun BufferedImage.scaleToSixelSize(maxWidth: Int = 480, maxHeight: Int = 360): BufferedImage {
    return Thumbnails.of(this).crop(Positions.CENTER).size(maxWidth, maxHeight).asBufferedImage()
}


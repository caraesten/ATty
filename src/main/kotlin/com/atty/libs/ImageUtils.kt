package com.atty.libs

import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

fun ImageInputStream.readToAscii(maxWidth: Int = 78, maxHeight: Int = 24): String {
    val bufferedImage = ImageIO.read(this)
    val scaled = Thumbnails.of(bufferedImage).crop(Positions.CENTER).size(maxWidth, maxHeight).asBufferedImage();
    return ASCII().convert(scaled)
}

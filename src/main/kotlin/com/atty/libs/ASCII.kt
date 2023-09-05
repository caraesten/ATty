package com.atty.libs

import kotlin.jvm.JvmOverloads
import java.awt.image.BufferedImage
import java.lang.StringBuilder
import java.awt.Color

/*Copyright (c) 2011 Aravind Rao
Modifications by Sam Barnum, 360Works 2012
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons
 * to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
class ASCII(var negative: Boolean = false) {

    fun convert(image: BufferedImage): String {
        val sb = StringBuilder((image.width + 1) * image.height)
        for (y in 0 until image.height) {
            if (sb.length != 0) sb.append("\n")
            for (x in 0 until image.width) {
                val pixelColor = Color(image.getRGB(x, y))
                val gValue =
                    pixelColor.red.toDouble() * 0.2989 + pixelColor.blue.toDouble() * 0.5870 + pixelColor.green.toDouble() * 0.1140
                val s = if (negative) returnStrNeg(gValue) else returnStrPos(gValue)
                sb.append(s)
            }
        }
        return sb.toString()
    }

    /**
     * Create a new string and assign to it a string based on the grayscale value.
     * If the grayscale value is very high, the pixel is very bright and assign characters
     * such as . and , that do not appear very dark. If the grayscale value is very lowm the pixel is very dark,
     * assign characters such as # and @ which appear very dark.
     *
     * @param g grayscale
     * @return char
     */
    private fun returnStrPos(g: Double): Char //takes the grayscale value as parameter
    {
        val str: Char = if (g >= 230.0) {
            ' '
        } else if (g >= 200.0) {
            '.'
        } else if (g >= 180.0) {
            '*'
        } else if (g >= 160.0) {
            ':'
        } else if (g >= 130.0) {
            'o'
        } else if (g >= 100.0) {
            '&'
        } else if (g >= 70.0) {
            '8'
        } else if (g >= 50.0) {
            '#'
        } else {
            '@'
        }
        return str // return the character
    }

    /**
     * Same method as above, except it reverses the darkness of the pixel. A dark pixel is given a light character and vice versa.
     *
     * @param g grayscale
     * @return char
     */
    private fun returnStrNeg(g: Double): Char {
        val str: Char = if (g >= 230.0) {
            '@'
        } else if (g >= 200.0) {
            '#'
        } else if (g >= 180.0) {
            '8'
        } else if (g >= 160.0) {
            '&'
        } else if (g >= 130.0) {
            'o'
        } else if (g >= 100.0) {
            ':'
        } else if (g >= 70.0) {
            '*'
        } else if (g >= 50.0) {
            '.'
        } else {
            ' '
        }
        return str
    }
}